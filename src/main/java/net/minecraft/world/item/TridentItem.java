package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TridentItem extends Item implements Vanishable {
   public static final int THROW_THRESHOLD_TIME = 10;
   public static final float BASE_DAMAGE = 8.0F;
   public static final float SHOOT_POWER = 2.5F;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public TridentItem(Item.Properties param0) {
      super(param0);
      Builder<Attribute, AttributeModifier> var2 = ImmutableMultimap.builder();
      var2.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 8.0, AttributeModifier.Operation.ADDITION));
      var2.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -2.9F, AttributeModifier.Operation.ADDITION));
      this.defaultModifiers = var2.build();
   }

   @Override
   public boolean canAttackBlock(BlockState param0, Level param1, BlockPos param2, Player param3) {
      return !param3.isCreative();
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.SPEAR;
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return 72000;
   }

   @Override
   public void releaseUsing(ItemStack param0, Level param1, LivingEntity param2, int param3) {
      if (param2 instanceof Player var5) {
         int var6 = this.getUseDuration(param0) - param3;
         if (var6 >= 10) {
            int var7 = EnchantmentHelper.getRiptide(param0);
            if (var7 <= 0 || var5.isInWaterOrRain()) {
               if (!param1.isClientSide) {
                  param0.hurtAndBreak(1, var5, l_param1 -> l_param1.broadcastBreakEvent(param2.getUsedItemHand()));
                  if (var7 == 0) {
                     ThrownTrident var8 = new ThrownTrident(param1, var5, param0);
                     var8.shootFromRotation(var5, var5.getXRot(), var5.getYRot(), 0.0F, 2.5F + var7 * 0.5F, 1.0F);
                     if (var5.getAbilities().instabuild) {
                        var8.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                     }

                     param1.addFreshEntity(var8);
                     param1.playSound(null, var8, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                     if (!var5.getAbilities().instabuild) {
                        var5.getInventory().removeItem(param0);
                     }
                  }
               }

               var5.awardStat(Stats.ITEM_USED.get(this));
               if (var7 > 0) {
                  float var8 = var5.getYRot();
                  float var9 = var5.getXRot();
                  float var10 = -Mth.sin(var8 * (float) (Math.PI / 180.0)) * Mth.cos(var9 * (float) (Math.PI / 180.0));
                  float var11 = -Mth.sin(var9 * (float) (Math.PI / 180.0));
                  float var12 = Mth.cos(var8 * (float) (Math.PI / 180.0)) * Mth.cos(var9 * (float) (Math.PI / 180.0));
                  float var13 = Mth.sqrt(var10 * var10 + var11 * var11 + var12 * var12);
                  float var14 = 3.0F * ((1.0F + var7) / 4.0F);
                  var10 *= var14 / var13;
                  var11 *= var14 / var13;
                  var12 *= var14 / var13;
                  var5.push(var10, var11, var12);
                  var5.startAutoSpinAttack(20);
                  if (var5.isOnGround()) {
                     float var15 = 1.1999999F;
                     var5.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
                  }

                  SoundEvent var15;
                  if (var7 >= 3) {
                     var15 = SoundEvents.TRIDENT_RIPTIDE_3;
                  } else if (var7 == 2) {
                     var15 = SoundEvents.TRIDENT_RIPTIDE_2;
                  } else {
                     var15 = SoundEvents.TRIDENT_RIPTIDE_1;
                  }

                  param1.playSound(null, var5, var15, SoundSource.PLAYERS, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      if (var4.getDamageValue() >= var4.getMaxDamage() - 1) {
         return InteractionResultHolder.fail(var4);
      }

      if (EnchantmentHelper.getRiptide(var4) > 0 && !param1.isInWaterOrRain()) {
         return InteractionResultHolder.fail(var4);
      }

      param1.startUsingItem(param2);
      return InteractionResultHolder.consume(var4);
   }

   @Override
   public boolean hurtEnemy(ItemStack param0, LivingEntity param1, LivingEntity param2) {
      param0.hurtAndBreak(1, param2, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      return true;
   }

   @Override
   public boolean mineBlock(ItemStack param0, Level param1, BlockState param2, BlockPos param3, LivingEntity param4) {
      if (param2.getDestroySpeed(param1, param3) != 0.0) {
         param0.hurtAndBreak(2, param4, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   @Override
   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot param0) {
      return param0 == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(param0);
   }

   @Override
   public int getEnchantmentValue() {
      return 1;
   }
}
