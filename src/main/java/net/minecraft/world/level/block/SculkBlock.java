package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
   public SculkBlock(BlockBehaviour.Properties param0) {
      super(param0, ConstantInt.of(1));
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor param0, LevelAccessor param1, BlockPos param2, RandomSource param3, SculkSpreader param4, boolean param5
   ) {
      int var7 = param0.getCharge();
      if (var7 != 0 && param3.nextInt(param4.chargeDecayRate()) == 0) {
         BlockPos var8 = param0.getPos();
         boolean var9 = var8.closerThan(param2, param4.noGrowthRadius());
         if (!var9 && canPlaceGrowth(param1, var8)) {
            int var10 = param4.growthSpawnCost();
            if (param3.nextInt(var10) < var7) {
               BlockPos var11 = var8.above();
               BlockState var12 = this.getRandomGrowthState(param1, var11, param3, param4.isWorldGeneration());
               param1.setBlock(var11, var12, 3);
               param1.playSound(null, var8, var12.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return Math.max(0, var7 - var10);
         } else {
            return param3.nextInt(param4.additionalDecayRate()) != 0 ? var7 : var7 - (var9 ? 1 : getDecayPenalty(param4, var8, param2, var7));
         }
      } else {
         return var7;
      }
   }

   private static int getDecayPenalty(SculkSpreader param0, BlockPos param1, BlockPos param2, int param3) {
      int var4 = param0.noGrowthRadius();
      float var5 = Mth.square((float)Math.sqrt(param1.distSqr(param2)) - var4);
      int var6 = Mth.square(24 - var4);
      float var7 = Math.min(1.0F, var5 / var6);
      return Math.max(1, (int)(param3 * var7 * 0.5F));
   }

   private BlockState getRandomGrowthState(LevelAccessor param0, BlockPos param1, RandomSource param2, boolean param3) {
      BlockState var5;
      if (param2.nextInt(11) == 0) {
         var5 = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, param3);
      } else {
         var5 = Blocks.SCULK_SENSOR.defaultBlockState();
      }

      return var5.hasProperty(BlockStateProperties.WATERLOGGED) && !param0.getFluidState(param1).isEmpty()
         ? var5.setValue(BlockStateProperties.WATERLOGGED, true)
         : var5;
   }

   private static boolean canPlaceGrowth(LevelAccessor param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1.above());
      if (var2.isAir() || var2.is(Blocks.WATER) && var2.getFluidState().is(Fluids.WATER)) {
         int var3 = 0;

         for (BlockPos var5 : BlockPos.betweenClosed(param1.offset(-4, 0, -4), param1.offset(4, 2, 4))) {
            BlockState var6 = param0.getBlockState(var5);
            if (var6.is(Blocks.SCULK_SENSOR) || var6.is(Blocks.SCULK_SHRIEKER)) {
               var3++;
            }

            if (var3 > 2) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canChangeBlockStateOnSpread() {
      return false;
   }
}
