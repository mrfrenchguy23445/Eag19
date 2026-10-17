package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Function;
import java.util.function.Supplier;

public enum SpecialGlyphs implements GlyphInfo {
   WHITE(() -> generate(5, 8, (l_param0, l_param1) -> -1)),
   MISSING(() -> {
      int var0 = 5;
      int var1 = 8;
      return generate(5, 8, (l_param0, l_param1) -> {
         boolean l_var2 = l_param0 == 0 || l_param0 + 1 == 5 || l_param1 == 0 || l_param1 + 1 == 8;
         return l_var2 ? -1 : 0;
      });
   });

   final NativeImage image;

   private static NativeImage generate(int param0, int param1, SpecialGlyphs.PixelProvider param2) {
      NativeImage var3 = new NativeImage(NativeImage.Format.RGBA, param0, param1, false);

      for (int var4 = 0; var4 < param1; var4++) {
         for (int var5 = 0; var5 < param0; var5++) {
            var3.setPixelRGBA(var5, var4, param2.getColor(var5, var4));
         }
      }

      var3.untrack();
      return var3;
   }

   SpecialGlyphs(Supplier<NativeImage> param2) {
      this.image = param2.get();
   }

   @Override
   public float getAdvance() {
      return this.image.getWidth() + 1;
   }

   @Override
   public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> param0) {
      return param0.apply(new SheetGlyphInfo() {
         @Override
         public int getPixelWidth() {
            return SpecialGlyphs.this.image.getWidth();
         }

         @Override
         public int getPixelHeight() {
            return SpecialGlyphs.this.image.getHeight();
         }

         @Override
         public float getOversample() {
            return 1.0F;
         }

         @Override
         public void upload(int l_param0, int l_param1) {
            SpecialGlyphs.this.image.upload(0, l_param0, l_param1, false);
         }

         @Override
         public boolean isColored() {
            return true;
         }
      });
   }

   @FunctionalInterface
   interface PixelProvider {
      int getColor(int var1, int var2);
   }
}
