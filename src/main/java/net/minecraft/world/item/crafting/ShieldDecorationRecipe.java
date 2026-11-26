package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ShieldDecorationRecipe extends CustomRecipe {
   public ShieldDecorationRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      ItemStack var3 = ItemStack.EMPTY;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.getItem() instanceof BannerItem) {
               if (!var4.isEmpty()) {
                  return false;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.SHIELD)) {
                  return false;
               }

               if (!var3.isEmpty()) {
                  return false;
               }

               if (BlockItem.getBlockEntityData(var6) != null) {
                  return false;
               }

               var3 = var6;
            }
         }
      }

      return !var3.isEmpty() && !var4.isEmpty();
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = ItemStack.EMPTY;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.getItem() instanceof BannerItem) {
               var3 = var6;
            } else if (var6.is(Items.SHIELD)) {
               var4 = var6.copy();
            }
         }
      }

      if (var4.isEmpty()) {
         return var4;
      }

      CompoundTag var5 = BlockItem.getBlockEntityData(var3);
      CompoundTag var6 = var5 == null ? new CompoundTag() : var5.copy();
      var6.putInt("Base", ((BannerItem)var3.getItem()).getColor().getId());
      BlockItem.setBlockEntityData(var4, BlockEntityType.BANNER, var6);
      return var4;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHIELD_DECORATION;
   }
}
