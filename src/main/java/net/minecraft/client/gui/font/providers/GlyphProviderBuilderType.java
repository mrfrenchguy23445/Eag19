package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.font.SpaceProvider;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;

public enum GlyphProviderBuilderType {
   BITMAP("bitmap", BitmapProvider.Builder::fromJson),
   TTF("ttf", TrueTypeGlyphProviderBuilder::fromJson),
   SPACE("space", SpaceProvider::builderFromJson),
   LEGACY_UNICODE("legacy_unicode", LegacyUnicodeBitmapsProvider.Builder::fromJson);

   private static final Map<String, GlyphProviderBuilderType> BY_NAME = Util.make(Maps.newHashMap(), param0 -> {
      for (GlyphProviderBuilderType var4 : values()) {
         param0.put(var4.name, var4);
      }
   });
   private final String name;
   private final Function<JsonObject, GlyphProviderBuilder> factory;

   GlyphProviderBuilderType(String param2, Function<JsonObject, GlyphProviderBuilder> param3) {
      this.name = param2;
      this.factory = param3;
   }

   public static GlyphProviderBuilderType byName(String param0) {
      GlyphProviderBuilderType var1 = BY_NAME.get(param0);
      if (var1 == null) {
         throw new IllegalArgumentException("Invalid type: " + param0);
      } else {
         return var1;
      }
   }

   public GlyphProviderBuilder create(JsonObject param0) {
      return this.factory.apply(param0);
   }
}
