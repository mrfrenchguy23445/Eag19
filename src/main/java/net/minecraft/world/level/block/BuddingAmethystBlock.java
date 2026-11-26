package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

public class BuddingAmethystBlock extends AmethystBlock {
   public static final int GROWTH_CHANCE = 5;
   private static final Direction[] DIRECTIONS = Direction.values();

   public BuddingAmethystBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param3.nextInt(5) == 0) {
         Direction var5 = DIRECTIONS[param3.nextInt(DIRECTIONS.length)];
         BlockPos var6 = param2.relative(var5);
         BlockState var7 = param1.getBlockState(var6);
         Block var8 = null;
         if (canClusterGrowAtState(var7)) {
            var8 = Blocks.SMALL_AMETHYST_BUD;
         } else if (var7.is(Blocks.SMALL_AMETHYST_BUD) && var7.getValue(AmethystClusterBlock.FACING) == var5) {
            var8 = Blocks.MEDIUM_AMETHYST_BUD;
         } else if (var7.is(Blocks.MEDIUM_AMETHYST_BUD) && var7.getValue(AmethystClusterBlock.FACING) == var5) {
            var8 = Blocks.LARGE_AMETHYST_BUD;
         } else if (var7.is(Blocks.LARGE_AMETHYST_BUD) && var7.getValue(AmethystClusterBlock.FACING) == var5) {
            var8 = Blocks.AMETHYST_CLUSTER;
         }

         if (var8 != null) {
            BlockState var9 = var8.defaultBlockState()
               .setValue(AmethystClusterBlock.FACING, var5)
               .setValue(AmethystClusterBlock.WATERLOGGED, var7.getFluidState().getType() == Fluids.WATER);
            param1.setBlockAndUpdate(var6, var9);
         }
      }
   }

   public static boolean canClusterGrowAtState(BlockState param0) {
      return param0.isAir() || param0.is(Blocks.WATER) && param0.getFluidState().getAmount() == 8;
   }
}
