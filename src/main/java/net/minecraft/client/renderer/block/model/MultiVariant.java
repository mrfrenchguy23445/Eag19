package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.resources.ResourceLocation;

public class MultiVariant implements UnbakedModel {
   private final List<Variant> variants;

   public MultiVariant(List<Variant> param0) {
      this.variants = param0;
   }

   public List<Variant> getVariants() {
      return this.variants;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return param0 instanceof MultiVariant var2 ? this.variants.equals(var2.variants) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.variants.hashCode();
   }

   @Override
   public Collection<ResourceLocation> getDependencies() {
      return this.getVariants().stream().map(Variant::getModelLocation).collect(Collectors.toSet());
   }

   @Override
   public void resolveParents(Function<ResourceLocation, UnbakedModel> param0) {
      this.getVariants().stream().map(Variant::getModelLocation).distinct().forEach(l_param1 -> param0.apply(l_param1).resolveParents(param0));
   }

   @Nullable
   @Override
   public BakedModel bake(ModelBaker param0, Function<Material, TextureAtlasSprite> param1, ModelState param2, ResourceLocation param3) {
      if (this.getVariants().isEmpty()) {
         return null;
      }

      WeightedBakedModel.Builder var5 = new WeightedBakedModel.Builder();

      for (Variant var7 : this.getVariants()) {
         BakedModel var8 = param0.bake(var7.getModelLocation(), var7);
         var5.add(var8, var7.getWeight());
      }

      return var5.build();
   }

   public static class Deserializer implements JsonDeserializer<MultiVariant> {
      public MultiVariant deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         List<Variant> var4 = Lists.newArrayList();
         if (param0.isJsonArray()) {
            JsonArray var5 = param0.getAsJsonArray();
            if (var5.size() == 0) {
               throw new JsonParseException("Empty variant array");
            }

            for (JsonElement var7 : var5) {
               var4.add(param2.deserialize(var7, Variant.class));
            }
         } else {
            var4.add(param2.deserialize(param0, Variant.class));
         }

         return new MultiVariant(var4);
      }
   }
}
