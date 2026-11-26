package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherrackBlock extends Block implements BonemealableBlock {
   public NetherrackBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      if (!param0.getBlockState(param1.above()).propagatesSkylightDown(param0, param1)) {
         return false;
      }

      for (BlockPos var6 : BlockPos.betweenClosed(param1.offset(-1, -1, -1), param1.offset(1, 1, 1))) {
         if (param0.getBlockState(var6).is(BlockTags.NYLIUM)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      boolean var5 = false;
      boolean var6 = false;

      for (BlockPos var8 : BlockPos.betweenClosed(param2.offset(-1, -1, -1), param2.offset(1, 1, 1))) {
         BlockState var9 = param0.getBlockState(var8);
         if (var9.is(Blocks.WARPED_NYLIUM)) {
            var6 = true;
         }

         if (var9.is(Blocks.CRIMSON_NYLIUM)) {
            var5 = true;
         }

         if (var6 && var5) {
            break;
         }
      }

      if (var6 && var5) {
         param0.setBlock(param2, param1.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      } else if (var6) {
         param0.setBlock(param2, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
      } else if (var5) {
         param0.setBlock(param2, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      }
   }
}
