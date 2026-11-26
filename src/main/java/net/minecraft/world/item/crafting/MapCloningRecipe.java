package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapCloningRecipe extends CustomRecipe {
   public MapCloningRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.FILLED_MAP)) {
               if (!var4.isEmpty()) {
                  return false;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.MAP)) {
                  return false;
               }

               var3++;
            }
         }
      }

      return !var4.isEmpty() && var3 > 0;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.FILLED_MAP)) {
               if (!var4.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.MAP)) {
                  return ItemStack.EMPTY;
               }

               var3++;
            }
         }
      }

      if (!var4.isEmpty() && var3 >= 1) {
         ItemStack var5 = var4.copy();
         var5.setCount(var3 + 1);
         return var5;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 >= 3 && param1 >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_CLONING;
   }
}
