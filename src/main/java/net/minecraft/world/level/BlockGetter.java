package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockGetter extends LevelHeightAccessor {
   @Nullable
   BlockEntity getBlockEntity(BlockPos var1);

   default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos param0, BlockEntityType<T> param1) {
      BlockEntity var3 = this.getBlockEntity(param0);
      return var3 != null && var3.getType() == param1 ? Optional.of((T)var3) : Optional.empty();
   }

   BlockState getBlockState(BlockPos var1);

   FluidState getFluidState(BlockPos var1);

   default int getLightEmission(BlockPos param0) {
      return this.getBlockState(param0).getLightEmission();
   }

   default int getMaxLightLevel() {
      return 15;
   }

   default Stream<BlockState> getBlockStates(AABB param0) {
      return BlockPos.betweenClosedStream(param0).map(this::getBlockState);
   }

   default BlockHitResult isBlockInLine(ClipBlockStateContext param0) {
      return traverseBlocks(
         param0.getFrom(),
         param0.getTo(),
         param0,
         (l_param0, l_param1) -> {
            BlockState l_var3 = this.getBlockState(l_param1);
            Vec3 l_var4 = l_param0.getFrom().subtract(l_param0.getTo());
            return l_param0.isTargetBlock().test(l_var3)
               ? new BlockHitResult(l_param0.getTo(), Direction.getNearest(l_var4.x, l_var4.y, l_var4.z), BlockPos.containing(l_param0.getTo()), false)
               : null;
         },
         l_param0 -> {
            Vec3 l_var1 = l_param0.getFrom().subtract(l_param0.getTo());
            return BlockHitResult.miss(l_param0.getTo(), Direction.getNearest(l_var1.x, l_var1.y, l_var1.z), BlockPos.containing(l_param0.getTo()));
         }
      );
   }

   default BlockHitResult clip(ClipContext param0) {
      return traverseBlocks(param0.getFrom(), param0.getTo(), param0, (l_param0, l_param1) -> {
         BlockState l_var3 = this.getBlockState(l_param1);
         FluidState l_var4 = this.getFluidState(l_param1);
         Vec3 l_var5 = l_param0.getFrom();
         Vec3 l_var6 = l_param0.getTo();
         VoxelShape l_var7 = l_param0.getBlockShape(l_var3, this, l_param1);
         BlockHitResult l_var8 = this.clipWithInteractionOverride(l_var5, l_var6, l_param1, l_var7, l_var3);
         VoxelShape l_var9 = l_param0.getFluidShape(l_var4, this, l_param1);
         BlockHitResult l_var10 = l_var9.clip(l_var5, l_var6, l_param1);
         double l_var11 = l_var8 == null ? Double.MAX_VALUE : l_param0.getFrom().distanceToSqr(l_var8.getLocation());
         double l_var13 = l_var10 == null ? Double.MAX_VALUE : l_param0.getFrom().distanceToSqr(l_var10.getLocation());
         return l_var11 <= l_var13 ? l_var8 : l_var10;
      }, l_param0 -> {
         Vec3 l_var1 = l_param0.getFrom().subtract(l_param0.getTo());
         return BlockHitResult.miss(l_param0.getTo(), Direction.getNearest(l_var1.x, l_var1.y, l_var1.z), BlockPos.containing(l_param0.getTo()));
      });
   }

   @Nullable
   default BlockHitResult clipWithInteractionOverride(Vec3 param0, Vec3 param1, BlockPos param2, VoxelShape param3, BlockState param4) {
      BlockHitResult var6 = param3.clip(param0, param1, param2);
      if (var6 != null) {
         BlockHitResult var7 = param4.getInteractionShape(this, param2).clip(param0, param1, param2);
         if (var7 != null && var7.getLocation().subtract(param0).lengthSqr() < var6.getLocation().subtract(param0).lengthSqr()) {
            return var6.withDirection(var7.getDirection());
         }
      }

      return var6;
   }

   default double getBlockFloorHeight(VoxelShape param0, Supplier<VoxelShape> param1) {
      if (!param0.isEmpty()) {
         return param0.max(Direction.Axis.Y);
      }

      double var3 = param1.get().max(Direction.Axis.Y);
      return var3 >= 1.0 ? var3 - 1.0 : Double.NEGATIVE_INFINITY;
   }

   default double getBlockFloorHeight(BlockPos param0) {
      return this.getBlockFloorHeight(this.getBlockState(param0).getCollisionShape(this, param0), () -> {
         BlockPos l_var2 = param0.below();
         return this.getBlockState(l_var2).getCollisionShape(this, l_var2);
      });
   }

   static <T, C> T traverseBlocks(Vec3 param0, Vec3 param1, C param2, BiFunction<C, BlockPos, T> param3, Function<C, T> param4) {
      if (param0.equals(param1)) {
         return param4.apply(param2);
      }

      double var5 = Mth.lerp(-1.0E-7, param1.x, param0.x);
      double var7 = Mth.lerp(-1.0E-7, param1.y, param0.y);
      double var9 = Mth.lerp(-1.0E-7, param1.z, param0.z);
      double var11 = Mth.lerp(-1.0E-7, param0.x, param1.x);
      double var13 = Mth.lerp(-1.0E-7, param0.y, param1.y);
      double var15 = Mth.lerp(-1.0E-7, param0.z, param1.z);
      int var17 = Mth.floor(var11);
      int var18 = Mth.floor(var13);
      int var19 = Mth.floor(var15);
      BlockPos.MutableBlockPos var20 = new BlockPos.MutableBlockPos(var17, var18, var19);
      T var21 = param3.apply(param2, var20);
      if (var21 != null) {
         return var21;
      }

      double var22 = var5 - var11;
      double var24 = var7 - var13;
      double var26 = var9 - var15;
      int var28 = Mth.sign(var22);
      int var29 = Mth.sign(var24);
      int var30 = Mth.sign(var26);
      double var31 = var28 == 0 ? Double.MAX_VALUE : var28 / var22;
      double var33 = var29 == 0 ? Double.MAX_VALUE : var29 / var24;
      double var35 = var30 == 0 ? Double.MAX_VALUE : var30 / var26;
      double var37 = var31 * (var28 > 0 ? 1.0 - Mth.frac(var11) : Mth.frac(var11));
      double var39 = var33 * (var29 > 0 ? 1.0 - Mth.frac(var13) : Mth.frac(var13));
      double var41 = var35 * (var30 > 0 ? 1.0 - Mth.frac(var15) : Mth.frac(var15));

      while (var37 <= 1.0 || var39 <= 1.0 || var41 <= 1.0) {
         if (var37 < var39) {
            if (var37 < var41) {
               var17 += var28;
               var37 += var31;
            } else {
               var19 += var30;
               var41 += var35;
            }
         } else if (var39 < var41) {
            var18 += var29;
            var39 += var33;
         } else {
            var19 += var30;
            var41 += var35;
         }

         T var43 = param3.apply(param2, var20.set(var17, var18, var19));
         if (var43 != null) {
            return var43;
         }
      }

      return param4.apply(param2);
   }
}
