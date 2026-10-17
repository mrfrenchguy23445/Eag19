package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class RepairItemRecipe extends CustomRecipe {
   public RepairItemRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      List<ItemStack> var3 = Lists.newArrayList();

      for (int var4 = 0; var4 < param0.getContainerSize(); var4++) {
         ItemStack var5 = param0.getItem(var4);
         if (!var5.isEmpty()) {
            var3.add(var5);
            if (var3.size() > 1) {
               ItemStack var6 = var3.get(0);
               if (!var5.is(var6.getItem()) || var6.getCount() != 1 || var5.getCount() != 1 || !var6.getItem().canBeDepleted()) {
                  return false;
               }
            }
         }
      }

      return var3.size() == 2;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      List<ItemStack> var3 = Lists.newArrayList();

      for (int var4 = 0; var4 < param0.getContainerSize(); var4++) {
         ItemStack var5 = param0.getItem(var4);
         if (!var5.isEmpty()) {
            var3.add(var5);
            if (var3.size() > 1) {
               ItemStack var6 = var3.get(0);
               if (!var5.is(var6.getItem()) || var6.getCount() != 1 || var5.getCount() != 1 || !var6.getItem().canBeDepleted()) {
                  return ItemStack.EMPTY;
               }
            }
         }
      }

      if (var3.size() == 2) {
         ItemStack var4 = var3.get(0);
         ItemStack var5 = var3.get(1);
         if (var4.is(var5.getItem()) && var4.getCount() == 1 && var5.getCount() == 1 && var4.getItem().canBeDepleted()) {
            Item var6 = var4.getItem();
            int var7 = var6.getMaxDamage() - var4.getDamageValue();
            int var8 = var6.getMaxDamage() - var5.getDamageValue();
            int var9 = var7 + var8 + var6.getMaxDamage() * 5 / 100;
            int var10 = var6.getMaxDamage() - var9;
            if (var10 < 0) {
               var10 = 0;
            }

            ItemStack var11 = new ItemStack(var4.getItem());
            var11.setDamageValue(var10);
            Map<Enchantment, Integer> var12 = Maps.newHashMap();
            Map<Enchantment, Integer> var13 = EnchantmentHelper.getEnchantments(var4);
            Map<Enchantment, Integer> var14 = EnchantmentHelper.getEnchantments(var5);
            BuiltInRegistries.ENCHANTMENT.stream().filter(Enchantment::isCurse).forEach(l_param3 -> {
               int l_var4 = Math.max(var13.getOrDefault(l_param3, 0), var14.getOrDefault(l_param3, 0));
               if (l_var4 > 0) {
                  var12.put(l_param3, l_var4);
               }
            });
            if (!var12.isEmpty()) {
               EnchantmentHelper.setEnchantments(var12, var11);
            }

            return var11;
         }
      }

      return ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.REPAIR_ITEM;
   }
}
