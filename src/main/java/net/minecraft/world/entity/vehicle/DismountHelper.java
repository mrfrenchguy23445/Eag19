package net.minecraft.world.entity.vehicle;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DismountHelper {
   public static int[][] offsetsForDirection(Direction param0) {
      Direction var1 = param0.getClockWise();
      Direction var2 = var1.getOpposite();
      Direction var3 = param0.getOpposite();
      return new int[][]{
         {var1.getStepX(), var1.getStepZ()},
         {var2.getStepX(), var2.getStepZ()},
         {var3.getStepX() + var1.getStepX(), var3.getStepZ() + var1.getStepZ()},
         {var3.getStepX() + var2.getStepX(), var3.getStepZ() + var2.getStepZ()},
         {param0.getStepX() + var1.getStepX(), param0.getStepZ() + var1.getStepZ()},
         {param0.getStepX() + var2.getStepX(), param0.getStepZ() + var2.getStepZ()},
         {var3.getStepX(), var3.getStepZ()},
         {param0.getStepX(), param0.getStepZ()}
      };
   }

   public static boolean isBlockFloorValid(double param0) {
      return !Double.isInfinite(param0) && param0 < 1.0;
   }

   public static boolean canDismountTo(CollisionGetter param0, LivingEntity param1, AABB param2) {
      for (VoxelShape var5 : param0.getBlockCollisions(param1, param2)) {
         if (!var5.isEmpty()) {
            return false;
         }
      }

      return param0.getWorldBorder().isWithinBounds(param2);
   }

   public static boolean canDismountTo(CollisionGetter param0, Vec3 param1, LivingEntity param2, Pose param3) {
      return canDismountTo(param0, param2, param2.getLocalBoundsForPose(param3).move(param1));
   }

   public static VoxelShape nonClimbableShape(BlockGetter param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      return !var2.is(BlockTags.CLIMBABLE) && (!(var2.getBlock() instanceof TrapDoorBlock) || !var2.getValue(TrapDoorBlock.OPEN))
         ? var2.getCollisionShape(param0, param1)
         : Shapes.empty();
   }

   public static double findCeilingFrom(BlockPos param0, int param1, Function<BlockPos, VoxelShape> param2) {
      BlockPos.MutableBlockPos var3 = param0.mutable();
      int var4 = 0;

      while (var4 < param1) {
         VoxelShape var5 = param2.apply(var3);
         if (!var5.isEmpty()) {
            return param0.getY() + var4 + var5.min(Direction.Axis.Y);
         }

         var4++;
         var3.move(Direction.UP);
      }

      return Double.POSITIVE_INFINITY;
   }

   @Nullable
   public static Vec3 findSafeDismountLocation(EntityType<?> param0, CollisionGetter param1, BlockPos param2, boolean param3) {
      if (param3 && param0.isBlockDangerous(param1.getBlockState(param2))) {
         return null;
      }

      double var4 = param1.getBlockFloorHeight(nonClimbableShape(param1, param2), () -> nonClimbableShape(param1, param2.below()));
      if (!isBlockFloorValid(var4)) {
         return null;
      }

      if (param3 && var4 <= 0.0 && param0.isBlockDangerous(param1.getBlockState(param2.below()))) {
         return null;
      }

      Vec3 var6 = Vec3.upFromBottomCenterOf(param2, var4);
      AABB var7 = param0.getDimensions().makeBoundingBox(var6);

      for (VoxelShape var10 : param1.getBlockCollisions(null, var7)) {
         if (!var10.isEmpty()) {
            return null;
         }
      }

      if (param0 != EntityType.PLAYER
         || !param1.getBlockState(param2).is(BlockTags.INVALID_SPAWN_INSIDE) && !param1.getBlockState(param2.above()).is(BlockTags.INVALID_SPAWN_INSIDE)) {
         return !param1.getWorldBorder().isWithinBounds(var7) ? null : var6;
      } else {
         return null;
      }
   }
}
