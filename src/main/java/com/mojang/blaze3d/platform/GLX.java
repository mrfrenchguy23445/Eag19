package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.lax1dude.eaglercraft.v1_8.Display;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;
import net.minecraft.client.renderer.GameRenderer;
import org.slf4j.Logger;

/**
 * Window-system integration, routed through the Eaglercraft platform layer.
 *
 * <p>Every GLFW entry point upstream is called from has a direct counterpart in Eaglercraft, which
 * drives the browser canvas (or the LWJGL window on the desktop target) rather than GLFW:
 *
 * <ul>
 *   <li>{@code glfwInit} -- nothing to initialise. The canvas and its GL context already exist
 *       before the client runs, so {@link #_initGlfw()} returns a time source directly.</li>
 *   <li>{@code glfwGetTime} -- {@link PlatformRuntime#nanoTime()}, the same monotonic clock
 *       Eaglercraft's own frame limiter uses.</li>
 *   <li>{@code glfwWindowShouldClose} -- {@link Display#isCloseRequested()}, which the Eaglercraft
 *       runtime sets from the canvas/window close event.</li>
 *   <li>{@code glfwGetWindowMonitor}/{@code glfwGetVideoMode} -- Eaglercraft exposes no monitor or
 *       video-mode enumeration, so {@link #_getRefreshRate(Window)} reports 0.</li>
 *   <li>{@code glfwSetErrorCallback} -- Eaglercraft has no GLFW error channel;
 *       {@link #_setGlfwErrorCallback} is retained as a no-op so callers keep compiling.</li>
 * </ul>
 *
 * <p>{@link #_getLWJGLVersion()} no longer reports an LWJGL version, since there is no LWJGL here;
 * it reports the TeaVM/Eaglercraft runtime identity instead, which is what the debug overlay's
 * "LWJGL version" line is actually for.
 */
@DontObfuscate
public class GLX {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static String cpuInfo;

   public static String getOpenGLVersionString() {
      RenderSystem.assertOnRenderThread();
      return GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
   }

   public static int _getRefreshRate(Window param0) {
      RenderSystem.assertOnRenderThread();
      // Eaglercraft has no monitor enumeration and no GLFW video mode, so there is no refresh rate
      // to report. 0 is the same value vanilla produces when GLFW cannot resolve a video mode, and
      // Window.getRefreshRate() is only used to seed the video-mode picker.
      return 0;
   }

   public static String _getLWJGLVersion() {
      RenderSystem.assertInInitPhase();
      return "n/a (Eaglercraft, TeaVM " + PlatformRuntime.getPlatformType() + ")";
   }

   public static LongSupplier _initGlfw() {
      RenderSystem.assertInInitPhase();
      // Upstream returns () -> (long)(GLFW.glfwGetTime() * 1.0E9) after a glfwInit() that also
      // collects init-time errors into a list. Neither step exists here: the GL context is created
      // by the Eaglercraft runtime before main() runs, and it reports its own errors through
      // PlatformRuntime.writeCrashReport. PlatformRuntime.nanoTime() is the same clock
      // Eaglercraft's own Display.sync() uses for frame timing, so this keeps RenderSystem's
      // "getTime" contract intact.
      return PlatformRuntime::nanoTime;
   }

   public static void _setGlfwErrorCallback(BiConsumer<Integer, String> param0) {
      RenderSystem.assertInInitPhase();
      // No-op: there is no GLFW error channel to install a callback on. Eaglercraft surfaces GL and
      // runtime failures through PlatformRuntime's own error path.
   }

   public static boolean _shouldClose(Window param0) {
      return Display.isCloseRequested();
   }

   public static void _init(int param0, boolean param1) {
      RenderSystem.assertInInitPhase();

      // Upstream reads the CPU brand string through oshi, which is JNI-backed and therefore absent
      // on WASM. net.minecraft.SystemReport already does the equivalent through
      // PlatformRuntime; keep the two consistent rather than reintroducing a native probe.
      cpuInfo = "1x " + PlatformRuntime.getPlatformOS();

      GlDebug.enableDebugCallback(param0, param1);
   }

   public static String _getCpuInfo() {
      return cpuInfo == null ? "<unknown>" : cpuInfo;
   }

   public static void _renderCrosshair(int param0, boolean param1, boolean param2, boolean param3) {
      RenderSystem.assertOnRenderThread();
      GlStateManager._depthMask(false);
      GlStateManager._disableCull();
      RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
      Tesselator var4 = RenderSystem.renderThreadTesselator();
      BufferBuilder var5 = var4.getBuilder();
      RenderSystem.lineWidth(4.0F);
      var5.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      if (param1) {
         var5.vertex(0.0, 0.0, 0.0).color(0, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
         var5.vertex(param0, 0.0, 0.0).color(0, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
      }

      if (param2) {
         var5.vertex(0.0, 0.0, 0.0).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
         var5.vertex(0.0, param0, 0.0).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
      }

      if (param3) {
         var5.vertex(0.0, 0.0, 0.0).color(0, 0, 0, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
         var5.vertex(0.0, 0.0, param0).color(0, 0, 0, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
      }

      var4.end();
      RenderSystem.lineWidth(2.0F);
      var5.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      if (param1) {
         var5.vertex(0.0, 0.0, 0.0).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
         var5.vertex(param0, 0.0, 0.0).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
      }

      if (param2) {
         var5.vertex(0.0, 0.0, 0.0).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
         var5.vertex(0.0, param0, 0.0).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
      }

      if (param3) {
         var5.vertex(0.0, 0.0, 0.0).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
         var5.vertex(0.0, 0.0, param0).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
      }

      var4.end();
      RenderSystem.lineWidth(1.0F);
      GlStateManager._enableCull();
      GlStateManager._depthMask(true);
   }

   public static <T> T make(Supplier<T> param0) {
      return param0.get();
   }

   public static <T> T make(T param0, Consumer<T> param1) {
      param1.accept(param0);
      return param0;
   }
}
