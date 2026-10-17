package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends Feature<PointedDripstoneConfiguration> {
   public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> param0) {
      LevelAccessor var2 = param0.level();
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      PointedDripstoneConfiguration var5 = param0.config();
      Optional<Direction> var6 = getTipDirection(var2, var3, var4);
      if (var6.isEmpty()) {
         return false;
      }

      BlockPos var7 = var3.relative(var6.get().getOpposite());
      createPatchOfDripstoneBlocks(var2, var4, var7, var5);
      int var8 = var4.nextFloat() < var5.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater(var2.getBlockState(var3.relative(var6.get()))) ? 2 : 1;
      DripstoneUtils.growPointedDripstone(var2, var3, var6.get(), var8, false);
      return true;
   }

   private static Optional<Direction> getTipDirection(LevelAccessor param0, BlockPos param1, RandomSource param2) {
      boolean var3 = DripstoneUtils.isDripstoneBase(param0.getBlockState(param1.above()));
      boolean var4 = DripstoneUtils.isDripstoneBase(param0.getBlockState(param1.below()));
      if (var3 && var4) {
         return Optional.of(param2.nextBoolean() ? Direction.DOWN : Direction.UP);
      } else if (var3) {
         return Optional.of(Direction.DOWN);
      } else {
         return var4 ? Optional.of(Direction.UP) : Optional.empty();
      }
   }

   private static void createPatchOfDripstoneBlocks(LevelAccessor param0, RandomSource param1, BlockPos param2, PointedDripstoneConfiguration param3) {
      DripstoneUtils.placeDripstoneBlockIfPossible(param0, param2);

      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         if (!(param1.nextFloat() > param3.chanceOfDirectionalSpread)) {
            BlockPos var6 = param2.relative(var5);
            DripstoneUtils.placeDripstoneBlockIfPossible(param0, var6);
            if (!(param1.nextFloat() > param3.chanceOfSpreadRadius2)) {
               BlockPos var7 = var6.relative(Direction.getRandom(param1));
               DripstoneUtils.placeDripstoneBlockIfPossible(param0, var7);
               if (!(param1.nextFloat() > param3.chanceOfSpreadRadius3)) {
                  BlockPos var8 = var7.relative(Direction.getRandom(param1));
                  DripstoneUtils.placeDripstoneBlockIfPossible(param0, var8);
               }
            }
         }
      }
   }
}
