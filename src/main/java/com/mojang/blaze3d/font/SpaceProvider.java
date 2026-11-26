package com.mojang.blaze3d.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.Arrays;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.providers.GlyphProviderBuilder;
import net.minecraft.util.GsonHelper;

public class SpaceProvider implements GlyphProvider {
   private final Int2ObjectMap<GlyphInfo.SpaceGlyphInfo> glyphs;

   public SpaceProvider(Int2FloatMap param0) {
      this.glyphs = new Int2ObjectOpenHashMap<>(param0.size());
      Int2FloatMaps.fastForEach(param0, l_param0 -> {
         float l_var2 = l_param0.getFloatValue();
         this.glyphs.put(l_param0.getIntKey(), () -> l_var2);
      });
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

   public static GlyphProviderBuilder builderFromJson(JsonObject param0) {
      Int2FloatMap var1 = new Int2FloatOpenHashMap();
      JsonObject var2 = GsonHelper.getAsJsonObject(param0, "advances");

      for (Entry<String, JsonElement> var4 : var2.entrySet()) {
         int[] var5 = var4.getKey().codePoints().toArray();
         if (var5.length != 1) {
            throw new JsonParseException("Expected single codepoint, got " + Arrays.toString(var5));
         }

         float var6 = GsonHelper.convertToFloat(var4.getValue(), "advance");
         var1.put(var5[0], var6);
      }

      return l_param1 -> new SpaceProvider(var1);
   }
}
