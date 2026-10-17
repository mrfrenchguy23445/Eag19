package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
   private final boolean prefersShallowSwimming;
   private float oldWalkableCost;
   private float oldWaterBorderCost;

   public AmphibiousNodeEvaluator(boolean param0) {
      this.prefersShallowSwimming = param0;
   }

   @Override
   public void prepare(PathNavigationRegion param0, Mob param1) {
      super.prepare(param0, param1);
      param1.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
      this.oldWalkableCost = param1.getPathfindingMalus(BlockPathTypes.WALKABLE);
      param1.setPathfindingMalus(BlockPathTypes.WALKABLE, 6.0F);
      this.oldWaterBorderCost = param1.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
      param1.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 4.0F);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableCost);
      this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderCost);
      super.done();
   }

   @Override
   public Node getStart() {
      return !this.mob.isInWater()
         ? super.getStart()
         : this.getStartNode(
            new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ))
         );
   }

   @Override
   public Target getGoal(double param0, double param2, double param4) {
      return this.getTargetFromNode(this.getNode(Mth.floor(param0), Mth.floor(param2 + 0.5), Mth.floor(param4)));
   }

   @Override
   public int getNeighbors(Node[] param0, Node param1) {
      int var3 = super.getNeighbors(param0, param1);
      BlockPathTypes var5 = this.getCachedBlockType(this.mob, param1.x, param1.y + 1, param1.z);
      BlockPathTypes var6 = this.getCachedBlockType(this.mob, param1.x, param1.y, param1.z);
      int var4;
      if (this.mob.getPathfindingMalus(var5) >= 0.0F && var6 != BlockPathTypes.STICKY_HONEY) {
         var4 = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      } else {
         var4 = 0;
      }

      double var7 = this.getFloorLevel(new BlockPos(param1.x, param1.y, param1.z));
      Node var9 = this.findAcceptedNode(param1.x, param1.y + 1, param1.z, Math.max(0, var4 - 1), var7, Direction.UP, var6);
      Node var10 = this.findAcceptedNode(param1.x, param1.y - 1, param1.z, var4, var7, Direction.DOWN, var6);
      if (this.isVerticalNeighborValid(var9, param1)) {
         param0[var3++] = var9;
      }

      if (this.isVerticalNeighborValid(var10, param1) && var6 != BlockPathTypes.TRAPDOOR) {
         param0[var3++] = var10;
      }

      for (int var11 = 0; var11 < var3; var11++) {
         Node var12 = param0[var11];
         if (var12.type == BlockPathTypes.WATER && this.prefersShallowSwimming && var12.y < this.mob.level.getSeaLevel() - 10) {
            var12.costMalus++;
         }
      }

      return var3;
   }

   private boolean isVerticalNeighborValid(@Nullable Node param0, Node param1) {
      return this.isNeighborValid(param0, param1) && param0.type == BlockPathTypes.WATER;
   }

   @Override
   protected boolean isAmphibious() {
      return true;
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3) {
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();
      BlockPathTypes var6 = getBlockPathTypeRaw(param0, var5.set(param1, param2, param3));
      if (var6 == BlockPathTypes.WATER) {
         for (Direction var10 : Direction.values()) {
            BlockPathTypes var11 = getBlockPathTypeRaw(param0, var5.set(param1, param2, param3).move(var10));
            if (var11 == BlockPathTypes.BLOCKED) {
               return BlockPathTypes.WATER_BORDER;
            }
         }

         return BlockPathTypes.WATER;
      } else {
         return getBlockPathTypeStatic(param0, var5);
      }
   }
}
