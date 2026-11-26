package net.minecraft.client.gui.font;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.providers.GlyphProviderBuilderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class FontManager implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String FONTS_PATH = "fonts.json";
   public static final ResourceLocation MISSING_FONT = new ResourceLocation("minecraft", "missing");
   static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
   private final FontSet missingFontSet;
   final Map<ResourceLocation, FontSet> fontSets = Maps.newHashMap();
   final TextureManager textureManager;
   private Map<ResourceLocation, ResourceLocation> renames = ImmutableMap.of();
   private final PreparableReloadListener reloadListener = new SimplePreparableReloadListener<Map<ResourceLocation, List<GlyphProvider>>>() {
      protected Map<ResourceLocation, List<GlyphProvider>> prepare(ResourceManager param0, ProfilerFiller param1) {
         param1.startTick();
         Gson var3 = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
         Map<ResourceLocation, List<GlyphProvider>> var4 = Maps.newHashMap();

         for (Entry<ResourceLocation, List<Resource>> var6 : FontManager.FONT_DEFINITIONS.listMatchingResourceStacks(param0).entrySet()) {
            ResourceLocation var7 = var6.getKey();
            ResourceLocation var8 = FontManager.FONT_DEFINITIONS.fileToId(var7);
            List<GlyphProvider> var9 = var4.computeIfAbsent(var8, l_param0 -> Lists.newArrayList(new AllMissingGlyphProvider()));
            param1.push(var8::toString);

            for (Resource var11 : var6.getValue()) {
               param1.push(var11.sourcePackId());

               try (Reader var12 = var11.openAsReader()) {
                  try {
                     param1.push("reading");
                     JsonArray var13 = GsonHelper.getAsJsonArray(GsonHelper.fromJson(var3, var12, JsonObject.class), "providers");
                     param1.popPush("parsing");

                     for (int var14 = var13.size() - 1; var14 >= 0; var14--) {
                        JsonObject var15 = GsonHelper.convertToJsonObject(var13.get(var14), "providers[" + var14 + "]");
                        String var16 = GsonHelper.getAsString(var15, "type");
                        GlyphProviderBuilderType var17 = GlyphProviderBuilderType.byName(var16);

                        try {
                           param1.push(var16);
                           GlyphProvider var18 = var17.create(var15).create(param0);
                           if (var18 != null) {
                              var9.add(var18);
                           }
                        } finally {
                           param1.pop();
                        }
                     }
                  } finally {
                     param1.pop();
                  }
               } catch (Exception var12) {
                  FontManager.LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", var8, "fonts.json", var11.sourcePackId(), var12);
               }

               param1.pop();
            }

            param1.push("caching");
            IntSet var10 = new IntOpenHashSet();

            for (GlyphProvider var12 : var9) {
               var10.addAll(var12.getSupportedGlyphs());
            }

            var10.forEach(l_param1 -> {
               if (l_param1 != 32) {
                  for (GlyphProvider l_var3 : Lists.reverse(var9)) {
                     if (l_var3.getGlyph(l_param1) != null) {
                        break;
                     }
                  }
               }
            });
            param1.pop();
            param1.pop();
         }

         param1.endTick();
         return var4;
      }

      protected void apply(Map<ResourceLocation, List<GlyphProvider>> param0, ResourceManager param1, ProfilerFiller param2) {
         param2.startTick();
         param2.push("closing");
         FontManager.this.fontSets.values().forEach(FontSet::close);
         FontManager.this.fontSets.clear();
         param2.popPush("reloading");
         param0.forEach((l_param0, l_param1) -> {
            FontSet l_var3 = new FontSet(FontManager.this.textureManager, l_param0);
            l_var3.reload(Lists.reverse((List<GlyphProvider>)l_param1));
            FontManager.this.fontSets.put(l_param0, l_var3);
         });
         param2.pop();
         param2.endTick();
      }

      @Override
      public String getName() {
         return "FontManager";
      }
   };

   public FontManager(TextureManager param0) {
      this.textureManager = param0;
      this.missingFontSet = Util.make(new FontSet(param0, MISSING_FONT), l_param0 -> l_param0.reload(Lists.newArrayList(new AllMissingGlyphProvider())));
   }

   public void setRenames(Map<ResourceLocation, ResourceLocation> param0) {
      this.renames = param0;
   }

   public Font createFont() {
      return new Font(l_param0 -> this.fontSets.getOrDefault(this.renames.getOrDefault(l_param0, l_param0), this.missingFontSet), false);
   }

   public Font createFontFilterFishy() {
      return new Font(l_param0 -> this.fontSets.getOrDefault(this.renames.getOrDefault(l_param0, l_param0), this.missingFontSet), true);
   }

   public PreparableReloadListener getReloadListener() {
      return this.reloadListener;
   }

   @Override
   public void close() {
      this.fontSets.values().forEach(FontSet::close);
      this.missingFontSet.close();
   }
}
