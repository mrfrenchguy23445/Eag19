package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class DripstoneClusterFeature extends Feature<DripstoneClusterConfiguration> {
   public DripstoneClusterFeature(Codec<DripstoneClusterConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      DripstoneClusterConfiguration var4 = param0.config();
      RandomSource var5 = param0.random();
      if (!DripstoneUtils.isEmptyOrWater(var2, var3)) {
         return false;
      }

      int var6 = var4.height.sample(var5);
      float var7 = var4.wetness.sample(var5);
      float var8 = var4.density.sample(var5);
      int var9 = var4.radius.sample(var5);
      int var10 = var4.radius.sample(var5);

      for (int var11 = -var9; var11 <= var9; var11++) {
         for (int var12 = -var10; var12 <= var10; var12++) {
            double var13 = this.getChanceOfStalagmiteOrStalactite(var9, var10, var11, var12, var4);
            BlockPos var15 = var3.offset(var11, 0, var12);
            this.placeColumn(var2, var5, var15, var11, var12, var7, var13, var6, var8, var4);
         }
      }

      return true;
   }

   private void placeColumn(
      WorldGenLevel param0,
      RandomSource param1,
      BlockPos param2,
      int param3,
      int param4,
      float param5,
      double param6,
      int param8,
      float param9,
      DripstoneClusterConfiguration param10
   ) {
      Optional<Column> var12 = Column.scan(
         param0, param2, param10.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isNeitherEmptyNorWater
      );
      if (var12.isPresent()) {
         OptionalInt var13 = var12.get().getCeiling();
         OptionalInt var14 = var12.get().getFloor();
         if (var13.isPresent() || var14.isPresent()) {
            boolean var15 = param1.nextFloat() < param5;
            Column var16;
            if (var15 && var14.isPresent() && this.canPlacePool(param0, param2.atY(var14.getAsInt()))) {
               int var17 = var14.getAsInt();
               var16 = var12.get().withFloor(OptionalInt.of(var17 - 1));
               param0.setBlock(param2.atY(var17), Blocks.WATER.defaultBlockState(), 2);
            } else {
               var16 = var12.get();
            }

            OptionalInt var17 = var16.getFloor();
            boolean var19 = param1.nextDouble() < param6;
            int var18;
            if (var13.isPresent() && var19 && !this.isLava(param0, param2.atY(var13.getAsInt()))) {
               int var20 = param10.dripstoneBlockLayerThickness.sample(param1);
               this.replaceBlocksWithDripstoneBlocks(param0, param2.atY(var13.getAsInt()), var20, Direction.UP);
               int var21;
               if (var17.isPresent()) {
                  var21 = Math.min(param8, var13.getAsInt() - var17.getAsInt());
               } else {
                  var21 = param8;
               }

               var18 = this.getDripstoneHeight(param1, param3, param4, param9, var21, param10);
            } else {
               var18 = 0;
            }

            boolean var21 = param1.nextDouble() < param6;
            int var20;
            if (var17.isPresent() && var21 && !this.isLava(param0, param2.atY(var17.getAsInt()))) {
               int var22 = param10.dripstoneBlockLayerThickness.sample(param1);
               this.replaceBlocksWithDripstoneBlocks(param0, param2.atY(var17.getAsInt()), var22, Direction.DOWN);
               if (var13.isPresent()) {
                  var20 = Math.max(
                     0, var18 + Mth.randomBetweenInclusive(param1, -param10.maxStalagmiteStalactiteHeightDiff, param10.maxStalagmiteStalactiteHeightDiff)
                  );
               } else {
                  var20 = this.getDripstoneHeight(param1, param3, param4, param9, param8, param10);
               }
            } else {
               var20 = 0;
            }

            int var23;
            int var22;
            if (var13.isPresent() && var17.isPresent() && var13.getAsInt() - var18 <= var17.getAsInt() + var20) {
               int var24 = var17.getAsInt();
               int var25 = var13.getAsInt();
               int var26 = Math.max(var25 - var18, var24 + 1);
               int var27 = Math.min(var24 + var20, var25 - 1);
               int var28 = Mth.randomBetweenInclusive(param1, var26, var27 + 1);
               int var29 = var28 - 1;
               var22 = var25 - var28;
               var23 = var29 - var24;
            } else {
               var22 = var18;
               var23 = var20;
            }

            boolean var24 = param1.nextBoolean() && var22 > 0 && var23 > 0 && var16.getHeight().isPresent() && var22 + var23 == var16.getHeight().getAsInt();
            if (var13.isPresent()) {
               DripstoneUtils.growPointedDripstone(param0, param2.atY(var13.getAsInt() - 1), Direction.DOWN, var22, var24);
            }

            if (var17.isPresent()) {
               DripstoneUtils.growPointedDripstone(param0, param2.atY(var17.getAsInt() + 1), Direction.UP, var23, var24);
            }
         }
      }
   }

   private boolean isLava(LevelReader param0, BlockPos param1) {
      return param0.getBlockState(param1).is(Blocks.LAVA);
   }

   private int getDripstoneHeight(RandomSource param0, int param1, int param2, float param3, int param4, DripstoneClusterConfiguration param5) {
      if (param0.nextFloat() > param3) {
         return 0;
      }

      int var7 = Math.abs(param1) + Math.abs(param2);
      float var8 = (float)Mth.clampedMap(var7, 0.0, param5.maxDistanceFromCenterAffectingHeightBias, param4 / 2.0, 0.0);
      return (int)randomBetweenBiased(param0, 0.0F, param4, var8, param5.heightDeviation);
   }

   private boolean canPlacePool(WorldGenLevel param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      if (!var3.is(Blocks.WATER) && !var3.is(Blocks.DRIPSTONE_BLOCK) && !var3.is(Blocks.POINTED_DRIPSTONE)) {
         if (param0.getBlockState(param1.above()).getFluidState().is(FluidTags.WATER)) {
            return false;
         }

         for (Direction var5 : Direction.Plane.HORIZONTAL) {
            if (!this.canBeAdjacentToWater(param0, param1.relative(var5))) {
               return false;
            }
         }

         return this.canBeAdjacentToWater(param0, param1.below());
      } else {
         return false;
      }
   }

   private boolean canBeAdjacentToWater(LevelAccessor param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      return var3.is(BlockTags.BASE_STONE_OVERWORLD) || var3.getFluidState().is(FluidTags.WATER);
   }

   private void replaceBlocksWithDripstoneBlocks(WorldGenLevel param0, BlockPos param1, int param2, Direction param3) {
      BlockPos.MutableBlockPos var5 = param1.mutable();

      for (int var6 = 0; var6 < param2; var6++) {
         if (!DripstoneUtils.placeDripstoneBlockIfPossible(param0, var5)) {
            return;
         }

         var5.move(param3);
      }
   }

   private double getChanceOfStalagmiteOrStalactite(int param0, int param1, int param2, int param3, DripstoneClusterConfiguration param4) {
      int var6 = param0 - Math.abs(param2);
      int var7 = param1 - Math.abs(param3);
      int var8 = Math.min(var6, var7);
      return Mth.clampedMap(var8, 0.0F, param4.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, param4.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F);
   }

   private static float randomBetweenBiased(RandomSource param0, float param1, float param2, float param3, float param4) {
      return ClampedNormalFloat.sample(param0, param3, param4, param1, param2);
   }
}
