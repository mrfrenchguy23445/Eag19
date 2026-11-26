package net.minecraft.world.item.enchantment;

import java.util.Map.Entry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class ThornsEnchantment extends Enchantment {
   private static final float CHANCE_PER_LEVEL = 0.15F;

   public ThornsEnchantment(Enchantment.Rarity param0, EquipmentSlot... param1) {
      super(param0, EnchantmentCategory.ARMOR_CHEST, param1);
   }

   @Override
   public int getMinCost(int param0) {
      return 10 + 20 * (param0 - 1);
   }

   @Override
   public int getMaxCost(int param0) {
      return super.getMinCost(param0) + 50;
   }

   @Override
   public int getMaxLevel() {
      return 3;
   }

   @Override
   public boolean canEnchant(ItemStack param0) {
      return param0.getItem() instanceof ArmorItem ? true : super.canEnchant(param0);
   }

   @Override
   public void doPostHurt(LivingEntity param0, Entity param1, int param2) {
      RandomSource var4 = param0.getRandom();
      Entry<EquipmentSlot, ItemStack> var5 = EnchantmentHelper.getRandomItemWith(Enchantments.THORNS, param0);
      if (shouldHit(param2, var4)) {
         if (param1 != null) {
            param1.hurt(param0.damageSources().thorns(param0), getDamage(param2, var4));
         }

         if (var5 != null) {
            var5.getValue().hurtAndBreak(2, param0, l_param1 -> l_param1.broadcastBreakEvent(var5.getKey()));
         }
      }
   }

   public static boolean shouldHit(int param0, RandomSource param1) {
      return param0 <= 0 ? false : param1.nextFloat() < 0.15F * param0;
   }

   public static int getDamage(int param0, RandomSource param1) {
      return param0 > 10 ? param0 - 10 : 1 + param1.nextInt(4);
   }
}
