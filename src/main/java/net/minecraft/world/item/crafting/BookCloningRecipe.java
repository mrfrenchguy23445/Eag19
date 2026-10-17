package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

public class BookCloningRecipe extends CustomRecipe {
   public BookCloningRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.WRITTEN_BOOK)) {
               if (!var4.isEmpty()) {
                  return false;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.WRITABLE_BOOK)) {
                  return false;
               }

               var3++;
            }
         }
      }

      return !var4.isEmpty() && var4.hasTag() && var3 > 0;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.WRITTEN_BOOK)) {
               if (!var4.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.WRITABLE_BOOK)) {
                  return ItemStack.EMPTY;
               }

               var3++;
            }
         }
      }

      if (!var4.isEmpty() && var4.hasTag() && var3 >= 1 && WrittenBookItem.getGeneration(var4) < 2) {
         ItemStack var5 = new ItemStack(Items.WRITTEN_BOOK, var3);
         CompoundTag var6 = var4.getTag().copy();
         var6.putInt("generation", WrittenBookItem.getGeneration(var4) + 1);
         var5.setTag(var6);
         return var5;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public NonNullList<ItemStack> getRemainingItems(CraftingContainer param0) {
      NonNullList<ItemStack> var2 = NonNullList.withSize(param0.getContainerSize(), ItemStack.EMPTY);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         ItemStack var4 = param0.getItem(var3);
         if (var4.getItem().hasCraftingRemainingItem()) {
            var2.set(var3, new ItemStack(var4.getItem().getCraftingRemainingItem()));
         } else if (var4.getItem() instanceof WrittenBookItem) {
            ItemStack var5 = var4.copy();
            var5.setCount(1);
            var2.set(var3, var5);
            break;
         }
      }

      return var2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.BOOK_CLONING;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 >= 3 && param1 >= 3;
   }
}
