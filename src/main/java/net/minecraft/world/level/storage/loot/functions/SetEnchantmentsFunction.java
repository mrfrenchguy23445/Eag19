package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
   final Map<Enchantment, NumberProvider> enchantments;
   final boolean add;

   SetEnchantmentsFunction(LootItemCondition[] param0, Map<Enchantment, NumberProvider> param1, boolean param2) {
      super(param0);
      this.enchantments = ImmutableMap.copyOf(param1);
      this.add = param2;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ENCHANTMENTS;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.enchantments.values().stream().flatMap(l_param0 -> l_param0.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      Object2IntMap<Enchantment> var3 = new Object2IntOpenHashMap<>();
      this.enchantments.forEach((l_param2, l_param3) -> var3.put(l_param2, l_param3.getInt(param1)));
      if (param0.getItem() == Items.BOOK) {
         ItemStack var4 = new ItemStack(Items.ENCHANTED_BOOK);
         var3.forEach((l_param1, l_param2) -> EnchantedBookItem.addEnchantment(var4, new EnchantmentInstance(l_param1, l_param2)));
         return var4;
      }

      Map<Enchantment, Integer> var4 = EnchantmentHelper.getEnchantments(param0);
      if (this.add) {
         var3.forEach((l_param1, l_param2) -> updateEnchantment(var4, l_param1, Math.max(var4.getOrDefault(l_param1, 0) + l_param2, 0)));
      } else {
         var3.forEach((l_param1, l_param2) -> updateEnchantment(var4, l_param1, Math.max(l_param2, 0)));
      }

      EnchantmentHelper.setEnchantments(var4, param0);
      return param0;
   }

   private static void updateEnchantment(Map<Enchantment, Integer> param0, Enchantment param1, int param2) {
      if (param2 == 0) {
         param0.remove(param1);
      } else {
         param0.put(param1, param2);
      }
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetEnchantmentsFunction.Builder> {
      private final Map<Enchantment, NumberProvider> enchantments = Maps.newHashMap();
      private final boolean add;

      public Builder() {
         this(false);
      }

      public Builder(boolean param0) {
         this.add = param0;
      }

      protected SetEnchantmentsFunction.Builder getThis() {
         return this;
      }

      public SetEnchantmentsFunction.Builder withEnchantment(Enchantment param0, NumberProvider param1) {
         this.enchantments.put(param0, param1);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetEnchantmentsFunction(this.getConditions(), this.enchantments, this.add);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetEnchantmentsFunction> {
      public void serialize(JsonObject param0, SetEnchantmentsFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         JsonObject var4 = new JsonObject();
         param1.enchantments.forEach((l_param2, l_param3) -> {
            ResourceLocation l_var4 = BuiltInRegistries.ENCHANTMENT.getKey(l_param2);
            if (l_var4 == null) {
               throw new IllegalArgumentException("Don't know how to serialize enchantment " + l_param2);
            }

            var4.add(l_var4.toString(), param2.serialize(l_param3));
         });
         param0.add("enchantments", var4);
         param0.addProperty("add", param1.add);
      }

      public SetEnchantmentsFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         Map<Enchantment, NumberProvider> var4 = Maps.newHashMap();
         if (param0.has("enchantments")) {
            JsonObject var5 = GsonHelper.getAsJsonObject(param0, "enchantments");

            for (Entry<String, JsonElement> var7 : var5.entrySet()) {
               String var8 = var7.getKey();
               JsonElement var9 = var7.getValue();
               Enchantment var10 = BuiltInRegistries.ENCHANTMENT
                  .getOptional(new ResourceLocation(var8))
                  .orElseThrow(() -> new JsonSyntaxException("Unknown enchantment '" + var8 + "'"));
               NumberProvider var11 = param1.deserialize(var9, NumberProvider.class);
               var4.put(var10, var11);
            }
         }

         boolean var5 = GsonHelper.getAsBoolean(param0, "add", false);
         return new SetEnchantmentsFunction(param2, var4, var5);
      }
   }
}
