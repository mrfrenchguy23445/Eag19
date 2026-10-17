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
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallSignBlock extends SignBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   protected static final float AABB_THICKNESS = 2.0F;
   protected static final float AABB_BOTTOM = 4.5F;
   protected static final float AABB_TOP = 12.5F;
   private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(
      ImmutableMap.of(
         Direction.NORTH,
         Block.box(0.0, 4.5, 14.0, 16.0, 12.5, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 4.5, 0.0, 16.0, 12.5, 2.0),
         Direction.EAST,
         Block.box(0.0, 4.5, 0.0, 2.0, 12.5, 16.0),
         Direction.WEST,
         Block.box(14.0, 4.5, 0.0, 16.0, 12.5, 16.0)
      )
   );

   public WallSignBlock(BlockBehaviour.Properties param0, WoodType param1) {
      super(param0.sound(param1.soundType()), param1);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   public String getDescriptionId() {
      return this.asItem().getDescriptionId();
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return AABBS.get(param0.getValue(FACING));
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return param1.getBlockState(param2.relative(param0.getValue(FACING).getOpposite())).getMaterial().isSolid();
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = this.defaultBlockState();
      FluidState var3 = param0.getLevel().getFluidState(param0.getClickedPos());
      LevelReader var4 = param0.getLevel();
      BlockPos var5 = param0.getClickedPos();
      Direction[] var6 = param0.getNearestLookingDirections();

      for (Direction var10 : var6) {
         if (var10.getAxis().isHorizontal()) {
            Direction var11 = var10.getOpposite();
            var2 = var2.setValue(FACING, var11);
            if (var2.canSurvive(var4, var5)) {
               return var2.setValue(WATERLOGGED, var3.getType() == Fluids.WATER);
            }
         }
      }

      return null;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1.getOpposite() == param0.getValue(FACING) && !param0.canSurvive(param3, param4)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(param0, param1, param2, param3, param4, param5);
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
}
