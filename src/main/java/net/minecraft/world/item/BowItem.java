package net.minecraft.world.item;

import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class BowItem extends ProjectileWeaponItem implements Vanishable {
   public static final int MAX_DRAW_DURATION = 20;
   public static final int DEFAULT_RANGE = 15;

   public BowItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public void releaseUsing(ItemStack param0, Level param1, LivingEntity param2, int param3) {
      if (param2 instanceof Player var5) {
         boolean var6 = var5.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, param0) > 0;
         ItemStack var7 = var5.getProjectile(param0);
         if (!var7.isEmpty() || var6) {
            if (var7.isEmpty()) {
               var7 = new ItemStack(Items.ARROW);
            }

            int var8 = this.getUseDuration(param0) - param3;
            float var9 = getPowerForTime(var8);
            if (!(var9 < 0.1)) {
               boolean var10 = var6 && var7.is(Items.ARROW);
               if (!param1.isClientSide) {
                  ArrowItem var11 = (ArrowItem)(var7.getItem() instanceof ArrowItem ? var7.getItem() : Items.ARROW);
                  AbstractArrow var12 = var11.createArrow(param1, var7, var5);
                  var12.shootFromRotation(var5, var5.getXRot(), var5.getYRot(), 0.0F, var9 * 3.0F, 1.0F);
                  if (var9 == 1.0F) {
                     var12.setCritArrow(true);
                  }

                  int var13 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, param0);
                  if (var13 > 0) {
                     var12.setBaseDamage(var12.getBaseDamage() + var13 * 0.5 + 0.5);
                  }

                  int var14 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, param0);
                  if (var14 > 0) {
                     var12.setKnockback(var14);
                  }

                  if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, param0) > 0) {
                     var12.setSecondsOnFire(100);
                  }

                  param0.hurtAndBreak(1, var5, l_param1 -> l_param1.broadcastBreakEvent(var5.getUsedItemHand()));
                  if (var10 || var5.getAbilities().instabuild && (var7.is(Items.SPECTRAL_ARROW) || var7.is(Items.TIPPED_ARROW))) {
                     var12.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                  }

                  param1.addFreshEntity(var12);
               }

               param1.playSound(
                  null,
                  var5.getX(),
                  var5.getY(),
                  var5.getZ(),
                  SoundEvents.ARROW_SHOOT,
                  SoundSource.PLAYERS,
                  1.0F,
                  1.0F / (param1.getRandom().nextFloat() * 0.4F + 1.2F) + var9 * 0.5F
               );
               if (!var10 && !var5.getAbilities().instabuild) {
                  var7.shrink(1);
                  if (var7.isEmpty()) {
                     var5.getInventory().removeItem(var7);
                  }
               }

               var5.awardStat(Stats.ITEM_USED.get(this));
            }
         }
      }
   }

   public static float getPowerForTime(int param0) {
      float var1 = param0 / 20.0F;
      var1 = (var1 * var1 + var1 * 2.0F) / 3.0F;
      if (var1 > 1.0F) {
         var1 = 1.0F;
      }

      return var1;
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return 72000;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.BOW;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      boolean var5 = !param1.getProjectile(var4).isEmpty();
      if (!param1.getAbilities().instabuild && !var5) {
         return InteractionResultHolder.fail(var4);
      }

      param1.startUsingItem(param2);
      return InteractionResultHolder.consume(var4);
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public int getDefaultProjectileRange() {
      return 15;
   }
}
