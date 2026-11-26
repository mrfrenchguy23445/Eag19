package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
   public TwistingVinesFeature(Codec<TwistingVinesConfig> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<TwistingVinesConfig> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      if (isInvalidPlacementLocation(var2, var3)) {
         return false;
      }

      RandomSource var4 = param0.random();
      TwistingVinesConfig var5 = param0.config();
      int var6 = var5.spreadWidth();
      int var7 = var5.spreadHeight();
      int var8 = var5.maxHeight();
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

      for (int var10 = 0; var10 < var6 * var6; var10++) {
         var9.set(var3).move(Mth.nextInt(var4, -var6, var6), Mth.nextInt(var4, -var7, var7), Mth.nextInt(var4, -var6, var6));
         if (findFirstAirBlockAboveGround(var2, var9) && !isInvalidPlacementLocation(var2, var9)) {
            int var11 = Mth.nextInt(var4, 1, var8);
            if (var4.nextInt(6) == 0) {
               var11 *= 2;
            }

            if (var4.nextInt(5) == 0) {
               var11 = 1;
            }

            int var12 = 17;
            int var13 = 25;
            placeWeepingVinesColumn(var2, var4, var9, var11, 17, 25);
         }
      }

      return true;
   }

   private static boolean findFirstAirBlockAboveGround(LevelAccessor param0, BlockPos.MutableBlockPos param1) {
      do {
         param1.move(0, -1, 0);
         if (param0.isOutsideBuildHeight(param1)) {
            return false;
         }
      } while (param0.getBlockState(param1).isAir());

      param1.move(0, 1, 0);
      return true;
   }

   public static void placeWeepingVinesColumn(LevelAccessor param0, RandomSource param1, BlockPos.MutableBlockPos param2, int param3, int param4, int param5) {
      for (int var6 = 1; var6 <= param3; var6++) {
         if (param0.isEmptyBlock(param2)) {
            if (var6 == param3 || !param0.isEmptyBlock(param2.above())) {
               param0.setBlock(param2, Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(param1, param4, param5)), 2);
               break;
            }

            param0.setBlock(param2, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
         }

         param2.move(Direction.UP);
      }
   }

   private static boolean isInvalidPlacementLocation(LevelAccessor param0, BlockPos param1) {
      if (!param0.isEmptyBlock(param1)) {
         return true;
      }

      BlockState var2 = param0.getBlockState(param1.below());
      return !var2.is(Blocks.NETHERRACK) && !var2.is(Blocks.WARPED_NYLIUM) && !var2.is(Blocks.WARPED_WART_BLOCK);
   }
}
