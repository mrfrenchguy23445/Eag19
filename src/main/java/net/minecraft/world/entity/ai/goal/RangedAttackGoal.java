package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class RangedAttackGoal extends Goal {
   private final Mob mob;
   private final RangedAttackMob rangedAttackMob;
   @Nullable
   private LivingEntity target;
   private int attackTime = -1;
   private final double speedModifier;
   private int seeTime;
   private final int attackIntervalMin;
   private final int attackIntervalMax;
   private final float attackRadius;
   private final float attackRadiusSqr;

   public RangedAttackGoal(RangedAttackMob param0, double param1, int param3, float param4) {
      this(param0, param1, param3, param3, param4);
   }

   public RangedAttackGoal(RangedAttackMob param0, double param1, int param3, int param4, float param5) {
      if (!(param0 instanceof LivingEntity)) {
         throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
      }

      this.rangedAttackMob = param0;
      this.mob = (Mob)param0;
      this.speedModifier = param1;
      this.attackIntervalMin = param3;
      this.attackIntervalMax = param4;
      this.attackRadius = param5;
      this.attackRadiusSqr = param5 * param5;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      LivingEntity var1 = this.mob.getTarget();
      if (var1 != null && var1.isAlive()) {
         this.target = var1;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse() || this.target.isAlive() && !this.mob.getNavigation().isDone();
   }

   @Override
   public void stop() {
      this.target = null;
      this.seeTime = 0;
      this.attackTime = -1;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      double var1 = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
      boolean var3 = this.mob.getSensing().hasLineOfSight(this.target);
      if (var3) {
         this.seeTime++;
      } else {
         this.seeTime = 0;
      }

      if (!(var1 > this.attackRadiusSqr) && this.seeTime >= 5) {
         this.mob.getNavigation().stop();
      } else {
         this.mob.getNavigation().moveTo(this.target, this.speedModifier);
      }

      this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
      if (--this.attackTime == 0) {
         if (!var3) {
            return;
         }

         float var4 = (float)Math.sqrt(var1) / this.attackRadius;
         float var5 = Mth.clamp(var4, 0.1F, 1.0F);
         this.rangedAttackMob.performRangedAttack(this.target, var5);
         this.attackTime = Mth.floor(var4 * (this.attackIntervalMax - this.attackIntervalMin) + this.attackIntervalMin);
      } else if (this.attackTime < 0) {
         this.attackTime = Mth.floor(Mth.lerp(Math.sqrt(var1) / this.attackRadius, this.attackIntervalMin, this.attackIntervalMax));
      }
   }
}
