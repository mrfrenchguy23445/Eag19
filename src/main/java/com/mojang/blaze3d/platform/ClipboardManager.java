package com.mojang.blaze3d.platform;

import net.minecraft.util.StringDecomposer;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformApplication;

/**
 * System clipboard access, via the Eaglercraft platform layer.
 *
 * <p>Upstream stages clipboard text through a scratch {@code ByteBuffer} because
 * {@code glfwSetClipboardString} takes an NUL-terminated UTF-8 pointer. Eaglercraft's
 * {@code PlatformApplication.getClipboard()} / {@code setClipboard(String)} deal in Java
 * {@link String}s and handle the browser clipboard (and the async permission prompt that comes with
 * it) on the JavaScript side, so the scratch buffer and both {@code MemoryUtil} allocations are gone
 * -- only the UTF-8 encoding of the outgoing value is still relevant, and only for parity with
 * whatever the platform does with it.
 *
 * <p>The window handle argument is retained and ignored: it exists on the signature because
 * {@code KeyboardHandler} passes {@code Window.getWindow()}, which is a single synthetic handle
 * (see {@link Window}). Upstream's {@code FORMAT_UNAVAILABLE} error path is likewise gone; there is
 * no GLFW error channel to report through, so a denied clipboard read is indistinguishable from an
 * empty clipboard here.
 */
public class ClipboardManager {
   public static final int FORMAT_UNAVAILABLE = 65545;

   public String getClipboard(long param0, java.util.function.BiConsumer<Integer, String> param2) {
      String var1 = PlatformApplication.getClipboard();
      return var1 != null ? StringDecomposer.filterBrokenSurrogates(var1) : "";
   }

   public void setClipboard(long param0, String param1) {
      if (param1 != null && !param1.isEmpty()) {
         PlatformApplication.setClipboard(param1);
      }
   }
}
