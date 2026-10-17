package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Variant implements Supplier<JsonElement> {
   private final Map<VariantProperty<?>, VariantProperty<?>.Value> values = Maps.newLinkedHashMap();

   public <T> Variant with(VariantProperty<T> param0, T param1) {
      VariantProperty<?>.Value var3 = this.values.put(param0, param0.withValue(param1));
      if (var3 != null) {
         throw new IllegalStateException("Replacing value of " + var3 + " with " + param1);
      } else {
         return this;
      }
   }

   public static Variant variant() {
      return new Variant();
   }

   public static Variant merge(Variant param0, Variant param1) {
      Variant var2 = new Variant();
      var2.values.putAll(param0.values);
      var2.values.putAll(param1.values);
      return var2;
   }

   public JsonElement get() {
      JsonObject var1 = new JsonObject();
      this.values.values().forEach(l_param1 -> l_param1.addToVariant(var1));
      return var1;
   }

   public static JsonElement convertList(List<Variant> param0) {
      if (param0.size() == 1) {
         return param0.get(0).get();
      }

      JsonArray var1 = new JsonArray();
      param0.forEach(l_param1 -> var1.add(l_param1.get()));
      return var1;
   }
}
