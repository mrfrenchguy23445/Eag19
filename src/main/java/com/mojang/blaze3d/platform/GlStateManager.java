package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformOpenGL;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// PORT NOTE (Eag19 / TeaVM WASM-GC)
//
// This file was unmodified 1.19.4 decompiled code calling LWJGL directly
// (GL11.glEnable, GL15.glBindBuffer, GL20.glUniform4fv, GL30.glBindVertexArray,
// ... 96 call sites). LWJGL is declared compileOnly in the root build so javac
// can type-check blaze3d while TeaVM never sees it, which left every
// org.lwjgl.opengl.GL* class absent at TeaVM link time -- 16
// "Class org.lwjgl.opengl.GL15 was not found" errors, and TeaVM reports roughly
// one per method, so the real surface was all 96 sites.
//
// All of them now go to the _gl* entry points on PlatformOpenGL, which take
// plain Java ints exactly where desktop GL takes a GLuint name. See the long
// comment on that block in src/platform-api/java/.../PlatformOpenGL.java for why
// org.lwjgl.* is not re-created as source: glShaderSource used to pass a raw
// native pointer to GL (GL20C.nglShaderSource(id, 1, ptr, 0L)) and there is no
// such thing as a pointer on WASM.
//
// Callers kept their Mojang signatures; only the callee moved. Two parameters
// are still typed long where vanilla had them long (the offsets in
// _vertexAttribPointer and _drawElements) but are no longer client-memory
// pointers -- they are byte offsets into the bound GL buffer, and
// PlatformOpenGL.checkOffset() range-checks them.
@DontObfuscate
public class GlStateManager {
   private static final boolean ON_LINUX = Util.getPlatform() == Util.OS.LINUX;
   public static final int TEXTURE_COUNT = 12;
   private static final GlStateManager.BlendState BLEND = new GlStateManager.BlendState();
   private static final GlStateManager.DepthState DEPTH = new GlStateManager.DepthState();
   private static final GlStateManager.CullState CULL = new GlStateManager.CullState();
   private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
   private static final GlStateManager.ColorLogicState COLOR_LOGIC = new GlStateManager.ColorLogicState();
   private static final GlStateManager.StencilState STENCIL = new GlStateManager.StencilState();
   private static final GlStateManager.ScissorState SCISSOR = new GlStateManager.ScissorState();
   private static int activeTexture;
   private static final GlStateManager.TextureState[] TEXTURES = IntStream.range(0, 12)
      .mapToObj(param0 -> new GlStateManager.TextureState())
      .toArray(GlStateManager.TextureState[]::new);
   private static final GlStateManager.ColorMask COLOR_MASK = new GlStateManager.ColorMask();

   public static void _disableScissorTest() {
      RenderSystem.assertOnRenderThreadOrInit();
      SCISSOR.mode.disable();
   }

   public static void _enableScissorTest() {
      RenderSystem.assertOnRenderThreadOrInit();
      SCISSOR.mode.enable();
   }

   public static void _scissorBox(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glScissor(param0, param1, param2, param3);
   }

   public static void _disableDepthTest() {
      RenderSystem.assertOnRenderThreadOrInit();
      DEPTH.mode.disable();
   }

   public static void _enableDepthTest() {
      RenderSystem.assertOnRenderThreadOrInit();
      DEPTH.mode.enable();
   }

   public static void _depthFunc(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (param0 != DEPTH.func) {
         DEPTH.func = param0;
         PlatformOpenGL._glDepthFunc(param0);
      }
   }

   public static void _depthMask(boolean param0) {
      RenderSystem.assertOnRenderThread();
      if (param0 != DEPTH.mask) {
         DEPTH.mask = param0;
         PlatformOpenGL._glDepthMask(param0);
      }
   }

   public static void _disableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.disable();
   }

   public static void _enableBlend() {
      RenderSystem.assertOnRenderThread();
      BLEND.mode.enable();
   }

   public static void _blendFunc(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      if (param0 != BLEND.srcRgb || param1 != BLEND.dstRgb) {
         BLEND.srcRgb = param0;
         BLEND.dstRgb = param1;
         PlatformOpenGL._glBlendFunc(param0, param1);
      }
   }

   public static void _blendFuncSeparate(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThread();
      if (param0 != BLEND.srcRgb || param1 != BLEND.dstRgb || param2 != BLEND.srcAlpha || param3 != BLEND.dstAlpha) {
         BLEND.srcRgb = param0;
         BLEND.dstRgb = param1;
         BLEND.srcAlpha = param2;
         BLEND.dstAlpha = param3;
         glBlendFuncSeparate(param0, param1, param2, param3);
      }
   }

   public static void _blendEquation(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glBlendEquation(param0);
   }

   public static int glGetProgrami(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetProgrami(param0, param1);
   }

   public static void glAttachShader(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glAttachShader(param0, param1);
   }

   public static void glDeleteShader(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDeleteShader(param0);
   }

   public static int glCreateShader(int param0) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glCreateShader(param0);
   }

   public static void glShaderSource(int param0, List<String> param1) {
      RenderSystem.assertOnRenderThread();
      StringBuilder var2 = new StringBuilder();

      for (String var4 : param1) {
         var2.append(var4);
      }

      PlatformOpenGL._glShaderSource(param0, var2.toString());
   }

   public static void glCompileShader(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glCompileShader(param0);
   }

   public static int glGetShaderi(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetShaderi(param0, param1);
   }

   public static void _glUseProgram(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUseProgram(param0);
   }

   public static int glCreateProgram() {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glCreateProgram();
   }

   public static void glDeleteProgram(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDeleteProgram(param0);
   }

   public static void glLinkProgram(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glLinkProgram(param0);
   }

   public static int _glGetUniformLocation(int param0, CharSequence param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetUniformLocation(param0, param1);
   }

   public static void _glUniform1(int param0, IntBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform1iv(param0, param1);
   }

   public static void _glUniform1i(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform1i(param0, param1);
   }

   public static void _glUniform1(int param0, FloatBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform1fv(param0, param1);
   }

   public static void _glUniform2(int param0, IntBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform2iv(param0, param1);
   }

   public static void _glUniform2(int param0, FloatBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform2fv(param0, param1);
   }

   public static void _glUniform3(int param0, IntBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform3iv(param0, param1);
   }

   public static void _glUniform3(int param0, FloatBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform3fv(param0, param1);
   }

   public static void _glUniform4(int param0, IntBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform4iv(param0, param1);
   }

   public static void _glUniform4(int param0, FloatBuffer param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniform4fv(param0, param1);
   }

   public static void _glUniformMatrix2(int param0, boolean param1, FloatBuffer param2) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniformMatrix2fv(param0, param1, param2);
   }

   public static void _glUniformMatrix3(int param0, boolean param1, FloatBuffer param2) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniformMatrix3fv(param0, param1, param2);
   }

   public static void _glUniformMatrix4(int param0, boolean param1, FloatBuffer param2) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glUniformMatrix4fv(param0, param1, param2);
   }

   public static int _glGetAttribLocation(int param0, CharSequence param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetAttribLocation(param0, param1);
   }

   public static void _glBindAttribLocation(int param0, int param1, CharSequence param2) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glBindAttribLocation(param0, param1, param2);
   }

   public static int _glGenBuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGenBuffers();
   }

   public static int _glGenVertexArrays() {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGenVertexArrays();
   }

   public static void _glBindBuffer(int param0, int param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBindBuffer(param0, param1);
   }

   public static void _glBindVertexArray(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBindVertexArray(param0);
   }

   public static void _glBufferData(int param0, ByteBuffer param1, int param2) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBufferData(param0, param1, param2);
   }

   public static void _glBufferData(int param0, long param1, int param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBufferData(param0, param1, param3);
   }

   @Nullable
   public static ByteBuffer _glMapBuffer(int param0, int param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glMapBuffer(param0, param1);
   }

   public static void _glUnmapBuffer(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glUnmapBuffer(param0);
   }

   public static void _glDeleteBuffers(int param0) {
      RenderSystem.assertOnRenderThread();
      if (ON_LINUX) {
         PlatformOpenGL._glBindBuffer(34962, param0);
         PlatformOpenGL._glBufferData(34962, 0L, 35048);
         PlatformOpenGL._glBindBuffer(34962, 0);
      }

      PlatformOpenGL._glDeleteBuffers(param0);
   }

   public static void _glCopyTexSubImage2D(int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glCopyTexSubImage2D(param0, param1, param2, param3, param4, param5, param6, param7);
   }

   public static void _glDeleteVertexArrays(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDeleteVertexArrays(param0);
   }

   public static void _glBindFramebuffer(int param0, int param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBindFramebuffer(param0, param1);
   }

   public static void _glBlitFrameBuffer(int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBlitFramebuffer(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9);
   }

   public static void _glBindRenderbuffer(int param0, int param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glBindRenderbuffer(param0, param1);
   }

   public static void _glDeleteRenderbuffers(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glDeleteRenderbuffers(param0);
   }

   public static void _glDeleteFramebuffers(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glDeleteFramebuffers(param0);
   }

   public static int glGenFramebuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGenFramebuffers();
   }

   public static int glGenRenderbuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGenRenderbuffers();
   }

   public static void _glRenderbufferStorage(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glRenderbufferStorage(param0, param1, param2, param3);
   }

   public static void _glFramebufferRenderbuffer(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glFramebufferRenderbuffer(param0, param1, param2, param3);
   }

   public static int glCheckFramebufferStatus(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glCheckFramebufferStatus(param0);
   }

   public static void _glFramebufferTexture2D(int param0, int param1, int param2, int param3, int param4) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glFramebufferTexture2D(param0, param1, param2, param3, param4);
   }

   public static int getBoundFramebuffer() {
      RenderSystem.assertOnRenderThread();
      return _getInteger(36006);
   }

   public static void glActiveTexture(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glActiveTexture(param0);
   }

   public static void glBlendFuncSeparate(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glBlendFuncSeparate(param0, param1, param2, param3);
   }

   public static String glGetShaderInfoLog(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetShaderInfoLog(param0, param1);
   }

   public static String glGetProgramInfoLog(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetProgramInfoLog(param0, param1);
   }

   public static void setupLevelDiffuseLighting(Vector3f param0, Vector3f param1, Matrix4f param2) {
      RenderSystem.assertOnRenderThread();
      Vector4f var3 = param2.transform(new Vector4f(param0, 1.0F));
      Vector4f var4 = param2.transform(new Vector4f(param1, 1.0F));
      RenderSystem.setShaderLights(new Vector3f(var3.x(), var3.y(), var3.z()), new Vector3f(var4.x(), var4.y(), var4.z()));
   }

   public static void setupGuiFlatDiffuseLighting(Vector3f param0, Vector3f param1) {
      RenderSystem.assertOnRenderThread();
      Matrix4f var2 = new Matrix4f().scaling(1.0F, -1.0F, 1.0F).rotateY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
      setupLevelDiffuseLighting(param0, param1, var2);
   }

   public static void setupGui3DDiffuseLighting(Vector3f param0, Vector3f param1) {
      RenderSystem.assertOnRenderThread();
      Matrix4f var2 = new Matrix4f().rotationYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((float) (-Math.PI / 8), (float) (Math.PI * 3.0 / 4.0), 0.0F);
      setupLevelDiffuseLighting(param0, param1, var2);
   }

   public static void _enableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.enable();
   }

   public static void _disableCull() {
      RenderSystem.assertOnRenderThread();
      CULL.enable.disable();
   }

   public static void _polygonMode(int param0, int param1) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glPolygonMode(param0, param1);
   }

   public static void _enablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.enable();
   }

   public static void _disablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      POLY_OFFSET.fill.disable();
   }

   public static void _polygonOffset(float param0, float param1) {
      RenderSystem.assertOnRenderThread();
      if (param0 != POLY_OFFSET.factor || param1 != POLY_OFFSET.units) {
         POLY_OFFSET.factor = param0;
         POLY_OFFSET.units = param1;
         PlatformOpenGL._glPolygonOffset(param0, param1);
      }
   }

   public static void _enableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      COLOR_LOGIC.enable.enable();
   }

   public static void _disableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      COLOR_LOGIC.enable.disable();
   }

   public static void _logicOp(int param0) {
      RenderSystem.assertOnRenderThread();
      if (param0 != COLOR_LOGIC.op) {
         COLOR_LOGIC.op = param0;
         PlatformOpenGL._glLogicOp(param0);
      }
   }

   public static void _activeTexture(int param0) {
      RenderSystem.assertOnRenderThread();
      if (activeTexture != param0 - 33984) {
         activeTexture = param0 - 33984;
         glActiveTexture(param0);
      }
   }

   public static void _texParameter(int param0, int param1, float param2) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glTexParameterf(param0, param1, param2);
   }

   public static void _texParameter(int param0, int param1, int param2) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glTexParameteri(param0, param1, param2);
   }

   public static int _getTexLevelParameter(int param0, int param1, int param2) {
      RenderSystem.assertInInitPhase();
      return PlatformOpenGL._glGetTexLevelParameteri(param0, param1, param2);
   }

   public static int _genTexture() {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGenTextures();
   }

   public static void _genTextures(int[] param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glGenTextures(param0);
   }

   public static void _deleteTexture(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glDeleteTextures(param0);

      for (GlStateManager.TextureState var4 : TEXTURES) {
         if (var4.binding == param0) {
            var4.binding = -1;
         }
      }
   }

   public static void _deleteTextures(int[] param0) {
      RenderSystem.assertOnRenderThreadOrInit();

      for (GlStateManager.TextureState var4 : TEXTURES) {
         for (int var8 : param0) {
            if (var4.binding == var8) {
               var4.binding = -1;
            }
         }
      }

      PlatformOpenGL._glDeleteTextures(param0);
   }

   public static void _bindTexture(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (param0 != TEXTURES[activeTexture].binding) {
         TEXTURES[activeTexture].binding = param0;
         PlatformOpenGL._glBindTexture(3553, param0);
      }
   }

   public static int _getActiveTexture() {
      return activeTexture + 33984;
   }

   public static void _texImage2D(int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, @Nullable IntBuffer param8) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glTexImage2D(param0, param1, param2, param3, param4, param5, param6, param7, param8);
   }

   public static void _texSubImage2D(int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, long param8) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glTexSubImage2D(param0, param1, param2, param3, param4, param5, param6, param7, param8);
   }

   /**
    * ByteBuffer overload of {@code _texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)}.
    *
    * <p>Added by the LWJGL removal. Vanilla passes a native pointer because NativeImage
    * allocated with {@code MemoryUtil.nmemAlloc}; this port keeps image pixels in a
    * Java {@code byte[]}, so the upload has to hand WebGL a buffer it can view. The
    * pointer overload still exists for the call sites that have no image buffer at all
    * (a null upload), and throws on the target as documented on PlatformOpenGL.
    *
    * <p>The xoffset/yoffset/width/height the caller passes already describe the sub-rect
    * being uploaded, and the source pixel offset within {@code pixels} is carried by the
    * UNPACK_ROW_LENGTH / SKIP_PIXELS / SKIP_ROWS state the caller sets via
    * {@code _pixelStore} just before calling -- exactly as in vanilla, where the same
    * state covered the equivalent pointer arithmetic. Nothing is re-derived here.
    */
   public static void _texSubImage2D(int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glTexSubImage2D(param0, param1, param2, param3, param4, param5, param6, param7, param8);
   }

   public static void _getTexImage(int param0, int param1, int param2, int param3, long param4) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glGetTexImage(param0, param1, param2, param3, param4);
   }

   public static void _viewport(int param0, int param1, int param2, int param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager.Viewport.INSTANCE.x = param0;
      GlStateManager.Viewport.INSTANCE.y = param1;
      GlStateManager.Viewport.INSTANCE.width = param2;
      GlStateManager.Viewport.INSTANCE.height = param3;
      PlatformOpenGL._glViewport(param0, param1, param2, param3);
   }

   public static void _colorMask(boolean param0, boolean param1, boolean param2, boolean param3) {
      RenderSystem.assertOnRenderThread();
      if (param0 != COLOR_MASK.red || param1 != COLOR_MASK.green || param2 != COLOR_MASK.blue || param3 != COLOR_MASK.alpha) {
         COLOR_MASK.red = param0;
         COLOR_MASK.green = param1;
         COLOR_MASK.blue = param2;
         COLOR_MASK.alpha = param3;
         PlatformOpenGL._glColorMask(param0, param1, param2, param3);
      }
   }

   public static void _stencilFunc(int param0, int param1, int param2) {
      RenderSystem.assertOnRenderThread();
      if (param0 != STENCIL.func.func || param0 != STENCIL.func.ref || param0 != STENCIL.func.mask) {
         STENCIL.func.func = param0;
         STENCIL.func.ref = param1;
         STENCIL.func.mask = param2;
         PlatformOpenGL._glStencilFunc(param0, param1, param2);
      }
   }

   public static void _stencilMask(int param0) {
      RenderSystem.assertOnRenderThread();
      if (param0 != STENCIL.mask) {
         STENCIL.mask = param0;
         PlatformOpenGL._glStencilMask(param0);
      }
   }

   public static void _stencilOp(int param0, int param1, int param2) {
      RenderSystem.assertOnRenderThread();
      if (param0 != STENCIL.fail || param1 != STENCIL.zfail || param2 != STENCIL.zpass) {
         STENCIL.fail = param0;
         STENCIL.zfail = param1;
         STENCIL.zpass = param2;
         PlatformOpenGL._glStencilOp(param0, param1, param2);
      }
   }

   public static void _clearDepth(double param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glClearDepth(param0);
   }

   public static void _clearColor(float param0, float param1, float param2, float param3) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glClearColor(param0, param1, param2, param3);
   }

   public static void _clearStencil(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glClearStencil(param0);
   }

   public static void _clear(int param0, boolean param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glClear(param0);
      if (param1) {
         _getError();
      }
   }

   public static void _glDrawPixels(int param0, int param1, int param2, int param3, long param4) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDrawPixels(param0, param1, param2, param3, param4);
   }

   public static void _vertexAttribPointer(int param0, int param1, int param2, boolean param3, int param4, long param5) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glVertexAttribPointer(param0, param1, param2, param3, param4, param5);
   }

   public static void _vertexAttribIPointer(int param0, int param1, int param2, int param3, long param4) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glVertexAttribIPointer(param0, param1, param2, param3, param4);
   }

   public static void _enableVertexAttribArray(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glEnableVertexAttribArray(param0);
   }

   public static void _disableVertexAttribArray(int param0) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDisableVertexAttribArray(param0);
   }

   public static void _drawElements(int param0, int param1, int param2, long param3) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glDrawElements(param0, param1, param2, param3);
   }

   public static void _pixelStore(int param0, int param1) {
      RenderSystem.assertOnRenderThreadOrInit();
      PlatformOpenGL._glPixelStorei(param0, param1);
   }

   public static void _readPixels(int param0, int param1, int param2, int param3, int param4, int param5, ByteBuffer param6) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glReadPixels(param0, param1, param2, param3, param4, param5, param6);
   }

   public static void _readPixels(int param0, int param1, int param2, int param3, int param4, int param5, long param6) {
      RenderSystem.assertOnRenderThread();
      PlatformOpenGL._glReadPixels(param0, param1, param2, param3, param4, param5, param6);
   }

   public static int _getError() {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetError();
   }

   public static String _getString(int param0) {
      RenderSystem.assertOnRenderThread();
      return PlatformOpenGL._glGetString(param0);
   }

   public static int _getInteger(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      return PlatformOpenGL._glGetInteger(param0);
   }

   static class BlendState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3042);
      public int srcRgb = 1;
      public int dstRgb = 0;
      public int srcAlpha = 1;
      public int dstAlpha = 0;
   }

   static class BooleanState {
      private final int state;
      private boolean enabled;

      public BooleanState(int param0) {
         this.state = param0;
      }

      public void disable() {
         this.setEnabled(false);
      }

      public void enable() {
         this.setEnabled(true);
      }

      public void setEnabled(boolean param0) {
         RenderSystem.assertOnRenderThreadOrInit();
         if (param0 != this.enabled) {
            this.enabled = param0;
            if (param0) {
               PlatformOpenGL._glEnable(this.state);
            } else {
               PlatformOpenGL._glDisable(this.state);
            }
         }
      }
   }

   static class ColorLogicState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(3058);
      public int op = 5379;
   }

   static class ColorMask {
      public boolean red = true;
      public boolean green = true;
      public boolean blue = true;
      public boolean alpha = true;
   }

   static class CullState {
      public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(2884);
      public int mode = 1029;
   }

   static class DepthState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2929);
      public boolean mask = true;
      public int func = 513;
   }

   @DontObfuscate
   public enum DestFactor {
      CONSTANT_ALPHA(32771),
      CONSTANT_COLOR(32769),
      DST_ALPHA(772),
      DST_COLOR(774),
      ONE(1),
      ONE_MINUS_CONSTANT_ALPHA(32772),
      ONE_MINUS_CONSTANT_COLOR(32770),
      ONE_MINUS_DST_ALPHA(773),
      ONE_MINUS_DST_COLOR(775),
      ONE_MINUS_SRC_ALPHA(771),
      ONE_MINUS_SRC_COLOR(769),
      SRC_ALPHA(770),
      SRC_COLOR(768),
      ZERO(0);

      public final int value;

      DestFactor(int param2) {
         this.value = param2;
      }
   }

   public enum LogicOp {
      AND(5377),
      AND_INVERTED(5380),
      AND_REVERSE(5378),
      CLEAR(5376),
      COPY(5379),
      COPY_INVERTED(5388),
      EQUIV(5385),
      INVERT(5386),
      NAND(5390),
      NOOP(5381),
      NOR(5384),
      OR(5383),
      OR_INVERTED(5389),
      OR_REVERSE(5387),
      SET(5391),
      XOR(5382);

      public final int value;

      LogicOp(int param2) {
         this.value = param2;
      }
   }

   static class PolygonOffsetState {
      public final GlStateManager.BooleanState fill = new GlStateManager.BooleanState(32823);
      public final GlStateManager.BooleanState line = new GlStateManager.BooleanState(10754);
      public float factor;
      public float units;
   }

   static class ScissorState {
      public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3089);
   }

   @DontObfuscate
   public enum SourceFactor {
      CONSTANT_ALPHA(32771),
      CONSTANT_COLOR(32769),
      DST_ALPHA(772),
      DST_COLOR(774),
      ONE(1),
      ONE_MINUS_CONSTANT_ALPHA(32772),
      ONE_MINUS_CONSTANT_COLOR(32770),
      ONE_MINUS_DST_ALPHA(773),
      ONE_MINUS_DST_COLOR(775),
      ONE_MINUS_SRC_ALPHA(771),
      ONE_MINUS_SRC_COLOR(769),
      SRC_ALPHA(770),
      SRC_ALPHA_SATURATE(776),
      SRC_COLOR(768),
      ZERO(0);

      public final int value;

      SourceFactor(int param2) {
         this.value = param2;
      }
   }

   static class StencilFunc {
      public int func = 519;
      public int ref;
      public int mask = -1;
   }

   static class StencilState {
      public final GlStateManager.StencilFunc func = new GlStateManager.StencilFunc();
      public int mask = -1;
      public int fail = 7680;
      public int zfail = 7680;
      public int zpass = 7680;
   }

   static class TextureState {
      public int binding;
   }

   public enum Viewport {
      INSTANCE;

      protected int x;
      protected int y;
      protected int width;
      protected int height;

      public static int x() {
         return INSTANCE.x;
      }

      public static int y() {
         return INSTANCE.y;
      }

      public static int width() {
         return INSTANCE.width;
      }

      public static int height() {
         return INSTANCE.height;
      }
   }
}
