package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LayerLightEngine;

public abstract class SpreadingSnowyDirtBlock extends SnowyDirtBlock {
   protected SpreadingSnowyDirtBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   private static boolean canBeGrass(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var3 = param2.above();
      BlockState var4 = param1.getBlockState(var3);
      if (var4.is(Blocks.SNOW) && var4.getValue(SnowLayerBlock.LAYERS) == 1) {
         return true;
      }

      if (var4.getFluidState().getAmount() == 8) {
         return false;
      }

      int var5 = LayerLightEngine.getLightBlockInto(param1, param0, param2, var4, var3, Direction.UP, var4.getLightBlock(param1, var3));
      return var5 < param1.getMaxLightLevel();
   }

   private static boolean canPropagate(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var3 = param2.above();
      return canBeGrass(param0, param1, param2) && !param1.getFluidState(var3).is(FluidTags.WATER);
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!canBeGrass(param0, param1, param2)) {
         param1.setBlockAndUpdate(param2, Blocks.DIRT.defaultBlockState());
      } else {
         if (param1.getMaxLocalRawBrightness(param2.above()) >= 9) {
            BlockState var5 = this.defaultBlockState();

            for (int var6 = 0; var6 < 4; var6++) {
               BlockPos var7 = param2.offset(param3.nextInt(3) - 1, param3.nextInt(5) - 3, param3.nextInt(3) - 1);
               if (param1.getBlockState(var7).is(Blocks.DIRT) && canPropagate(var5, param1, var7)) {
                  param1.setBlockAndUpdate(var7, var5.setValue(SNOWY, param1.getBlockState(var7.above()).is(Blocks.SNOW)));
               }
            }
         }
      }
   }
}
