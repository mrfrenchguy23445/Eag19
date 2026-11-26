package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
   ApplyExplosionDecay(LootItemCondition[] param0) {
      super(param0);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.EXPLOSION_DECAY;
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      Float var3 = param1.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
      if (var3 != null) {
         RandomSource var4 = param1.getRandom();
         float var5 = 1.0F / var3;
         int var6 = param0.getCount();
         int var7 = 0;

         for (int var8 = 0; var8 < var6; var8++) {
            if (var4.nextFloat() <= var5) {
               var7++;
            }
         }

         param0.setCount(var7);
      }

      return param0;
   }

   public static LootItemConditionalFunction.Builder<?> explosionDecay() {
      return simpleBuilder(ApplyExplosionDecay::new);
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<ApplyExplosionDecay> {
      public ApplyExplosionDecay deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         return new ApplyExplosionDecay(param2);
      }
   }
}
