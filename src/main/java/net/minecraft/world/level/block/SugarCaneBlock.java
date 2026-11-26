package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SugarCaneBlock extends Block {
   public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
   protected static final float AABB_OFFSET = 6.0F;
   protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

   protected SugarCaneBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!param0.canSurvive(param1, param2)) {
         param1.destroyBlock(param2, true);
      }
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.isEmptyBlock(param2.above())) {
         int var5 = 1;

         while (param1.getBlockState(param2.below(var5)).is(this)) {
            var5++;
         }

         if (var5 < 3) {
            int var6 = param0.getValue(AGE);
            if (var6 == 15) {
               param1.setBlockAndUpdate(param2.above(), this.defaultBlockState());
               param1.setBlock(param2, param0.setValue(AGE, 0), 4);
            } else {
               param1.setBlock(param2, param0.setValue(AGE, var6 + 1), 4);
            }
         }
      }
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (!param0.canSurvive(param3, param4)) {
         param3.scheduleTick(param4, this, 1);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockState var4 = param1.getBlockState(param2.below());
      if (var4.is(this)) {
         return true;
      }

      if (var4.is(BlockTags.DIRT) || var4.is(Blocks.SAND) || var4.is(Blocks.RED_SAND)) {
         BlockPos var5 = param2.below();

         for (Direction var7 : Direction.Plane.HORIZONTAL) {
            BlockState var8 = param1.getBlockState(var5.relative(var7));
            FluidState var9 = param1.getFluidState(var5.relative(var7));
            if (var9.is(FluidTags.WATER) || var8.is(Blocks.FROSTED_ICE)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }
}
