package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
   public MapExtendingRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(
         param0,
         "",
         param1,
         3,
         3,
         NonNullList.of(
            Ingredient.EMPTY,
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.FILLED_MAP),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER),
            Ingredient.of(Items.PAPER)
         ),
         new ItemStack(Items.MAP)
      );
   }

   @Override
   public boolean matches(CraftingContainer param0, Level param1) {
      if (!super.matches(param0, param1)) {
         return false;
      }

      ItemStack var3 = ItemStack.EMPTY;

      for (int var4 = 0; var4 < param0.getContainerSize() && var3.isEmpty(); var4++) {
         ItemStack var5 = param0.getItem(var4);
         if (var5.is(Items.FILLED_MAP)) {
            var3 = var5;
         }
      }

      if (var3.isEmpty()) {
         return false;
      } else {
         MapItemSavedData var4 = MapItem.getSavedData(var3, param1);
         if (var4 == null) {
            return false;
         } else {
            return var4.isExplorationMap() ? false : var4.scale < 4;
         }
      }
   }

   @Override
   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = ItemStack.EMPTY;

      for (int var4 = 0; var4 < param0.getContainerSize() && var3.isEmpty(); var4++) {
         ItemStack var5 = param0.getItem(var4);
         if (var5.is(Items.FILLED_MAP)) {
            var3 = var5;
         }
      }

      var3 = var3.copy();
      var3.setCount(1);
      var3.getOrCreateTag().putInt("map_scale_direction", 1);
      return var3;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_EXTENDING;
   }
}
