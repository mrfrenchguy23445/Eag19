package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetStewEffectFunction extends LootItemConditionalFunction {
   final Map<MobEffect, NumberProvider> effectDurationMap;

   SetStewEffectFunction(LootItemCondition[] param0, Map<MobEffect, NumberProvider> param1) {
      super(param0);
      this.effectDurationMap = ImmutableMap.copyOf(param1);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_STEW_EFFECT;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.effectDurationMap
         .values()
         .stream()
         .flatMap(l_param0 -> l_param0.getReferencedContextParams().stream())
         .collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      if (param0.is(Items.SUSPICIOUS_STEW) && !this.effectDurationMap.isEmpty()) {
         RandomSource var3 = param1.getRandom();
         int var4 = var3.nextInt(this.effectDurationMap.size());
         Entry<MobEffect, NumberProvider> var5 = Iterables.get(this.effectDurationMap.entrySet(), var4);
         MobEffect var6 = var5.getKey();
         int var7 = var5.getValue().getInt(param1);
         if (!var6.isInstantenous()) {
            var7 *= 20;
         }

         SuspiciousStewItem.saveMobEffect(param0, var6, var7);
         return param0;
      } else {
         return param0;
      }
   }

   public static SetStewEffectFunction.Builder stewEffect() {
      return new SetStewEffectFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetStewEffectFunction.Builder> {
      private final Map<MobEffect, NumberProvider> effectDurationMap = Maps.newLinkedHashMap();

      protected SetStewEffectFunction.Builder getThis() {
         return this;
      }

      public SetStewEffectFunction.Builder withEffect(MobEffect param0, NumberProvider param1) {
         this.effectDurationMap.put(param0, param1);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetStewEffectFunction(this.getConditions(), this.effectDurationMap);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetStewEffectFunction> {
      public void serialize(JsonObject param0, SetStewEffectFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         if (!param1.effectDurationMap.isEmpty()) {
            JsonArray var4 = new JsonArray();

            for (MobEffect var6 : param1.effectDurationMap.keySet()) {
               JsonObject var7 = new JsonObject();
               ResourceLocation var8 = BuiltInRegistries.MOB_EFFECT.getKey(var6);
               if (var8 == null) {
                  throw new IllegalArgumentException("Don't know how to serialize mob effect " + var6);
               }

               var7.add("type", new JsonPrimitive(var8.toString()));
               var7.add("duration", param2.serialize(param1.effectDurationMap.get(var6)));
               var4.add(var7);
            }

            param0.add("effects", var4);
         }
      }

      public SetStewEffectFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         Map<MobEffect, NumberProvider> var4 = Maps.newLinkedHashMap();
         if (param0.has("effects")) {
            for (JsonElement var7 : GsonHelper.getAsJsonArray(param0, "effects")) {
               String var8 = GsonHelper.getAsString(var7.getAsJsonObject(), "type");
               MobEffect var9 = BuiltInRegistries.MOB_EFFECT
                  .getOptional(new ResourceLocation(var8))
                  .orElseThrow(() -> new JsonSyntaxException("Unknown mob effect '" + var8 + "'"));
               NumberProvider var10 = GsonHelper.getAsObject(var7.getAsJsonObject(), "duration", param1, NumberProvider.class);
               var4.put(var9, var10);
            }
         }

         return new SetStewEffectFunction(param2, var4);
      }
   }
}
