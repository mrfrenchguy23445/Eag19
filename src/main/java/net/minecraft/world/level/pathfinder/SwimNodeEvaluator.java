package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
   private final boolean allowBreaching;
   private final Long2ObjectMap<BlockPathTypes> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();

   public SwimNodeEvaluator(boolean param0) {
      this.allowBreaching = param0;
   }

   @Override
   public void prepare(PathNavigationRegion param0, Mob param1) {
      super.prepare(param0, param1);
      this.pathTypesByPosCache.clear();
   }

   @Override
   public void done() {
      super.done();
      this.pathTypesByPosCache.clear();
   }

   @Override
   public Node getStart() {
      return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
   }

   @Override
   public Target getGoal(double param0, double param2, double param4) {
      return this.getTargetFromNode(this.getNode(Mth.floor(param0), Mth.floor(param2), Mth.floor(param4)));
   }

   @Override
   public int getNeighbors(Node[] param0, Node param1) {
      int var3 = 0;
      Map<Direction, Node> var4 = Maps.newEnumMap(Direction.class);

      for (Direction var8 : Direction.values()) {
         Node var9 = this.findAcceptedNode(param1.x + var8.getStepX(), param1.y + var8.getStepY(), param1.z + var8.getStepZ());
         var4.put(var8, var9);
         if (this.isNodeValid(var9)) {
            param0[var3++] = var9;
         }
      }

      for (Direction var6 : Direction.Plane.HORIZONTAL) {
         Direction var7 = var6.getClockWise();
         Node var8 = this.findAcceptedNode(param1.x + var6.getStepX() + var7.getStepX(), param1.y, param1.z + var6.getStepZ() + var7.getStepZ());
         if (this.isDiagonalNodeValid(var8, var4.get(var6), var4.get(var7))) {
            param0[var3++] = var8;
         }
      }

      return var3;
   }

   protected boolean isNodeValid(@Nullable Node param0) {
      return param0 != null && !param0.closed;
   }

   protected boolean isDiagonalNodeValid(@Nullable Node param0, @Nullable Node param1, @Nullable Node param2) {
      return this.isNodeValid(param0) && param1 != null && param1.costMalus >= 0.0F && param2 != null && param2.costMalus >= 0.0F;
   }

   @Nullable
   protected Node findAcceptedNode(int param0, int param1, int param2) {
      Node var4 = null;
      BlockPathTypes var5 = this.getCachedBlockType(param0, param1, param2);
      if (this.allowBreaching && var5 == BlockPathTypes.BREACH || var5 == BlockPathTypes.WATER) {
         float var6 = this.mob.getPathfindingMalus(var5);
         if (var6 >= 0.0F) {
            var4 = this.getNode(param0, param1, param2);
            var4.type = var5;
            var4.costMalus = Math.max(var4.costMalus, var6);
            if (this.level.getFluidState(new BlockPos(param0, param1, param2)).isEmpty()) {
               var4.costMalus += 8.0F;
            }
         }
      }

      return var4;
   }

   protected BlockPathTypes getCachedBlockType(int param0, int param1, int param2) {
      return this.pathTypesByPosCache
         .computeIfAbsent(BlockPos.asLong(param0, param1, param2), l_param3 -> this.getBlockPathType(this.level, param0, param1, param2));
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3) {
      return this.getBlockPathType(param0, param1, param2, param3, this.mob);
   }

   @Override
   public BlockPathTypes getBlockPathType(BlockGetter param0, int param1, int param2, int param3, Mob param4) {
      BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

      for (int var7 = param1; var7 < param1 + this.entityWidth; var7++) {
         for (int var8 = param2; var8 < param2 + this.entityHeight; var8++) {
            for (int var9 = param3; var9 < param3 + this.entityDepth; var9++) {
               FluidState var10 = param0.getFluidState(var6.set(var7, var8, var9));
               BlockState var11 = param0.getBlockState(var6.set(var7, var8, var9));
               if (var10.isEmpty() && var11.isPathfindable(param0, var6.below(), PathComputationType.WATER) && var11.isAir()) {
                  return BlockPathTypes.BREACH;
               }

               if (!var10.is(FluidTags.WATER)) {
                  return BlockPathTypes.BLOCKED;
               }
            }
         }
      }

      BlockState var7 = param0.getBlockState(var6);
      return var7.isPathfindable(param0, var6, PathComputationType.WATER) ? BlockPathTypes.WATER : BlockPathTypes.BLOCKED;
   }
}
