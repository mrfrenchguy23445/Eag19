package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;

@DontObfuscate
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

   public static int generateTextureId() {
      RenderSystem.assertOnRenderThreadOrInit();
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         int[] var0 = new int[ThreadLocalRandom.current().nextInt(15) + 1];
         GlStateManager._genTextures(var0);
         int var1 = GlStateManager._genTexture();
         GlStateManager._deleteTextures(var0);
         return var1;
      } else {
         return GlStateManager._genTexture();
      }
   }

   public static void releaseTextureId(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._deleteTexture(param0);
   }

   public static void prepareImage(int param0, int param1, int param2) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, param0, 0, param1, param2);
   }

   public static void prepareImage(NativeImage.InternalGlFormat param0, int param1, int param2, int param3) {
      prepareImage(param0, param1, 0, param2, param3);
   }

   public static void prepareImage(int param0, int param1, int param2, int param3) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, param0, param1, param2, param3);
   }

   public static void prepareImage(NativeImage.InternalGlFormat param0, int param1, int param2, int param3, int param4) {
      RenderSystem.assertOnRenderThreadOrInit();
      bind(param1);
      if (param2 >= 0) {
         GlStateManager._texParameter(3553, 33085, param2);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, param2);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      for (int var5 = 0; var5 <= param2; var5++) {
         GlStateManager._texImage2D(3553, var5, param0.glFormat(), param3 >> var5, param4 >> var5, 0, 6408, 5121, null);
      }
   }

   private static void bind(int param0) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._bindTexture(param0);
   }

   public static ByteBuffer readResource(InputStream param0) throws IOException {
      ReadableByteChannel var1 = Channels.newChannel(param0);
      return var1 instanceof SeekableByteChannel var2 ? readResource(var1, (int)var2.size() + 1) : readResource(var1, 8192);
   }

   private static ByteBuffer readResource(ReadableByteChannel param0, int param1) throws IOException {
      // Upstream grows the buffer with LWJGL's MemoryUtil.memAlloc/memRealloc/memFree. None of those
      // exist in the WASM-GC classlib, so this is the same loop over a direct ByteBuffer, growing
      // by allocating a fresh one and copying the contents. The old buffer is simply left to the
      // collector -- there is no explicit free on a GC'd heap, and the growth is geometric, so the
      // total copying is bounded.
      ByteBuffer var2 = ByteBuffer.allocateDirect(param1);

      while (param0.read(var2) != -1) {
         if (!var2.hasRemaining()) {
            ByteBuffer var3 = ByteBuffer.allocateDirect(var2.capacity() * 2);
            var2.flip();
            var3.put(var2);
            var2 = var3;
         }
      }

      // Both callers (NativeImage.read and TrueTypeGlyphProviderBuilder.readFont) hand the result
      // straight to a decoder that consumes the entire buffer, and upstream likewise returns the
      // fully-consumed buffer, so the final position/limit pair is deliberately left untouched.
      return var2;
   }

   public static void writeAsPNG(Path param0, String param1, int param2, int param3, int param4, int param5) {
      RenderSystem.assertOnRenderThread();
      bind(param2);

      for (int var6 = 0; var6 <= param3; var6++) {
         int var7 = param4 >> var6;
         int var8 = param5 >> var6;

         try (NativeImage var9 = new NativeImage(var7, var8, false)) {
            var9.downloadTexture(var6, false);
            Path var10 = param0.resolve(param1 + "_" + var6 + ".png");
            var9.writeToFile(var10);
            LOGGER.debug("Exported png to: {}", var10.toAbsolutePath());
         } catch (IOException var9) {
            LOGGER.debug("Unable to write: ", var9);
         }
      }
   }

   public static Path getDebugTexturePath(Path param0) {
      return param0.resolve("screenshots").resolve("debug");
   }

   public static Path getDebugTexturePath() {
      return getDebugTexturePath(Path.of("."));
   }
}
