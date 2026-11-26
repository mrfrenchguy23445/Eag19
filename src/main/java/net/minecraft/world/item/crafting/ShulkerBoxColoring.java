package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ShulkerBoxColoring extends CustomRecipe {
   public ShulkerBoxColoring(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      int var3 = 0;
      int var4 = 0;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (Block.byItem(var6.getItem()) instanceof ShulkerBoxBlock) {
               var3++;
            } else {
               if (!(var6.getItem() instanceof DyeItem)) {
                  return false;
               }

               var4++;
            }

            if (var4 > 1 || var3 > 1) {
               return false;
            }
         }
      }

      return var3 == 1 && var4 == 1;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = ItemStack.EMPTY;
      DyeItem var4 = (DyeItem)Items.WHITE_DYE;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            Item var7 = var6.getItem();
            if (Block.byItem(var7) instanceof ShulkerBoxBlock) {
               var3 = var6;
            } else if (var7 instanceof DyeItem) {
               var4 = (DyeItem)var7;
            }
         }
      }

      ItemStack var5 = ShulkerBoxBlock.getColoredItemStack(var4.getDyeColor());
      if (var3.hasTag()) {
         var5.setTag(var3.getTag().copy());
      }

      return var5;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHULKER_BOX_COLORING;
   }
}
