package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public class BitmapProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private final NativeImage image;
   private final Int2ObjectMap<BitmapProvider.Glyph> glyphs;

   BitmapProvider(NativeImage param0, Int2ObjectMap<BitmapProvider.Glyph> param1) {
      this.image = param0;
      this.glyphs = param1;
   }

   @Override
   public void close() {
      this.image.close();
   }

   @Nullable
   @Override
   public GlyphInfo getGlyph(int param0) {
      return this.glyphs.get(param0);
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return IntSets.unmodifiable(this.glyphs.keySet());
   }

   public static class Builder implements GlyphProviderBuilder {
      private final ResourceLocation texture;
      private final List<int[]> chars;
      private final int height;
      private final int ascent;

      public Builder(ResourceLocation param0, int param1, int param2, List<int[]> param3) {
         this.texture = param0.withPrefix("textures/");
         this.chars = param3;
         this.height = param1;
         this.ascent = param2;
      }

      public static BitmapProvider.Builder fromJson(JsonObject param0) {
         int var1 = GsonHelper.getAsInt(param0, "height", 8);
         int var2 = GsonHelper.getAsInt(param0, "ascent");
         if (var2 > var1) {
            throw new JsonParseException("Ascent " + var2 + " higher than height " + var1);
         }

         List<int[]> var3 = Lists.newArrayList();
         JsonArray var4 = GsonHelper.getAsJsonArray(param0, "chars");

         for (int var5 = 0; var5 < var4.size(); var5++) {
            String var6 = GsonHelper.convertToString(var4.get(var5), "chars[" + var5 + "]");
            int[] var7 = var6.codePoints().toArray();
            if (var5 > 0) {
               int var8 = var3.get(0).length;
               if (var7.length != var8) {
                  throw new JsonParseException(
                     "Elements of chars have to be the same length (found: " + var7.length + ", expected: " + var8 + "), pad with space or \\u0000"
                  );
               }
            }

            var3.add(var7);
         }

         if (!var3.isEmpty() && var3.get(0).length != 0) {
            return new BitmapProvider.Builder(new ResourceLocation(GsonHelper.getAsString(param0, "file")), var1, var2, var3);
         } else {
            throw new JsonParseException("Expected to find data in chars, found none.");
         }
      }

      @Nullable
      @Override
      public GlyphProvider create(ResourceManager param0) {
         try (InputStream var2 = param0.open(this.texture)) {
            NativeImage var3 = NativeImage.read(NativeImage.Format.RGBA, var2);
            int var4 = var3.getWidth();
            int var5 = var3.getHeight();
            int var6 = var4 / this.chars.get(0).length;
            int var7 = var5 / this.chars.size();
            float var8 = (float)this.height / var7;
            Int2ObjectMap<BitmapProvider.Glyph> var9 = new Int2ObjectOpenHashMap<>();

            for (int var10 = 0; var10 < this.chars.size(); var10++) {
               int var11 = 0;

               for (int var15 : this.chars.get(var10)) {
                  int var16 = var11++;
                  if (var15 != 0) {
                     int var17 = this.getActualGlyphWidth(var3, var6, var7, var16, var10);
                     BitmapProvider.Glyph var18 = var9.put(
                        var15, new BitmapProvider.Glyph(var8, var3, var16 * var6, var10 * var7, var6, var7, (int)(0.5 + var17 * var8) + 1, this.ascent)
                     );
                     if (var18 != null) {
                        BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(var15), this.texture);
                     }
                  }
               }
            }

            return new BitmapProvider(var3, var9);
         } catch (IOException var2) {
            throw new RuntimeException(var2.getMessage());
         }
      }

      private int getActualGlyphWidth(NativeImage param0, int param1, int param2, int param3, int param4) {
         int var6;
         for (var6 = param1 - 1; var6 >= 0; var6--) {
            int var7 = param3 * param1 + var6;

            for (int var8 = 0; var8 < param2; var8++) {
               int var9 = param4 * param2 + var8;
               if (param0.getLuminanceOrAlpha(var7, var9) != 0) {
                  return var6 + 1;
               }
            }
         }

         return var6 + 1;
      }
   }

   record Glyph(float scale, NativeImage image, int offsetX, int offsetY, int width, int height, int advance, int ascent) implements GlyphInfo {

      @Override
      public float getAdvance() {
         return this.advance;
      }

      @Override
      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> param0) {
         return param0.apply(new SheetGlyphInfo() {
            @Override
            public float getOversample() {
               return 1.0F / Glyph.this.scale;
            }

            @Override
            public int getPixelWidth() {
               return Glyph.this.width;
            }

            @Override
            public int getPixelHeight() {
               return Glyph.this.height;
            }

            @Override
            public float getBearingY() {
               return SheetGlyphInfo.super.getBearingY() + 7.0F - Glyph.this.ascent;
            }

            @Override
            public void upload(int l_param0, int l_param1) {
               Glyph.this.image.upload(0, l_param0, l_param1, Glyph.this.offsetX, Glyph.this.offsetY, Glyph.this.width, Glyph.this.height, false, false);
            }

            @Override
            public boolean isColored() {
               return Glyph.this.image.format().components() > 1;
            }
         });
      }
   }
}
