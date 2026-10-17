package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;

   public GroundPathNavigation(Mob param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected PathFinder createPathFinder(int param0) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      this.nodeEvaluator.setCanPassDoors(true);
      return new PathFinder(this.nodeEvaluator, param0);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.mob.isOnGround() || this.isInLiquid() || this.mob.isPassenger();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
   }

   @Override
   public Path createPath(BlockPos param0, int param1) {
      if (this.level.getBlockState(param0).isAir()) {
         BlockPos var3 = param0.below();

         while (var3.getY() > this.level.getMinBuildHeight() && this.level.getBlockState(var3).isAir()) {
            var3 = var3.below();
         }

         if (var3.getY() > this.level.getMinBuildHeight()) {
            return super.createPath(var3.above(), param1);
         }

         while (var3.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(var3).isAir()) {
            var3 = var3.above();
         }

         param0 = var3;
      }

      if (!this.level.getBlockState(param0).getMaterial().isSolid()) {
         return super.createPath(param0, param1);
      }

      BlockPos var3 = param0.above();

      while (var3.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(var3).getMaterial().isSolid()) {
         var3 = var3.above();
      }

      return super.createPath(var3, param1);
   }

   @Override
   public Path createPath(Entity param0, int param1) {
      return this.createPath(param0.blockPosition(), param1);
   }

   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int var1 = this.mob.getBlockY();
         BlockState var2 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), var1, this.mob.getZ()));
         int var3 = 0;

         while (var2.is(Blocks.WATER)) {
            var2 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++var1, this.mob.getZ()));
            if (++var3 > 16) {
               return this.mob.getBlockY();
            }
         }

         return var1;
      } else {
         return Mth.floor(this.mob.getY() + 0.5);
      }
   }

   @Override
   protected void trimPath() {
      super.trimPath();
      if (this.avoidSun) {
         if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
            return;
         }

         for (int var1 = 0; var1 < this.path.getNodeCount(); var1++) {
            Node var2 = this.path.getNode(var1);
            if (this.level.canSeeSky(new BlockPos(var2.x, var2.y, var2.z))) {
               this.path.truncateNodes(var1);
               return;
            }
         }
      }
   }

   protected boolean hasValidPathType(BlockPathTypes param0) {
      if (param0 == BlockPathTypes.WATER) {
         return false;
      } else {
         return param0 == BlockPathTypes.LAVA ? false : param0 != BlockPathTypes.OPEN;
      }
   }

   public void setCanOpenDoors(boolean param0) {
      this.nodeEvaluator.setCanOpenDoors(param0);
   }

   public boolean canPassDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setCanPassDoors(boolean param0) {
      this.nodeEvaluator.setCanPassDoors(param0);
   }

   public boolean canOpenDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setAvoidSun(boolean param0) {
      this.avoidSun = param0;
   }

   public void setCanWalkOverFences(boolean param0) {
      this.nodeEvaluator.setCanWalkOverFences(param0);
   }
}
