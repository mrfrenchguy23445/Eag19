package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class MoveTowardsTargetGoal extends Goal {
   private final PathfinderMob mob;
   @Nullable
   private LivingEntity target;
   private double wantedX;
   private double wantedY;
   private double wantedZ;
   private final double speedModifier;
   private final float within;

   public MoveTowardsTargetGoal(PathfinderMob param0, double param1, float param3) {
      this.mob = param0;
      this.speedModifier = param1;
      this.within = param3;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      this.target = this.mob.getTarget();
      if (this.target == null) {
         return false;
      }

      if (this.target.distanceToSqr(this.mob) > this.within * this.within) {
         return false;
      }

      Vec3 var1 = DefaultRandomPos.getPosTowards(this.mob, 16, 7, this.target.position(), (float) (Math.PI / 2));
      if (var1 == null) {
         return false;
      }

      this.wantedX = var1.x;
      this.wantedY = var1.y;
      this.wantedZ = var1.z;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.getNavigation().isDone() && this.target.isAlive() && this.target.distanceToSqr(this.mob) < this.within * this.within;
   }

   @Override
   public void stop() {
      this.target = null;
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
   }
}
