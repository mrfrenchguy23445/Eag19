package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootPool {
   final LootPoolEntryContainer[] entries;
   final LootItemCondition[] conditions;
   private final Predicate<LootContext> compositeCondition;
   final LootItemFunction[] functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
   final NumberProvider rolls;
   final NumberProvider bonusRolls;

   LootPool(LootPoolEntryContainer[] param0, LootItemCondition[] param1, LootItemFunction[] param2, NumberProvider param3, NumberProvider param4) {
      this.entries = param0;
      this.conditions = param1;
      this.compositeCondition = LootItemConditions.andConditions(param1);
      this.functions = param2;
      this.compositeFunction = LootItemFunctions.compose(param2);
      this.rolls = param3;
      this.bonusRolls = param4;
   }

   private void addRandomItem(Consumer<ItemStack> param0, LootContext param1) {
      RandomSource var3 = param1.getRandom();
      List<LootPoolEntry> var4 = Lists.newArrayList();
      MutableInt var5 = new MutableInt();

      for (LootPoolEntryContainer var9 : this.entries) {
         var9.expand(param1, l_param3 -> {
            int l_var4 = l_param3.getWeight(param1.getLuck());
            if (l_var4 > 0) {
               var4.add(l_param3);
               var5.add(l_var4);
            }
         });
      }

      int var6 = var4.size();
      if (var5.intValue() != 0 && var6 != 0) {
         if (var6 == 1) {
            var4.get(0).createItemStack(param0, param1);
         } else {
            int var7 = var3.nextInt(var5.intValue());

            for (LootPoolEntry var9 : var4) {
               var7 -= var9.getWeight(param1.getLuck());
               if (var7 < 0) {
                  var9.createItemStack(param0, param1);
                  return;
               }
            }
         }
      }
   }

   public void addRandomItems(Consumer<ItemStack> param0, LootContext param1) {
      if (this.compositeCondition.test(param1)) {
         Consumer<ItemStack> var3 = LootItemFunction.decorate(this.compositeFunction, param0, param1);
         int var4 = this.rolls.getInt(param1) + Mth.floor(this.bonusRolls.getFloat(param1) * param1.getLuck());

         for (int var5 = 0; var5 < var4; var5++) {
            this.addRandomItem(var3, param1);
         }
      }
   }

   public void validate(ValidationContext param0) {
      for (int var2 = 0; var2 < this.conditions.length; var2++) {
         this.conditions[var2].validate(param0.forChild(".condition[" + var2 + "]"));
      }

      for (int var2 = 0; var2 < this.functions.length; var2++) {
         this.functions[var2].validate(param0.forChild(".functions[" + var2 + "]"));
      }

      for (int var2 = 0; var2 < this.entries.length; var2++) {
         this.entries[var2].validate(param0.forChild(".entries[" + var2 + "]"));
      }

      this.rolls.validate(param0.forChild(".rolls"));
      this.bonusRolls.validate(param0.forChild(".bonusRolls"));
   }

   public static LootPool.Builder lootPool() {
      return new LootPool.Builder();
   }

   public static class Builder implements FunctionUserBuilder<LootPool.Builder>, ConditionUserBuilder<LootPool.Builder> {
      private final List<LootPoolEntryContainer> entries = Lists.newArrayList();
      private final List<LootItemCondition> conditions = Lists.newArrayList();
      private final List<LootItemFunction> functions = Lists.newArrayList();
      private NumberProvider rolls = ConstantValue.exactly(1.0F);
      private NumberProvider bonusRolls = ConstantValue.exactly(0.0F);

      public LootPool.Builder setRolls(NumberProvider param0) {
         this.rolls = param0;
         return this;
      }

      public LootPool.Builder unwrap() {
         return this;
      }

      public LootPool.Builder setBonusRolls(NumberProvider param0) {
         this.bonusRolls = param0;
         return this;
      }

      public LootPool.Builder add(LootPoolEntryContainer.Builder<?> param0) {
         this.entries.add(param0.build());
         return this;
      }

      public LootPool.Builder when(LootItemCondition.Builder param0) {
         this.conditions.add(param0.build());
         return this;
      }

      public LootPool.Builder apply(LootItemFunction.Builder param0) {
         this.functions.add(param0.build());
         return this;
      }

      public LootPool build() {
         if (this.rolls == null) {
            throw new IllegalArgumentException("Rolls not set");
         } else {
            return new LootPool(
               this.entries.toArray(new LootPoolEntryContainer[0]),
               this.conditions.toArray(new LootItemCondition[0]),
               this.functions.toArray(new LootItemFunction[0]),
               this.rolls,
               this.bonusRolls
            );
         }
      }
   }

   public static class Serializer implements JsonDeserializer<LootPool>, JsonSerializer<LootPool> {
      public LootPool deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = GsonHelper.convertToJsonObject(param0, "loot pool");
         LootPoolEntryContainer[] var5 = GsonHelper.getAsObject(var4, "entries", param2, LootPoolEntryContainer[].class);
         LootItemCondition[] var6 = GsonHelper.getAsObject(var4, "conditions", new LootItemCondition[0], param2, LootItemCondition[].class);
         LootItemFunction[] var7 = GsonHelper.getAsObject(var4, "functions", new LootItemFunction[0], param2, LootItemFunction[].class);
         NumberProvider var8 = GsonHelper.getAsObject(var4, "rolls", param2, NumberProvider.class);
         NumberProvider var9 = GsonHelper.getAsObject(var4, "bonus_rolls", ConstantValue.exactly(0.0F), param2, NumberProvider.class);
         return new LootPool(var5, var6, var7, var8, var9);
      }

      public JsonElement serialize(LootPool param0, Type param1, JsonSerializationContext param2) {
         JsonObject var4 = new JsonObject();
         var4.add("rolls", param2.serialize(param0.rolls));
         var4.add("bonus_rolls", param2.serialize(param0.bonusRolls));
         var4.add("entries", param2.serialize(param0.entries));
         if (!ArrayUtils.isEmpty(param0.conditions)) {
            var4.add("conditions", param2.serialize(param0.conditions));
         }

         if (!ArrayUtils.isEmpty(param0.functions)) {
            var4.add("functions", param2.serialize(param0.functions));
         }

         return var4;
      }
   }
}
