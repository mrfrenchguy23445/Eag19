package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;

public class MipmapGenerator {
   private static final int ALPHA_CUTOUT_CUTOFF = 96;
   private static final float[] POW22 = Util.make(new float[256], param0 -> {
      for (int var1 = 0; var1 < param0.length; var1++) {
         param0[var1] = (float)Math.pow(var1 / 255.0F, 2.2);
      }
   });

   private MipmapGenerator() {
   }

   public static NativeImage[] generateMipLevels(NativeImage[] param0, int param1) {
      if (param1 + 1 <= param0.length) {
         return param0;
      }

      NativeImage[] var2 = new NativeImage[param1 + 1];
      var2[0] = param0[0];
      boolean var3 = hasTransparentPixel(var2[0]);

      for (int var4 = 1; var4 <= param1; var4++) {
         if (var4 < param0.length) {
            var2[var4] = param0[var4];
         } else {
            NativeImage var5 = var2[var4 - 1];
            NativeImage var6 = new NativeImage(var5.getWidth() >> 1, var5.getHeight() >> 1, false);
            int var7 = var6.getWidth();
            int var8 = var6.getHeight();

            for (int var9 = 0; var9 < var7; var9++) {
               for (int var10 = 0; var10 < var8; var10++) {
                  var6.setPixelRGBA(
                     var9,
                     var10,
                     alphaBlend(
                        var5.getPixelRGBA(var9 * 2 + 0, var10 * 2 + 0),
                        var5.getPixelRGBA(var9 * 2 + 1, var10 * 2 + 0),
                        var5.getPixelRGBA(var9 * 2 + 0, var10 * 2 + 1),
                        var5.getPixelRGBA(var9 * 2 + 1, var10 * 2 + 1),
                        var3
                     )
                  );
               }
            }

            var2[var4] = var6;
         }
      }

      return var2;
   }

   private static boolean hasTransparentPixel(NativeImage param0) {
      for (int var1 = 0; var1 < param0.getWidth(); var1++) {
         for (int var2 = 0; var2 < param0.getHeight(); var2++) {
            if (param0.getPixelRGBA(var1, var2) >> 24 == 0) {
               return true;
            }
         }
      }

      return false;
   }

   private static int alphaBlend(int param0, int param1, int param2, int param3, boolean param4) {
      if (param4) {
         float var5 = 0.0F;
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = 0.0F;
         if (param0 >> 24 != 0) {
            var5 += getPow22(param0 >> 24);
            var6 += getPow22(param0 >> 16);
            var7 += getPow22(param0 >> 8);
            var8 += getPow22(param0 >> 0);
         }

         if (param1 >> 24 != 0) {
            var5 += getPow22(param1 >> 24);
            var6 += getPow22(param1 >> 16);
            var7 += getPow22(param1 >> 8);
            var8 += getPow22(param1 >> 0);
         }

         if (param2 >> 24 != 0) {
            var5 += getPow22(param2 >> 24);
            var6 += getPow22(param2 >> 16);
            var7 += getPow22(param2 >> 8);
            var8 += getPow22(param2 >> 0);
         }

         if (param3 >> 24 != 0) {
            var5 += getPow22(param3 >> 24);
            var6 += getPow22(param3 >> 16);
            var7 += getPow22(param3 >> 8);
            var8 += getPow22(param3 >> 0);
         }

         var5 /= 4.0F;
         var6 /= 4.0F;
         var7 /= 4.0F;
         var8 /= 4.0F;
         int var9 = (int)(Math.pow(var5, 0.45454545454545453) * 255.0);
         int var10 = (int)(Math.pow(var6, 0.45454545454545453) * 255.0);
         int var11 = (int)(Math.pow(var7, 0.45454545454545453) * 255.0);
         int var12 = (int)(Math.pow(var8, 0.45454545454545453) * 255.0);
         if (var9 < 96) {
            var9 = 0;
         }

         return var9 << 24 | var10 << 16 | var11 << 8 | var12;
      } else {
         int var5 = gammaBlend(param0, param1, param2, param3, 24);
         int var6 = gammaBlend(param0, param1, param2, param3, 16);
         int var7 = gammaBlend(param0, param1, param2, param3, 8);
         int var8 = gammaBlend(param0, param1, param2, param3, 0);
         return var5 << 24 | var6 << 16 | var7 << 8 | var8;
      }
   }

   private static int gammaBlend(int param0, int param1, int param2, int param3, int param4) {
      float var5 = getPow22(param0 >> param4);
      float var6 = getPow22(param1 >> param4);
      float var7 = getPow22(param2 >> param4);
      float var8 = getPow22(param3 >> param4);
      float var9 = (float)((float)Math.pow((var5 + var6 + var7 + var8) * 0.25, 0.45454545454545453));
      return (int)(var9 * 255.0);
   }

   private static float getPow22(int param0) {
      return POW22[param0 & 0xFF];
   }
}
