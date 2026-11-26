package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class FontSet implements AutoCloseable {
   private static final RandomSource RANDOM = RandomSource.create();
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private final TextureManager textureManager;
   private final ResourceLocation name;
   private BakedGlyph missingGlyph;
   private BakedGlyph whiteGlyph;
   private final List<GlyphProvider> providers = Lists.newArrayList();
   private final Int2ObjectMap<BakedGlyph> glyphs = new Int2ObjectOpenHashMap<>();
   private final Int2ObjectMap<FontSet.GlyphInfoFilter> glyphInfos = new Int2ObjectOpenHashMap<>();
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
   private final List<FontTexture> textures = Lists.newArrayList();

   public FontSet(TextureManager param0, ResourceLocation param1) {
      this.textureManager = param0;
      this.name = param1;
   }

   public void reload(List<GlyphProvider> param0) {
      this.closeProviders();
      this.closeTextures();
      this.glyphs.clear();
      this.glyphInfos.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = SpecialGlyphs.MISSING.bake(this::stitch);
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this::stitch);
      IntSet var2 = new IntOpenHashSet();

      for (GlyphProvider var4 : param0) {
         var2.addAll(var4.getSupportedGlyphs());
      }

      Set<GlyphProvider> var3 = Sets.newHashSet();
      var2.forEach(l_param2 -> {
         for (GlyphProvider l_var5 : param0) {
            GlyphInfo l_var6 = l_var5.getGlyph(l_param2);
            if (l_var6 != null) {
               var3.add(l_var5);
               if (l_var6 != SpecialGlyphs.MISSING) {
                  this.glyphsByWidth.computeIfAbsent(Mth.ceil(l_var6.getAdvance(false)), l2_param0 -> new IntArrayList()).add(l_param2);
               }
               break;
            }
         }
      });
      param0.stream().filter(var3::contains).forEach(this.providers::add);
   }

   @Override
   public void close() {
      this.closeProviders();
      this.closeTextures();
   }

   private void closeProviders() {
      for (GlyphProvider var2 : this.providers) {
         var2.close();
      }

      this.providers.clear();
   }

   private void closeTextures() {
      for (FontTexture var2 : this.textures) {
         var2.close();
      }

      this.textures.clear();
   }

   private static boolean hasFishyAdvance(GlyphInfo param0) {
      float var1 = param0.getAdvance(false);
      if (!(var1 < 0.0F) && !(var1 > 32.0F)) {
         float var2 = param0.getAdvance(true);
         return var2 < 0.0F || var2 > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.GlyphInfoFilter computeGlyphInfo(int param0) {
      GlyphInfo var2 = null;

      for (GlyphProvider var4 : this.providers) {
         GlyphInfo var5 = var4.getGlyph(param0);
         if (var5 != null) {
            if (var2 == null) {
               var2 = var5;
            }

            if (!hasFishyAdvance(var5)) {
               return new FontSet.GlyphInfoFilter(var2, var5);
            }
         }
      }

      return var2 != null ? new FontSet.GlyphInfoFilter(var2, SpecialGlyphs.MISSING) : FontSet.GlyphInfoFilter.MISSING;
   }

   public GlyphInfo getGlyphInfo(int param0, boolean param1) {
      return this.glyphInfos.computeIfAbsent(param0, this::computeGlyphInfo).select(param1);
   }

   private BakedGlyph computeBakedGlyph(int param0) {
      for (GlyphProvider var3 : this.providers) {
         GlyphInfo var4 = var3.getGlyph(param0);
         if (var4 != null) {
            return var4.bake(this::stitch);
         }
      }

      return this.missingGlyph;
   }

   public BakedGlyph getGlyph(int param0) {
      return this.glyphs.computeIfAbsent(param0, this::computeBakedGlyph);
   }

   private BakedGlyph stitch(SheetGlyphInfo param0) {
      for (FontTexture var3 : this.textures) {
         BakedGlyph var4 = var3.add(param0);
         if (var4 != null) {
            return var4;
         }
      }

      FontTexture var2 = new FontTexture(this.name.withPath(l_param0 -> l_param0 + "/" + this.textures.size()), param0.isColored());
      this.textures.add(var2);
      this.textureManager.register(var2.getName(), var2);
      BakedGlyph var3 = var2.add(param0);
      return var3 == null ? this.missingGlyph : var3;
   }

   public BakedGlyph getRandomGlyph(GlyphInfo param0) {
      IntList var2 = this.glyphsByWidth.get(Mth.ceil(param0.getAdvance(false)));
      return var2 != null && !var2.isEmpty() ? this.getGlyph(var2.getInt(RANDOM.nextInt(var2.size()))) : this.missingGlyph;
   }

   public BakedGlyph whiteGlyph() {
      return this.whiteGlyph;
   }

   record GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfoNotFishy) {
      static final FontSet.GlyphInfoFilter MISSING = new FontSet.GlyphInfoFilter(SpecialGlyphs.MISSING, SpecialGlyphs.MISSING);

      GlyphInfo select(boolean param0) {
         return param0 ? this.glyphInfoNotFishy : this.glyphInfo;
      }
   }
}
