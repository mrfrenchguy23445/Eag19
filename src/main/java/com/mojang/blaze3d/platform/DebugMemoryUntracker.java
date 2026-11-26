package com.mojang.blaze3d.platform;

/**
 * Port note (LWJGL removal): this class is now a no-op.
 *
 * <p>Vanilla reached into LWJGL's {@code MemoryUtil$LazyInit.ALLOCATOR}, checked whether it
 * was a {@code MemoryManage$DebugAllocator}, and if so called its private {@code untrack}
 * reflectively so a long-lived native buffer would stop counting against LWJGL's
 * memory-usage counter. That counter exists to debug native allocations.
 *
 * <p>This port has no native allocations at all -- image pixels, buffers and vertex data
 * are ordinary Java arrays and objects on the wasm-gc heap -- so there is no allocator to
 * interrogate and nothing to untrack. Rather than delete the class, which would break the
 * callers in {@code SpecialGlyphs} and {@code MissingTextureAtlasSprite}, the methods are
 * kept as empty no-ops. The {@code long} and {@code Pointer} forms are both gone: a pointer
 * cannot be formed without native memory, and every former caller passed an image address.
 */
public class DebugMemoryUntracker {
   public static void untrack(long param0) {
   }
}
