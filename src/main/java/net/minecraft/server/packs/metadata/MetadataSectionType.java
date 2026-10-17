package net.minecraft.server.packs.metadata;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

public interface MetadataSectionType<T> extends MetadataSectionSerializer<T> {
   JsonObject toJson(T var1);

   static <T> MetadataSectionType<T> fromCodec(final String param0, final Codec<T> param1) {
      return new MetadataSectionType<T>() {
         @Override
         public String getMetadataSectionName() {
            return param0;
         }

         @Override
         public T fromJson(JsonObject l_param0) {
            return param1.parse(JsonOps.INSTANCE, l_param0).getOrThrow(false, l2_param0 -> {});
         }

         @Override
         public JsonObject toJson(T l_param0) {
            return param1.encodeStart(JsonOps.INSTANCE, l_param0).getOrThrow(false, l2_param0 -> {}).getAsJsonObject();
         }
      };
   }
}
