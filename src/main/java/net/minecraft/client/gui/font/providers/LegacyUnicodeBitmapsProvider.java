package net.minecraft.client.gui.font.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class LegacyUnicodeBitmapsProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int UNICODE_SHEETS = 256;
   private static final int CODEPOINTS_PER_SHEET = 256;
   private static final int TEXTURE_SIZE = 256;
   private static final byte NO_GLYPH = 0;
   private static final int TOTAL_CODEPOINTS = 65536;
   private final byte[] sizes;
   private final LegacyUnicodeBitmapsProvider.Sheet[] sheets = new LegacyUnicodeBitmapsProvider.Sheet[256];

   public LegacyUnicodeBitmapsProvider(ResourceManager param0, byte[] param1, String param2) {
      this.sizes = param1;
      Set<ResourceLocation> var4 = new HashSet<>();

      for (int var5 = 0; var5 < 256; var5++) {
         int var6 = var5 * 256;
         var4.add(getSheetLocation(param2, var6));
      }

      String var5 = getCommonSearchPrefix(var4);
      Map<ResourceLocation, CompletableFuture<NativeImage>> var6 = new HashMap<>();
      param0.listResources(var5, var4::contains).forEach((l_param1, l_param2) -> var6.put(l_param1, CompletableFuture.supplyAsync(() -> {
         try (InputStream l2_var2 = l_param2.open()) {
            return NativeImage.read(NativeImage.Format.RGBA, l2_var2);
         } catch (IOException l2_var2) {
            LOGGER.error("Failed to read resource {} from pack {}", l_param1, l_param2.sourcePackId());
            return null;
         }
      }, Util.backgroundExecutor())));
      List<CompletableFuture<?>> var7 = new ArrayList<>(256);

      for (int var8 = 0; var8 < 256; var8++) {
         int var9 = var8 * 256;
         int var10 = var8;
         ResourceLocation var11 = getSheetLocation(param2, var9);
         CompletableFuture<NativeImage> var12 = var6.get(var11);
         if (var12 != null) {
            var7.add(var12.thenAcceptAsync(l_param3 -> {
               if (l_param3 != null) {
                  if (l_param3.getWidth() == 256 && l_param3.getHeight() == 256) {
                     for (int l_var5 = 0; l_var5 < 256; l_var5++) {
                        byte l_var6 = param1[var9 + l_var5];
                        if (l_var6 != 0 && getLeft(l_var6) > getRight(l_var6)) {
                           param1[var9 + l_var5] = 0;
                        }
                     }

                     this.sheets[var10] = new LegacyUnicodeBitmapsProvider.Sheet(param1, l_param3);
                  } else {
                     l_param3.close();
                     Arrays.fill(param1, var9, var9 + 256, (byte)0);
                  }
               }
            }, Util.backgroundExecutor()));
         }
      }

      CompletableFuture.allOf(var7.toArray(CompletableFuture[]::new)).join();
   }

   private static String getCommonSearchPrefix(Set<ResourceLocation> param0) {
      String var1 = StringUtils.getCommonPrefix(param0.stream().map(ResourceLocation::getPath).toArray(String[]::new));
      int var2 = var1.lastIndexOf("/");
      return var2 == -1 ? "" : var1.substring(0, var2);
   }

   @Override
   public void close() {
      for (LegacyUnicodeBitmapsProvider.Sheet var4 : this.sheets) {
         if (var4 != null) {
            var4.close();
         }
      }
   }

   private static ResourceLocation getSheetLocation(String param0, int param1) {
      String var2 = String.format(Locale.ROOT, "%02x", param1 / 256);
      ResourceLocation var3 = new ResourceLocation(String.format(Locale.ROOT, param0, var2));
      return var3.withPrefix("textures/");
   }

   @Nullable
   @Override
   public GlyphInfo getGlyph(int param0) {
      if (param0 >= 0 && param0 < this.sizes.length) {
         int var2 = param0 / 256;
         LegacyUnicodeBitmapsProvider.Sheet var3 = this.sheets[var2];
         return var3 != null ? var3.getGlyph(param0) : null;
      } else {
         return null;
      }
   }

   @Override
   public IntSet getSupportedGlyphs() {
      IntSet var1 = new IntOpenHashSet();

      for (int var2 = 0; var2 < this.sizes.length; var2++) {
         if (this.sizes[var2] != 0) {
            var1.add(var2);
         }
      }

      return var1;
   }

   static int getLeft(byte param0) {
      return param0 >> 4 & 15;
   }

   static int getRight(byte param0) {
      return (param0 & 15) + 1;
   }

   public static class Builder implements GlyphProviderBuilder {
      private final ResourceLocation metadata;
      private final String texturePattern;

      public Builder(ResourceLocation param0, String param1) {
         this.metadata = param0;
         this.texturePattern = param1;
      }

      public static GlyphProviderBuilder fromJson(JsonObject param0) {
         return new LegacyUnicodeBitmapsProvider.Builder(new ResourceLocation(GsonHelper.getAsString(param0, "sizes")), getTemplate(param0));
      }

      private static String getTemplate(JsonObject param0) {
         String var1 = GsonHelper.getAsString(param0, "template");

         try {
            String.format(Locale.ROOT, var1, "");
            return var1;
         } catch (IllegalFormatException var2) {
            throw new JsonParseException("Invalid legacy unicode template supplied, expected single '%s': " + var1);
         }
      }

      @Nullable
      @Override
      public GlyphProvider create(ResourceManager param0) {
         try (InputStream var2 = Minecraft.getInstance().getResourceManager().open(this.metadata)) {
            byte[] var3 = var2.readNBytes(65536);
            return new LegacyUnicodeBitmapsProvider(param0, var3, this.texturePattern);
         } catch (IOException var2) {
            LegacyUnicodeBitmapsProvider.LOGGER.error("Cannot load {}, unicode glyphs will not render correctly", this.metadata);
            return null;
         }
      }
   }

   record Glyph(int sourceX, int sourceY, int width, int height, NativeImage source) implements GlyphInfo {

      @Override
      public float getAdvance() {
         return this.width / 2 + 1;
      }

      @Override
      public float getShadowOffset() {
         return 0.5F;
      }

      @Override
      public float getBoldOffset() {
         return 0.5F;
      }

      @Override
      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> param0) {
         return param0.apply(new SheetGlyphInfo() {
            @Override
            public float getOversample() {
               return 2.0F;
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
            public void upload(int l_param0, int l_param1) {
               Glyph.this.source.upload(0, l_param0, l_param1, Glyph.this.sourceX, Glyph.this.sourceY, Glyph.this.width, Glyph.this.height, false, false);
            }

            @Override
            public boolean isColored() {
               return Glyph.this.source.format().components() > 1;
            }
         });
      }
   }

   static class Sheet implements AutoCloseable {
      private final byte[] sizes;
      private final NativeImage source;

      Sheet(byte[] param0, NativeImage param1) {
         this.sizes = param0;
         this.source = param1;
      }

      @Override
      public void close() {
         this.source.close();
      }

      @Nullable
      public GlyphInfo getGlyph(int param0) {
         byte var2 = this.sizes[param0];
         if (var2 != 0) {
            int var3 = LegacyUnicodeBitmapsProvider.getLeft(var2);
            return new LegacyUnicodeBitmapsProvider.Glyph(
               param0 % 16 * 16 + var3, (param0 & 0xFF) / 16 * 16, LegacyUnicodeBitmapsProvider.getRight(var2) - var3, 16, this.source
            );
         } else {
            return null;
         }
      }
   }
}
