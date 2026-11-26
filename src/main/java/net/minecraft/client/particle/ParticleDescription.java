package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class ParticleDescription {
   private final List<ResourceLocation> textures;

   private ParticleDescription(List<ResourceLocation> param0) {
      this.textures = param0;
   }

   public List<ResourceLocation> getTextures() {
      return this.textures;
   }

   public static ParticleDescription fromJson(JsonObject param0) {
      JsonArray var1 = GsonHelper.getAsJsonArray(param0, "textures", null);
      if (var1 == null) {
         return new ParticleDescription(List.of());
      }

      List<ResourceLocation> var2 = Streams.stream(var1)
         .map(l_param0 -> GsonHelper.convertToString(l_param0, "texture"))
         .map(ResourceLocation::new)
         .collect(ImmutableList.toImmutableList());
      return new ParticleDescription(var2);
   }
}
