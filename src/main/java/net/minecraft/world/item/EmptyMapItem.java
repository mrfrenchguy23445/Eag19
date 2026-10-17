package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EmptyMapItem extends ComplexItem {
   public EmptyMapItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      if (param0.isClientSide) {
         return InteractionResultHolder.success(var4);
      }

      if (!param1.getAbilities().instabuild) {
         var4.shrink(1);
      }

      param1.awardStat(Stats.ITEM_USED.get(this));
      param1.level.playSound(null, param1, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, param1.getSoundSource(), 1.0F, 1.0F);
      ItemStack var5 = MapItem.create(param0, param1.getBlockX(), param1.getBlockZ(), (byte)0, true, false);
      if (var4.isEmpty()) {
         return InteractionResultHolder.consume(var5);
      }

      if (!param1.getInventory().add(var5.copy())) {
         param1.drop(var5, false);
      }

      return InteractionResultHolder.consume(var4);
   }
}
