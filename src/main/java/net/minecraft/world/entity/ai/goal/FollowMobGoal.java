package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public class FollowMobGoal extends Goal {
   private final Mob mob;
   private final Predicate<Mob> followPredicate;
   @Nullable
   private Mob followingMob;
   private final double speedModifier;
   private final PathNavigation navigation;
   private int timeToRecalcPath;
   private final float stopDistance;
   private float oldWaterCost;
   private final float areaSize;

   public FollowMobGoal(Mob param0, double param1, float param3, float param4) {
      this.mob = param0;
      this.followPredicate = l_param1 -> l_param1 != null && param0.getClass() != l_param1.getClass();
      this.speedModifier = param1;
      this.navigation = param0.getNavigation();
      this.stopDistance = param3;
      this.areaSize = param4;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      if (!(param0.getNavigation() instanceof GroundPathNavigation) && !(param0.getNavigation() instanceof FlyingPathNavigation)) {
         throw new IllegalArgumentException("Unsupported mob type for FollowMobGoal");
      }
   }

   @Override
   public boolean canUse() {
      List<Mob> var1 = this.mob.level.getEntitiesOfClass(Mob.class, this.mob.getBoundingBox().inflate(this.areaSize), this.followPredicate);
      if (!var1.isEmpty()) {
         for (Mob var3 : var1) {
            if (!var3.isInvisible()) {
               this.followingMob = var3;
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean canContinueToUse() {
      return this.followingMob != null && !this.navigation.isDone() && this.mob.distanceToSqr(this.followingMob) > this.stopDistance * this.stopDistance;
   }

   @Override
   public void start() {
      this.timeToRecalcPath = 0;
      this.oldWaterCost = this.mob.getPathfindingMalus(BlockPathTypes.WATER);
      this.mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
   }

   @Override
   public void stop() {
      this.followingMob = null;
      this.navigation.stop();
      this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
   }

   @Override
   public void tick() {
      if (this.followingMob != null && !this.mob.isLeashed()) {
         this.mob.getLookControl().setLookAt(this.followingMob, 10.0F, this.mob.getMaxHeadXRot());
         if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            double var1 = this.mob.getX() - this.followingMob.getX();
            double var3 = this.mob.getY() - this.followingMob.getY();
            double var5 = this.mob.getZ() - this.followingMob.getZ();
            double var7 = var1 * var1 + var3 * var3 + var5 * var5;
            if (!(var7 <= this.stopDistance * this.stopDistance)) {
               this.navigation.moveTo(this.followingMob, this.speedModifier);
            } else {
               this.navigation.stop();
               LookControl var9 = this.followingMob.getLookControl();
               if (var7 <= this.stopDistance
                  || var9.getWantedX() == this.mob.getX() && var9.getWantedY() == this.mob.getY() && var9.getWantedZ() == this.mob.getZ()) {
                  double var10 = this.followingMob.getX() - this.mob.getX();
                  double var12 = this.followingMob.getZ() - this.mob.getZ();
                  this.navigation.moveTo(this.mob.getX() - var10, this.mob.getY(), this.mob.getZ() - var12, this.speedModifier);
               }
            }
         }
      }
   }
}
