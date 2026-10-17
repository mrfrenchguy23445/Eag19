package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public interface Equipable extends Vanishable {
   EquipmentSlot getEquipmentSlot();

   default SoundEvent getEquipSound() {
      return SoundEvents.ARMOR_EQUIP_GENERIC;
   }

   default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item param0, Level param1, Player param2, InteractionHand param3) {
      ItemStack var5 = param2.getItemInHand(param3);
      EquipmentSlot var6 = Mob.getEquipmentSlotForItem(var5);
      ItemStack var7 = param2.getItemBySlot(var6);
      if (!EnchantmentHelper.hasBindingCurse(var7) && !ItemStack.matches(var5, var7)) {
         param2.setItemSlot(var6, var5.copy());
         if (!param1.isClientSide()) {
            param2.awardStat(Stats.ITEM_USED.get(param0));
         }

         if (var7.isEmpty()) {
            var5.setCount(0);
         } else {
            param2.setItemInHand(param3, var7.copy());
         }

         return InteractionResultHolder.sidedSuccess(var5, param1.isClientSide());
      } else {
         return InteractionResultHolder.fail(var5);
      }
   }

   @Nullable
   static Equipable get(ItemStack param0) {
      if (param0.getItem() instanceof Equipable var1) {
         return var1;
      } else {
         return param0.getItem() instanceof BlockItem var1 && var1.getBlock() instanceof Equipable var2 ? var2 : null;
      }
   }
}
