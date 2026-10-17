package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HoneyBottleItem extends Item {
   private static final int DRINK_DURATION = 40;

   public HoneyBottleItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public ItemStack finishUsingItem(ItemStack param0, Level param1, LivingEntity param2) {
      super.finishUsingItem(param0, param1, param2);
      if (param2 instanceof ServerPlayer var4) {
         CriteriaTriggers.CONSUME_ITEM.trigger(var4, param0);
         var4.awardStat(Stats.ITEM_USED.get(this));
      }

      if (!param1.isClientSide) {
         param2.removeEffect(MobEffects.POISON);
      }

      if (param0.isEmpty()) {
         return new ItemStack(Items.GLASS_BOTTLE);
      }

      if (param2 instanceof Player && !((Player)param2).getAbilities().instabuild) {
         ItemStack var4 = new ItemStack(Items.GLASS_BOTTLE);
         Player var5 = (Player)param2;
         if (!var5.getInventory().add(var4)) {
            var5.drop(var4, false);
         }
      }

      return param0;
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return 40;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.DRINK;
   }

   @Override
   public SoundEvent getDrinkingSound() {
      return SoundEvents.HONEY_DRINK;
   }

   @Override
   public SoundEvent getEatingSound() {
      return SoundEvents.HONEY_DRINK;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      return ItemUtils.startUsingInstantly(param0, param1, param2);
   }
}
