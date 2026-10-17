package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;

public class EndRodBlock extends RodBlock {
   protected EndRodBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      Direction var2 = param0.getClickedFace();
      BlockState var3 = param0.getLevel().getBlockState(param0.getClickedPos().relative(var2.getOpposite()));
      return var3.is(this) && var3.getValue(FACING) == var2
         ? this.defaultBlockState().setValue(FACING, var2.getOpposite())
         : this.defaultBlockState().setValue(FACING, var2);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      Direction var5 = param0.getValue(FACING);
      double var6 = param2.getX() + 0.55 - param3.nextFloat() * 0.1F;
      double var8 = param2.getY() + 0.55 - param3.nextFloat() * 0.1F;
      double var10 = param2.getZ() + 0.55 - param3.nextFloat() * 0.1F;
      double var12 = 0.4F - (param3.nextFloat() + param3.nextFloat()) * 0.4F;
      if (param3.nextInt(5) == 0) {
         param1.addParticle(
            ParticleTypes.END_ROD,
            var6 + var5.getStepX() * var12,
            var8 + var5.getStepY() * var12,
            var10 + var5.getStepZ() * var12,
            param3.nextGaussian() * 0.005,
            param3.nextGaussian() * 0.005,
            param3.nextGaussian() * 0.005
         );
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING);
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.NORMAL;
   }
}
