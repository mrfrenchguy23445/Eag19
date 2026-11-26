/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package net.lax1dude.eaglercraft.v1_8.internal;

import java.util.List;

import net.lax1dude.eaglercraft.v1_8.internal.buffer.ByteBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.FloatBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.IntBuffer;

public class PlatformOpenGL {

	public static native void _wglEnable(int glEnum);

	public static native void _wglDisable(int glEnum);

	public static native void _wglClearColor(float r, float g, float b, float a);

	public static native void _wglClearDepth(float f);

	public static native void _wglClear(int bits);

	public static native void _wglDepthFunc(int glEnum);

	public static native void _wglDepthMask(boolean mask);

	public static native void _wglCullFace(int glEnum);

	public static native void _wglViewport(int x, int y, int w, int h);

	public static native void _wglBlendFunc(int src, int dst);

	public static native void _wglBlendFuncSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha);

	public static native void _wglBlendEquation(int glEnum);

	public static native void _wglBlendColor(float r, float g, float b, float a);

	public static native void _wglColorMask(boolean r, boolean g, boolean b, boolean a);

	public static native void _wglDrawBuffers(int buffer);

	public static native void _wglDrawBuffers(int[] buffers);

	public static native void _wglReadBuffer(int glEnum);

	public static native void _wglReadPixels(int x, int y, int width, int height, int format, int type,
			ByteBuffer buffer);

	public static native void _wglReadPixels_u16(int x, int y, int width, int height, int format, int type,
			ByteBuffer buffer);

	public static native void _wglReadPixels(int x, int y, int width, int height, int format, int type,
			IntBuffer buffer);

	public static native void _wglReadPixels(int x, int y, int width, int height, int format, int type,
			FloatBuffer buffer);

	public static native void _wglPolygonOffset(float f1, float f2);

	public static native void _wglLineWidth(float width);

	public static native IBufferGL _wglGenBuffers();

	public static native ITextureGL _wglGenTextures();

	public static native IVertexArrayGL _wglGenVertexArrays();

	public static native IProgramGL _wglCreateProgram();

	public static native IShaderGL _wglCreateShader(int type);

	public static native IFramebufferGL _wglCreateFramebuffer();

	public static native IRenderbufferGL _wglCreateRenderbuffer();

	public static native IQueryGL _wglGenQueries();

	public static native void _wglDeleteBuffers(IBufferGL objId);

	public static native void _wglDeleteTextures(ITextureGL objId);

	public static native void _wglDeleteVertexArrays(IVertexArrayGL objId);

	public static native void _wglDeleteProgram(IProgramGL objId);

	public static native void _wglDeleteShader(IShaderGL objId);

	public static native void _wglDeleteFramebuffer(IFramebufferGL objId);

	public static native void _wglDeleteRenderbuffer(IRenderbufferGL objId);

	public static native void _wglDeleteQueries(IQueryGL objId);

	public static native void _wglBindBuffer(int target, IBufferGL bufObj);

	public static native void _wglBufferData(int target, int size, int usage);

	public static native void _wglBufferData(int target, ByteBuffer buffer, int usage);

	public static native void _wglBufferData(int target, IntBuffer buffer, int usage);

	public static native void _wglBufferData(int target, FloatBuffer buffer, int usage);

	public static native void _wglBufferSubData(int target, int dstOffset, ByteBuffer buffer);

	public static native void _wglBufferSubData(int target, int dstOffset, IntBuffer buffer);

	public static native void _wglBufferSubData(int target, int dstOffset, FloatBuffer buffer);

	public static native void _wglBindVertexArray(IVertexArrayGL objId);

	public static native void _wglEnableVertexAttribArray(int index);

	public static native void _wglDisableVertexAttribArray(int index);

	public static native void _wglVertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
			int offset);

	public static native void _wglVertexAttribDivisor(int index, int divisor);

	public static native void _wglActiveTexture(int texture);

	public static native void _wglBindTexture(int target, ITextureGL objId);

	public static native void _wglTexParameterf(int target, int param, float value);

	public static native void _wglTexParameteri(int target, int param, int value);

	public static native void _wglTexImage3D(int target, int level, int internalFormat, int width, int height,
			int depth, int border, int format, int type, ByteBuffer data);

	public static native void _wglTexImage2D(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, ByteBuffer data);

	public static native void _wglTexImage2Du16(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, ByteBuffer data);

	public static native void _wglTexImage2Df32(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, ByteBuffer data);

	public static native void _wglTexImage2D(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, IntBuffer data);

	public static native void _wglTexImage2Df32(int target, int level, int internalFormat, int width, int height,
			int border, int format, int type, FloatBuffer data);

	public static native void _wglTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
			int format, int type, ByteBuffer data);

	public static native void _wglTexSubImage2Du16(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, ByteBuffer data);

	public static native void _wglTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
			int format, int type, IntBuffer data);

	public static native void _wglTexSubImage2Df32(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, FloatBuffer data);

	public static native void _wglCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
			int width, int height);

	public static native void _wglTexStorage2D(int target, int levels, int internalFormat, int w, int h);

	public static native void _wglPixelStorei(int pname, int value);

	public static native void _wglGenerateMipmap(int target);

	public static native void _wglShaderSource(IShaderGL shader, String str);

	public static native void _wglCompileShader(IShaderGL shader);

	public static native int _wglGetShaderi(IShaderGL shader, int param);

	public static native String _wglGetShaderInfoLog(IShaderGL shader);

	public static native void _wglUseProgram(IProgramGL prog);

	public static native void _wglAttachShader(IProgramGL prog, IShaderGL shader);

	public static native void _wglDetachShader(IProgramGL prog, IShaderGL shader);

	public static native void _wglLinkProgram(IProgramGL prog);

	public static native int _wglGetProgrami(IProgramGL prog, int param);

	public static native String _wglGetProgramInfoLog(IProgramGL prog);

	public static native void _wglDrawArrays(int mode, int first, int count);

	public static native void _wglDrawElements(int mode, int count, int type, int offset);

	public static native void _wglDrawRangeElements(int mode, int start, int end, int count, int type, int offset);

	public static native void _wglDrawArraysInstanced(int mode, int first, int count, int instanced);

	public static native void _wglDrawElementsInstanced(int mode, int count, int type, int offset, int instanced);

	public static native void _wglBindAttribLocation(IProgramGL prog, int index, String str);

	public static native int _wglGetAttribLocation(IProgramGL prog, String str);

	public static native IUniformGL _wglGetUniformLocation(IProgramGL prog, String str);

	public static native int _wglGetUniformBlockIndex(IProgramGL prog, String str);

	public static native void _wglBindBufferRange(int target, int index, IBufferGL bufferId, int offset, int size);

	public static native void _wglUniformBlockBinding(IProgramGL prog, int blockIndex, int bufferIndex);

	public static native void _wglUniform1f(IUniformGL uniformIndex, float x);

	public static native void _wglUniform2f(IUniformGL uniformIndex, float x, float y);

	public static native void _wglUniform3f(IUniformGL uniformIndex, float x, float y, float z);

	public static native void _wglUniform4f(IUniformGL uniformIndex, float x, float y, float z, float w);

	public static native void _wglUniform1i(IUniformGL uniformIndex, int x);

	public static native void _wglUniform2i(IUniformGL uniformIndex, int x, int y);

	public static native void _wglUniform3i(IUniformGL uniformIndex, int x, int y, int z);

	public static native void _wglUniform4i(IUniformGL uniformIndex, int x, int y, int z, int w);

	public static native void _wglUniformMatrix2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglUniformMatrix3fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglUniformMatrix4fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglUniformMatrix3x2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglUniformMatrix4x2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglUniformMatrix4x3fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer);

	public static native void _wglBindFramebuffer(int target, IFramebufferGL framebuffer);

	public static native int _wglCheckFramebufferStatus(int target);

	public static native void _wglBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0,
			int dstX1, int dstY1, int bits, int filter);

	public static native void _wglRenderbufferStorage(int target, int internalformat, int width, int height);

	public static native void _wglFramebufferTexture2D(int target, int attachment, int texTarget, ITextureGL texObj,
			int level);

	public static native void _wglFramebufferTextureLayer(int target, int attachment, ITextureGL texObj, int level,
			int layer);

	public static native void _wglBindRenderbuffer(int target, IRenderbufferGL renderbuffer);

	public static native void _wglFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
			IRenderbufferGL renderbufferId);

	public static native String _wglGetString(int param);

	public static native int _wglGetInteger(int param);

	public static native int _wglGetError();

	public static native int checkOpenGLESVersion();

	public static native boolean checkEXTGPUShader5Capable();

	public static native boolean checkOESGPUShader5Capable();

	public static native boolean checkFBORenderMipmapCapable();

	public static native boolean checkVAOCapable();

	public static native boolean checkInstancingCapable();

	public static native boolean checkTexStorageCapable();

	public static native boolean checkTextureLODCapable();

	public static native boolean checkNPOTCapable();

	public static native boolean checkHDRFramebufferSupport(int bits);

	public static native boolean checkLinearHDRFilteringSupport(int bits);

	// legacy
	public static native boolean checkLinearHDR32FSupport();

	public static native boolean checkAnisotropicFilteringSupport();

	public static native String[] getAllExtensions();

	public static native List<String> dumpActiveExtensions();

	public static native void enterVAOEmulationHook();

	// ---------------------------------------------------------------------------
	// Added for the 1.14-derived GL layer (opengl/GlStateManager, opengl/
	// EaglercraftGPU). These four entry points did not exist in the 1.19.4-era
	// PlatformOpenGL, but are called by the ported GPU code. Each must be
	// implemented by every runtime that supplies PlatformOpenGL - notably
	// src/wasm-gc-teavm/java and src/lwjgl/java.
	// ---------------------------------------------------------------------------

	public static native void _wglScissor(int x, int y, int width, int height);

	/**
	 * Multi-draw extension entry point. Mirrors glMultiDrawElements(GLenum mode,
	 * const GLsizei *count, GLenum type, const void **indices, GLsizei drawcount).
	 * Only called when checkMultiDrawCapable() returns true, so runtimes that
	 * report false are never required to implement it meaningfully.
	 */
	public static native void _wglMultiDrawElements(int mode, IntBuffer counts, int type, IntBuffer firsts, int drawCount);

	/** Copy between buffer targets; call only when the copy-buffer extension is present. */
	public static native void _wglCopyBufferSubData(int target, int srcTarget, int srcOffset, int dstOffset, int size);

	public static native boolean checkMultiDrawCapable();

	// ---------------------------------------------------------------------------
	// Int-based GL entry points, for Mojang's 1.19.4 blaze3d code.
	//
	// WHY THIS LAYER EXISTS
	//
	// com.mojang.blaze3d.platform.GlStateManager (885 lines, 97 call sites) is
	// unmodified 1.19.4 decompiled code and talks to LWJGL: GL11.glEnable,
	// GL15.glBindBuffer, GL20.glUniform4fv, GL30.glBindVertexArray and so on.
	// org.lwjgl:joml-adjacent LWJGL is declared `compileOnly` in the root build
	// precisely so javac can resolve those types while TeaVM never sees them --
	// otherwise the TeaVM targets would try to compile LWJGL's JNI bindings to
	// WASM. So for the WASM client all eight org.lwjgl.opengl.GL* classes are
	// simply absent, which is 16 "Class org.lwjgl.opengl.GL15 was not found"
	// build errors and counting: TeaVM reports roughly one error per method and
	// gives up on the rest, so the real surface is all 97 sites.
	//
	// The fix is NOT to re-create org.lwjgl.* as source. GlStateManager:158
	// builds its shader source with `GL20C.nglShaderSource(id, 1,
	// stack.mallocPointer(1).address0(), 0L)`, i.e. it hands a raw native
	// pointer to GL. There is no pointer to hand over on WASM, so any faithful
	// LWJGL facade would have to invent one. Instead GlStateManager is ported to
	// call the _gl* methods below, which take plain Java ints exactly where
	// desktop GL takes a GLuint name.
	//
	// _gl* (this layer) sits ON TOP OF _wgl* (the object-handle layer above).
	// The difference is only the handle representation: _wgl* passes
	// IBufferGL / ITextureGL / IProgramGL wrapper objects, because that is what
	// the WebGL bindings can hold. _gl* keeps Mojang's integer names and each
	// runtime translates them, allocating a wrapper on glGen* and resolving it
	// back on glBind*/glDelete*.
	//
	// BUFFERS
	//
	// The java.nio buffers below are deliberate and are NOT the lax1dude ones
	// imported at the top of this file. GlStateManager's methods are called from
	// RenderSystem, BufferBuilder and the vertex formats with java.nio buffers,
	// and those signatures are Mojang's, so changing them would ripple far past
	// the GL layer. Each runtime converts at this boundary.
	//
	// SIGNED/OFFSET PARAMETERS
	//
	// Where desktop GL takes a `long` offset into client memory
	// (glDrawElements, glTexSubImage2D, glVertexAttribPointer, glReadPixels,
	// glDrawPixels, glGetTexImage) the parameter is kept as `long` here so the
	// Mojang call sites keep their exact spelling, but it is a *buffer index*,
	// not a pointer, and runtimes must range-check it rather than dereference it.
	//
	// IMPLEMENTATION STATUS -- read before implementing a runtime
	//
	// Roughly 49 of these map onto a WebGL entry point. The remainder have no
	// WebGL equivalent at all and every runtime must throw
	// UnsupportedOperationException from them rather than approximate:
	//
	//   _glPolygonMode, _glLogicOp, _glDrawPixels   (fixed-function / GL 1.x)
	//   _glStencilFunc, _glStencilMask, _glStencilOp, _glClearStencil  (no stencil in WebGL)
	//   _glGetTexImage, _glGetTexLevelParameteri     (WebGL cannot read texture storage)
	//   _glMapBuffer, _glUnmapBuffer                 (no client-side buffer mapping)
	//   _glVertexAttribIPointer                     (no integer attribute pointers in WebGL 1)
	//
	// A silent no-op there would render wrong rather than fail, which is the
	// worse outcome, so these refuse loudly.
	//
	// RUNTIMES
	//
	// Implemented in src/wasm-gc-teavm/java (the build under construction).
	// src/lwjgl/java and src/teavm/java ship their own PlatformOpenGL and will
	// need this layer too before those targets build; they are not wired up yet.
	// ---------------------------------------------------------------------------

	// --- GL11 ---
	public static native void _glEnable(int cap);

	public static native void _glDisable(int cap);

	public static native void _glDepthFunc(int func);

	public static native void _glDepthMask(boolean flag);

	public static native void _glBlendFunc(int src, int dst);

	/** No WebGL equivalent. Must throw. */
	public static native void _glPolygonMode(int face, int mode);

	public static native void _glPolygonOffset(float factor, float units);

	/** No WebGL equivalent. Must throw. */
	public static native void _glLogicOp(int op);

	public static native void _glTexParameterf(int target, int pname, float param);

	public static native void _glTexParameteri(int target, int pname, int param);

	/** No WebGL equivalent. Must throw. */
	public static native int _glGetTexLevelParameteri(int target, int level, int pname);

	public static native int _glGenTextures();

	public static native void _glGenTextures(int[] textures);

	public static native void _glDeleteTextures(int texture);

	public static native void _glDeleteTextures(int[] textures);

	public static native void _glBindTexture(int target, int texture);

	public static native void _glTexImage2D(int target, int level, int internalformat, int width, int height,
			int border, int format, int type, java.nio.IntBuffer pixels);

	/** No WebGL equivalent for the offset form. Must throw. */
	public static native void _glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, long offset);

	/**
	 * ByteBuffer form of {@code glTexSubImage2D}. WebGL takes an ArrayBufferView, not a
	 * client address, so this is the only form that can actually upload. NativeImage
	 * holds pixels in a Java byte[] and reaches this through GlStateManager._texSubImage2D.
	 */
	public static native void _glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, java.nio.ByteBuffer pixels);

	/** No WebGL equivalent. Must throw. */
	public static native void _glGetTexImage(int target, int level, int format, int type, long offset);

	public static native void _glViewport(int x, int y, int width, int height);

	public static native void _glColorMask(boolean r, boolean g, boolean b, boolean a);

	/** No WebGL equivalent. Must throw. */
	public static native void _glStencilFunc(int func, int ref, int mask);

	/** No WebGL equivalent. Must throw. */
	public static native void _glStencilMask(int mask);

	/** No WebGL equivalent. Must throw. */
	public static native void _glStencilOp(int sfail, int dpfail, int dppass);

	/** Desktop GL takes a double; WebGL only has a float depth clear. Narrowed. */
	public static native void _glClearDepth(double depth);

	public static native void _glClearColor(float r, float g, float b, float a);

	/** No WebGL equivalent. Must throw. */
	public static native void _glClearStencil(int s);

	public static native void _glClear(int mask);

	/** No WebGL equivalent. Must throw. */
	public static native void _glDrawPixels(int width, int height, int format, int type, long offset);

	public static native void _glDrawElements(int mode, int count, int type, long offset);

	public static native void _glPixelStorei(int pname, int value);

	public static native void _glReadPixels(int x, int y, int width, int height, int format, int type,
			java.nio.ByteBuffer pixels);

	/** No WebGL equivalent for the raw-offset form. Must throw. */
	public static native void _glReadPixels(int x, int y, int width, int height, int format, int type, long offset);

	public static native int _glGetError();

	public static native String _glGetString(int name);

	public static native int _glGetInteger(int pname);

	// --- GL13 ---
	public static native void _glActiveTexture(int texture);

	// --- GL14 ---
	public static native void _glBlendEquation(int mode);

	public static native void _glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

	// --- GL15 (and GL32C, whose three glBindBuffer/glBufferData uses are identical) ---
	public static native int _glGenBuffers();

	public static native void _glBindBuffer(int target, int buffer);

	public static native void _glBufferData(int target, java.nio.ByteBuffer data, int usage);

	public static native void _glBufferData(int target, long size, int usage);

	/** No WebGL equivalent. Must throw. */
	public static native java.nio.ByteBuffer _glMapBuffer(int target, int access);

	/** No WebGL equivalent. Must throw. */
	public static native boolean _glUnmapBuffer(int target);

	public static native void _glDeleteBuffers(int buffer);

	// --- GL20 ---
	public static native void _glScissor(int x, int y, int width, int height);

	public static native int _glGetProgrami(int program, int pname);

	public static native void _glAttachShader(int program, int shader);

	public static native void _glDeleteShader(int shader);

	public static native int _glCreateShader(int type);

	/** Replaces GL20C.nglShaderSource, which passed a raw pointer to a C string. */
	public static native void _glShaderSource(int shader, String source);

	public static native void _glCompileShader(int shader);

	public static native int _glGetShaderi(int shader, int pname);

	public static native void _glUseProgram(int program);

	public static native int _glCreateProgram();

	public static native void _glDeleteProgram(int program);

	public static native void _glLinkProgram(int program);

	public static native int _glGetUniformLocation(int program, CharSequence name);

	public static native void _glUniform1i(int location, int v0);

	public static native void _glUniform1iv(int location, java.nio.IntBuffer value);

	public static native void _glUniform1f(int location, float v0);

	public static native void _glUniform1fv(int location, java.nio.FloatBuffer value);

	public static native void _glUniform2i(int location, int v0, int v1);

	public static native void _glUniform2iv(int location, java.nio.IntBuffer value);

	public static native void _glUniform2f(int location, float v0, float v1);

	public static native void _glUniform2fv(int location, java.nio.FloatBuffer value);

	public static native void _glUniform3i(int location, int v0, int v1, int v2);

	public static native void _glUniform3iv(int location, java.nio.IntBuffer value);

	public static native void _glUniform3f(int location, float v0, float v1, float v2);

	public static native void _glUniform3fv(int location, java.nio.FloatBuffer value);

	public static native void _glUniform4i(int location, int v0, int v1, int v2, int v3);

	public static native void _glUniform4iv(int location, java.nio.IntBuffer value);

	public static native void _glUniform4f(int location, float v0, float v1, float v2, float v3);

	public static native void _glUniform4fv(int location, java.nio.FloatBuffer value);

	public static native void _glUniformMatrix2fv(int location, boolean transpose, java.nio.FloatBuffer value);

	public static native void _glUniformMatrix3fv(int location, boolean transpose, java.nio.FloatBuffer value);

	public static native void _glUniformMatrix4fv(int location, boolean transpose, java.nio.FloatBuffer value);

	public static native int _glGetAttribLocation(int program, CharSequence name);

	public static native void _glBindAttribLocation(int program, int index, CharSequence name);

	public static native void _glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x,
			int y, int width, int height);

	public static native void _glEnableVertexAttribArray(int index);

	public static native void _glDisableVertexAttribArray(int index);

	public static native void _glVertexAttribPointer(int index, int size, int type, boolean normalized,
			int stride, long offset);

	/** Desktop GL takes a maxLength; WebGL has no length limit, so it is ignored. */
	public static native String _glGetShaderInfoLog(int shader, int maxLength);

	/** Desktop GL takes a maxLength; WebGL has no length limit, so it is ignored. */
	public static native String _glGetProgramInfoLog(int program, int maxLength);

	// --- GL30 ---
	public static native int _glGenVertexArrays();

	public static native void _glBindVertexArray(int array);

	public static native void _glDeleteVertexArrays(int array);

	public static native void _glBindFramebuffer(int target, int framebuffer);

	public static native void _glDeleteFramebuffers(int framebuffer);

	public static native int _glGenFramebuffers();

	public static native void _glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
			int dstY0, int dstX1, int dstY1, int mask, int filter);

	public static native void _glBindRenderbuffer(int target, int renderbuffer);

	public static native void _glDeleteRenderbuffers(int renderbuffer);

	public static native int _glGenRenderbuffers();

	public static native void _glRenderbufferStorage(int target, int internalformat, int width, int height);

	public static native void _glFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
			int renderbuffer);

	public static native int _glCheckFramebufferStatus(int target);

	public static native void _glFramebufferTexture2D(int target, int attachment, int textarget, int texture,
			int level);

	/** No WebGL equivalent. Must throw. */
	public static native void _glVertexAttribIPointer(int index, int size, int type, int stride, long offset);

}