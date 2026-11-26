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
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature extends Feature<NoneFeatureConfiguration> {
   private static final Direction[] DIRECTIONS = Direction.values();

   public WeepingVinesFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      if (!var2.isEmptyBlock(var3)) {
         return false;
      }

      BlockState var5 = var2.getBlockState(var3.above());
      if (!var5.is(Blocks.NETHERRACK) && !var5.is(Blocks.NETHER_WART_BLOCK)) {
         return false;
      }

      this.placeRoofNetherWart(var2, var4, var3);
      this.placeRoofWeepingVines(var2, var4, var3);
      return true;
   }

   private void placeRoofNetherWart(LevelAccessor param0, RandomSource param1, BlockPos param2) {
      param0.setBlock(param2, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int var6 = 0; var6 < 200; var6++) {
         var4.setWithOffset(param2, param1.nextInt(6) - param1.nextInt(6), param1.nextInt(2) - param1.nextInt(5), param1.nextInt(6) - param1.nextInt(6));
         if (param0.isEmptyBlock(var4)) {
            int var7 = 0;

            for (Direction var11 : DIRECTIONS) {
               BlockState var12 = param0.getBlockState(var5.setWithOffset(var4, var11));
               if (var12.is(Blocks.NETHERRACK) || var12.is(Blocks.NETHER_WART_BLOCK)) {
                  var7++;
               }

               if (var7 > 1) {
                  break;
               }
            }

            if (var7 == 1) {
               param0.setBlock(var4, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
            }
         }
      }
   }

   private void placeRoofWeepingVines(LevelAccessor param0, RandomSource param1, BlockPos param2) {
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();

      for (int var5 = 0; var5 < 100; var5++) {
         var4.setWithOffset(param2, param1.nextInt(8) - param1.nextInt(8), param1.nextInt(2) - param1.nextInt(7), param1.nextInt(8) - param1.nextInt(8));
         if (param0.isEmptyBlock(var4)) {
            BlockState var6 = param0.getBlockState(var4.above());
            if (var6.is(Blocks.NETHERRACK) || var6.is(Blocks.NETHER_WART_BLOCK)) {
               int var7 = Mth.nextInt(param1, 1, 8);
               if (param1.nextInt(6) == 0) {
                  var7 *= 2;
               }

               if (param1.nextInt(5) == 0) {
                  var7 = 1;
               }

               int var8 = 17;
               int var9 = 25;
               placeWeepingVinesColumn(param0, param1, var4, var7, 17, 25);
            }
         }
      }
   }

   public static void placeWeepingVinesColumn(LevelAccessor param0, RandomSource param1, BlockPos.MutableBlockPos param2, int param3, int param4, int param5) {
      for (int var6 = 0; var6 <= param3; var6++) {
         if (param0.isEmptyBlock(param2)) {
            if (var6 == param3 || !param0.isEmptyBlock(param2.below())) {
               param0.setBlock(param2, Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(param1, param4, param5)), 2);
               break;
            }

            param0.setBlock(param2, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
         }

         param2.move(Direction.DOWN);
      }
   }
}
