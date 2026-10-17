/*
 * Copyright (c) 2024 lax1dude. All Rights Reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderbuffer;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLTexture;
import org.teavm.jso.webgl.WebGLUniformLocation;

import net.lax1dude.eaglercraft.v1_8.internal.buffer.ByteBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.FloatBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.IntBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.WASMGCBufferAllocator;
import net.lax1dude.eaglercraft.v1_8.internal.buffer.WASMGCDirectArrayCopy;
import net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.BetterJSStringConverter;
import net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.MemoryViews;
import net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.WebGLBackBuffer;
import net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.WebGLQuery;
import net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.WebGLVertexArray;

public class PlatformOpenGL {

	static int glesVers = -1;

	static final int VAO_IMPL_NONE = -1;
	static final int VAO_IMPL_CORE = 0;
	static final int VAO_IMPL_OES = 1;
	static int vertexArrayImpl = VAO_IMPL_NONE;

	static final int INSTANCE_IMPL_NONE = -1;
	static final int INSTANCE_IMPL_CORE = 0;
	static final int INSTANCE_IMPL_ANGLE = 1;
	static int instancingImpl = INSTANCE_IMPL_NONE;

	static final int CAP_A_BIT_EXT_GPU_SHADER5 = 1;
	static final int CAP_A_BIT_OES_GPU_SHADER5 = 2;
	static final int CAP_A_BIT_FBO_RENDER_MIPMAP = 4;
	static final int CAP_A_BIT_TEXTURE_LOD_CAPABLE = 8;
	static final int CAP_A_BIT_NPOT_CAPABLE = 16;
	static final int CAP_A_BIT_HDR_FBO16F = 32;
	static final int CAP_A_BIT_HDR_FBO32F = 64;
	static final int CAP_A_BIT_ANISOTROPIC = 128;
	static int capABits = 0;

	static final int CAP_B_BIT_HDR_LINEAR16F = 1;
	static final int CAP_B_BIT_HDR_LINEAR32F = 2;
	static int capBBits = 0;

	static void initContext() {
		glesVers = getCapBits(0);
		vertexArrayImpl = getCapBits(1);
		instancingImpl = getCapBits(2);
		capABits = getCapBits(3);
		capBBits = getCapBits(4);
		_wglClearColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Import(module = "platformOpenGL", name = "getCapBits")
	static native int getCapBits(int idx);

	@Import(module = "platformOpenGL", name = "glEnable")
	public static native void _wglEnable(int glEnum);

	@Import(module = "platformOpenGL", name = "glDisable")
	public static native void _wglDisable(int glEnum);

	@Import(module = "platformOpenGL", name = "glClearColor")
	public static native void _wglClearColor(float r, float g, float b, float a);

	@Import(module = "platformOpenGL", name = "glClearDepth")
	public static native void _wglClearDepth(float f);

	@Import(module = "platformOpenGL", name = "glClear")
	public static native void _wglClear(int bits);

	@Import(module = "platformOpenGL", name = "glDepthFunc")
	public static native void _wglDepthFunc(int glEnum);

	@Import(module = "platformOpenGL", name = "glDepthMask")
	public static native void _wglDepthMask(boolean mask);

	@Import(module = "platformOpenGL", name = "glCullFace")
	public static native void _wglCullFace(int glEnum);

	@Import(module = "platformOpenGL", name = "glViewport")
	public static native void _wglViewport(int x, int y, int w, int h);

	@Import(module = "platformOpenGL", name = "glScissor")
	public static native void _wglScissor(int x, int y, int w, int h);

	@Import(module = "platformOpenGL", name = "glBlendFunc")
	public static native void _wglBlendFunc(int src, int dst);

	@Import(module = "platformOpenGL", name = "glBlendFuncSeparate")
	public static native void _wglBlendFuncSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha);

	@Import(module = "platformOpenGL", name = "glBlendEquation")
	public static native void _wglBlendEquation(int glEnum);

	@Import(module = "platformOpenGL", name = "glBlendColor")
	public static native void _wglBlendColor(float r, float g, float b, float a);

	@Import(module = "platformOpenGL", name = "glColorMask")
	public static native void _wglColorMask(boolean r, boolean g, boolean b, boolean a);

	private static final JSArray<JSNumber> drawBuffers = new JSArray<>();

	@JSBody(params = { "arr", "idx", "num" }, script = "arr[idx] = num;")
	private static native void setArrayInt(JSArray<JSNumber> arr, int idx, int num);

	public static void _wglDrawBuffers(int buffer) {
		if(glesVers == 200) {
			if(buffer != 0x8CE0) { // GL_COLOR_ATTACHMENT0
				throw new UnsupportedOperationException();
			}
		}else {
			drawBuffers.setLength(1);
			setArrayInt(drawBuffers, 0, buffer);
			_wglDrawBuffersN(drawBuffers);
		}
	}

	public static void _wglDrawBuffers(int[] buffers) {
		if(glesVers == 200) {
			if(buffers.length != 1 || buffers[0] != 0x8CE0) { // GL_COLOR_ATTACHMENT0
				throw new UnsupportedOperationException();
			}
		}else {
			int cnt = buffers.length;
			drawBuffers.setLength(cnt);
			for(int i = 0; i < cnt; ++i) {
				setArrayInt(drawBuffers, i, buffers[i]);
			}
			_wglDrawBuffersN(drawBuffers);
		}
	}

	@Import(module = "platformOpenGL", name = "glDrawBuffers")
	private static native void _wglDrawBuffersN(JSArray<JSNumber> buffers);

	@Import(module = "platformOpenGL", name = "glReadBuffer")
	public static native void _wglReadBuffer(int glEnum);

	public static void _wglReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
		if (glesVers == 200) {
			_wglReadPixelsN(x, y, width, height, format, type, WASMGCBufferAllocator.getUnsignedByteBufferView(buffer));
		} else {
			_wglReadPixelsN(x, y, width, height, format, type, MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(buffer));
		}
	}

	public static void _wglReadPixels_u16(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
		if (glesVers == 200) {
			_wglReadPixelsN(x, y, width, height, format, type, WASMGCBufferAllocator.getUnsignedShortBufferView(buffer));
		} else {
			_wglReadPixelsN(x, y, width, height, format, type, MemoryViews.u16, WASMGCBufferAllocator.getShortBufferViewIndex(buffer));
		}
	}

	public static void _wglReadPixels(int x, int y, int width, int height, int format, int type, IntBuffer buffer) {
		if (glesVers == 200) {
			_wglReadPixelsN(x, y, width, height, format, type, WASMGCBufferAllocator.getIntBufferView(buffer));
		} else {
			_wglReadPixelsN(x, y, width, height, format, type, MemoryViews.i32, WASMGCBufferAllocator.getIntBufferViewIndex(buffer));
		}
	}

	public static void _wglReadPixels(int x, int y, int width, int height, int format, int type, FloatBuffer buffer) {
		if (glesVers == 200) {
			_wglReadPixelsN(x, y, width, height, format, type, WASMGCBufferAllocator.getFloatBufferView(buffer));
		} else {
			_wglReadPixelsN(x, y, width, height, format, type, MemoryViews.f32, WASMGCBufferAllocator.getFloatBufferViewIndex(buffer));
		}
	}

	@Import(module = "platformOpenGL", name = "glReadPixels")
	static native void _wglReadPixelsN(int x, int y, int width, int height, int format, int type, ArrayBufferView array);

	@Import(module = "platformOpenGL", name = "glReadPixels")
	static native void _wglReadPixelsN(int x, int y, int width, int height, int format, int type, ArrayBufferView array, int offset);

	@Import(module = "platformOpenGL", name = "glPolygonOffset")
	public static native void _wglPolygonOffset(float f1, float f2);

	@Import(module = "platformOpenGL", name = "glLineWidth")
	public static native void _wglLineWidth(float width);

	public static IBufferGL _wglGenBuffers() {
		return new OpenGLObjects.BufferGL(_wglGenBuffersN());
	}

	@Import(module = "platformOpenGL", name = "glGenBuffers")
	static native WebGLBuffer _wglGenBuffersN();

	public static ITextureGL _wglGenTextures() {
		return new OpenGLObjects.TextureGL(_wglGenTexturesN());
	}

	@Import(module = "platformOpenGL", name = "glGenTextures")
	static native WebGLTexture _wglGenTexturesN();

	public static IVertexArrayGL _wglGenVertexArrays() {
		return new OpenGLObjects.VertexArrayGL(_wglGenVertexArraysN());
	}

	@Import(module = "platformOpenGL", name = "glGenVertexArrays")
	public static native WebGLVertexArray _wglGenVertexArraysN();

	public static IProgramGL _wglCreateProgram() {
		return new OpenGLObjects.ProgramGL(_wglCreateProgramN());
	}

	@Import(module = "platformOpenGL", name = "glCreateProgram")
	static native WebGLProgram _wglCreateProgramN();

	public static IShaderGL _wglCreateShader(int type) {
		WebGLShader shader = _wglCreateShaderN(type);
		if(shader == null) {
			// Workaround for chrome not handling lost context correctly in shaderSource
			// "Failed to execute 'shaderSource' on 'WebGL2RenderingContext': parameter 1 is not of type 'WebGLShader'."
			if (PlatformInput.contextLost()) {
				throw new ContextLostError();
			} else {
				throw new Error("createShader returned null and the context is not lost");
			}
		}
		return new OpenGLObjects.ShaderGL(shader);
	}

	@Import(module = "platformOpenGL", name = "glCreateShader")
	static native WebGLShader _wglCreateShaderN(int type);

	public static IFramebufferGL _wglCreateFramebuffer() {
		return new OpenGLObjects.FramebufferGL(_wglCreateFramebufferN());
	}

	@Import(module = "platformOpenGL", name = "glCreateFramebuffer")
	static native WebGLFramebuffer _wglCreateFramebufferN();

	public static IRenderbufferGL _wglCreateRenderbuffer() {
		return new OpenGLObjects.RenderbufferGL(_wglCreateRenderbufferN());
	}

	@Import(module = "platformOpenGL", name = "glCreateRenderbuffer")
	static native WebGLRenderbuffer _wglCreateRenderbufferN();

	public static IQueryGL _wglGenQueries() {
		return new OpenGLObjects.QueryGL(_wglGenQueriesN());
	}

	@Import(module = "platformOpenGL", name = "glGenQueries")
	static native WebGLQuery _wglGenQueriesN();

	public static void _wglDeleteBuffers(IBufferGL objId) {
		_wglDeleteBuffersN(((OpenGLObjects.BufferGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteBuffers")
	static native void _wglDeleteBuffersN(WebGLBuffer objId);

	public static void _wglDeleteTextures(ITextureGL objId) {
		_wglDeleteTexturesN(((OpenGLObjects.TextureGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteTextures")
	static native void _wglDeleteTexturesN(WebGLTexture objId);

	public static void _wglDeleteVertexArrays(IVertexArrayGL objId) {
		_wglDeleteVertexArraysN(((OpenGLObjects.VertexArrayGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteVertexArrays")
	static native void _wglDeleteVertexArraysN(WebGLVertexArray objId);

	public static void _wglDeleteProgram(IProgramGL objId) {
		_wglDeleteProgramN(((OpenGLObjects.ProgramGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteProgram")
	static native void _wglDeleteProgramN(WebGLProgram objId);

	public static void _wglDeleteShader(IShaderGL objId) {
		_wglDeleteShaderN(((OpenGLObjects.ShaderGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteShader")
	static native void _wglDeleteShaderN(WebGLShader objId);

	public static void _wglDeleteFramebuffer(IFramebufferGL objId) {
		_wglDeleteFramebufferN(((OpenGLObjects.FramebufferGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteFramebuffer")
	static native void _wglDeleteFramebufferN(WebGLFramebuffer objId);

	public static void _wglDeleteRenderbuffer(IRenderbufferGL objId) {
		_wglDeleteRenderbufferN(((OpenGLObjects.RenderbufferGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteRenderbuffer")
	static native void _wglDeleteRenderbufferN(WebGLRenderbuffer objId);

	public static void _wglDeleteQueries(IQueryGL objId) {
		_wglDeleteQueriesN(((OpenGLObjects.QueryGL)objId).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDeleteQueries")
	static native void _wglDeleteQueriesN(WebGLQuery objId);

	public static void _wglBindBuffer(int target, IBufferGL bufObj) {
		_wglBindBufferN(target, bufObj != null ? ((OpenGLObjects.BufferGL)bufObj).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glBindBuffer")
	static native void _wglBindBufferN(int target, WebGLBuffer bufObj);

	@Import(module = "platformOpenGL", name = "glBufferData")
	public static native void _wglBufferData(int target, int size, int usage);

	public static void _wglBufferData(int target, ByteBuffer buffer, int usage) {
		if (glesVers == 200) {
			_wglBufferDataN(target, WASMGCBufferAllocator.getUnsignedByteBufferView(buffer), usage);
		} else {
			_wglBufferDataN(target, MemoryViews.u8, usage, WASMGCBufferAllocator.getByteBufferViewIndex(buffer), buffer.remaining());
		}
	}

	public static void _wglBufferData(int target, IntBuffer buffer, int usage) {
		if (glesVers == 200) {
			_wglBufferDataN(target, WASMGCBufferAllocator.getIntBufferView(buffer), usage);
		} else {
			_wglBufferDataN(target, MemoryViews.i32, usage, WASMGCBufferAllocator.getIntBufferViewIndex(buffer), buffer.remaining());
		}
	}

	public static void _wglBufferData(int target, FloatBuffer buffer, int usage) {
		if (glesVers == 200) {
			_wglBufferDataN(target, WASMGCBufferAllocator.getFloatBufferView(buffer), usage);
		} else {
			_wglBufferDataN(target, MemoryViews.f32, usage, WASMGCBufferAllocator.getFloatBufferViewIndex(buffer), buffer.remaining());
		}
	}

	@Import(module = "platformOpenGL", name = "glBufferData")
	static native void _wglBufferDataN(int target, ArrayBufferView typedArray, int usage);

	@Import(module = "platformOpenGL", name = "glBufferData")
	static native void _wglBufferDataN(int target, ArrayBufferView typedArray, int usage, int srcOffset, int length);

	public static void _wglBufferSubData(int target, int dstOffset, ByteBuffer buffer) {
		if (glesVers == 200) {
			_wglBufferSubDataN(target, dstOffset, WASMGCBufferAllocator.getUnsignedByteBufferView(buffer));
		} else {
			_wglBufferSubDataN(target, dstOffset, MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(buffer), buffer.remaining());
		}
	}

	public static void _wglBufferSubData(int target, int dstOffset, IntBuffer buffer) {
		if (glesVers == 200) {
			_wglBufferSubDataN(target, dstOffset, WASMGCBufferAllocator.getIntBufferView(buffer));
		} else {
			_wglBufferSubDataN(target, dstOffset, MemoryViews.i32, WASMGCBufferAllocator.getIntBufferViewIndex(buffer), buffer.remaining());
		}
	}

	public static void _wglBufferSubData(int target, int dstOffset, FloatBuffer buffer) {
		if (glesVers == 200) {
			_wglBufferSubDataN(target, dstOffset, WASMGCBufferAllocator.getFloatBufferView(buffer));
		} else {
			_wglBufferSubDataN(target, dstOffset, MemoryViews.f32, WASMGCBufferAllocator.getFloatBufferViewIndex(buffer), buffer.remaining());
		}
	}

	@Import(module = "platformOpenGL", name = "glBufferSubData")
	static native void _wglBufferSubDataN(int target, int dstOffset, ArrayBufferView typedArray);

	@Import(module = "platformOpenGL", name = "glBufferSubData")
	static native void _wglBufferSubDataN(int target, int dstOffset, ArrayBufferView typedArray, int srcOffset, int length);

	public static void _wglBindVertexArray(IVertexArrayGL objId) {
		_wglBindVertexArrayN(objId != null ? ((OpenGLObjects.VertexArrayGL)objId).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glBindVertexArray")
	static native void _wglBindVertexArrayN(WebGLVertexArray objId);

	@Import(module = "platformOpenGL", name = "glEnableVertexAttribArray")
	public static native void _wglEnableVertexAttribArray(int index);

	@Import(module = "platformOpenGL", name = "glDisableVertexAttribArray")
	public static native void _wglDisableVertexAttribArray(int index);

	@Import(module = "platformOpenGL", name = "glVertexAttribPointer")
	public static native void _wglVertexAttribPointer(int index, int size, int type,
			boolean normalized, int stride, int offset);

	// PORT NOTE (Eag19 / TeaVM WASM-GC): WebGL2 (GLSL ES 3.00) has
	// vertexAttribIPointer; it is only WebGL1 that lacks it. This port requests a
	// WebGL2 context, so the integer-format vertex attributes of
	// DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP (the GL_SHORT lightmap UV2)
	// can be bound normally - see _glVertexAttribIPointer below.
	@Import(module = "platformOpenGL", name = "glVertexAttribIPointer")
	public static native void _wglVertexAttribIPointer(int index, int size, int type,
			int stride, int offset);

	@Import(module = "platformOpenGL", name = "glVertexAttribDivisor")
	public static native void _wglVertexAttribDivisor(int index, int divisor);

	@Import(module = "platformOpenGL", name = "glActiveTexture")
	public static native void _wglActiveTexture(int texture);

	public static void _wglBindTexture(int target, ITextureGL objId) {
		_wglBindTextureN(target, objId != null ? ((OpenGLObjects.TextureGL)objId).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glBindTexture")
	static native void _wglBindTextureN(int target, WebGLTexture objId);

	@Import(module = "platformOpenGL", name = "glTexParameterf")
	public static native void _wglTexParameterf(int target, int param, float value);

	@Import(module = "platformOpenGL", name = "glTexParameteri")
	public static native void _wglTexParameteri(int target, int param, int value);

	public static void _wglTexImage3D(int target, int level, int internalFormat, int width, int height, int depth,
			int border, int format, int type, ByteBuffer data) {
		if (data == null) {
			_wglTexImage3DN(target, level, internalFormat, width, height, depth, border, format, type, null);
		} else {
			_wglTexImage3DN(target, level, internalFormat, width, height, depth, border, format, type,
					MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(data));
		}
	}

	@Import(module = "platformOpenGL", name = "glTexImage3D")
	static native void _wglTexImage3DN(int target, int level, int internalFormat, int width, int height, int depth,
			int border, int format, int type, ArrayBufferView typedArray);

	@Import(module = "platformOpenGL", name = "glTexImage3D")
	static native void _wglTexImage3DN(int target, int level, int internalFormat, int width, int height, int depth,
			int border, int format, int type, ArrayBufferView typedArray, int offset);

	public static void _wglTexImage2D(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ByteBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedByteBufferView(data) : null);
		} else {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(data));
		}
	}

	public static void _wglTexImage2Du16(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ByteBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedShortBufferView(data) : null);
		} else {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					MemoryViews.u16, WASMGCBufferAllocator.getShortBufferViewIndex(data));
		}
	}

	public static void _wglTexImage2Df32(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ByteBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					data != null ? WASMGCBufferAllocator.getFloatBufferView(data) : null);
		} else {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					MemoryViews.f32, WASMGCBufferAllocator.getFloatBufferViewIndex(data));
		}
	}

	public static void _wglTexImage2D(int target, int level, int internalFormat, int width,
			int height, int border, int format, int type, IntBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedByteBufferView(data) : null);
		} else {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(data));
		}
	}

	public static void _wglTexImage2Df32(int target, int level, int internalFormat, int width,
			int height, int border, int format, int type, FloatBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					data != null ? WASMGCBufferAllocator.getFloatBufferView(data) : null);
		} else {
			_wglTexImage2DN(target, level, internalFormat, width, height, border, format, type,
					MemoryViews.f32, WASMGCBufferAllocator.getFloatBufferViewIndex(data));
		}
	}

	@Import(module = "platformOpenGL", name = "glTexImage2D")
	static native void _wglTexImage2DN(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ArrayBufferView typedArray);

	@Import(module = "platformOpenGL", name = "glTexImage2D")
	static native void _wglTexImage2DN(int target, int level, int internalFormat, int width, int height, int border,
			int format, int type, ArrayBufferView typedArray, int offset);

	public static void _wglTexSubImage2D(int target, int level, int xoffset, int yoffset,
			int width, int height, int format, int type, ByteBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedByteBufferView(data) : null);
		} else {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(data));
		}
	}

	public static void _wglTexSubImage2Du16(int target, int level, int xoffset, int yoffset,
			int width, int height, int format, int type, ByteBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedShortBufferView(data) : null);
		} else {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					MemoryViews.u16, WASMGCBufferAllocator.getShortBufferViewIndex(data));
		}
	}

	public static void _wglTexSubImage2D(int target, int level, int xoffset, int yoffset,
			int width, int height, int format, int type, IntBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					data != null ? WASMGCBufferAllocator.getUnsignedByteBufferView(data) : null);
		} else {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					MemoryViews.u8, WASMGCBufferAllocator.getByteBufferViewIndex(data));
		}
	}

	public static void _wglTexSubImage2Df32(int target, int level, int xoffset, int yoffset,
			int width, int height, int format, int type, FloatBuffer data) {
		if (data == null || glesVers == 200) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					data != null ? WASMGCBufferAllocator.getFloatBufferView(data) : null);
		} else {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					MemoryViews.f32, WASMGCBufferAllocator.getFloatBufferViewIndex(data));
		}
	}

	@Import(module = "platformOpenGL", name = "glTexSubImage2D")
	static native void _wglTexSubImage2D(int target, int level, int offsetx, int offsety, int width, int height,
			int format, int type, ArrayBufferView typedArray);

	@Import(module = "platformOpenGL", name = "glTexSubImage2D")
	static native void _wglTexSubImage2D(int target, int level, int offsetx, int offsety, int width, int height,
			int format, int type, ArrayBufferView typedArray, int offset);

	@Import(module = "platformOpenGL", name = "glCopyTexSubImage2D")
	public static native void _wglCopyTexSubImage2D(int target, int level, int xoffset, int yoffset,
			int x, int y, int width, int height);

	@Import(module = "platformOpenGL", name = "glTexStorage2D")
	public static native void _wglTexStorage2D(int target, int levels, int internalFormat, int w, int h);

	@Import(module = "platformOpenGL", name = "glPixelStorei")
	public static native void _wglPixelStorei(int pname, int value);

	@Import(module = "platformOpenGL", name = "glGenerateMipmap")
	public static native void _wglGenerateMipmap(int target);

	public static void _wglShaderSource(IShaderGL shader, String str) {
		_wglShaderSourceN(((OpenGLObjects.ShaderGL)shader).ptr, BetterJSStringConverter.stringToJS(str));
	}

	@Import(module = "platformOpenGL", name = "glShaderSource")
	static native void _wglShaderSourceN(WebGLShader shader, JSString str);

	public static void _wglCompileShader(IShaderGL shader) {
		_wglCompileShaderN(((OpenGLObjects.ShaderGL)shader).ptr);
	}

	@Import(module = "platformOpenGL", name = "glCompileShader")
	static native void _wglCompileShaderN(WebGLShader shader);

	public static int _wglGetShaderi(IShaderGL shader, int param) {
		return _wglGetShaderiN(((OpenGLObjects.ShaderGL)shader).ptr, param);
	}

	@Import(module = "platformOpenGL", name = "glGetShaderi")
	static native int _wglGetShaderiN(WebGLShader shader, int param);

	public static String _wglGetShaderInfoLog(IShaderGL shader) {
		return BetterJSStringConverter.stringFromJS(_wglGetShaderInfoLogN(((OpenGLObjects.ShaderGL)shader).ptr));
	}

	@Import(module = "platformOpenGL", name = "glGetShaderInfoLog")
	static native JSString _wglGetShaderInfoLogN(WebGLShader shader);

	public static void _wglUseProgram(IProgramGL prog) {
		_wglUseProgramN(prog != null ? ((OpenGLObjects.ProgramGL)prog).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glUseProgram")
	static native void _wglUseProgramN(WebGLProgram prog);

	public static void _wglAttachShader(IProgramGL prog, IShaderGL shader) {
		_wglAttachShaderN(((OpenGLObjects.ProgramGL)prog).ptr, ((OpenGLObjects.ShaderGL)shader).ptr);
	}

	@Import(module = "platformOpenGL", name = "glAttachShader")
	static native void _wglAttachShaderN(WebGLProgram prog, WebGLShader shader);

	public static void _wglDetachShader(IProgramGL prog, IShaderGL shader) {
		_wglDetachShaderN(((OpenGLObjects.ProgramGL)prog).ptr, ((OpenGLObjects.ShaderGL)shader).ptr);
	}

	@Import(module = "platformOpenGL", name = "glDetachShader")
	public static native void _wglDetachShaderN(WebGLProgram prog, WebGLShader shader);

	public static void _wglLinkProgram(IProgramGL prog) {
		_wglLinkProgramN(((OpenGLObjects.ProgramGL)prog).ptr);
	}

	@Import(module = "platformOpenGL", name = "glLinkProgram")
	static native void _wglLinkProgramN(WebGLProgram prog);

	public static int _wglGetProgrami(IProgramGL prog, int param) {
		return _wglGetProgramiN(((OpenGLObjects.ProgramGL)prog).ptr, param);
	}

	@Import(module = "platformOpenGL", name = "glGetProgrami")
	static native int _wglGetProgramiN(WebGLProgram prog, int param);

	public static String _wglGetProgramInfoLog(IProgramGL prog) {
		return BetterJSStringConverter.stringFromJS(_wglGetProgramInfoLogN(((OpenGLObjects.ProgramGL)prog).ptr));
	}

	@Import(module = "platformOpenGL", name = "glGetProgramInfoLog")
	static native JSString _wglGetProgramInfoLogN(WebGLProgram prog);

	@Import(module = "platformOpenGL", name = "glDrawArrays")
	public static native void _wglDrawArrays(int mode, int first, int count);

	@Import(module = "platformOpenGL", name = "glDrawElements")
	public static native void _wglDrawElements(int mode, int count, int type, int offset);

	@Import(module = "platformOpenGL", name = "glDrawRangeElements")
	public static native void _wglDrawRangeElements(int mode, int start, int end, int count, int type, int offset);

	@Import(module = "platformOpenGL", name = "glDrawArraysInstanced")
	public static native void _wglDrawArraysInstanced(int mode, int first, int count, int instanced);

	@Import(module = "platformOpenGL", name = "glDrawElementsInstanced")
	public static native void _wglDrawElementsInstanced(int mode, int count, int type, int offset, int instanced);

	public static void _wglBindAttribLocation(IProgramGL prog, int index, String str) {
		_wglBindAttribLocationN(((OpenGLObjects.ProgramGL)prog).ptr, index, BetterJSStringConverter.stringToJS(str));
	}

	@Import(module = "platformOpenGL", name = "glBindAttribLocation")
	static native void _wglBindAttribLocationN(WebGLProgram prog, int index, JSString str);

	public static int _wglGetAttribLocation(IProgramGL prog, String str) {
		return _wglGetAttribLocationN(((OpenGLObjects.ProgramGL)prog).ptr, BetterJSStringConverter.stringToJS(str));
	}

	@Import(module = "platformOpenGL", name = "glGetAttribLocation")
	static native int _wglGetAttribLocationN(WebGLProgram prog, JSString str);

	public static IUniformGL _wglGetUniformLocation(IProgramGL prog, String str) {
		WebGLUniformLocation ret = _wglGetUniformLocationN(((OpenGLObjects.ProgramGL)prog).ptr, BetterJSStringConverter.stringToJS(str));
		return ret != null ? new OpenGLObjects.UniformGL(ret) : null;
	}

	@Import(module = "platformOpenGL", name = "glGetUniformLocation")
	static native WebGLUniformLocation _wglGetUniformLocationN(WebGLProgram prog, JSString str);

	public static int _wglGetUniformBlockIndex(IProgramGL prog, String str) {
		return _wglGetUniformBlockIndexN(((OpenGLObjects.ProgramGL)prog).ptr, BetterJSStringConverter.stringToJS(str));
	}

	@Import(module = "platformOpenGL", name = "glGetUniformBlockIndex")
	static native int _wglGetUniformBlockIndexN(WebGLProgram prog, JSString str);

	public static void _wglBindBufferRange(int target, int index, IBufferGL bufferId, int offset, int size) {
		_wglBindBufferRangeN(target, index, ((OpenGLObjects.BufferGL)bufferId).ptr, offset, size);
	}

	@Import(module = "platformOpenGL", name = "glBindBufferRange")
	static native void _wglBindBufferRangeN(int target, int index, WebGLBuffer bufferId, int offset, int size);

	public static void _wglUniformBlockBinding(IProgramGL prog, int blockIndex, int bufferIndex) {
		_wglUniformBlockBindingN(((OpenGLObjects.ProgramGL)prog).ptr, blockIndex, bufferIndex);
	}

	@Import(module = "platformOpenGL", name = "glUniformBlockBinding")
	static native void _wglUniformBlockBindingN(WebGLProgram prog, int blockIndex, int bufferIndex);

	public static void _wglUniform1f(IUniformGL uniformIndex, float x) {
		if(uniformIndex != null) _wglUniform1fN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x);
	}

	@Import(module = "platformOpenGL", name = "glUniform1f")
	public static native void _wglUniform1fN(WebGLUniformLocation uniformIndex, float x);

	public static void _wglUniform2f(IUniformGL uniformIndex, float x, float y) {
		if(uniformIndex != null) _wglUniform2fN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y);
	}

	@Import(module = "platformOpenGL", name = "glUniform2f")
	public static native void _wglUniform2fN(WebGLUniformLocation uniformIndex, float x, float y);

	public static void _wglUniform3f(IUniformGL uniformIndex, float x, float y, float z) {
		if(uniformIndex != null) _wglUniform3fN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y, z);
	}

	@Import(module = "platformOpenGL", name = "glUniform3f")
	public static native void _wglUniform3fN(WebGLUniformLocation uniformIndex, float x, float y, float z);

	public static void _wglUniform4f(IUniformGL uniformIndex, float x, float y, float z, float w) {
		if(uniformIndex != null) _wglUniform4fN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y, z, w);
	}

	@Import(module = "platformOpenGL", name = "glUniform4f")
	public static native void _wglUniform4fN(WebGLUniformLocation uniformIndex, float x, float y, float z, float w);

	public static void _wglUniform1i(IUniformGL uniformIndex, int x) {
		if(uniformIndex != null) _wglUniform1iN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x);
	}

	@Import(module = "platformOpenGL", name = "glUniform1i")
	public static native void _wglUniform1iN(WebGLUniformLocation uniformIndex, int x);

	public static void _wglUniform2i(IUniformGL uniformIndex, int x, int y) {
		if(uniformIndex != null) _wglUniform2iN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y);
	}

	@Import(module = "platformOpenGL", name = "glUniform2i")
	public static native void _wglUniform2iN(WebGLUniformLocation uniformIndex, int x, int y);

	public static void _wglUniform3i(IUniformGL uniformIndex, int x, int y, int z) {
		if(uniformIndex != null) _wglUniform3iN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y, z);
	}

	@Import(module = "platformOpenGL", name = "glUniform3i")
	public static native void _wglUniform3iN(WebGLUniformLocation uniformIndex, int x, int y, int z);

	public static void _wglUniform4i(IUniformGL uniformIndex, int x, int y, int z, int w) {
		if(uniformIndex != null) _wglUniform4iN(((OpenGLObjects.UniformGL)uniformIndex).ptr, x, y, z, w);
	}

	@Import(module = "platformOpenGL", name = "glUniform4i")
	public static native void _wglUniform4iN(WebGLUniformLocation uniformIndex, int x, int y, int z, int w);

	public static void _wglUniformMatrix2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix2fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix2fv")
	static native void _wglUniformMatrix2fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglUniformMatrix3fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix3fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix3fv")
	static native void _wglUniformMatrix3fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglUniformMatrix4fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix4fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix4fv")
	static native void _wglUniformMatrix4fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglUniformMatrix3x2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix3x2fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix3x2fv")
	static native void _wglUniformMatrix3x2fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglUniformMatrix4x2fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix4x2fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix4x2fv")
	static native void _wglUniformMatrix4x2fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglUniformMatrix4x3fv(IUniformGL uniformIndex, boolean transpose, FloatBuffer buffer) {
		if (uniformIndex != null)
			_wglUniformMatrix4x3fvN(((OpenGLObjects.UniformGL) uniformIndex).ptr, transpose,
					WASMGCBufferAllocator.getFloatBufferView(buffer));
	}

	@Import(module = "platformOpenGL", name = "glUniformMatrix4x3fv")
	static native void _wglUniformMatrix4x3fvN(WebGLUniformLocation uniformIndex, boolean transpose, Float32Array typedArray);

	public static void _wglBindFramebuffer(int target, IFramebufferGL framebuffer) {
		if(framebuffer == null) {
			framebuffer = WebGLBackBuffer.getBackBuffer();
		}
		_wglBindFramebufferN(target, ((OpenGLObjects.FramebufferGL)framebuffer).ptr);
	}

	public static void _wglBindFramebufferLow(int target, IFramebufferGL framebuffer) {
		_wglBindFramebufferN(target, framebuffer != null ? ((OpenGLObjects.FramebufferGL)framebuffer).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glBindFramebuffer")
	static native void _wglBindFramebufferN(int target, WebGLFramebuffer framebuffer);

	@Import(module = "platformOpenGL", name = "glCheckFramebufferStatus")
	public static native int _wglCheckFramebufferStatus(int target);

	@Import(module = "platformOpenGL", name = "glBlitFramebuffer")
	public static native void _wglBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0,
			int dstX1, int dstY1, int bits, int filter);

	@Import(module = "platformOpenGL", name = "glRenderbufferStorage")
	public static native void _wglRenderbufferStorage(int target, int internalformat, int width, int height);

	public static void _wglFramebufferTexture2D(int target, int attachment, int texTarget, ITextureGL texObj, int level) {
		_wglFramebufferTexture2DN(target, attachment, texTarget, ((OpenGLObjects.TextureGL)texObj).ptr, level);
	}

	@Import(module = "platformOpenGL", name = "glFramebufferTexture2D")
	static native void _wglFramebufferTexture2DN(int target, int attachment, int texTarget, WebGLTexture texObj, int level);

	public static void _wglFramebufferTextureLayer(int target, int attachment, ITextureGL texObj, int level, int layer) {
		_wglFramebufferTextureLayerN(target, attachment, ((OpenGLObjects.TextureGL)texObj).ptr, level, layer);
	}

	@Import(module = "platformOpenGL", name = "glFramebufferTextureLayer")
	static native void _wglFramebufferTextureLayerN(int target, int attachment, WebGLTexture texObj, int level, int layer);

	public static void _wglBindRenderbuffer(int target, IRenderbufferGL renderbuffer) {
		_wglBindRenderbufferN(target, renderbuffer != null ? ((OpenGLObjects.RenderbufferGL)renderbuffer).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glBindRenderbuffer")
	static native void _wglBindRenderbufferN(int target, WebGLRenderbuffer renderbuffer);

	public static void _wglFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
			IRenderbufferGL renderbufferId) {
		_wglFramebufferRenderbufferN(target, attachment, renderbufferTarget,
				renderbufferId != null ? ((OpenGLObjects.RenderbufferGL) renderbufferId).ptr : null);
	}

	@Import(module = "platformOpenGL", name = "glFramebufferRenderbuffer")
	static native void _wglFramebufferRenderbufferN(int target, int attachment, int renderbufferTarget, WebGLRenderbuffer renderbufferId);

	public static String _wglGetString(int param) {
		return BetterJSStringConverter.stringFromJS(_wglGetStringN(param));
	}

	@Import(module = "platformOpenGL", name = "glGetString")
	static native JSString _wglGetStringN(int param);

	@Import(module = "platformOpenGL", name = "glGetInteger")
	public static native int _wglGetInteger(int param);

	@Import(module = "platformOpenGL", name = "glGetError")
	public static native int _wglGetError();

	public static int checkOpenGLESVersion() {
		return glesVers;
	}

	public static boolean checkEXTGPUShader5Capable() {
		return (capABits & CAP_A_BIT_EXT_GPU_SHADER5) != 0;
	}

	public static boolean checkOESGPUShader5Capable() {
		return (capABits & CAP_A_BIT_OES_GPU_SHADER5) != 0;
	}

	public static boolean checkFBORenderMipmapCapable() {
		return (capABits & CAP_A_BIT_FBO_RENDER_MIPMAP) != 0;
	}

	public static boolean checkVAOCapable() {
		return vertexArrayImpl != VAO_IMPL_NONE;
	}

	public static boolean checkInstancingCapable() {
		return instancingImpl != INSTANCE_IMPL_NONE;
	}

	public static boolean checkTexStorageCapable() {
		return glesVers >= 300;
	}

	public static boolean checkTextureLODCapable() {
		return (capABits & CAP_A_BIT_TEXTURE_LOD_CAPABLE) != 0;
	}

	public static boolean checkNPOTCapable() {
		return (capABits & CAP_A_BIT_NPOT_CAPABLE) != 0;
	}

	public static boolean checkHDRFramebufferSupport(int bits) {
		switch(bits) {
		case 16:
			return (capABits & CAP_A_BIT_HDR_FBO16F) != 0;
		case 32:
			return (capABits & CAP_A_BIT_HDR_FBO32F) != 0;
		default:
			return false;
		}
	}

	public static boolean checkLinearHDRFilteringSupport(int bits) {
		switch(bits) {
		case 16:
			return (capBBits & CAP_B_BIT_HDR_LINEAR16F) != 0;
		case 32:
			return (capBBits & CAP_B_BIT_HDR_LINEAR32F) != 0;
		default:
			return false;
		}
	}

	// legacy
	public static boolean checkLinearHDR32FSupport() {
		return (capBBits & CAP_B_BIT_HDR_LINEAR32F) != 0;
	}

	public static boolean checkAnisotropicFilteringSupport() {
		return (capABits & CAP_A_BIT_ANISOTROPIC) != 0;
	}

	public static String[] getAllExtensions() {
		return BetterJSStringConverter.stringArrayFromJS(getAllExtensions0());
	}

	@Import(module = "platformOpenGL", name = "getAllExtensions")
	static native JSArray<JSString> getAllExtensions0();

	public static List<String> dumpActiveExtensions() {
		return Arrays.asList(BetterJSStringConverter.stringArrayFromJS(dumpActiveExtensions0()));
	}

	@Import(module = "platformOpenGL", name = "dumpActiveExtensions")
	static native JSArray<JSString> dumpActiveExtensions0();

	public static void enterVAOEmulationHook() {
		WebGLBackBuffer.enterVAOEmulationPhase();
	}

	// =============================================================================
	// Int-based GL entry points (_gl*), for Mojang's 1.19.4 blaze3d code.
	//
	// See the long comment on the same block in
	// src/platform-api/java/.../PlatformOpenGL.java for why this layer exists and
	// why org.lwjgl.opengl.GL* is not being re-created as source. In short:
	// com.mojang.blaze3d.platform.GlStateManager is unmodified decompiled code that
	// calls LWJGL, LWJGL is compileOnly and therefore absent to TeaVM, and
	// GlStateManager:158 hands a raw native pointer to GL so a faithful LWJGL
	// facade is not possible on WASM.
	//
	// Everything below is a thin translation from Mojang's integer object names to
	// the _wgl* handle layer above.
	// =============================================================================

	// -----------------------------------------------------------------------------
	// Integer-name registry.
	//
	// Desktop GL hands out GLuint names: glGenBuffers returns one, and the same
	// integer goes back to glBindBuffer/glBufferData/glDeleteBuffers. WebGL has no
	// integer names at all -- the bindings only hold object references -- so each
	// glGen* allocates the wrapper here and hands back an index into a dense
	// array.
	//
	// Index 0 is reserved and always resolves to null, which is what the GL spec
	// requires for "no object": GlStateManager unbinds with a literal 0 in
	// _glDeleteBuffers (GL32C.glBindBuffer(34962, 0)) and Minecraft rebinds
	// textures to 0 constantly. The _wgl* bind methods already accept null and
	// forward it, so resolving to null is all that is needed.
	//
	// Ids are never recycled. A 32-bit int buys four billion objects, and
	// Minecraft allocates a few hundred, so a free list would add a data structure
	// to save nothing.
	//
	// Each object kind gets its own registry rather than sharing one id space, so
	// that a buffer id and a texture id can never be confused for one another if
	// Mojang ever compares across kinds.
	// -----------------------------------------------------------------------------

	private static final class Registry<T> {
		private Object[] items = new Object[64];
		private int top = 1; // slot 0 is the reserved "no object"

		int add(T item) {
			if (top == items.length) {
				items = Arrays.copyOf(items, items.length << 1);
			}
			items[top] = item;
			return top++;
		}

		@SuppressWarnings("unchecked")
		T get(int id) {
			if (id <= 0 || id >= top) {
				return null;
			}
			return (T) items[id];
		}

		void remove(int id) {
			if (id > 0 && id < top) {
				items[id] = null;
			}
		}
	}

	private static final Registry<IBufferGL> bufferRegistry = new Registry<>();
	private static final Registry<ITextureGL> textureRegistry = new Registry<>();
	private static final Registry<IVertexArrayGL> vertexArrayRegistry = new Registry<>();
	private static final Registry<IProgramGL> programRegistry = new Registry<>();
	private static final Registry<IShaderGL> shaderRegistry = new Registry<>();
	private static final Registry<IFramebufferGL> framebufferRegistry = new Registry<>();
	private static final Registry<IRenderbufferGL> renderbufferRegistry = new Registry<>();
	private static final Registry<IUniformGL> uniformRegistry = new Registry<>();

	// State for the glBufferData/glMapBuffer/glUnmapBuffer staging path. See
	// _glMapBuffer for why this is needed at all.
	//
	// boundBufferIds: GL buffer target -> our integer name for the buffer bound
	// there. The GL spec makes the binding target-global, and _glMapBuffer has to
	// resolve "which buffer is this" without an object to ask.
	private static final HashMap<Integer, Integer> boundBufferIds = new HashMap<>();

	// Buffer name -> {size, usage} from the last glBufferData(target, size, usage).
	private static final HashMap<Integer, int[]> pendingSizing = new HashMap<>();

	// Buffer name -> the staging buffer a live glMapBuffer handed out.
	private static final HashMap<Integer, java.nio.ByteBuffer> pendingMapData = new HashMap<>();

	/**
	 * Resolves an object id that the caller believes must exist, for the calls
	 * where a null would turn into a confusing NullPointerException inside a
	 * WebGL binding rather than an error at the call site.
	 */
	private static IBufferGL requireBuffer(int id, String call) {
		IBufferGL b = bufferRegistry.get(id);
		if (b == null) {
			throw new IllegalArgumentException("gl" + call + ": " + id + " is not a buffer name");
		}
		return b;
	}

	private static IProgramGL requireProgram(int id, String call) {
		IProgramGL p = programRegistry.get(id);
		if (p == null) {
			throw new IllegalArgumentException("gl" + call + ": " + id + " is not a program name");
		}
		return p;
	}

	private static IShaderGL requireShader(int id, String call) {
		IShaderGL s = shaderRegistry.get(id);
		if (s == null) {
			throw new IllegalArgumentException("gl" + call + ": " + id + " is not a shader name");
		}
		return s;
	}

	private static IUniformGL requireUniform(int id, String call) {
		IUniformGL u = uniformRegistry.get(id);
		if (u == null) {
			throw new IllegalArgumentException("gl" + call + ": " + id
					+ " is not a uniform location. Note that glGetUniformLocation returns -1 for a"
					+ " name that the linked program does not use, and the desktop spec allows that"
					+ " to be passed back to glUniform* as a silent no-op.");
		}
		return u;
	}

	/**
	 * Validates a client-memory offset that desktop GL would treat as a pointer.
	 *
	 * <p>These arrive from Mojang as {@code long} because that is what LWJGL's
	 * signatures use, but on this target there is no client memory to address --
	 * the only thing behind the call is a WebGL buffer object. So the value is a
	 * byte offset into that buffer and the only meaningful thing to do with it is
	 * range-check it. Throwing is correct here rather than defensive: reaching
	 * one of these means the caller took a path that has no WebGL implementation.
	 */
	private static int checkOffset(long offset, String call) {
		if (offset < 0 || offset > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("gl" + call + ": byte offset " + offset
					+ " is not addressable on WASM, where there is no client memory to point into."
					+ " Only offsets into a bound WebGL buffer object are meaningful.");
		}
		return (int) offset;
	}

	// -----------------------------------------------------------------------------
	// java.nio -> lax1dude buffer bridge.
	//
	// The _gl* layer takes java.nio buffers because those are Mojang's signatures
	// (RenderSystem and BufferBuilder pass them) and the _wgl* layer takes
	// lax1dude ones because those are backed by a WASM Address. These helpers sit
	// between.
	//
	// They use the absolute bulk get/put (get(int index, byte[] dst, int off, int
	// len)) rather than the relative ones, so the source buffer's position is NOT
	// advanced. That matters: LWJGL hands GL the buffer's memory region and leaves
	// position alone, and Mojang reuses these buffers afterwards -- advancing
	// position would silently corrupt the next upload.
	//
	// The read is an intermediate byte[]/int[]/float[] copy rather than a direct
	// view. That is one extra copy, but these are upload paths that WebGL is about
	// to copy again anyway, and a direct view would mean exposing WASM linear
	// memory as a java.nio buffer, which is not something this class can do.
	// -----------------------------------------------------------------------------

	private static ByteBuffer toEaglerByte(java.nio.ByteBuffer src) {
		int len = src.remaining();
		ByteBuffer dst = WASMGCBufferAllocator.allocateByteBuffer(len);
		if (len > 0) {
			byte[] tmp = new byte[len];
			src.get(src.position(), tmp, 0, len);
			dst.put(tmp, 0, len);
			// put() advanced position to len, which would make remaining() zero and
			// point getByteBufferViewIndex() one past the data; callers upload using
			// position()/remaining(), so flip back to [0, len).
			dst.rewind();
		}
		return dst;
	}

	private static void fromEaglerByte(java.nio.ByteBuffer dst, ByteBuffer src) {
		int len = dst.remaining();
		if (len > 0) {
			byte[] tmp = new byte[len];
			src.get(tmp, 0, len);
			dst.put(dst.position(), tmp, 0, len);
		}
	}

	private static IntBuffer toEaglerInt(java.nio.IntBuffer src) {
		int len = src.remaining();
		IntBuffer dst = WASMGCBufferAllocator.allocateIntBuffer(len);
		if (len > 0) {
			int[] tmp = new int[len];
			src.get(src.position(), tmp, 0, len);
			dst.put(tmp, 0, len);
			dst.rewind();
		}
		return dst;
	}

	// -----------------------------------------------------------------------------
	// GL11
	// -----------------------------------------------------------------------------

	public static void _glEnable(int cap) {
		_wglEnable(cap);
	}

	public static void _glDisable(int cap) {
		_wglDisable(cap);
	}

	public static void _glDepthFunc(int func) {
		_wglDepthFunc(func);
	}

	public static void _glDepthMask(boolean flag) {
		_wglDepthMask(flag);
	}

	public static void _glBlendFunc(int src, int dst) {
		_wglBlendFunc(src, dst);
	}

	public static void _glPolygonOffset(float factor, float units) {
		_wglPolygonOffset(factor, units);
	}

	public static void _glPolygonMode(int face, int mode) {
		throw new UnsupportedOperationException("glPolygonMode: fixed-function face culling has no"
				+ " WebGL equivalent. GlStateManager._polygonMode is only reached when the caller"
				+ " asks for a wireframe or a non-solid fill mode, which vanilla 1.19.4 never does.");
	}

	public static void _glLogicOp(int op) {
		// PORT NOTE (Eag19 / TeaVM WASM-GC): WebGL 2 has no glLogicOp state. The only
		// callers in vanilla 1.19.4 are the OR_REVERSE text-selection tint in EditBox,
		// MultiLineEditBox, BookEditScreen and AbstractSignEditScreen. Throwing here
		// hard-crashed the client the moment any of them had a selection, so treat it
		// as a no-op; EditBox additionally switches to a translucent fill so the
		// selected glyphs stay readable instead of being covered by an opaque block.
	}

	public static void _glTexParameterf(int target, int pname, float param) {
		// PORT NOTE (Eag19 / TeaVM WASM-GC): GL_TEXTURE_LOD_BIAS (0x8501) is a
		// desktop-only texture parameter - it is not in OpenGL ES 3.0 / WebGL2, so
		// forwarding it raises "texParameter: invalid parameter name" for every
		// texture prepared. Vanilla only ever sets it to 0 (TextureUtil.prepareImage),
		// and zero is the default, so dropping it is a no-op rather than a lost
		// feature. (The 2D LOD parameters ES 3.00 does have, GL_TEXTURE_MIN_LOD 0x813A
		// and GL_TEXTURE_MAX_LOD 0x813B, are forwarded normally.)
		if (pname == 34049) {
			return;
		}
		_wglTexParameterf(target, pname, param);
	}

	public static void _glTexParameteri(int target, int pname, int param) {
		_wglTexParameteri(target, pname, param);
	}

	public static int _glGetTexLevelParameteri(int target, int level, int pname) {
		throw new UnsupportedOperationException("glGetTexLevelParameteri: WebGL cannot query texture"
				+ " storage back. GlStateManager._getTexLevelParameter is only used to read texture"
				+ " size and format, which Eaglercraft already tracks per texture instead.");
	}

	public static int _glGenTextures() {
		return textureRegistry.add(_wglGenTextures());
	}

	public static void _glGenTextures(int[] textures) {
		for (int i = 0; i < textures.length; i++) {
			textures[i] = _glGenTextures();
		}
	}

	public static void _glDeleteTextures(int texture) {
		if (texture == 0) {
			return; // glDeleteTextures(0) is explicitly a no-op in the GL spec
		}
		ITextureGL tex = textureRegistry.get(texture);
		if (tex != null) {
			_wglDeleteTextures(tex);
			textureRegistry.remove(texture);
		}
	}

	public static void _glDeleteTextures(int[] textures) {
		for (int i = 0; i < textures.length; i++) {
			_glDeleteTextures(textures[i]);
		}
	}

	public static void _glBindTexture(int target, int texture) {
		_wglBindTexture(target, textureRegistry.get(texture));
	}

	public static void _glTexImage2D(int target, int level, int internalformat, int width, int height,
			int border, int format, int type, java.nio.IntBuffer pixels) {
		// PORT NOTE (Eag19 / TeaVM WASM-GC)
		//
		// WebGL2/ES3 rejects the *unsized* GL_DEPTH_COMPONENT (0x1902) internal
		// format that desktop GL 3.2 accepts: texImage2D logs
		// "INVALID_VALUE: invalid internalformat" and leaves the texture incomplete,
		// which then fails framebuffer completeness with
		// GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT. Minecraft asks for exactly this on
		// both of its depth-texture allocations - MainTarget.allocateDepthAttachment
		// (MainTarget.java:88) and RenderTarget.createDepthTexture
		// (RenderTarget.java:105) - always as (internalformat=DEPTH_COMPONENT,
		// format=DEPTH_COMPONENT, type=FLOAT, pixels=null). ES3 has no unsized depth
		// format, so promote the request to the sized DEPTH_COMPONENT24 and the
		// integer type that format allows. Only the allocation is requested (no
		// pixels), so the type change is validation-only and the texture is still a
		// plain renderable depth buffer. Eaglercraft's own engine already uses sized
		// depth formats (GameOverlayFramebuffer uses DEPTH_COMPONENT16), so this
		// matches the rest of the port.
		if (internalformat == 6402 /* GL_DEPTH_COMPONENT */) {
			internalformat = 33190; /* GL_DEPTH_COMPONENT24 */
			type = 5125; /* GL_UNSIGNED_INT */
		}
		if (pixels == null) {
			_wglTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer) null);
		} else {
			_wglTexImage2D(target, level, internalformat, width, height, border, format, type,
					toEaglerInt(pixels));
		}
	}

	public static void _glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, long offset) {
		throw new UnsupportedOperationException("glTexSubImage2D with a client-memory offset ("
				+ offset + "): WebGL has no client memory to sub-image from. GlStateManager._texSubImage2D"
				+ " reaches this form; the buffer-backed form is what texture atlases actually use.");
	}

	public static void _glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, ByteBuffer pixels) {
		// Straight delegation: _wglTexSubImage2D's ByteBuffer form already resolves the
		// WASM-GC buffer to an ArrayBufferView, which is exactly what WebGL wants. Any
		// UNPACK_* pixel-store state the caller set is honoured by the GL side, so no
		// row-length or skip handling belongs here.
		_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}

	/**
	 * java.nio.ByteBuffer form of {@code glTexSubImage2D}, declared by the
	 * platform-api signature this class implements.
	 *
	 * <p>{@link NativeImage#upload} reaches this one: its pixel storage is a plain
	 * {@code byte[]} that predates this port's raw-memory buffer type, and it wraps a
	 * slice of that array in a {@code java.nio.ByteBuffer}. A {@code java.nio} buffer is
	 * not a WebGL ArrayBufferView, and under WASM-GC a Java array has no address the JS
	 * side could view, so the bytes are staged into raw memory and the view is made over
	 * that. The copy is per-upload, not per-frame: NativeImage uploads a texture once
	 * when it is created or dirtied.
	 */
	public static void _glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, int type, java.nio.ByteBuffer pixels) {
		if (pixels == null) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ByteBuffer) null);
			return;
		}

		if (!pixels.hasArray()) {
			// A direct java.nio.ByteBuffer would have no reachable storage at all here, so
			// this is a real limitation rather than an unimplemented convenience.
			throw new UnsupportedOperationException(
					"glTexSubImage2D from a direct java.nio.ByteBuffer: under WASM-GC there is no "
							+ "off-heap ByteBuffer to hand WebGL. NativeImage uploads from a byte[], which "
							+ "hasArray() reports.");
		}

		int offset = pixels.arrayOffset() + pixels.position();
		int length = pixels.remaining();
		if (length <= 0) {
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ByteBuffer) null);
			return;
		}

		Address staged = WASMGCBufferAllocator.malloc(length);
		try {
			WASMGCDirectArrayCopy.memcpy(staged, pixels.array(), offset, length);
			_wglTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
					WASMGCBufferAllocator.getUnsignedByteBufferView0(staged, length));
		} finally {
			WASMGCBufferAllocator.free(staged);
		}
	}

	public static void _glGetTexImage(int target, int level, int format, int type, long offset) {
		throw new UnsupportedOperationException("glGetTexImage: WebGL cannot read texture contents back"
				+ " into client memory. Used by GlStateManager._getTexImage for screenshot and"
				+ " texture-dump paths.");
	}

	public static void _glViewport(int x, int y, int width, int height) {
		_wglViewport(x, y, width, height);
	}

	public static void _glColorMask(boolean r, boolean g, boolean b, boolean a) {
		_wglColorMask(r, g, b, a);
	}

	public static void _glStencilFunc(int func, int ref, int mask) {
		throw new UnsupportedOperationException("glStencilFunc: WebGL has no stencil buffer."
				+ " GlStateManager._stencilFunc is on the render path but vanilla 1.19.4 never draws"
				+ " with a stencil test enabled.");
	}

	public static void _glStencilMask(int mask) {
		throw new UnsupportedOperationException("glStencilMask: WebGL has no stencil buffer.");
	}

	public static void _glStencilOp(int sfail, int dpfail, int dppass) {
		throw new UnsupportedOperationException("glStencilOp: WebGL has no stencil buffer.");
	}

	public static void _glClearStencil(int s) {
		throw new UnsupportedOperationException("glClearStencil: WebGL has no stencil bit in glClear.");
	}

	/**
	 * Desktop GL takes a double depth; WebGL only has glClearDepth with a float.
	 *
	 * <p>Narrowing double to float is safe for every value a depth clear can
	 * meaningfully hold. The default is 1.0, and Minecraft only ever writes depths
	 * near the ends of the [0, 1] range, where float has ~1e-7 resolution. The one
	 * value that would round badly is something halfway between two floats, which
	 * no depth test cares about because it is compared against a depth buffer that
	 * holds no more precision than the test does.
	 */
	public static void _glClearDepth(double depth) {
		_wglClearDepth((float) depth);
	}

	public static void _glClearColor(float r, float g, float b, float a) {
		_wglClearColor(r, g, b, a);
	}

	public static void _glClear(int mask) {
		// WebGL's glClear takes the same bit constants as desktop GL (COLOR_BUFFER_BIT
		// 0x4000, DEPTH_BUFFER_BIT 0x100, STENCIL_BUFFER_BIT 0x400). There is no stencil
		// buffer to clear, but passing the bit through unchanged is harmless: WebGL
		// ignores bits it does not know. Masking it out here instead would hide the
		// caller's intent, and clearing is a cheap no-op when the bit is absent.
		_wglClear(mask);
	}

	public static void _glDrawPixels(int width, int height, int format, int type, long offset) {
		throw new UnsupportedOperationException("glDrawPixels: fixed-function rasterisation has no"
				+ " WebGL equivalent. GlStateManager._glDrawPixels is the legacy immediate-mode"
				+ " path, which 1.19.4 does not use.");
	}

	public static void _glDrawElements(int mode, int count, int type, long offset) {
		_wglDrawElements(mode, count, type, checkOffset(offset, "DrawElements"));
	}

	public static void _glPixelStorei(int pname, int value) {
		_wglPixelStorei(pname, value);
	}

	public static void _glReadPixels(int x, int y, int width, int height, int format, int type,
			java.nio.ByteBuffer pixels) {
		int len = pixels.remaining();
		ByteBuffer tmp = WASMGCBufferAllocator.allocateByteBuffer(len);
		_wglReadPixels(x, y, width, height, format, type, tmp);
		fromEaglerByte(pixels, tmp);
		WASMGCBufferAllocator.freeByteBuffer(tmp);
	}

	public static void _glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
		throw new UnsupportedOperationException("glReadPixels into client memory at offset " + offset
				+ ": WebGL has no client memory to read into. Use the buffer overload.");
	}

	public static int _glGetError() {
		return _wglGetError();
	}

	public static String _glGetString(int name) {
		return _wglGetString(name);
	}

	public static int _glGetInteger(int pname) {
		return _wglGetInteger(pname);
	}

	// -----------------------------------------------------------------------------
	// GL13 / GL14
	// -----------------------------------------------------------------------------

	public static void _glActiveTexture(int texture) {
		_wglActiveTexture(texture);
	}

	public static void _glBlendEquation(int mode) {
		_wglBlendEquation(mode);
	}

	public static void _glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
		_wglBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
	}

	// -----------------------------------------------------------------------------
	// GL15 / GL32C
	// -----------------------------------------------------------------------------

	public static int _glGenBuffers() {
		return bufferRegistry.add(_wglGenBuffers());
	}

	public static void _glBindBuffer(int target, int buffer) {
		boundBufferIds.put(target, buffer);
		_wglBindBuffer(target, bufferRegistry.get(buffer));
	}

	public static void _glBufferData(int target, java.nio.ByteBuffer data, int usage) {
		ByteBuffer eagler = toEaglerByte(data);
		_wglBufferData(target, eagler, usage);
		WASMGCBufferAllocator.freeByteBuffer(eagler);
	}

	public static void _glBufferData(int target, long size, int usage) {
		// This is the "allocate without data" form -- glBufferData(target, size,
		// usage). RenderSystem$AutoStorageIndexBuffer uses it to reserve index
		// storage, so the long really is a size, not an address.
		if (size < 0 || size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("glBufferData: size " + size + " out of range");
		}
		_wglBufferData(target, (int) size, usage);
		// Remember the size for a following glMapBuffer, which has no way to ask
		// WebGL how big the buffer is.
		Integer id = boundBufferIds.get(target);
		if (id != null) {
			pendingSizing.put(id, new int[] { (int) size, usage });
		}
	}

	/**
	 * Hands back a staging buffer to write indices into, for the
	 * glMapBuffer/glUnmapBuffer pair that {@code RenderSystem.AutoStorageIndexBuffer.ensureStorage}
	 * uses to grow its index buffer.
	 *
	 * <p>WebGL has no client-side buffer mapping, so there is nothing to hand back
	 * and this cannot be a real map. What it can be is a buffer of the right size
	 * that {@link #_glUnmapBuffer} uploads: the caller sizes the GL buffer with
	 * glBufferData immediately before mapping it, writes the bytes it wants
	 * stored through an {@code IntConsumer}, then unmaps. Staging the writes and
	 * flushing them on unmap preserves the observable behaviour, because the
	 * whole point of the map is to avoid a CPU-side copy of data that is about to
	 * be uploaded anyway -- and WebGL is going to copy it regardless.
	 *
	 * <p>Returning null where desktop GL would return NULL is deliberate and
	 * load-bearing: the caller checks for null and raises
	 * {@code RuntimeException("Failed to map GL buffer")}, so "no buffer is bound"
	 * and "this buffer was never sized" surface as a loud error rather than a
	 * silent zero-length upload.
	 */
	public static java.nio.ByteBuffer _glMapBuffer(int target, int access) {
		Integer id = boundBufferIds.get(target);
		if (id == null) {
			return null;
		}
		int[] sizing = pendingSizing.get(id);
		if (sizing == null) {
			return null;
		}
		// nativeOrder() to match the desktop GL client-memory the caller believes it is writing
		// into: the bytes are uploaded verbatim by _glUnmapBuffer, and WebGL reads them natively.
		java.nio.ByteBuffer staging = java.nio.ByteBuffer.allocateDirect(sizing[0])
				.order(java.nio.ByteOrder.nativeOrder());
		pendingMapData.put(id, staging);
		return staging;
	}

	/**
	 * Uploads whatever {@link #_glMapBuffer} handed out.
	 *
	 * <p>The staged bytes replace the whole buffer rather than being appended,
	 * which is what desktop GL does too: the caller wrote every index from 0
	 * upwards and sized the buffer to hold them, so the written range and the
	 * buffer's extent are the same thing. flip() first, because the caller advanced
	 * position while writing and the converter reads from position to limit.
	 */
	public static boolean _glUnmapBuffer(int target) {
		Integer id = boundBufferIds.get(target);
		if (id == null) {
			return false;
		}
		java.nio.ByteBuffer staging = pendingMapData.remove(id);
		int[] sizing = pendingSizing.get(id);
		if (staging == null || sizing == null) {
			return false;
		}
		staging.flip();
		ByteBuffer eagler = toEaglerByte(staging);
		_wglBufferData(target, eagler, sizing[1]);
		WASMGCBufferAllocator.freeByteBuffer(eagler);
		return true;
	}

	public static void _glDeleteBuffers(int buffer) {
		if (buffer == 0) {
			return;
		}
		IBufferGL buf = bufferRegistry.get(buffer);
		if (buf != null) {
			_wglDeleteBuffers(buf);
			bufferRegistry.remove(buffer);
		}
	}

	// -----------------------------------------------------------------------------
	// GL20
	// -----------------------------------------------------------------------------

	public static void _glScissor(int x, int y, int width, int height) {
		_wglScissor(x, y, width, height);
	}

	public static int _glGetProgrami(int program, int pname) {
		return _wglGetProgrami(requireProgram(program, "GetProgrami"), pname);
	}

	public static void _glAttachShader(int program, int shader) {
		_wglAttachShader(requireProgram(program, "AttachShader"), requireShader(shader, "AttachShader"));
	}

	public static void _glDeleteShader(int shader) {
		if (shader == 0) {
			return;
		}
		IShaderGL s = shaderRegistry.get(shader);
		if (s != null) {
			_wglDeleteShader(s);
			shaderRegistry.remove(shader);
		}
	}

	public static int _glCreateShader(int type) {
		return shaderRegistry.add(_wglCreateShader(type));
	}

	public static void _glShaderSource(int shader, String source) {
		_wglShaderSource(requireShader(shader, "ShaderSource"), translateVanillaGlsl(source));
	}

	/**
	 * PORT NOTE (Eag19 / TeaVM WASM-GC)
	 *
	 * <p>This is the Blaze3D shader path only. {@code _glShaderSource} has exactly one caller -
	 * {@code GlStateManager.glShaderSource} - which in turn is called only by
	 * {@code Program.compileShaderInternal}, i.e. it compiles the vanilla
	 * {@code assets/minecraft/shaders/core/*.vsh|*.fsh}. Eaglercraft's own shaders never come
	 * through here; they call {@code _wglShaderSource} directly with a header built by
	 * {@code GLSLHeader} (and the GLSL 300/310 ES header it prepends).
	 *
	 * <p>Vanilla 1.19.4 ships desktop GLSL {@code "#version 150"} (all 157 directives in
	 * assets.epk), but the browser context is WebGL 2.0, which only accepts GLSL ES
	 * {@code "#version 300 es"} / {@code "#version 100"}. Feeding it 150 makes
	 * {@code ctx.compileShader} fail, and because {@code Program.compileShaderInternal} checks
	 * the compile status and throws, the very first shader the game compiles -
	 * {@code blit_screen} via {@code GameRenderer.preloadUiShader} - aborts the constructor.
	 *
	 * <p>The vanilla shaders are already modern (they use {@code in}/{@code out},
	 * {@code texture()}, no {@code varying}/{@code attribute}/{@code gl_FragColor}), so the only
	 * translation needed is the version directive plus the fragment default precision that ES
	 * 3.00 requires (desktop 150 has implicit highp). {@code #moj_import} has already been
	 * inlined by {@code GlslPreprocessor} before the source reaches here.
	 *
	 * <p>One exception, found at boot: desktop GLSL 1.50 performs implicit int -&gt; float
	 * <em>vector</em> conversions in arithmetic (GLSL 1.50 spec 4.1.10 "Implicit Conversions":
	 * {@code int -> float}, {@code ivecN -> vecN}), whereas GLSL ES 3.00 removed them (spec 5.9
	 * requires both operands of {@code /} to have the same base type). Vanilla's
	 * {@code shaders/include/light.glsl} relies on it:
	 *
	 * <pre>vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
	 *     return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
	 * }</pre>
	 *
	 * {@code uv} is {@code ivec2}, so desktop accepts {@code ivec2 / float} but ES 3.00 rejects
	 * it ({@code "'/' : wrong operand types ... left-hand operand of type 'in highp 2-component
	 * vector of int' and a right operand of type 'const float'"}, plus the knock-on
	 * {@code clamp()}/{@code texture()}/{@code return} overload failures). Every vanilla shader
	 * that samples the lightmap includes {@code light.glsl}, so the first such shader compiled
	 * ({@code rendertype_solid}) aborts the whole shader reload. Make that one conversion
	 * explicit; {@code vec2(uv)} is also a valid no-op on a {@code vec2}, so this is safe
	 * regardless of the argument's type. All the other vanilla int-vector arithmetic is
	 * {@code ivec2 / int} (e.g. {@code UV2 / 16}), which ES 3.00 does allow.
	 *
	 * <p>The same implicit conversion appears as a plain assignment in
	 * {@code rendertype_entity_translucent_cull.vsh} / {@code rendertype_item_entity_translucent_cull.vsh},
	 * which declare {@code in ivec2 UV2;} but copy it into {@code out vec2 texCoord2;}: desktop
	 * accepts {@code texCoord2 = UV2} (ivec2 -> vec2), ES 3.00 rejects it ("'assign' : cannot
	 * convert from 'in highp 2-component vector of int' to 'out highp 2-component vector of
	 * float'"). Those two are the only core shaders that both declare {@code UV2} as
	 * {@code ivec2} and assign it to an output; every other {@code texCoord2 = UV2;} shader
	 * already declares {@code UV2} as {@code vec2}. {@code vec2(UV2)} is a valid no-op on a
	 * {@code vec2}, so the rewrite is safe either way.
	 *
	 * <p>Both rewrites were chosen by compiling all 116 core shaders offline against the same
	 * WebGL2 GLSL ES 3.00 compiler the game uses (see {@code scripts/validate-shaders.sh});
	 * the set is complete, i.e. no shader needs any other desktop-to-ES fix.
	 */
	private static String translateVanillaGlsl(String source) {
		int var1 = source.indexOf("#version");
		if (var1 < 0) {
			return source;
		}
		int var2 = source.indexOf('\n', var1);
		if (var2 < 0) {
			var2 = source.length();
		}
		String body = source.substring(var2)
				.replace("uv / 256.0", "vec2(uv) / 256.0")
				.replace("texCoord2 = UV2;", "texCoord2 = vec2(UV2);");
		return source.substring(0, var1)
				+ "#version 300 es\nprecision highp float;\nprecision highp int;"
				+ body;
	}

	public static void _glCompileShader(int shader) {
		_wglCompileShader(requireShader(shader, "CompileShader"));
	}

	public static int _glGetShaderi(int shader, int pname) {
		return _wglGetShaderi(requireShader(shader, "GetShaderi"), pname);
	}

	public static void _glUseProgram(int program) {
		_wglUseProgram(programRegistry.get(program));
	}

	public static int _glCreateProgram() {
		return programRegistry.add(_wglCreateProgram());
	}

	public static void _glDeleteProgram(int program) {
		if (program == 0) {
			return;
		}
		IProgramGL p = programRegistry.get(program);
		if (p != null) {
			_wglDeleteProgram(p);
			programRegistry.remove(program);
		}
	}

	public static void _glLinkProgram(int program) {
		_wglLinkProgram(requireProgram(program, "LinkProgram"));
	}

	/**
	 * Returns a registry id for the uniform location.
	 *
	 * <p>glGetUniformLocation returns -1 when the name is not an active uniform of
	 * the linked program, and the GL spec requires that -1 to be silently ignored by
	 * every glUniform* call rather than treated as an error. So -1 is passed through
	 * as -1 here, and {@link #requireUniform} maps it to null, which the _wgl*
	 * uniform setters already handle by skipping the write. Anything else that fails
	 * to resolve is a real bug and does throw.
	 */
	public static int _glGetUniformLocation(int program, CharSequence name) {
		IUniformGL loc = _wglGetUniformLocation(requireProgram(program, "GetUniformLocation"), name.toString());
		return loc == null ? -1 : uniformRegistry.add(loc);
	}

	public static void _glUniform1i(int location, int v0) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform1i(u, v0);
		}
	}

	public static void _glUniform1f(int location, float v0) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform1f(u, v0);
		}
	}

	public static void _glUniform2i(int location, int v0, int v1) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform2i(u, v0, v1);
		}
	}

	public static void _glUniform2f(int location, float v0, float v1) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform2f(u, v0, v1);
		}
	}

	public static void _glUniform3i(int location, int v0, int v1, int v2) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform3i(u, v0, v1, v2);
		}
	}

	public static void _glUniform3f(int location, float v0, float v1, float v2) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform3f(u, v0, v1, v2);
		}
	}

	public static void _glUniform4i(int location, int v0, int v1, int v2, int v3) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform4i(u, v0, v1, v2, v3);
		}
	}

	public static void _glUniform4f(int location, float v0, float v1, float v2, float v3) {
		IUniformGL u = uniformRegistry.get(location);
		if (u != null) {
			_wglUniform4f(u, v0, v1, v2, v3);
		}
	}

	// The uniform *array* forms. PlatformOpenGL only ever had the scalar
	// setters, so each of these walks the buffer and issues one scalar call per
	// element. That matches what glUniform*v does anyway -- the array is just
	// consecutive scalar stores -- so this is not an approximation, just a
	// different spelling of the same work. The one difference from desktop GL is
	// the number of JS calls, which is not a correctness concern but is worth
	// knowing if these ever show up in a profile.
	//
	// count is read from the buffer's remaining() exactly as desktop GL reads it
	// from the pointer/length pair Mojang's LWJGL overload supplies, and the
	// source position is not advanced.

	public static void _glUniform1iv(int location, java.nio.IntBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining();
		int base = value.position();
		for (int i = 0; i < n; i++) {
			_wglUniform1i(u, value.get(base + i));
		}
	}

	public static void _glUniform1fv(int location, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining();
		int base = value.position();
		for (int i = 0; i < n; i++) {
			_wglUniform1f(u, value.get(base + i));
		}
	}

	public static void _glUniform2iv(int location, java.nio.IntBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 2;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			_wglUniform2i(u, value.get(base + i * 2), value.get(base + i * 2 + 1));
		}
	}

	public static void _glUniform2fv(int location, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 2;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			_wglUniform2f(u, value.get(base + i * 2), value.get(base + i * 2 + 1));
		}
	}

	public static void _glUniform3iv(int location, java.nio.IntBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 3;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			int o = base + i * 3;
			_wglUniform3i(u, value.get(o), value.get(o + 1), value.get(o + 2));
		}
	}

	public static void _glUniform3fv(int location, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 3;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			int o = base + i * 3;
			_wglUniform3f(u, value.get(o), value.get(o + 1), value.get(o + 2));
		}
	}

	public static void _glUniform4iv(int location, java.nio.IntBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 4;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			int o = base + i * 4;
			_wglUniform4i(u, value.get(o), value.get(o + 1), value.get(o + 2), value.get(o + 3));
		}
	}

	public static void _glUniform4fv(int location, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int n = value.remaining() / 4;
		int base = value.position();
		for (int i = 0; i < n; i++) {
			int o = base + i * 4;
			_wglUniform4f(u, value.get(o), value.get(o + 1), value.get(o + 2), value.get(o + 3));
		}
	}

	// The matrix forms already exist on the _wgl* layer taking a lax1dude
	// FloatBuffer, so these convert and delegate rather than looping.

	public static void _glUniformMatrix2fv(int location, boolean transpose, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int len = value.remaining();
		FloatBuffer eagler = WASMGCBufferAllocator.allocateFloatBuffer(len);
		float[] tmp = new float[len];
		if (len > 0) {
			value.get(value.position(), tmp, 0, len);
			eagler.put(tmp, 0, len);
		}
		// PORT NOTE (Eag19 / TeaVM WASM-GC): put() leaves position == len, so
		// getFloatBufferView() would build a zero-length Float32Array and WebGL would
		// reject the upload with "uniformMatrix2fv: no array". Rewind so the view
		// covers the data that was just written.
		eagler.position(0);
		_wglUniformMatrix2fv(u, transpose, eagler);
		WASMGCBufferAllocator.freeFloatBuffer(eagler);
	}

	public static void _glUniformMatrix3fv(int location, boolean transpose, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int len = value.remaining();
		FloatBuffer eagler = WASMGCBufferAllocator.allocateFloatBuffer(len);
		float[] tmp = new float[len];
		if (len > 0) {
			value.get(value.position(), tmp, 0, len);
			eagler.put(tmp, 0, len);
		}
		// PORT NOTE (Eag19 / TeaVM WASM-GC): put() leaves position == len, so
		// getFloatBufferView() would build a zero-length Float32Array and WebGL would
		// reject the upload with "uniformMatrix3fv: no array". Rewind so the view
		// covers the data that was just written.
		eagler.position(0);
		_wglUniformMatrix3fv(u, transpose, eagler);
		WASMGCBufferAllocator.freeFloatBuffer(eagler);
	}

	public static void _glUniformMatrix4fv(int location, boolean transpose, java.nio.FloatBuffer value) {
		IUniformGL u = uniformRegistry.get(location);
		if (u == null) {
			return;
		}
		int len = value.remaining();
		FloatBuffer eagler = WASMGCBufferAllocator.allocateFloatBuffer(len);
		float[] tmp = new float[len];
		if (len > 0) {
			value.get(value.position(), tmp, 0, len);
			eagler.put(tmp, 0, len);
		}
		// PORT NOTE (Eag19 / TeaVM WASM-GC): put() leaves position == len, so
		// getFloatBufferView() would build a zero-length Float32Array and WebGL would
		// reject the upload with "uniformMatrix4fv: no array". Rewind so the view
		// covers the data that was just written. This floods every frame because
		// every matrix uniform (ModelViewMat, ProjMat, ...) goes through here.
		eagler.position(0);
		_wglUniformMatrix4fv(u, transpose, eagler);
		WASMGCBufferAllocator.freeFloatBuffer(eagler);
	}

	public static int _glGetAttribLocation(int program, CharSequence name) {
		return _wglGetAttribLocation(requireProgram(program, "GetAttribLocation"), name.toString());
	}

	public static void _glBindAttribLocation(int program, int index, CharSequence name) {
		_wglBindAttribLocation(requireProgram(program, "BindAttribLocation"), index, name.toString());
	}

	public static void _glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x,
			int y, int width, int height) {
		_wglCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	}

	public static void _glEnableVertexAttribArray(int index) {
		_wglEnableVertexAttribArray(index);
	}

	public static void _glDisableVertexAttribArray(int index) {
		_wglDisableVertexAttribArray(index);
	}

	public static void _glVertexAttribPointer(int index, int size, int type, boolean normalized,
			int stride, long offset) {
		_wglVertexAttribPointer(index, size, type, normalized, stride, checkOffset(offset, "VertexAttribPointer"));
	}

	// maxLength is ignored: WebGL's getShaderInfoLog has no length parameter and
	// returns the whole log. Truncating to maxLength would be harmless but Mojang
	// passes Int.MAX_VALUE here anyway, so there is nothing to honour.

	public static String _glGetShaderInfoLog(int shader, int maxLength) {
		return _wglGetShaderInfoLog(requireShader(shader, "GetShaderInfoLog"));
	}

	public static String _glGetProgramInfoLog(int program, int maxLength) {
		return _wglGetProgramInfoLog(requireProgram(program, "GetProgramInfoLog"));
	}

	// -----------------------------------------------------------------------------
	// GL30
	// -----------------------------------------------------------------------------

	public static int _glGenVertexArrays() {
		return vertexArrayRegistry.add(_wglGenVertexArrays());
	}

	public static void _glBindVertexArray(int array) {
		_wglBindVertexArray(vertexArrayRegistry.get(array));
	}

	public static void _glDeleteVertexArrays(int array) {
		if (array == 0) {
			return;
		}
		IVertexArrayGL vao = vertexArrayRegistry.get(array);
		if (vao != null) {
			_wglDeleteVertexArrays(vao);
			vertexArrayRegistry.remove(array);
		}
	}

	public static void _glBindFramebuffer(int target, int framebuffer) {
		// _wglBindFramebuffer maps null to the back buffer, which is exactly the
		// GL 3 behaviour of binding 0, so the registry's reserved 0 needs no
		// special case here.
		_wglBindFramebuffer(target, framebufferRegistry.get(framebuffer));
	}

	public static void _glDeleteFramebuffers(int framebuffer) {
		if (framebuffer == 0) {
			return;
		}
		IFramebufferGL fbo = framebufferRegistry.get(framebuffer);
		if (fbo != null) {
			_wglDeleteFramebuffer(fbo);
			framebufferRegistry.remove(framebuffer);
		}
	}

	public static int _glGenFramebuffers() {
		return framebufferRegistry.add(_wglCreateFramebuffer());
	}

	public static void _glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
			int dstY0, int dstX1, int dstY1, int mask, int filter) {
		_wglBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
	}

	public static void _glBindRenderbuffer(int target, int renderbuffer) {
		_wglBindRenderbuffer(target, renderbufferRegistry.get(renderbuffer));
	}

	public static void _glDeleteRenderbuffers(int renderbuffer) {
		if (renderbuffer == 0) {
			return;
		}
		IRenderbufferGL rbo = renderbufferRegistry.get(renderbuffer);
		if (rbo != null) {
			_wglDeleteRenderbuffer(rbo);
			renderbufferRegistry.remove(renderbuffer);
		}
	}

	public static int _glGenRenderbuffers() {
		return renderbufferRegistry.add(_wglCreateRenderbuffer());
	}

	public static void _glRenderbufferStorage(int target, int internalformat, int width, int height) {
		_wglRenderbufferStorage(target, internalformat, width, height);
	}

	public static void _glFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
			int renderbuffer) {
		_wglFramebufferRenderbuffer(target, attachment, renderbufferTarget,
				renderbufferRegistry.get(renderbuffer));
	}

	public static int _glCheckFramebufferStatus(int target) {
		return _wglCheckFramebufferStatus(target);
	}

	public static void _glFramebufferTexture2D(int target, int attachment, int textarget, int texture,
			int level) {
		_wglFramebufferTexture2D(target, attachment, textarget, textureRegistry.get(texture), level);
	}

	/**
	 * PORT NOTE (Eag19 / TeaVM WASM-GC)
	 *
	 * <p>Vanilla 1.19.4 binds integer-format vertex attributes (the {@code GL_SHORT}
	 * lightmap UV2 element of {@code POSITION_COLOR_TEX_LIGHTMAP} and friends) with
	 * {@code glVertexAttribIPointer}. That entry point is core in GLSL ES 3.00 /
	 * WebGL2; it is only absent from WebGL1. This port asks for a WebGL2 context,
	 * so forward it like the float variant. This is the last GL call on the very
	 * first {@code TitleScreen.render} - {@code Font.drawShadow} draws a
	 * {@code POSITION_COLOR_TEX_LIGHTMAP} text batch, whose lightmap element is an
	 * integer attribute - so before this the game died with a
	 * {@code UnsupportedOperationException} on the first frame.
	 */
	public static void _glVertexAttribIPointer(int index, int size, int type, int stride, long offset) {
		_wglVertexAttribIPointer(index, size, type, stride, checkOffset(offset, "VertexAttribIPointer"));
	}

}