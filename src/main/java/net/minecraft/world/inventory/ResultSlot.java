package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class ResultSlot extends Slot {
   private final CraftingContainer craftSlots;
   private final Player player;
   private int removeCount;

   public ResultSlot(Player param0, CraftingContainer param1, Container param2, int param3, int param4, int param5) {
      super(param2, param3, param4, param5);
      this.player = param0;
      this.craftSlots = param1;
   }

   @Override
   public boolean mayPlace(ItemStack param0) {
      return false;
   }

   @Override
   public ItemStack remove(int param0) {
      if (this.hasItem()) {
         this.removeCount = this.removeCount + Math.min(param0, this.getItem().getCount());
      }

      return super.remove(param0);
   }

   @Override
   protected void onQuickCraft(ItemStack param0, int param1) {
      this.removeCount += param1;
      this.checkTakeAchievements(param0);
   }

   @Override
   protected void onSwapCraft(int param0) {
      this.removeCount += param0;
   }

   @Override
   protected void checkTakeAchievements(ItemStack param0) {
      if (this.removeCount > 0) {
         param0.onCraftedBy(this.player.level, this.player, this.removeCount);
      }

      if (this.container instanceof RecipeHolder) {
         ((RecipeHolder)this.container).awardUsedRecipes(this.player);
      }

      this.removeCount = 0;
   }

   @Override
   public void onTake(Player param0, ItemStack param1) {
      this.checkTakeAchievements(param1);
      NonNullList<ItemStack> var3 = param0.level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, this.craftSlots, param0.level);

      for (int var4 = 0; var4 < var3.size(); var4++) {
         ItemStack var5 = this.craftSlots.getItem(var4);
         ItemStack var6 = var3.get(var4);
         if (!var5.isEmpty()) {
            this.craftSlots.removeItem(var4, 1);
            var5 = this.craftSlots.getItem(var4);
         }

         if (!var6.isEmpty()) {
            if (var5.isEmpty()) {
               this.craftSlots.setItem(var4, var6);
            } else if (ItemStack.isSame(var5, var6) && ItemStack.tagMatches(var5, var6)) {
               var6.grow(var5.getCount());
               this.craftSlots.setItem(var4, var6);
            } else if (!this.player.getInventory().add(var6)) {
               this.player.drop(var6, false);
            }
         }
      }
   }
}
