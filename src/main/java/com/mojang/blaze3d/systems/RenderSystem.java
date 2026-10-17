package com.mojang.blaze3d.systems;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.Queue;
import java.util.function.BiConsumer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.lax1dude.eaglercraft.v1_8.Display;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

@DontObfuscate
public class RenderSystem {
   static final Logger LOGGER = LogUtils.getLogger();
   // Declared as Queue rather than ConcurrentLinkedQueue: that class is absent from the
   // WASM-GC TeaVM classlib, so Queues.newConcurrentLinkedQueue() returns a LinkedList.
   private static final Queue<RenderCall> recordingQueue = Queues.newConcurrentLinkedQueue();
   private static final Tesselator RENDER_THREAD_TESSELATOR = new Tesselator();
   private static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
   private static boolean isReplayingQueue;
   @Nullable
   private static Thread gameThread;
   @Nullable
   private static Thread renderThread;
   private static int MAX_SUPPORTED_TEXTURE_SIZE = -1;
   private static boolean isInInit;
   private static double lastDrawTime = Double.MIN_VALUE;
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (param0, param1) -> {
      param0.accept(param1 + 0);
      param0.accept(param1 + 1);
      param0.accept(param1 + 2);
      param0.accept(param1 + 2);
      param0.accept(param1 + 3);
      param0.accept(param1 + 0);
   });
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (param0, param1) -> {
      param0.accept(param1 + 0);
      param0.accept(param1 + 1);
      param0.accept(param1 + 2);
      param0.accept(param1 + 3);
      param0.accept(param1 + 2);
      param0.accept(param1 + 1);
   });
   private static Matrix3f inverseViewRotationMatrix = new Matrix3f().zero();
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f savedProjectionMatrix = new Matrix4f();
   private static final PoseStack modelViewStack = new PoseStack();
   private static Matrix4f modelViewMatrix = new Matrix4f();
   private static Matrix4f textureMatrix = new Matrix4f();
   private static final int[] shaderTextures = new int[12];
   private static final float[] shaderColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
   private static float shaderGlintAlpha = 1.0F;
   private static float shaderFogStart;
   private static float shaderFogEnd = 1.0F;
   private static final float[] shaderFogColor = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
   private static FogShape shaderFogShape = FogShape.SPHERE;
   private static final Vector3f[] shaderLightDirections = new Vector3f[2];
   private static float shaderGameTime;
   private static float shaderLineWidth = 1.0F;
   private static String apiDescription = "Unknown";
   @Nullable
   private static ShaderInstance shader;
   private static final AtomicLong pollEventsWaitStart = new AtomicLong();
   private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);

   public static void initRenderThread() {
      if (renderThread == null && gameThread != Thread.currentThread()) {
         renderThread = Thread.currentThread();
      } else {
         throw new IllegalStateException("Could not initialize render thread");
      }
   }

   public static boolean isOnRenderThread() {
      return Thread.currentThread() == renderThread;
   }

   public static boolean isOnRenderThreadOrInit() {
      return isInInit || isOnRenderThread();
   }

   public static void initGameThread(boolean param0) {
      boolean var1 = renderThread == Thread.currentThread();
      if (gameThread == null && renderThread != null && var1 != param0) {
         gameThread = Thread.currentThread();
      } else {
         throw new IllegalStateException("Could not initialize tick thread");
      }
   }

   public static boolean isOnGameThread() {
      return true;
   }

   public static void assertInInitPhase() {
      if (!isInInitPhase()) {
         throw constructThreadException();
      }
   }

   public static void assertOnGameThreadOrInit() {
      if (!isInInit && !isOnGameThread()) {
         throw constructThreadException();
      }
   }

   public static void assertOnRenderThreadOrInit() {
      if (!isInInit && !isOnRenderThread()) {
         throw constructThreadException();
      }
   }

   public static void assertOnRenderThread() {
      if (!isOnRenderThread()) {
         throw constructThreadException();
      }
   }

   public static void assertOnGameThread() {
      if (!isOnGameThread()) {
         throw constructThreadException();
      }
   }

   private static IllegalStateException constructThreadException() {
      return new IllegalStateException("Rendersystem called from wrong thread");
   }

   public static boolean isInInitPhase() {
      return true;
   }

   public static void recordRenderCall(RenderCall param0) {
      recordingQueue.add(param0);
   }

   private static void pollEvents() {
      // Upstream: GLFW.glfwPollEvents(). Eaglercraft's event queue is drained by
      // KeyboardHandler/MouseHandler once per tick instead (see the WASM-GC notes in
      // com.mojang.blaze3d.platform.InputConstants), so there is nothing to pump here. The
      // "frozen at poll events" watchdog is still maintained around the (now empty) body so
      // isFrozenAtPollEvents() keeps its original semantics for any caller that reaches it.
      pollEventsWaitStart.set(Util.getMillis());
      pollingEvents.set(true);
      pollingEvents.set(false);
   }

   public static boolean isFrozenAtPollEvents() {
      return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
   }

   public static void flipFrame(long param0) {
      // Upstream swaps via GLFW.glfwSwapBuffers(window). The Eaglercraft equivalent is
      // Display.update(), which presents the canvas to the compositor and, on the desktop
      // target, does the GLFW swap internally. The window handle is ignored: there is exactly
      // one implicit surface.
      pollEvents();
      replayQueue();
      Tesselator.getInstance().getBuilder().clear();
      Display.update();
      pollEvents();
   }

   public static void replayQueue() {
      isReplayingQueue = true;

      while (!recordingQueue.isEmpty()) {
         RenderCall var0 = recordingQueue.poll();
         var0.execute();
      }

      isReplayingQueue = false;
   }

   public static void limitDisplayFPS(int param0) {
      // Upstream busy-waits on GLFW.glfwGetTime()/glfwWaitEventsTimeout() against lastDrawTime.
      // Eaglercraft owns the frame clock itself, so this defers to Display.sync(), which is the
      // same limiter Eaglercraft 1.8's own client used and already accounts for vsync. lastDrawTime
      // is retained but no longer used to drive the wait.
      Display.sync(param0);
      lastDrawTime = param0;
   }

   public static void disableDepthTest() {
      assertOnRenderThread();
      GlStateManager._disableDepthTest();
   }

   public static void enableDepthTest() {
      assertOnGameThreadOrInit();
      GlStateManager._enableDepthTest();
   }

   public static void enableScissor(int param0, int param1, int param2, int param3) {
      assertOnGameThreadOrInit();
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(param0, param1, param2, param3);
   }

   public static void disableScissor() {
      assertOnGameThreadOrInit();
      GlStateManager._disableScissorTest();
   }

   public static void depthFunc(int param0) {
      assertOnRenderThread();
      GlStateManager._depthFunc(param0);
   }

   public static void depthMask(boolean param0) {
      assertOnRenderThread();
      GlStateManager._depthMask(param0);
   }

   public static void enableBlend() {
      assertOnRenderThread();
      GlStateManager._enableBlend();
   }

   public static void disableBlend() {
      assertOnRenderThread();
      GlStateManager._disableBlend();
   }

   public static void blendFunc(GlStateManager.SourceFactor param0, GlStateManager.DestFactor param1) {
      assertOnRenderThread();
      GlStateManager._blendFunc(param0.value, param1.value);
   }

   public static void blendFunc(int param0, int param1) {
      assertOnRenderThread();
      GlStateManager._blendFunc(param0, param1);
   }

   public static void blendFuncSeparate(
      GlStateManager.SourceFactor param0, GlStateManager.DestFactor param1, GlStateManager.SourceFactor param2, GlStateManager.DestFactor param3
   ) {
      assertOnRenderThread();
      GlStateManager._blendFuncSeparate(param0.value, param1.value, param2.value, param3.value);
   }

   public static void blendFuncSeparate(int param0, int param1, int param2, int param3) {
      assertOnRenderThread();
      GlStateManager._blendFuncSeparate(param0, param1, param2, param3);
   }

   public static void blendEquation(int param0) {
      assertOnRenderThread();
      GlStateManager._blendEquation(param0);
   }

   public static void enableCull() {
      assertOnRenderThread();
      GlStateManager._enableCull();
   }

   public static void disableCull() {
      assertOnRenderThread();
      GlStateManager._disableCull();
   }

   public static void polygonMode(int param0, int param1) {
      assertOnRenderThread();
      GlStateManager._polygonMode(param0, param1);
   }

   public static void enablePolygonOffset() {
      assertOnRenderThread();
      GlStateManager._enablePolygonOffset();
   }

   public static void disablePolygonOffset() {
      assertOnRenderThread();
      GlStateManager._disablePolygonOffset();
   }

   public static void polygonOffset(float param0, float param1) {
      assertOnRenderThread();
      GlStateManager._polygonOffset(param0, param1);
   }

   public static void enableColorLogicOp() {
      assertOnRenderThread();
      GlStateManager._enableColorLogicOp();
   }

   public static void disableColorLogicOp() {
      assertOnRenderThread();
      GlStateManager._disableColorLogicOp();
   }

   public static void logicOp(GlStateManager.LogicOp param0) {
      assertOnRenderThread();
      GlStateManager._logicOp(param0.value);
   }

   public static void activeTexture(int param0) {
      assertOnRenderThread();
      GlStateManager._activeTexture(param0);
   }

   public static void texParameter(int param0, int param1, int param2) {
      GlStateManager._texParameter(param0, param1, param2);
   }

   public static void deleteTexture(int param0) {
      assertOnGameThreadOrInit();
      GlStateManager._deleteTexture(param0);
   }

   public static void bindTextureForSetup(int param0) {
      bindTexture(param0);
   }

   public static void bindTexture(int param0) {
      GlStateManager._bindTexture(param0);
   }

   public static void viewport(int param0, int param1, int param2, int param3) {
      assertOnGameThreadOrInit();
      GlStateManager._viewport(param0, param1, param2, param3);
   }

   public static void colorMask(boolean param0, boolean param1, boolean param2, boolean param3) {
      assertOnRenderThread();
      GlStateManager._colorMask(param0, param1, param2, param3);
   }

   public static void stencilFunc(int param0, int param1, int param2) {
      assertOnRenderThread();
      GlStateManager._stencilFunc(param0, param1, param2);
   }

   public static void stencilMask(int param0) {
      assertOnRenderThread();
      GlStateManager._stencilMask(param0);
   }

   public static void stencilOp(int param0, int param1, int param2) {
      assertOnRenderThread();
      GlStateManager._stencilOp(param0, param1, param2);
   }

   public static void clearDepth(double param0) {
      assertOnGameThreadOrInit();
      GlStateManager._clearDepth(param0);
   }

   public static void clearColor(float param0, float param1, float param2, float param3) {
      assertOnGameThreadOrInit();
      GlStateManager._clearColor(param0, param1, param2, param3);
   }

   public static void clearStencil(int param0) {
      assertOnRenderThread();
      GlStateManager._clearStencil(param0);
   }

   public static void clear(int param0, boolean param1) {
      assertOnGameThreadOrInit();
      GlStateManager._clear(param0, param1);
   }

   public static void setShaderFogStart(float param0) {
      assertOnRenderThread();
      _setShaderFogStart(param0);
   }

   private static void _setShaderFogStart(float param0) {
      shaderFogStart = param0;
   }

   public static float getShaderFogStart() {
      assertOnRenderThread();
      return shaderFogStart;
   }

   public static void setShaderGlintAlpha(double param0) {
      setShaderGlintAlpha((float)param0);
   }

   public static void setShaderGlintAlpha(float param0) {
      assertOnRenderThread();
      _setShaderGlintAlpha(param0);
   }

   private static void _setShaderGlintAlpha(float param0) {
      shaderGlintAlpha = param0;
   }

   public static float getShaderGlintAlpha() {
      assertOnRenderThread();
      return shaderGlintAlpha;
   }

   public static void setShaderFogEnd(float param0) {
      assertOnRenderThread();
      _setShaderFogEnd(param0);
   }

   private static void _setShaderFogEnd(float param0) {
      shaderFogEnd = param0;
   }

   public static float getShaderFogEnd() {
      assertOnRenderThread();
      return shaderFogEnd;
   }

   public static void setShaderFogColor(float param0, float param1, float param2, float param3) {
      assertOnRenderThread();
      _setShaderFogColor(param0, param1, param2, param3);
   }

   public static void setShaderFogColor(float param0, float param1, float param2) {
      setShaderFogColor(param0, param1, param2, 1.0F);
   }

   private static void _setShaderFogColor(float param0, float param1, float param2, float param3) {
      shaderFogColor[0] = param0;
      shaderFogColor[1] = param1;
      shaderFogColor[2] = param2;
      shaderFogColor[3] = param3;
   }

   public static float[] getShaderFogColor() {
      assertOnRenderThread();
      return shaderFogColor;
   }

   public static void setShaderFogShape(FogShape param0) {
      assertOnRenderThread();
      _setShaderFogShape(param0);
   }

   private static void _setShaderFogShape(FogShape param0) {
      shaderFogShape = param0;
   }

   public static FogShape getShaderFogShape() {
      assertOnRenderThread();
      return shaderFogShape;
   }

   public static void setShaderLights(Vector3f param0, Vector3f param1) {
      assertOnRenderThread();
      _setShaderLights(param0, param1);
   }

   public static void _setShaderLights(Vector3f param0, Vector3f param1) {
      shaderLightDirections[0] = param0;
      shaderLightDirections[1] = param1;
   }

   public static void setupShaderLights(ShaderInstance param0) {
      assertOnRenderThread();
      if (param0.LIGHT0_DIRECTION != null) {
         param0.LIGHT0_DIRECTION.set(shaderLightDirections[0]);
      }

      if (param0.LIGHT1_DIRECTION != null) {
         param0.LIGHT1_DIRECTION.set(shaderLightDirections[1]);
      }
   }

   public static void setShaderColor(float param0, float param1, float param2, float param3) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> _setShaderColor(param0, param1, param2, param3));
      } else {
         _setShaderColor(param0, param1, param2, param3);
      }
   }

   private static void _setShaderColor(float param0, float param1, float param2, float param3) {
      shaderColor[0] = param0;
      shaderColor[1] = param1;
      shaderColor[2] = param2;
      shaderColor[3] = param3;
   }

   public static float[] getShaderColor() {
      assertOnRenderThread();
      return shaderColor;
   }

   public static void drawElements(int param0, int param1, int param2) {
      assertOnRenderThread();
      GlStateManager._drawElements(param0, param1, param2, 0L);
   }

   public static void lineWidth(float param0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> shaderLineWidth = param0);
      } else {
         shaderLineWidth = param0;
      }
   }

   public static float getShaderLineWidth() {
      assertOnRenderThread();
      return shaderLineWidth;
   }

   public static void pixelStore(int param0, int param1) {
      assertOnGameThreadOrInit();
      GlStateManager._pixelStore(param0, param1);
   }

   public static void readPixels(int param0, int param1, int param2, int param3, int param4, int param5, ByteBuffer param6) {
      assertOnRenderThread();
      GlStateManager._readPixels(param0, param1, param2, param3, param4, param5, param6);
   }

   public static void getString(int param0, Consumer<String> param1) {
      assertOnRenderThread();
      param1.accept(GlStateManager._getString(param0));
   }

   public static String getBackendDescription() {
      assertInInitPhase();
      return String.format(Locale.ROOT, "Renderer backend %s", GLX._getLWJGLVersion());
   }

   public static String getApiDescription() {
      return apiDescription;
   }

   public static TimeSource.NanoTimeSource initBackendSystem() {
      assertInInitPhase();
      return GLX._initGlfw()::getAsLong;
   }

   public static void initRenderer(int param0, boolean param1) {
      assertInInitPhase();
      GLX._init(param0, param1);
      apiDescription = GLX.getOpenGLVersionString();
   }

   public static void setErrorCallback(BiConsumer<Integer, String> param0) {
      assertInInitPhase();
      GLX._setGlfwErrorCallback(param0);
   }

   public static void renderCrosshair(int param0) {
      assertOnRenderThread();
      GLX._renderCrosshair(param0, true, true, true);
   }

   public static String getCapsString() {
      assertOnRenderThread();
      return "Using framebuffer using OpenGL 3.2";
   }

   public static void setupDefaultState(int param0, int param1, int param2, int param3) {
      assertInInitPhase();
      GlStateManager._clearDepth(1.0);
      GlStateManager._enableDepthTest();
      GlStateManager._depthFunc(515);
      projectionMatrix.identity();
      savedProjectionMatrix.identity();
      modelViewMatrix.identity();
      textureMatrix.identity();
      GlStateManager._viewport(param0, param1, param2, param3);
   }

   public static int maxSupportedTextureSize() {
      if (MAX_SUPPORTED_TEXTURE_SIZE == -1) {
         assertOnRenderThreadOrInit();
         // PORT NOTE (Eag19 / TeaVM WASM-GC)
         //
         // Upstream probes for the largest texture the driver really accepts by
         // uploading a GL_PROXY_TEXTURE_2D at each candidate size and reading the
         // result back with glGetTexLevelParameter. Neither half exists on WebGL:
         // there are no proxy textures to allocate, and texture storage cannot be
         // queried at all (PlatformOpenGL._glGetTexLevelParameteri). The probe is
         // only a workaround for desktop drivers whose GL_MAX_TEXTURE_SIZE lies;
         // WebGL guarantees that value is accurate, and it is clamped to >= 1024
         // below, so reading it directly is equivalent and cannot fail.
         MAX_SUPPORTED_TEXTURE_SIZE = Math.max(GlStateManager._getInteger(3379), 1024);
      }

      return MAX_SUPPORTED_TEXTURE_SIZE;
   }

   public static void glBindBuffer(int param0, IntSupplier param1) {
      GlStateManager._glBindBuffer(param0, param1.getAsInt());
   }

   public static void glBindVertexArray(Supplier<Integer> param0) {
      GlStateManager._glBindVertexArray(param0.get());
   }

   public static void glBufferData(int param0, ByteBuffer param1, int param2) {
      assertOnRenderThreadOrInit();
      GlStateManager._glBufferData(param0, param1, param2);
   }

   public static void glDeleteBuffers(int param0) {
      assertOnRenderThread();
      GlStateManager._glDeleteBuffers(param0);
   }

   public static void glDeleteVertexArrays(int param0) {
      assertOnRenderThread();
      GlStateManager._glDeleteVertexArrays(param0);
   }

   public static void glUniform1i(int param0, int param1) {
      assertOnRenderThread();
      GlStateManager._glUniform1i(param0, param1);
   }

   public static void glUniform1(int param0, IntBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform1(param0, param1);
   }

   public static void glUniform2(int param0, IntBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform2(param0, param1);
   }

   public static void glUniform3(int param0, IntBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform3(param0, param1);
   }

   public static void glUniform4(int param0, IntBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform4(param0, param1);
   }

   public static void glUniform1(int param0, FloatBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform1(param0, param1);
   }

   public static void glUniform2(int param0, FloatBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform2(param0, param1);
   }

   public static void glUniform3(int param0, FloatBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform3(param0, param1);
   }

   public static void glUniform4(int param0, FloatBuffer param1) {
      assertOnRenderThread();
      GlStateManager._glUniform4(param0, param1);
   }

   public static void glUniformMatrix2(int param0, boolean param1, FloatBuffer param2) {
      assertOnRenderThread();
      GlStateManager._glUniformMatrix2(param0, param1, param2);
   }

   public static void glUniformMatrix3(int param0, boolean param1, FloatBuffer param2) {
      assertOnRenderThread();
      GlStateManager._glUniformMatrix3(param0, param1, param2);
   }

   public static void glUniformMatrix4(int param0, boolean param1, FloatBuffer param2) {
      assertOnRenderThread();
      GlStateManager._glUniformMatrix4(param0, param1, param2);
   }

   public static void setupOverlayColor(IntSupplier param0, int param1) {
      assertOnRenderThread();
      int var2 = param0.getAsInt();
      setShaderTexture(1, var2);
   }

   public static void teardownOverlayColor() {
      assertOnRenderThread();
      setShaderTexture(1, 0);
   }

   public static void setupLevelDiffuseLighting(Vector3f param0, Vector3f param1, Matrix4f param2) {
      assertOnRenderThread();
      GlStateManager.setupLevelDiffuseLighting(param0, param1, param2);
   }

   public static void setupGuiFlatDiffuseLighting(Vector3f param0, Vector3f param1) {
      assertOnRenderThread();
      GlStateManager.setupGuiFlatDiffuseLighting(param0, param1);
   }

   public static void setupGui3DDiffuseLighting(Vector3f param0, Vector3f param1) {
      assertOnRenderThread();
      GlStateManager.setupGui3DDiffuseLighting(param0, param1);
   }

   public static void beginInitialization() {
      isInInit = true;
   }

   public static void finishInitialization() {
      isInInit = false;
      if (!recordingQueue.isEmpty()) {
         replayQueue();
      }

      if (!recordingQueue.isEmpty()) {
         throw new IllegalStateException("Recorded to render queue during initialization");
      }
   }

   public static void glGenBuffers(Consumer<Integer> param0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> param0.accept(GlStateManager._glGenBuffers()));
      } else {
         param0.accept(GlStateManager._glGenBuffers());
      }
   }

   public static void glGenVertexArrays(Consumer<Integer> param0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> param0.accept(GlStateManager._glGenVertexArrays()));
      } else {
         param0.accept(GlStateManager._glGenVertexArrays());
      }
   }

   public static Tesselator renderThreadTesselator() {
      assertOnRenderThread();
      return RENDER_THREAD_TESSELATOR;
   }

   public static void defaultBlendFunc() {
      blendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
   }

   @Deprecated
   public static void runAsFancy(Runnable param0) {
      boolean var1 = Minecraft.useShaderTransparency();
      if (!var1) {
         param0.run();
      } else {
         OptionInstance<GraphicsStatus> var2 = Minecraft.getInstance().options.graphicsMode();
         GraphicsStatus var3 = var2.get();
         var2.set(GraphicsStatus.FANCY);
         param0.run();
         var2.set(var3);
      }
   }

   public static void setShader(Supplier<ShaderInstance> param0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> shader = param0.get());
      } else {
         shader = param0.get();
      }
   }

   @Nullable
   public static ShaderInstance getShader() {
      assertOnRenderThread();
      return shader;
   }

   public static void setShaderTexture(int param0, ResourceLocation param1) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> _setShaderTexture(param0, param1));
      } else {
         _setShaderTexture(param0, param1);
      }
   }

   public static void _setShaderTexture(int param0, ResourceLocation param1) {
      if (param0 >= 0 && param0 < shaderTextures.length) {
         TextureManager var2 = Minecraft.getInstance().getTextureManager();
         AbstractTexture var3 = var2.getTexture(param1);
         shaderTextures[param0] = var3.getId();
      }
   }

   public static void setShaderTexture(int param0, int param1) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> _setShaderTexture(param0, param1));
      } else {
         _setShaderTexture(param0, param1);
      }
   }

   public static void _setShaderTexture(int param0, int param1) {
      if (param0 >= 0 && param0 < shaderTextures.length) {
         shaderTextures[param0] = param1;
      }
   }

   public static int getShaderTexture(int param0) {
      assertOnRenderThread();
      return param0 >= 0 && param0 < shaderTextures.length ? shaderTextures[param0] : 0;
   }

   public static void setProjectionMatrix(Matrix4f param0) {
      Matrix4f var1 = new Matrix4f(param0);
      if (!isOnRenderThread()) {
         recordRenderCall(() -> projectionMatrix = var1);
      } else {
         projectionMatrix = var1;
      }
   }

   public static void setInverseViewRotationMatrix(Matrix3f param0) {
      Matrix3f var1 = new Matrix3f(param0);
      if (!isOnRenderThread()) {
         recordRenderCall(() -> inverseViewRotationMatrix = var1);
      } else {
         inverseViewRotationMatrix = var1;
      }
   }

   public static void setTextureMatrix(Matrix4f param0) {
      Matrix4f var1 = new Matrix4f(param0);
      if (!isOnRenderThread()) {
         recordRenderCall(() -> textureMatrix = var1);
      } else {
         textureMatrix = var1;
      }
   }

   public static void resetTextureMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> textureMatrix.identity());
      } else {
         textureMatrix.identity();
      }
   }

   public static void applyModelViewMatrix() {
      Matrix4f var0 = new Matrix4f(modelViewStack.last().pose());
      if (!isOnRenderThread()) {
         recordRenderCall(() -> modelViewMatrix = var0);
      } else {
         modelViewMatrix = var0;
      }
   }

   public static void backupProjectionMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> _backupProjectionMatrix());
      } else {
         _backupProjectionMatrix();
      }
   }

   private static void _backupProjectionMatrix() {
      savedProjectionMatrix = projectionMatrix;
   }

   public static void restoreProjectionMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> _restoreProjectionMatrix());
      } else {
         _restoreProjectionMatrix();
      }
   }

   private static void _restoreProjectionMatrix() {
      projectionMatrix = savedProjectionMatrix;
   }

   public static Matrix4f getProjectionMatrix() {
      assertOnRenderThread();
      return projectionMatrix;
   }

   public static Matrix3f getInverseViewRotationMatrix() {
      assertOnRenderThread();
      return inverseViewRotationMatrix;
   }

   public static Matrix4f getModelViewMatrix() {
      assertOnRenderThread();
      return modelViewMatrix;
   }

   public static PoseStack getModelViewStack() {
      return modelViewStack;
   }

   public static Matrix4f getTextureMatrix() {
      assertOnRenderThread();
      return textureMatrix;
   }

   public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode param0) {
      assertOnRenderThread();

      return switch (param0) {
         case QUADS -> sharedSequentialQuad;
         case LINES -> sharedSequentialLines;
         default -> sharedSequential;
      };
   }

   public static void setShaderGameTime(long param0, float param2) {
      float var3 = ((float)(param0 % 24000L) + param2) / 24000.0F;
      if (!isOnRenderThread()) {
         recordRenderCall(() -> shaderGameTime = var3);
      } else {
         shaderGameTime = var3;
      }
   }

   public static float getShaderGameTime() {
      assertOnRenderThread();
      return shaderGameTime;
   }

   public static final class AutoStorageIndexBuffer {
      private final int vertexStride;
      private final int indexStride;
      private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
      private int name;
      private VertexFormat.IndexType type = VertexFormat.IndexType.BYTE;
      private int indexCount;

      AutoStorageIndexBuffer(int param0, int param1, RenderSystem.AutoStorageIndexBuffer.IndexGenerator param2) {
         this.vertexStride = param0;
         this.indexStride = param1;
         this.generator = param2;
      }

      public boolean hasStorage(int param0) {
         return param0 <= this.indexCount;
      }

      public void bind(int param0) {
         if (this.name == 0) {
            this.name = GlStateManager._glGenBuffers();
         }

         GlStateManager._glBindBuffer(34963, this.name);
         this.ensureStorage(param0);
      }

      private void ensureStorage(int param0) {
         if (!this.hasStorage(param0)) {
            param0 = Mth.roundToward(param0 * 2, this.indexStride);
            RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, param0);
            VertexFormat.IndexType var2 = VertexFormat.IndexType.least(param0);
            int var3 = Mth.roundToward(param0 * var2.bytes, 4);
            GlStateManager._glBufferData(34963, var3, 35048);
            ByteBuffer var4 = GlStateManager._glMapBuffer(34963, 35001);
            if (var4 == null) {
               throw new RuntimeException("Failed to map GL buffer");
            }

            this.type = var2;
            it.unimi.dsi.fastutil.ints.IntConsumer var5 = this.intConsumer(var4);

            for (int var6 = 0; var6 < param0; var6 += this.indexStride) {
               this.generator.accept(var5, var6 * this.vertexStride / this.indexStride);
            }

            GlStateManager._glUnmapBuffer(34963);
            this.indexCount = param0;
         }
      }

      private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer param0) {
         switch (this.type) {
            case BYTE:
               return l_param1 -> param0.put((byte)l_param1);
            case SHORT:
               return l_param1 -> param0.putShort((short)l_param1);
            case INT:
            default:
               return param0::putInt;
         }
      }

      public VertexFormat.IndexType type() {
         return this.type;
      }

      interface IndexGenerator {
         void accept(it.unimi.dsi.fastutil.ints.IntConsumer var1, int var2);
      }
   }
}
