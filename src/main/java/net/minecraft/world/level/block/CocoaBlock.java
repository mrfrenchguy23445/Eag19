package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CocoaBlock extends HorizontalDirectionalBlock implements BonemealableBlock {
   public static final int MAX_AGE = 2;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
   protected static final int AGE_0_WIDTH = 4;
   protected static final int AGE_0_HEIGHT = 5;
   protected static final int AGE_0_HALFWIDTH = 2;
   protected static final int AGE_1_WIDTH = 6;
   protected static final int AGE_1_HEIGHT = 7;
   protected static final int AGE_1_HALFWIDTH = 3;
   protected static final int AGE_2_WIDTH = 8;
   protected static final int AGE_2_HEIGHT = 9;
   protected static final int AGE_2_HALFWIDTH = 4;
   protected static final VoxelShape[] EAST_AABB = new VoxelShape[]{
      Block.box(11.0, 7.0, 6.0, 15.0, 12.0, 10.0), Block.box(9.0, 5.0, 5.0, 15.0, 12.0, 11.0), Block.box(7.0, 3.0, 4.0, 15.0, 12.0, 12.0)
   };
   protected static final VoxelShape[] WEST_AABB = new VoxelShape[]{
      Block.box(1.0, 7.0, 6.0, 5.0, 12.0, 10.0), Block.box(1.0, 5.0, 5.0, 7.0, 12.0, 11.0), Block.box(1.0, 3.0, 4.0, 9.0, 12.0, 12.0)
   };
   protected static final VoxelShape[] NORTH_AABB = new VoxelShape[]{
      Block.box(6.0, 7.0, 1.0, 10.0, 12.0, 5.0), Block.box(5.0, 5.0, 1.0, 11.0, 12.0, 7.0), Block.box(4.0, 3.0, 1.0, 12.0, 12.0, 9.0)
   };
   protected static final VoxelShape[] SOUTH_AABB = new VoxelShape[]{
      Block.box(6.0, 7.0, 11.0, 10.0, 12.0, 15.0), Block.box(5.0, 5.0, 9.0, 11.0, 12.0, 15.0), Block.box(4.0, 3.0, 7.0, 12.0, 12.0, 15.0)
   };

   public CocoaBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0));
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return param0.getValue(AGE) < 2;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.random.nextInt(5) == 0) {
         int var5 = param0.getValue(AGE);
         if (var5 < 2) {
            param1.setBlock(param2, param0.setValue(AGE, var5 + 1), 2);
         }
      }
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockState var4 = param1.getBlockState(param2.relative(param0.getValue(FACING)));
      return var4.is(BlockTags.JUNGLE_LOGS);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      int var5 = param0.getValue(AGE);
      switch ((Direction)param0.getValue(FACING)) {
         case SOUTH:
            return SOUTH_AABB[var5];
         case NORTH:
         default:
            return NORTH_AABB[var5];
         case WEST:
            return WEST_AABB[var5];
         case EAST:
            return EAST_AABB[var5];
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = this.defaultBlockState();
      LevelReader var3 = param0.getLevel();
      BlockPos var4 = param0.getClickedPos();

      for (Direction var8 : param0.getNearestLookingDirections()) {
         if (var8.getAxis().isHorizontal()) {
            var2 = var2.setValue(FACING, var8);
            if (var2.canSurvive(var3, var4)) {
               return var2;
            }
         }
      }

      return null;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1 == param0.getValue(FACING) && !param0.canSurvive(param3, param4)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      return param2.getValue(AGE) < 2;
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      param0.setBlock(param2, param3.setValue(AGE, param3.getValue(AGE) + 1), 2);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, AGE);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
