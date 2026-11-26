package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
   private final MerchantContainer slots;
   private final Player player;
   private int removeCount;
   private final Merchant merchant;

   public MerchantResultSlot(Player param0, Merchant param1, MerchantContainer param2, int param3, int param4, int param5) {
      super(param2, param3, param4, param5);
      this.player = param0;
      this.merchant = param1;
      this.slots = param2;
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
   protected void checkTakeAchievements(ItemStack param0) {
      param0.onCraftedBy(this.player.level, this.player, this.removeCount);
      this.removeCount = 0;
   }

   @Override
   public void onTake(Player param0, ItemStack param1) {
      this.checkTakeAchievements(param1);
      MerchantOffer var3 = this.slots.getActiveOffer();
      if (var3 != null) {
         ItemStack var4 = this.slots.getItem(0);
         ItemStack var5 = this.slots.getItem(1);
         if (var3.take(var4, var5) || var3.take(var5, var4)) {
            this.merchant.notifyTrade(var3);
            param0.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.slots.setItem(0, var4);
            this.slots.setItem(1, var5);
         }

         this.merchant.overrideXp(this.merchant.getVillagerXp() + var3.getXp());
      }
   }
}
