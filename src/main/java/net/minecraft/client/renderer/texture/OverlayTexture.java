package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

public class OverlayTexture implements AutoCloseable {
   private static final int SIZE = 16;
   public static final int NO_WHITE_U = 0;
   public static final int RED_OVERLAY_V = 3;
   public static final int WHITE_OVERLAY_V = 10;
   public static final int NO_OVERLAY = pack(0, 10);
   private final DynamicTexture texture = new DynamicTexture(16, 16, false);

   public OverlayTexture() {
      NativeImage var1 = this.texture.getPixels();

      for (int var2 = 0; var2 < 16; var2++) {
         for (int var3 = 0; var3 < 16; var3++) {
            if (var2 < 8) {
               var1.setPixelRGBA(var3, var2, -1308622593);
            } else {
               int var4 = (int)((1.0F - var3 / 15.0F * 0.75F) * 255.0F);
               var1.setPixelRGBA(var3, var2, var4 << 24 | 16777215);
            }
         }
      }

      RenderSystem.activeTexture(33985);
      this.texture.bind();
      var1.upload(0, 0, 0, 0, 0, var1.getWidth(), var1.getHeight(), false, true, false, false);
      RenderSystem.activeTexture(33984);
   }

   @Override
   public void close() {
      this.texture.close();
   }

   public void setupOverlayColor() {
      RenderSystem.setupOverlayColor(this.texture::getId, 16);
   }

   public static int u(float param0) {
      return (int)(param0 * 15.0F);
   }

   public static int v(boolean param0) {
      return param0 ? 3 : 10;
   }

   public static int pack(int param0, int param1) {
      return param0 | param1 << 16;
   }

   public static int pack(float param0, boolean param1) {
      return pack(u(param0), v(param1));
   }

   public void teardownOverlayColor() {
      RenderSystem.teardownOverlayColor();
   }
}
