package com.mojang.blaze3d.platform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Allocates the growable native buffers that back {@link com.mojang.blaze3d.vertex.BufferBuilder}.
 *
 * <p>Upstream uses {@code org.lwjgl.system.MemoryUtil}, whose {@code MemoryAllocator} calls
 * straight into LWJGL's native {@code malloc}/{@code realloc}. Neither {@code MemoryUtil} nor any
 * LWJGL native memory layer is present in the WASM-GC TeaVM classlib, so allocation goes through
 * {@link ByteBuffer#allocateDirect(int)}, which the classlib implements over the WASM linear memory
 * and which the TeaVM WASM-GC target sizes via its
 * {@code minDirectBuffersSize}/{@code maxDirectBuffersSize} settings. {@link ByteBuffer#allocate} is
 * deliberately not used: {@link com.mojang.blaze3d.vertex.BufferBuilder} takes {@code IntBuffer}
 * views of this buffer and writes them straight to GL, which needs the addressable backing store
 * that only the direct flavour provides.
 *
 * <p>The byte order is forced to {@link ByteOrder#nativeOrder()}, matching LWJGL's
 * {@code MemoryUtil}: {@code BufferBuilder} populates these buffers with relative
 * {@code putFloat}/{@code putShort}/{@code putInt} calls and the resulting bytes are handed to WebGL
 * unchanged, so they must be in the platform's native order. {@link ByteBuffer#allocateDirect} would
 * otherwise default to {@link ByteOrder#BIG_ENDIAN}, which byte-swaps every vertex attribute and
 * index and makes all geometry degenerate.
 *
 * <p>{@link #resize} allocates a fresh buffer and copies the old contents rather than reallocating
 * in place, because the WASM platform exposes no in-place resize. The caller-visible contract
 * (same contents, new capacity) is unchanged.
 */
public class MemoryTracker {
   public static ByteBuffer create(int param0) {
      try {
         return ByteBuffer.allocateDirect(param0).order(ByteOrder.nativeOrder());
      } catch (OutOfMemoryError var1) {
         throw new OutOfMemoryError("Failed to allocate " + param0 + " bytes");
      }
   }

   public static ByteBuffer resize(ByteBuffer param0, int param1) {
      ByteBuffer var1;
      try {
         var1 = ByteBuffer.allocateDirect(param1).order(ByteOrder.nativeOrder());
      } catch (OutOfMemoryError var2) {
         throw new OutOfMemoryError("Failed to resize buffer from " + param0.capacity() + " bytes to " + param1 + " bytes");
      }

      int var2 = Math.min(param0.capacity(), param1);
      var1.put(param0.duplicate().position(0).limit(var2));
      var1.rewind();
      return var1;
   }
}
