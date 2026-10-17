package com.mojang.blaze3d.platform;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Raw buffer allocation for GL upload staging.
 *
 * <p>Upstream wraps LWJGL's {@code MemoryUtil}, which is a thin front for native {@code malloc}/
 * {@code free}. Neither LWJGL nor any native allocator exists in the WASM-GC TeaVM classlib, so
 * allocation goes through {@link ByteBuffer#allocateDirect(int)}, which the classlib implements over
 * the WASM linear memory.
 *
 * <p>{@link #freeMemory(Buffer)} becomes a no-op, and that is semantically correct rather than a
 * leak: the WASM heap is garbage-collected, so an unreachable {@code ByteBuffer} is reclaimed
 * without the explicit {@code free} that a {@code malloc} would have needed. The method is kept
 * because callers use it to mark lifetime boundaries, and dropping it would change those call sites
 * for no benefit.
 */
public class GlUtil {
   public static ByteBuffer allocateMemory(int param0) {
      return ByteBuffer.allocateDirect(param0);
   }

   public static void freeMemory(Buffer param0) {
      // No-op: see the class comment. Nothing to free on a GC'd heap.
   }

   public static String getVendor() {
      return GlStateManager._getString(7936);
   }

   public static String getCpuInfo() {
      return GLX._getCpuInfo();
   }

   public static String getRenderer() {
      return GlStateManager._getString(7937);
   }

   public static String getOpenGLVersion() {
      return GlStateManager._getString(7938);
   }
}
