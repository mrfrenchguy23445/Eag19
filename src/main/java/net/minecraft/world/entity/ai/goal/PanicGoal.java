package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public class PanicGoal extends Goal {
   public static final int WATER_CHECK_DISTANCE_VERTICAL = 1;
   protected final PathfinderMob mob;
   protected final double speedModifier;
   protected double posX;
   protected double posY;
   protected double posZ;
   protected boolean isRunning;

   public PanicGoal(PathfinderMob param0, double param1) {
      this.mob = param0;
      this.speedModifier = param1;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (!this.shouldPanic()) {
         return false;
      }

      if (this.mob.isOnFire()) {
         BlockPos var1 = this.lookForWater(this.mob.level, this.mob, 5);
         if (var1 != null) {
            this.posX = var1.getX();
            this.posY = var1.getY();
            this.posZ = var1.getZ();
            return true;
         }
      }

      return this.findRandomPosition();
   }

   protected boolean shouldPanic() {
      return this.mob.getLastHurtByMob() != null || this.mob.isFreezing() || this.mob.isOnFire();
   }

   protected boolean findRandomPosition() {
      Vec3 var1 = DefaultRandomPos.getPos(this.mob, 5, 4);
      if (var1 == null) {
         return false;
      }

      this.posX = var1.x;
      this.posY = var1.y;
      this.posZ = var1.z;
      return true;
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
      this.isRunning = true;
   }

   @Override
   public void stop() {
      this.isRunning = false;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.getNavigation().isDone();
   }

   @Nullable
   protected BlockPos lookForWater(BlockGetter param0, Entity param1, int param2) {
      BlockPos var4 = param1.blockPosition();
      return !param0.getBlockState(var4).getCollisionShape(param0, var4).isEmpty()
         ? null
         : BlockPos.findClosestMatch(param1.blockPosition(), param2, 1, l_param1 -> param0.getFluidState(l_param1).is(FluidTags.WATER)).orElse(null);
   }
}
