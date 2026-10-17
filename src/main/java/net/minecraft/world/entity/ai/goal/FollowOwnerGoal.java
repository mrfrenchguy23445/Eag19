package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class FollowOwnerGoal extends Goal {
   public static final int TELEPORT_WHEN_DISTANCE_IS = 12;
   private static final int MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 2;
   private static final int MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 3;
   private static final int MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 1;
   private final TamableAnimal tamable;
   private LivingEntity owner;
   private final LevelReader level;
   private final double speedModifier;
   private final PathNavigation navigation;
   private int timeToRecalcPath;
   private final float stopDistance;
   private final float startDistance;
   private float oldWaterCost;
   private final boolean canFly;

   public FollowOwnerGoal(TamableAnimal param0, double param1, float param3, float param4, boolean param5) {
      this.tamable = param0;
      this.level = param0.level;
      this.speedModifier = param1;
      this.navigation = param0.getNavigation();
      this.startDistance = param3;
      this.stopDistance = param4;
      this.canFly = param5;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      if (!(param0.getNavigation() instanceof GroundPathNavigation) && !(param0.getNavigation() instanceof FlyingPathNavigation)) {
         throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
      }
   }

   @Override
   public boolean canUse() {
      LivingEntity var1 = this.tamable.getOwner();
      if (var1 == null) {
         return false;
      }

      if (var1.isSpectator()) {
         return false;
      }

      if (this.unableToMove()) {
         return false;
      }

      if (this.tamable.distanceToSqr(var1) < this.startDistance * this.startDistance) {
         return false;
      }

      this.owner = var1;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      if (this.navigation.isDone()) {
         return false;
      } else {
         return this.unableToMove() ? false : !(this.tamable.distanceToSqr(this.owner) <= this.stopDistance * this.stopDistance);
      }
   }

   private boolean unableToMove() {
      return this.tamable.isOrderedToSit() || this.tamable.isPassenger() || this.tamable.isLeashed();
   }

   @Override
   public void start() {
      this.timeToRecalcPath = 0;
      this.oldWaterCost = this.tamable.getPathfindingMalus(BlockPathTypes.WATER);
      this.tamable.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
   }

   @Override
   public void stop() {
      this.owner = null;
      this.navigation.stop();
      this.tamable.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
   }

   @Override
   public void tick() {
      this.tamable.getLookControl().setLookAt(this.owner, 10.0F, this.tamable.getMaxHeadXRot());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = this.adjustedTickDelay(10);
         if (this.tamable.distanceToSqr(this.owner) >= 144.0) {
            this.teleportToOwner();
         } else {
            this.navigation.moveTo(this.owner, this.speedModifier);
         }
      }
   }

   private void teleportToOwner() {
      BlockPos var1 = this.owner.blockPosition();

      for (int var2 = 0; var2 < 10; var2++) {
         int var3 = this.randomIntInclusive(-3, 3);
         int var4 = this.randomIntInclusive(-1, 1);
         int var5 = this.randomIntInclusive(-3, 3);
         boolean var6 = this.maybeTeleportTo(var1.getX() + var3, var1.getY() + var4, var1.getZ() + var5);
         if (var6) {
            return;
         }
      }
   }

   private boolean maybeTeleportTo(int param0, int param1, int param2) {
      if (Math.abs(param0 - this.owner.getX()) < 2.0 && Math.abs(param2 - this.owner.getZ()) < 2.0) {
         return false;
      }

      if (!this.canTeleportTo(new BlockPos(param0, param1, param2))) {
         return false;
      }

      this.tamable.moveTo(param0 + 0.5, param1, param2 + 0.5, this.tamable.getYRot(), this.tamable.getXRot());
      this.navigation.stop();
      return true;
   }

   private boolean canTeleportTo(BlockPos param0) {
      BlockPathTypes var2 = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, param0.mutable());
      if (var2 != BlockPathTypes.WALKABLE) {
         return false;
      }

      BlockState var3 = this.level.getBlockState(param0.below());
      if (!this.canFly && var3.getBlock() instanceof LeavesBlock) {
         return false;
      }

      BlockPos var4 = param0.subtract(this.tamable.blockPosition());
      return this.level.noCollision(this.tamable, this.tamable.getBoundingBox().move(var4));
   }

   private int randomIntInclusive(int param0, int param1) {
      return this.tamable.getRandom().nextInt(param1 - param0 + 1) + param0;
   }
}
