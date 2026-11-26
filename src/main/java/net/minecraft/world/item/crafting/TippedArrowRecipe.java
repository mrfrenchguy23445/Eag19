package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

public class TippedArrowRecipe extends CustomRecipe {
   public TippedArrowRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      if (param0.getWidth() == 3 && param0.getHeight() == 3) {
         for (int var3 = 0; var3 < param0.getWidth(); var3++) {
            for (int var4 = 0; var4 < param0.getHeight(); var4++) {
               ItemStack var5 = param0.getItem(var3 + var4 * param0.getWidth());
               if (var5.isEmpty()) {
                  return false;
               }

               if (var3 == 1 && var4 == 1) {
                  if (!var5.is(Items.LINGERING_POTION)) {
                     return false;
                  }
               } else if (!var5.is(Items.ARROW)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = param0.getItem(1 + param0.getWidth());
      if (!var3.is(Items.LINGERING_POTION)) {
         return ItemStack.EMPTY;
      }

      ItemStack var4 = new ItemStack(Items.TIPPED_ARROW, 8);
      PotionUtils.setPotion(var4, PotionUtils.getPotion(var3));
      PotionUtils.setCustomEffects(var4, PotionUtils.getCustomEffects(var3));
      return var4;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 >= 2 && param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.TIPPED_ARROW;
   }
}
