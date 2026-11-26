package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class FrostedIceBlock extends IceBlock {
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final int NEIGHBORS_TO_AGE = 4;
   private static final int NEIGHBORS_TO_MELT = 2;

   public FrostedIceBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      this.tick(param0, param1, param2, param3);
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if ((param3.nextInt(3) == 0 || this.fewerNeigboursThan(param1, param2, 4))
         && param1.getMaxLocalRawBrightness(param2) > 11 - param0.getValue(AGE) - param0.getLightBlock(param1, param2)
         && this.slightlyMelt(param0, param1, param2)) {
         BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

         for (Direction var9 : Direction.values()) {
            var5.setWithOffset(param2, var9);
            BlockState var10 = param1.getBlockState(var5);
            if (var10.is(this) && !this.slightlyMelt(var10, param1, var5)) {
               param1.scheduleTick(var5, this, Mth.nextInt(param3, 20, 40));
            }
         }
      } else {
         param1.scheduleTick(param2, this, Mth.nextInt(param3, 20, 40));
      }
   }

   private boolean slightlyMelt(BlockState param0, Level param1, BlockPos param2) {
      int var4 = param0.getValue(AGE);
      if (var4 < 3) {
         param1.setBlock(param2, param0.setValue(AGE, var4 + 1), 2);
         return false;
      } else {
         this.melt(param0, param1, param2);
         return true;
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (param3.defaultBlockState().is(this) && this.fewerNeigboursThan(param1, param2, 2)) {
         this.melt(param0, param1, param2);
      }

      super.neighborChanged(param0, param1, param2, param3, param4, param5);
   }

   private boolean fewerNeigboursThan(BlockGetter param0, BlockPos param1, int param2) {
      int var4 = 0;
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (Direction var9 : Direction.values()) {
         var5.setWithOffset(param1, var9);
         if (param0.getBlockState(var5).is(this)) {
            if (++var4 >= param2) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter param0, BlockPos param1, BlockState param2) {
      return ItemStack.EMPTY;
   }
}
