package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   final List<Enchantment> enchantments;

   EnchantRandomlyFunction(LootItemCondition[] param0, Collection<Enchantment> param1) {
      super(param0);
      this.enchantments = ImmutableList.copyOf(param1);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.ENCHANT_RANDOMLY;
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      RandomSource var4 = param1.getRandom();
      Enchantment var3;
      if (this.enchantments.isEmpty()) {
         boolean var5 = param0.is(Items.BOOK);
         List<Enchantment> var6 = BuiltInRegistries.ENCHANTMENT
            .stream()
            .filter(Enchantment::isDiscoverable)
            .filter(l_param2 -> var5 || l_param2.canEnchant(param0))
            .collect(Collectors.toList());
         if (var6.isEmpty()) {
            LOGGER.warn("Couldn't find a compatible enchantment for {}", param0);
            return param0;
         }

         var3 = var6.get(var4.nextInt(var6.size()));
      } else {
         var3 = this.enchantments.get(var4.nextInt(this.enchantments.size()));
      }

      return enchantItem(param0, var3, var4);
   }

   private static ItemStack enchantItem(ItemStack param0, Enchantment param1, RandomSource param2) {
      int var3 = Mth.nextInt(param2, param1.getMinLevel(), param1.getMaxLevel());
      if (param0.is(Items.BOOK)) {
         param0 = new ItemStack(Items.ENCHANTED_BOOK);
         EnchantedBookItem.addEnchantment(param0, new EnchantmentInstance(param1, var3));
      } else {
         param0.enchant(param1, var3);
      }

      return param0;
   }

   public static EnchantRandomlyFunction.Builder randomEnchantment() {
      return new EnchantRandomlyFunction.Builder();
   }

   public static LootItemConditionalFunction.Builder<?> randomApplicableEnchantment() {
      return simpleBuilder(l_param0 -> new EnchantRandomlyFunction(l_param0, ImmutableList.of()));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantRandomlyFunction.Builder> {
      private final Set<Enchantment> enchantments = Sets.newHashSet();

      protected EnchantRandomlyFunction.Builder getThis() {
         return this;
      }

      public EnchantRandomlyFunction.Builder withEnchantment(Enchantment param0) {
         this.enchantments.add(param0);
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantRandomlyFunction(this.getConditions(), this.enchantments);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<EnchantRandomlyFunction> {
      public void serialize(JsonObject param0, EnchantRandomlyFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         if (!param1.enchantments.isEmpty()) {
            JsonArray var4 = new JsonArray();

            for (Enchantment var6 : param1.enchantments) {
               ResourceLocation var7 = BuiltInRegistries.ENCHANTMENT.getKey(var6);
               if (var7 == null) {
                  throw new IllegalArgumentException("Don't know how to serialize enchantment " + var6);
               }

               var4.add(new JsonPrimitive(var7.toString()));
            }

            param0.add("enchantments", var4);
         }
      }

      public EnchantRandomlyFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         List<Enchantment> var4 = Lists.newArrayList();
         if (param0.has("enchantments")) {
            for (JsonElement var7 : GsonHelper.getAsJsonArray(param0, "enchantments")) {
               String var8 = GsonHelper.convertToString(var7, "enchantment");
               Enchantment var9 = BuiltInRegistries.ENCHANTMENT
                  .getOptional(new ResourceLocation(var8))
                  .orElseThrow(() -> new JsonSyntaxException("Unknown enchantment '" + var8 + "'"));
               var4.add(var9);
            }
         }

         return new EnchantRandomlyFunction(param2, var4);
      }
   }
}
