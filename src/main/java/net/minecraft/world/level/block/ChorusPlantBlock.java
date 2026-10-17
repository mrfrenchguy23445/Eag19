package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
   protected ChorusPlantBlock(BlockBehaviour.Properties param0) {
      super(0.3125F, param0);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.getStateForPlacement(param0.getLevel(), param0.getClickedPos());
   }

   public BlockState getStateForPlacement(BlockGetter param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1.below());
      BlockState var4 = param0.getBlockState(param1.above());
      BlockState var5 = param0.getBlockState(param1.north());
      BlockState var6 = param0.getBlockState(param1.east());
      BlockState var7 = param0.getBlockState(param1.south());
      BlockState var8 = param0.getBlockState(param1.west());
      return this.defaultBlockState()
         .setValue(DOWN, var3.is(this) || var3.is(Blocks.CHORUS_FLOWER) || var3.is(Blocks.END_STONE))
         .setValue(UP, var4.is(this) || var4.is(Blocks.CHORUS_FLOWER))
         .setValue(NORTH, var5.is(this) || var5.is(Blocks.CHORUS_FLOWER))
         .setValue(EAST, var6.is(this) || var6.is(Blocks.CHORUS_FLOWER))
         .setValue(SOUTH, var7.is(this) || var7.is(Blocks.CHORUS_FLOWER))
         .setValue(WEST, var8.is(this) || var8.is(Blocks.CHORUS_FLOWER));
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (!param0.canSurvive(param3, param4)) {
         param3.scheduleTick(param4, this, 1);
         return super.updateShape(param0, param1, param2, param3, param4, param5);
      } else {
         boolean var7 = param2.is(this) || param2.is(Blocks.CHORUS_FLOWER) || param1 == Direction.DOWN && param2.is(Blocks.END_STONE);
         return param0.setValue(PROPERTY_BY_DIRECTION.get(param1), var7);
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!param0.canSurvive(param1, param2)) {
         param1.destroyBlock(param2, true);
      }
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockState var4 = param1.getBlockState(param2.below());
      boolean var5 = !param1.getBlockState(param2.above()).isAir() && !var4.isAir();

      for (Direction var7 : Direction.Plane.HORIZONTAL) {
         BlockPos var8 = param2.relative(var7);
         BlockState var9 = param1.getBlockState(var8);
         if (var9.is(this)) {
            if (var5) {
               return false;
            }

            BlockState var10 = param1.getBlockState(var8.below());
            if (var10.is(this) || var10.is(Blocks.END_STONE)) {
               return true;
            }
         }
      }

      return var4.is(this) || var4.is(Blocks.END_STONE);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
