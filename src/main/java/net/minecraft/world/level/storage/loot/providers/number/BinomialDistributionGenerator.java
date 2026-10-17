package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public final class BinomialDistributionGenerator implements NumberProvider {
   final NumberProvider n;
   final NumberProvider p;

   BinomialDistributionGenerator(NumberProvider param0, NumberProvider param1) {
      this.n = param0;
      this.p = param1;
   }

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   @Override
   public int getInt(LootContext param0) {
      int var2 = this.n.getInt(param0);
      float var3 = this.p.getFloat(param0);
      RandomSource var4 = param0.getRandom();
      int var5 = 0;

      for (int var6 = 0; var6 < var2; var6++) {
         if (var4.nextFloat() < var3) {
            var5++;
         }
      }

      return var5;
   }

   @Override
   public float getFloat(LootContext param0) {
      return this.getInt(param0);
   }

   public static BinomialDistributionGenerator binomial(int param0, float param1) {
      return new BinomialDistributionGenerator(ConstantValue.exactly(param0), ConstantValue.exactly(param1));
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }

   public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<BinomialDistributionGenerator> {
      public BinomialDistributionGenerator deserialize(JsonObject param0, JsonDeserializationContext param1) {
         NumberProvider var3 = GsonHelper.getAsObject(param0, "n", param1, NumberProvider.class);
         NumberProvider var4 = GsonHelper.getAsObject(param0, "p", param1, NumberProvider.class);
         return new BinomialDistributionGenerator(var3, var4);
      }

      public void serialize(JsonObject param0, BinomialDistributionGenerator param1, JsonSerializationContext param2) {
         param0.add("n", param2.serialize(param1.n));
         param0.add("p", param2.serialize(param1.p));
      }
   }
}
