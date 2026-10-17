package net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class ItemOverride {
   private final ResourceLocation model;
   private final List<ItemOverride.Predicate> predicates;

   public ItemOverride(ResourceLocation param0, List<ItemOverride.Predicate> param1) {
      this.model = param0;
      this.predicates = ImmutableList.copyOf(param1);
   }

   public ResourceLocation getModel() {
      return this.model;
   }

   public Stream<ItemOverride.Predicate> getPredicates() {
      return this.predicates.stream();
   }

   protected static class Deserializer implements JsonDeserializer<ItemOverride> {
      public ItemOverride deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = param0.getAsJsonObject();
         ResourceLocation var5 = new ResourceLocation(GsonHelper.getAsString(var4, "model"));
         List<ItemOverride.Predicate> var6 = this.getPredicates(var4);
         return new ItemOverride(var5, var6);
      }

      protected List<ItemOverride.Predicate> getPredicates(JsonObject param0) {
         Map<ResourceLocation, Float> var2 = Maps.newLinkedHashMap();
         JsonObject var3 = GsonHelper.getAsJsonObject(param0, "predicate");

         for (Entry<String, JsonElement> var5 : var3.entrySet()) {
            var2.put(new ResourceLocation(var5.getKey()), GsonHelper.convertToFloat(var5.getValue(), var5.getKey()));
         }

         return var2.entrySet()
            .stream()
            .map(l_param0 -> new ItemOverride.Predicate(l_param0.getKey(), l_param0.getValue()))
            .collect(ImmutableList.toImmutableList());
      }
   }

   public static class Predicate {
      private final ResourceLocation property;
      private final float value;

      public Predicate(ResourceLocation param0, float param1) {
         this.property = param0;
         this.value = param1;
      }

      public ResourceLocation getProperty() {
         return this.property;
      }

      public float getValue() {
         return this.value;
      }
   }
}
