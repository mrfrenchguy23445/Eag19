package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.server.packs.resources.IoSupplier;
import net.lax1dude.eaglercraft.v1_8.Display;
import org.slf4j.Logger;

/**
 * The client surface, backed by the Eaglercraft platform layer rather than GLFW.
 *
 * <p>Eaglercraft has exactly one implicit surface: the {@code <canvas>} the page (or the LWJGL window
 * on the desktop target) provides, whose size is owned by the browser and can change at any moment
 * when the user resizes. Everything GLFW offered as a per-window capability maps onto that:
 *
 * <ul>
 *   <li>{@code glfwCreateWindow}/{@code glfwMakeContextCurrent}/{@code GL.createCapabilities} -- already
 *       done. The GL context exists before {@code main()} runs.</li>
 *   <li>{@code glfwGetFramebufferSize} -- {@link Display#getWidth()}/{@link Display#getHeight()}.</li>
 *   <li>{@code glfwSetFramebufferSizeCallback} -- {@link Display#wasResized()}, polled once per frame
 *       from {@link #updateDisplay()}. This is exactly how Eaglercraft 1.8's own client handles
 *       canvas resizes.</li>
 *   <li>{@code glfwSetWindowPosCallback}/{@code glfwSetWindowSizeCallback} -- no-op. A browser canvas
 *       has no position, and its size is read from {@link Display} instead of from a callback.</li>
 *   <li>{@code glfwSetWindowFocusCallback} -- {@link Display#isActive()}, polled the same way; see
 *       {@link #updateDisplay()}.</li>
 *   <li>{@code glfwSetCursorEnterCallback} -- the Eaglercraft mouse layer reports
 *       {@code Mouse.isInsideWindow()}, which {@code MouseHandler} consults directly.</li>
 *   <li>{@code glfwSetWindowMonitor}/{@code glfwSetWindowSize} -- {@link Display#toggleFullscreen()}
 *       for the mode change, and a no-op for explicit windowed sizing, which the page owns.</li>
 *   <li>{@code glfwSwapInterval} -- {@link Display#setVSync(boolean)}.</li>
 *   <li>{@code glfwSetWindowTitle} -- {@link Display#setTitle(String)}.</li>
 *   <li>{@code glfwWindowShouldClose}/{@code glfwTerminate} -- {@link Display#isCloseRequested()} and a
 *       no-op respectively.</li>
 *   <li>{@code glfwSetWindowIcon} -- no-op; the favicon is served by the Eaglercraft page shell from
 *       the asset package, via {@code PlatformApplication.faviconURLTeaVM()}, and cannot be set
 *       from game code.</li>
 *   <li>{@code glfwSetErrorCallback}/{@code glfwGetError} -- no-op; there is no GLFW error channel.
 *       Eaglercraft reports failures through {@code PlatformRuntime.writeCrashReport}.</li>
 * </ul>
 *
 * <p>{@link #getWindow()} still returns a {@code long} and keeps returning the same value everywhere,
 * because it is threaded through roughly thirty call sites
 * ({@code InputConstants.isKeyDown(window, key)}, {@code ClipboardManager}, the input handlers'
 * callback signatures). It is now a synthetic non-zero handle: the upstream code compares it against
 * {@code 0L} to mean "no window" and against itself to mean "this window", and a stable non-zero
 * constant satisfies both readings without every call site having to change.
 */
public final class Window implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();

   /**
    * Synthetic stand-in for a GLFW window pointer. See the class comment.
    */
   private static final long SYNTHETIC_WINDOW = 1L;

   private final WindowEventHandler eventHandler;
   private final ScreenManager screenManager;
   private final long window;
   private int windowedX;
   private int windowedY;
   private int windowedWidth;
   private int windowedHeight;
   private Optional<VideoMode> preferredFullscreenVideoMode;
   private boolean fullscreen;
   private boolean actuallyFullscreen;
   private int x;
   private int y;
   private int width;
   private int height;
   private int framebufferWidth;
   private int framebufferHeight;
   private int guiScaledWidth;
   private int guiScaledHeight;
   private double guiScale;
   private String errorSection = "";
   private boolean dirty;
   private int framerateLimit;
   private boolean vsync;
   private boolean wasActive = true;
   private boolean sizeChangePending;

   public Window(WindowEventHandler param0, ScreenManager param1, DisplayData param2, @Nullable String param3, String param4) {
      RenderSystem.assertInInitPhase();
      this.screenManager = param1;
      this.setErrorSection("Pre startup");
      this.eventHandler = param0;
      Optional<VideoMode> var6 = VideoMode.read(param3);
      if (var6.isPresent()) {
         this.preferredFullscreenVideoMode = var6;
      } else if (param2.fullscreenWidth.isPresent() && param2.fullscreenHeight.isPresent()) {
         this.preferredFullscreenVideoMode = Optional.of(new VideoMode(param2.fullscreenWidth.getAsInt(), param2.fullscreenHeight.getAsInt(), 8, 8, 8, 60));
      } else {
         this.preferredFullscreenVideoMode = Optional.empty();
      }

      this.window = SYNTHETIC_WINDOW;
      this.actuallyFullscreen = this.fullscreen = param2.isFullscreen;
      this.windowedWidth = this.width = param2.width > 0 ? param2.width : 1;
      this.windowedHeight = this.height = param2.height > 0 ? param2.height : 1;

      // Upstream creates the window here and then asks GLFW for its position. A browser canvas
      // always sits at the origin of its own box and is never independently movable, so x/y stay 0
      // and the initial size comes from the display rather than from the requested DisplayData --
      // matching what Eaglercraft 1.8 does (it ignores the requested size and takes Display's).
      this.x = this.windowedX = 0;
      this.y = this.windowedY = 0;
      this.setMode();
      this.refreshFramebufferSize();

      // Upstream also sets the title here. DisplayData has no title, and Minecraft sets it
      // separately through setTitle() once the window exists, so there is nothing to do.
   }

   public int getRefreshRate() {
      RenderSystem.assertOnRenderThread();
      return GLX._getRefreshRate(this);
   }

   public boolean shouldClose() {
      return GLX._shouldClose(this);
   }

   public static void checkGlfwError(BiConsumer<Integer, String> param0) {
      RenderSystem.assertInInitPhase();
      // No-op: there is no GLFW error queue. Eaglercraft's own startup failure reporting lives in
      // PlatformRuntime.writeCrashReport, which GLX._initGlfw no longer wraps either.
   }

   public void setIcon(IoSupplier<InputStream> param0, IoSupplier<InputStream> param1) {
      RenderSystem.assertInInitPhase();

      // No-op. Upstream decodes the PNGs with STBImage and hands the pixels to
      // glfwSetWindowIcon. There is no window icon to set here: the browser tab icon (and the
      // LWJGL window icon on the desktop target) comes from the Eaglercraft page shell, which reads
      // it out of the asset package through PlatformApplication.faviconURLTeaVM(). Decoding the
      // PNGs here would only burn CPU to throw the result away.
      //
      // The suppliers are still opened and closed so that a missing or corrupt icon resource is
      // still reported at startup, which is the one observable behaviour of the upstream call that
      // is worth keeping.
      try {
         try (InputStream var2 = param0.get()) {
            var2.close();
         }

         try (InputStream var3 = param1.get()) {
            var3.close();
         }
      } catch (IOException var2) {
         LOGGER.error("Couldn't set icon", var2);
      }
   }

   public void setErrorSection(String param0) {
      this.errorSection = param0;
   }

   public void defaultErrorCallback(Integer param0, String param1) {
      RenderSystem.assertOnRenderThread();
      LOGGER.error("########## GL ERROR ##########");
      LOGGER.error("@ {}", this.errorSection);
      LOGGER.error("{}: {}", param0, param1);
   }

   public void setDefaultErrorCallback() {
      // No-op: no GLFW error callback is installed, so there is none to restore. The
      // GLFWErrorCallback that upstream frees here never exists in this build.
   }

   public void updateVsync(boolean param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      this.vsync = param0;
      Display.setVSync(param0);
   }

   @Override
   public void close() {
      RenderSystem.assertOnRenderThread();
      // Upstream frees the GLFW callbacks, destroys the window and calls glfwTerminate. The Eaglercraft
      // canvas and its GL context are owned by the runtime and outlive the client object, so there
      // is nothing to tear down here; Closeable is kept only to preserve the upstream contract.
   }

   private void onFramebufferResize(int param0, int param1) {
      if (param0 != 0 && param1 != 0) {
         this.framebufferWidth = param0;
         this.framebufferHeight = param1;
         if (this.getWidth() != this.width || this.getHeight() != this.height) {
            this.sizeChangePending = true;
         }
      }
   }

   private void refreshFramebufferSize() {
      RenderSystem.assertInInitPhase();
      this.width = Display.getWidth() > 0 ? Display.getWidth() : 1;
      this.height = Display.getHeight() > 0 ? Display.getHeight() : 1;
      this.framebufferWidth = this.width;
      this.framebufferHeight = this.height;
   }

   public void setFramerateLimit(int param0) {
      this.framerateLimit = param0;
   }

   public int getFramerateLimit() {
      return this.framerateLimit;
   }

   public void updateDisplay() {
      // Replaces the GLFW resize/focus callbacks. Eaglercraft 1.8's client does exactly this in its
      // main loop: compare the current display state against the last one and act on the difference.
      boolean var1 = Display.wasResized();
      boolean var2 = Display.isActive();
      if (var1) {
         int var3 = this.getWidth();
         int var4 = this.getHeight();
         this.refreshFramebufferSize();
         this.onFramebufferResize(this.framebufferWidth, this.framebufferHeight);
         if (this.getWidth() != var3 || this.getHeight() != var4) {
            this.sizeChangePending = true;
         }
      }

      if (var2 != this.wasActive) {
         this.wasActive = var2;
         this.eventHandler.setWindowActive(var2);
      }

      if (this.sizeChangePending) {
         this.sizeChangePending = false;
         this.eventHandler.resizeDisplay();
      }

      if (this.fullscreen != this.actuallyFullscreen) {
         this.actuallyFullscreen = this.fullscreen;
         this.updateFullscreen(this.vsync);
      }

      RenderSystem.flipFrame(this.window);
   }

   public Optional<VideoMode> getPreferredFullscreenVideoMode() {
      return this.preferredFullscreenVideoMode;
   }

   public void setPreferredFullscreenVideoMode(Optional<VideoMode> param0) {
      boolean var2 = !param0.equals(this.preferredFullscreenVideoMode);
      this.preferredFullscreenVideoMode = param0;
      if (var2) {
         this.dirty = true;
      }
   }

   public void changeFullscreenVideoMode() {
      if (this.fullscreen && this.dirty) {
         this.dirty = false;
         this.setMode();
         this.eventHandler.resizeDisplay();
      }
   }

   private void setMode() {
      RenderSystem.assertInInitPhase();
      // Upstream switches the GLFW window between a monitor and the desktop, which is also how it
      // applies an explicit windowed size. Eaglercraft's fullscreen is a request to the platform to
      // take the canvas over the whole screen, so only the mode change has an effect here; the
      // windowed x/y/size are recorded for getScreenWidth()/getScreenHeight() but the page owns the
      // real geometry. See Display.toggleFullscreen().
      if (this.fullscreen) {
         if (this.screenManager.findBestMonitor(this) == null) {
            LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
            this.fullscreen = false;
            return;
         }

         if (this.actuallyFullscreen != this.fullscreen) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;
         }

         if (!Display.supportsFullscreen()) {
            LOGGER.warn("Fullscreen is not supported on this display");
            this.fullscreen = false;
            return;
         }

         if (!Display.isFullscreen()) {
            Display.toggleFullscreen();
         }
      } else {
         this.x = this.windowedX;
         this.y = this.windowedY;
         this.width = this.windowedWidth;
         this.height = this.windowedHeight;
         if (Display.isFullscreen()) {
            Display.toggleFullscreen();
         }
      }
   }

   public void toggleFullScreen() {
      this.fullscreen = !this.fullscreen;
   }

   public void setWindowed(int param0, int param1) {
      this.windowedWidth = param0;
      this.windowedHeight = param1;
      this.fullscreen = false;
      this.setMode();
   }

   private void updateFullscreen(boolean param0) {
      RenderSystem.assertOnRenderThread();

      try {
         this.setMode();
         this.eventHandler.resizeDisplay();
         this.updateVsync(param0);
         this.updateDisplay();
      } catch (Exception var2) {
         LOGGER.error("Couldn't toggle fullscreen", var2);
      }
   }

   public int calculateScale(int param0, boolean param1) {
      int var3 = 1;

      while (
         var3 != param0
            && var3 < this.framebufferWidth
            && var3 < this.framebufferHeight
            && this.framebufferWidth / (var3 + 1) >= 320
            && this.framebufferHeight / (var3 + 1) >= 240
      ) {
         var3++;
      }

      if (param1 && var3 % 2 != 0) {
         var3++;
      }

      return var3;
   }

   public void setGuiScale(double param0) {
      this.guiScale = param0;
      int var3 = (int)(this.framebufferWidth / param0);
      this.guiScaledWidth = this.framebufferWidth / param0 > var3 ? var3 + 1 : var3;
      int var4 = (int)(this.framebufferHeight / param0);
      this.guiScaledHeight = this.framebufferHeight / param0 > var4 ? var4 + 1 : var4;
   }

   public void setTitle(String param0) {
      Display.setTitle(String.format(Locale.ROOT, "%s", param0));
   }

   public long getWindow() {
      return this.window;
   }

   public boolean isFullscreen() {
      return this.fullscreen;
   }

   public int getWidth() {
      return this.framebufferWidth;
   }

   public int getHeight() {
      return this.framebufferHeight;
   }

   public void setWidth(int param0) {
      this.framebufferWidth = param0;
   }

   public void setHeight(int param0) {
      this.framebufferHeight = param0;
   }

   public int getScreenWidth() {
      return this.width;
   }

   public int getScreenHeight() {
      return this.height;
   }

   public int getGuiScaledWidth() {
      return this.guiScaledWidth;
   }

   public int getGuiScaledHeight() {
      return this.guiScaledHeight;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public double getGuiScale() {
      return this.guiScale;
   }

   @Nullable
   public Monitor findBestMonitor() {
      return this.screenManager.findBestMonitor(this);
   }

   public void updateRawMouseInput(boolean param0) {
      InputConstants.updateRawMouseInput(this.window, param0);
   }

   public static class WindowInitFailed extends SilentInitException {
      WindowInitFailed(String param0) {
         super(param0);
      }
   }
}
