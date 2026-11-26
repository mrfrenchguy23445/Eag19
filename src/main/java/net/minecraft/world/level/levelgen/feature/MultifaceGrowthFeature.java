package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

public class MultifaceGrowthFeature extends Feature<MultifaceGrowthConfiguration> {
   public MultifaceGrowthFeature(Codec<MultifaceGrowthConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<MultifaceGrowthConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      MultifaceGrowthConfiguration var5 = param0.config();
      if (!isAirOrWater(var2.getBlockState(var3))) {
         return false;
      }

      List<Direction> var6 = var5.getShuffledDirections(var4);
      if (placeGrowthIfPossible(var2, var3, var2.getBlockState(var3), var5, var4, var6)) {
         return true;
      }

      BlockPos.MutableBlockPos var7 = var3.mutable();

      for (Direction var9 : var6) {
         var7.set(var3);
         List<Direction> var10 = var5.getShuffledDirectionsExcept(var4, var9.getOpposite());

         for (int var11 = 0; var11 < var5.searchRange; var11++) {
            var7.setWithOffset(var3, var9);
            BlockState var12 = var2.getBlockState(var7);
            if (!isAirOrWater(var12) && !var12.is(var5.placeBlock)) {
               break;
            }

            if (placeGrowthIfPossible(var2, var7, var12, var5, var4, var10)) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean placeGrowthIfPossible(
      WorldGenLevel param0, BlockPos param1, BlockState param2, MultifaceGrowthConfiguration param3, RandomSource param4, List<Direction> param5
   ) {
      BlockPos.MutableBlockPos var6 = param1.mutable();

      for (Direction var8 : param5) {
         BlockState var9 = param0.getBlockState(var6.setWithOffset(param1, var8));
         if (var9.is(param3.canBePlacedOn)) {
            BlockState var10 = param3.placeBlock.getStateForPlacement(param2, param0, param1, var8);
            if (var10 == null) {
               return false;
            }

            param0.setBlock(param1, var10, 3);
            param0.getChunk(param1).markPosForPostprocessing(param1);
            if (param4.nextFloat() < param3.chanceOfSpreading) {
               param3.placeBlock.getSpreader().spreadFromFaceTowardRandomDirection(var10, param0, param1, var8, param4, true);
            }

            return true;
         }
      }

      return false;
   }

   private static boolean isAirOrWater(BlockState param0) {
      return param0.isAir() || param0.is(Blocks.WATER);
   }
}
