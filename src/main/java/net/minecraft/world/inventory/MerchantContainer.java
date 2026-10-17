package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantContainer implements Container {
   private final Merchant merchant;
   private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
   @Nullable
   private MerchantOffer activeOffer;
   private int selectionHint;
   private int futureXp;

   public MerchantContainer(Merchant param0) {
      this.merchant = param0;
   }

   @Override
   public int getContainerSize() {
      return this.itemStacks.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.itemStacks) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int param0) {
      return this.itemStacks.get(param0);
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      ItemStack var3 = this.itemStacks.get(param0);
      if (param0 == 2 && !var3.isEmpty()) {
         return ContainerHelper.removeItem(this.itemStacks, param0, var3.getCount());
      }

      ItemStack var4 = ContainerHelper.removeItem(this.itemStacks, param0, param1);
      if (!var4.isEmpty() && this.isPaymentSlot(param0)) {
         this.updateSellItem();
      }

      return var4;
   }

   private boolean isPaymentSlot(int param0) {
      return param0 == 0 || param0 == 1;
   }

   @Override
   public ItemStack removeItemNoUpdate(int param0) {
      return ContainerHelper.takeItem(this.itemStacks, param0);
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      this.itemStacks.set(param0, param1);
      if (!param1.isEmpty() && param1.getCount() > this.getMaxStackSize()) {
         param1.setCount(this.getMaxStackSize());
      }

      if (this.isPaymentSlot(param0)) {
         this.updateSellItem();
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.merchant.getTradingPlayer() == param0;
   }

   @Override
   public void setChanged() {
      this.updateSellItem();
   }

   public void updateSellItem() {
      this.activeOffer = null;
      ItemStack var1;
      ItemStack var2;
      if (this.itemStacks.get(0).isEmpty()) {
         var1 = this.itemStacks.get(1);
         var2 = ItemStack.EMPTY;
      } else {
         var1 = this.itemStacks.get(0);
         var2 = this.itemStacks.get(1);
      }

      if (var1.isEmpty()) {
         this.setItem(2, ItemStack.EMPTY);
         this.futureXp = 0;
      } else {
         MerchantOffers var3 = this.merchant.getOffers();
         if (!var3.isEmpty()) {
            MerchantOffer var4 = var3.getRecipeFor(var1, var2, this.selectionHint);
            if (var4 == null || var4.isOutOfStock()) {
               this.activeOffer = var4;
               var4 = var3.getRecipeFor(var2, var1, this.selectionHint);
            }

            if (var4 != null && !var4.isOutOfStock()) {
               this.activeOffer = var4;
               this.setItem(2, var4.assemble());
               this.futureXp = var4.getXp();
            } else {
               this.setItem(2, ItemStack.EMPTY);
               this.futureXp = 0;
            }
         }

         this.merchant.notifyTradeUpdated(this.getItem(2));
      }
   }

   @Nullable
   public MerchantOffer getActiveOffer() {
      return this.activeOffer;
   }

   public void setSelectionHint(int param0) {
      this.selectionHint = param0;
      this.updateSellItem();
   }

   @Override
   public void clearContent() {
      this.itemStacks.clear();
   }

   public int getFutureXp() {
      return this.futureXp;
   }
}
