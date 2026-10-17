package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class IntRange {
   @Nullable
   final NumberProvider min;
   @Nullable
   final NumberProvider max;
   private final IntRange.IntLimiter limiter;
   private final IntRange.IntChecker predicate;

   public Set<LootContextParam<?>> getReferencedContextParams() {
      Builder<LootContextParam<?>> var1 = ImmutableSet.builder();
      if (this.min != null) {
         var1.addAll(this.min.getReferencedContextParams());
      }

      if (this.max != null) {
         var1.addAll(this.max.getReferencedContextParams());
      }

      return var1.build();
   }

   IntRange(@Nullable NumberProvider param0, @Nullable NumberProvider param1) {
      this.min = param0;
      this.max = param1;
      if (param0 == null) {
         if (param1 == null) {
            this.limiter = (l_param0, l_param1) -> l_param1;
            this.predicate = (l_param0, l_param1) -> true;
         } else {
            this.limiter = (l_param1, l_param2) -> Math.min(param1.getInt(l_param1), l_param2);
            this.predicate = (l_param1, l_param2) -> l_param2 <= param1.getInt(l_param1);
         }
      } else if (param1 == null) {
         this.limiter = (l_param1, l_param2) -> Math.max(param0.getInt(l_param1), l_param2);
         this.predicate = (l_param1, l_param2) -> l_param2 >= param0.getInt(l_param1);
      } else {
         this.limiter = (l_param2, l_param3) -> Mth.clamp(l_param3, param0.getInt(l_param2), param1.getInt(l_param2));
         this.predicate = (l_param2, l_param3) -> l_param3 >= param0.getInt(l_param2) && l_param3 <= param1.getInt(l_param2);
      }
   }

   public static IntRange exact(int param0) {
      ConstantValue var1 = ConstantValue.exactly(param0);
      return new IntRange(var1, var1);
   }

   public static IntRange range(int param0, int param1) {
      return new IntRange(ConstantValue.exactly(param0), ConstantValue.exactly(param1));
   }

   public static IntRange lowerBound(int param0) {
      return new IntRange(ConstantValue.exactly(param0), null);
   }

   public static IntRange upperBound(int param0) {
      return new IntRange(null, ConstantValue.exactly(param0));
   }

   public int clamp(LootContext param0, int param1) {
      return this.limiter.apply(param0, param1);
   }

   public boolean test(LootContext param0, int param1) {
      return this.predicate.test(param0, param1);
   }

   @FunctionalInterface
   interface IntChecker {
      boolean test(LootContext var1, int var2);
   }

   @FunctionalInterface
   interface IntLimiter {
      int apply(LootContext var1, int var2);
   }

   public static class Serializer implements JsonDeserializer<IntRange>, JsonSerializer<IntRange> {
      public IntRange deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) {
         if (param0.isJsonPrimitive()) {
            return IntRange.exact(param0.getAsInt());
         }

         JsonObject var4 = GsonHelper.convertToJsonObject(param0, "value");
         NumberProvider var5 = var4.has("min") ? GsonHelper.getAsObject(var4, "min", param2, NumberProvider.class) : null;
         NumberProvider var6 = var4.has("max") ? GsonHelper.getAsObject(var4, "max", param2, NumberProvider.class) : null;
         return new IntRange(var5, var6);
      }

      public JsonElement serialize(IntRange param0, Type param1, JsonSerializationContext param2) {
         JsonObject var4 = new JsonObject();
         if (Objects.equals(param0.max, param0.min)) {
            return param2.serialize(param0.min);
         }

         if (param0.max != null) {
            var4.add("max", param2.serialize(param0.max));
         }

         if (param0.min != null) {
            var4.add("min", param2.serialize(param0.min));
         }

         return var4;
      }
   }
}
