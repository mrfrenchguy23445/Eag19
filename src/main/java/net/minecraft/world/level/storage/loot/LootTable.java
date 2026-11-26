package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

public class LootTable {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final LootTable EMPTY = new LootTable(LootContextParamSets.EMPTY, new LootPool[0], new LootItemFunction[0]);
   public static final LootContextParamSet DEFAULT_PARAM_SET = LootContextParamSets.ALL_PARAMS;
   final LootContextParamSet paramSet;
   final LootPool[] pools;
   final LootItemFunction[] functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

   LootTable(LootContextParamSet param0, LootPool[] param1, LootItemFunction[] param2) {
      this.paramSet = param0;
      this.pools = param1;
      this.functions = param2;
      this.compositeFunction = LootItemFunctions.compose(param2);
   }

   public static Consumer<ItemStack> createStackSplitter(LootContext param0, Consumer<ItemStack> param1) {
      return l_param2 -> {
         if (l_param2.isItemEnabled(param0.getLevel().enabledFeatures())) {
            if (l_param2.getCount() < l_param2.getMaxStackSize()) {
               param1.accept(l_param2);
            } else {
               int l_var3 = l_param2.getCount();

               while (l_var3 > 0) {
                  ItemStack l_var4 = l_param2.copy();
                  l_var4.setCount(Math.min(l_param2.getMaxStackSize(), l_var3));
                  l_var3 -= l_var4.getCount();
                  param1.accept(l_var4);
               }
            }
         }
      };
   }

   public void getRandomItemsRaw(LootContext param0, Consumer<ItemStack> param1) {
      if (param0.addVisitedTable(this)) {
         Consumer<ItemStack> var3 = LootItemFunction.decorate(this.compositeFunction, param1, param0);

         for (LootPool var7 : this.pools) {
            var7.addRandomItems(var3, param0);
         }

         param0.removeVisitedTable(this);
      } else {
         LOGGER.warn("Detected infinite loop in loot tables");
      }
   }

   public void getRandomItems(LootContext param0, Consumer<ItemStack> param1) {
      this.getRandomItemsRaw(param0, createStackSplitter(param0, param1));
   }

   public ObjectArrayList<ItemStack> getRandomItems(LootContext param0) {
      ObjectArrayList<ItemStack> var2 = new ObjectArrayList<>();
      this.getRandomItems(param0, var2::add);
      return var2;
   }

   public LootContextParamSet getParamSet() {
      return this.paramSet;
   }

   public void validate(ValidationContext param0) {
      for (int var2 = 0; var2 < this.pools.length; var2++) {
         this.pools[var2].validate(param0.forChild(".pools[" + var2 + "]"));
      }

      for (int var2 = 0; var2 < this.functions.length; var2++) {
         this.functions[var2].validate(param0.forChild(".functions[" + var2 + "]"));
      }
   }

   public void fill(Container param0, LootContext param1) {
      ObjectArrayList<ItemStack> var3 = this.getRandomItems(param1);
      RandomSource var4 = param1.getRandom();
      List<Integer> var5 = this.getAvailableSlots(param0, var4);
      this.shuffleAndSplitItems(var3, var5.size(), var4);

      for (ItemStack var7 : var3) {
         if (var5.isEmpty()) {
            LOGGER.warn("Tried to over-fill a container");
            return;
         }

         if (var7.isEmpty()) {
            param0.setItem(var5.remove(var5.size() - 1), ItemStack.EMPTY);
         } else {
            param0.setItem(var5.remove(var5.size() - 1), var7);
         }
      }
   }

   private void shuffleAndSplitItems(ObjectArrayList<ItemStack> param0, int param1, RandomSource param2) {
      List<ItemStack> var4 = Lists.newArrayList();
      Iterator<ItemStack> var5 = param0.iterator();

      while (var5.hasNext()) {
         ItemStack var6 = var5.next();
         if (var6.isEmpty()) {
            var5.remove();
         } else if (var6.getCount() > 1) {
            var4.add(var6);
            var5.remove();
         }
      }

      while (param1 - param0.size() - var4.size() > 0 && !var4.isEmpty()) {
         ItemStack var5x = var4.remove(Mth.nextInt(param2, 0, var4.size() - 1));
         int var6 = Mth.nextInt(param2, 1, var5x.getCount() / 2);
         ItemStack var7 = var5x.split(var6);
         if (var5x.getCount() > 1 && param2.nextBoolean()) {
            var4.add(var5x);
         } else {
            param0.add(var5x);
         }

         if (var7.getCount() > 1 && param2.nextBoolean()) {
            var4.add(var7);
         } else {
            param0.add(var7);
         }
      }

      param0.addAll(var4);
      Util.shuffle(param0, param2);
   }

   private List<Integer> getAvailableSlots(Container param0, RandomSource param1) {
      ObjectArrayList<Integer> var3 = new ObjectArrayList<>();

      for (int var4 = 0; var4 < param0.getContainerSize(); var4++) {
         if (param0.getItem(var4).isEmpty()) {
            var3.add(var4);
         }
      }

      Util.shuffle(var3, param1);
      return var3;
   }

   public static LootTable.Builder lootTable() {
      return new LootTable.Builder();
   }

   public static class Builder implements FunctionUserBuilder<LootTable.Builder> {
      private final List<LootPool> pools = Lists.newArrayList();
      private final List<LootItemFunction> functions = Lists.newArrayList();
      private LootContextParamSet paramSet = LootTable.DEFAULT_PARAM_SET;

      public LootTable.Builder withPool(LootPool.Builder param0) {
         this.pools.add(param0.build());
         return this;
      }

      public LootTable.Builder setParamSet(LootContextParamSet param0) {
         this.paramSet = param0;
         return this;
      }

      public LootTable.Builder apply(LootItemFunction.Builder param0) {
         this.functions.add(param0.build());
         return this;
      }

      public LootTable.Builder unwrap() {
         return this;
      }

      public LootTable build() {
         return new LootTable(this.paramSet, this.pools.toArray(new LootPool[0]), this.functions.toArray(new LootItemFunction[0]));
      }
   }

   public static class Serializer implements JsonDeserializer<LootTable>, JsonSerializer<LootTable> {
      public LootTable deserialize(JsonElement param0, Type param1, JsonDeserializationContext param2) throws JsonParseException {
         JsonObject var4 = GsonHelper.convertToJsonObject(param0, "loot table");
         LootPool[] var5 = GsonHelper.getAsObject(var4, "pools", new LootPool[0], param2, LootPool[].class);
         LootContextParamSet var6 = null;
         if (var4.has("type")) {
            String var7 = GsonHelper.getAsString(var4, "type");
            var6 = LootContextParamSets.get(new ResourceLocation(var7));
         }

         LootItemFunction[] var7 = GsonHelper.getAsObject(var4, "functions", new LootItemFunction[0], param2, LootItemFunction[].class);
         return new LootTable(var6 != null ? var6 : LootContextParamSets.ALL_PARAMS, var5, var7);
      }

      public JsonElement serialize(LootTable param0, Type param1, JsonSerializationContext param2) {
         JsonObject var4 = new JsonObject();
         if (param0.paramSet != LootTable.DEFAULT_PARAM_SET) {
            ResourceLocation var5 = LootContextParamSets.getKey(param0.paramSet);
            if (var5 != null) {
               var4.addProperty("type", var5.toString());
            } else {
               LootTable.LOGGER.warn("Failed to find id for param set {}", param0.paramSet);
            }
         }

         if (param0.pools.length > 0) {
            var4.add("pools", param2.serialize(param0.pools));
         }

         if (!ArrayUtils.isEmpty(param0.functions)) {
            var4.add("functions", param2.serialize(param0.functions));
         }

         return var4;
      }
   }
}
