package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralWallFanBlock extends BaseCoralFanBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> SHAPES = Maps.newEnumMap(
      ImmutableMap.of(
         Direction.NORTH,
         Block.box(0.0, 4.0, 5.0, 16.0, 12.0, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 4.0, 0.0, 16.0, 12.0, 11.0),
         Direction.WEST,
         Block.box(5.0, 4.0, 0.0, 16.0, 12.0, 16.0),
         Direction.EAST,
         Block.box(0.0, 4.0, 0.0, 11.0, 12.0, 16.0)
      )
   );

   protected BaseCoralWallFanBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, true));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPES.get(param0.getValue(FACING));
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      return param0.setValue(FACING, param1.rotate(param0.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      return param0.rotate(param1.getRotation(param0.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, WATERLOGGED);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return param1.getOpposite() == param0.getValue(FACING) && !param0.canSurvive(param3, param4) ? Blocks.AIR.defaultBlockState() : param0;
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      Direction var4 = param0.getValue(FACING);
      BlockPos var5 = param2.relative(var4.getOpposite());
      BlockState var6 = param1.getBlockState(var5);
      return var6.isFaceSturdy(param1, var5, var4);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = super.getStateForPlacement(param0);
      LevelReader var3 = param0.getLevel();
      BlockPos var4 = param0.getClickedPos();
      Direction[] var5 = param0.getNearestLookingDirections();

      for (Direction var9 : var5) {
         if (var9.getAxis().isHorizontal()) {
            var2 = var2.setValue(FACING, var9.getOpposite());
            if (var2.canSurvive(var3, var4)) {
               return var2;
            }
         }
      }

      return null;
   }
}
