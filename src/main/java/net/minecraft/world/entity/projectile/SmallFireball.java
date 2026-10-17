package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SmallFireball extends Fireball {
   public SmallFireball(EntityType<? extends SmallFireball> param0, Level param1) {
      super(param0, param1);
   }

   public SmallFireball(Level param0, LivingEntity param1, double param2, double param4, double param6) {
      super(EntityType.SMALL_FIREBALL, param1, param2, param4, param6, param0);
   }

   public SmallFireball(Level param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      super(EntityType.SMALL_FIREBALL, param1, param3, param5, param7, param9, param11, param0);
   }

   @Override
   protected void onHitEntity(EntityHitResult param0) {
      super.onHitEntity(param0);
      if (!this.level.isClientSide) {
         Entity var2 = param0.getEntity();
         Entity var3 = this.getOwner();
         int var4 = var2.getRemainingFireTicks();
         var2.setSecondsOnFire(5);
         if (!var2.hurt(this.damageSources().fireball(this, var3), 5.0F)) {
            var2.setRemainingFireTicks(var4);
         } else if (var3 instanceof LivingEntity) {
            this.doEnchantDamageEffects((LivingEntity)var3, var2);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult param0) {
      super.onHitBlock(param0);
      if (!this.level.isClientSide) {
         Entity var2 = this.getOwner();
         if (!(var2 instanceof Mob) || this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            BlockPos var3 = param0.getBlockPos().relative(param0.getDirection());
            if (this.level.isEmptyBlock(var3)) {
               this.level.setBlockAndUpdate(var3, BaseFireBlock.getState(this.level, var3));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult param0) {
      super.onHit(param0);
      if (!this.level.isClientSide) {
         this.discard();
      }
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      return false;
   }
}
