package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST_WALL = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH_WALL = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH_WALL = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST_WALL = BlockStateProperties.WEST_WALL;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Map<BlockState, VoxelShape> shapeByIndex;
   private final Map<BlockState, VoxelShape> collisionShapeByIndex;
   private static final int WALL_WIDTH = 3;
   private static final int WALL_HEIGHT = 14;
   private static final int POST_WIDTH = 4;
   private static final int POST_COVER_WIDTH = 1;
   private static final int WALL_COVER_START = 7;
   private static final int WALL_COVER_END = 9;
   private static final VoxelShape POST_TEST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape NORTH_TEST = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 9.0);
   private static final VoxelShape SOUTH_TEST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 16.0);
   private static final VoxelShape WEST_TEST = Block.box(0.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape EAST_TEST = Block.box(7.0, 0.0, 7.0, 16.0, 16.0, 9.0);

   public WallBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(UP, true)
            .setValue(NORTH_WALL, WallSide.NONE)
            .setValue(EAST_WALL, WallSide.NONE)
            .setValue(SOUTH_WALL, WallSide.NONE)
            .setValue(WEST_WALL, WallSide.NONE)
            .setValue(WATERLOGGED, false)
      );
      this.shapeByIndex = this.makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
      this.collisionShapeByIndex = this.makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
   }

   private static VoxelShape applyWallShape(VoxelShape param0, WallSide param1, VoxelShape param2, VoxelShape param3) {
      if (param1 == WallSide.TALL) {
         return Shapes.or(param0, param3);
      } else {
         return param1 == WallSide.LOW ? Shapes.or(param0, param2) : param0;
      }
   }

   private Map<BlockState, VoxelShape> makeShapes(float param0, float param1, float param2, float param3, float param4, float param5) {
      float var7 = 8.0F - param0;
      float var8 = 8.0F + param0;
      float var9 = 8.0F - param1;
      float var10 = 8.0F + param1;
      VoxelShape var11 = Block.box(var7, 0.0, var7, var8, param2, var8);
      VoxelShape var12 = Block.box(var9, param3, 0.0, var10, param4, var10);
      VoxelShape var13 = Block.box(var9, param3, var9, var10, param4, 16.0);
      VoxelShape var14 = Block.box(0.0, param3, var9, var10, param4, var10);
      VoxelShape var15 = Block.box(var9, param3, var9, 16.0, param4, var10);
      VoxelShape var16 = Block.box(var9, param3, 0.0, var10, param5, var10);
      VoxelShape var17 = Block.box(var9, param3, var9, var10, param5, 16.0);
      VoxelShape var18 = Block.box(0.0, param3, var9, var10, param5, var10);
      VoxelShape var19 = Block.box(var9, param3, var9, 16.0, param5, var10);
      Builder<BlockState, VoxelShape> var20 = ImmutableMap.builder();

      for (Boolean var22 : UP.getPossibleValues()) {
         for (WallSide var24 : EAST_WALL.getPossibleValues()) {
            for (WallSide var26 : NORTH_WALL.getPossibleValues()) {
               for (WallSide var28 : WEST_WALL.getPossibleValues()) {
                  for (WallSide var30 : SOUTH_WALL.getPossibleValues()) {
                     VoxelShape var31 = Shapes.empty();
                     var31 = applyWallShape(var31, var24, var15, var19);
                     var31 = applyWallShape(var31, var28, var14, var18);
                     var31 = applyWallShape(var31, var26, var12, var16);
                     var31 = applyWallShape(var31, var30, var13, var17);
                     if (var22) {
                        var31 = Shapes.or(var31, var11);
                     }

                     BlockState var32 = this.defaultBlockState()
                        .setValue(UP, var22)
                        .setValue(EAST_WALL, var24)
                        .setValue(WEST_WALL, var28)
                        .setValue(NORTH_WALL, var26)
                        .setValue(SOUTH_WALL, var30);
                     var20.put(var32.setValue(WATERLOGGED, false), var31);
                     var20.put(var32.setValue(WATERLOGGED, true), var31);
                  }
               }
            }
         }
      }

      return var20.build();
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.shapeByIndex.get(param0);
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.collisionShapeByIndex.get(param0);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   private boolean connectsTo(BlockState param0, boolean param1, Direction param2) {
      Block var4 = param0.getBlock();
      boolean var5 = var4 instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(param0, param2);
      return param0.is(BlockTags.WALLS) || !isExceptionForConnection(param0) && param1 || var4 instanceof IronBarsBlock || var5;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      LevelReader var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      FluidState var4 = param0.getLevel().getFluidState(param0.getClickedPos());
      BlockPos var5 = var3.north();
      BlockPos var6 = var3.east();
      BlockPos var7 = var3.south();
      BlockPos var8 = var3.west();
      BlockPos var9 = var3.above();
      BlockState var10 = var2.getBlockState(var5);
      BlockState var11 = var2.getBlockState(var6);
      BlockState var12 = var2.getBlockState(var7);
      BlockState var13 = var2.getBlockState(var8);
      BlockState var14 = var2.getBlockState(var9);
      boolean var15 = this.connectsTo(var10, var10.isFaceSturdy(var2, var5, Direction.SOUTH), Direction.SOUTH);
      boolean var16 = this.connectsTo(var11, var11.isFaceSturdy(var2, var6, Direction.WEST), Direction.WEST);
      boolean var17 = this.connectsTo(var12, var12.isFaceSturdy(var2, var7, Direction.NORTH), Direction.NORTH);
      boolean var18 = this.connectsTo(var13, var13.isFaceSturdy(var2, var8, Direction.EAST), Direction.EAST);
      BlockState var19 = this.defaultBlockState().setValue(WATERLOGGED, var4.getType() == Fluids.WATER);
      return this.updateShape(var2, var19, var9, var14, var15, var16, var17, var18);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      if (param1 == Direction.DOWN) {
         return super.updateShape(param0, param1, param2, param3, param4, param5);
      } else {
         return param1 == Direction.UP ? this.topUpdate(param3, param0, param5, param2) : this.sideUpdate(param3, param4, param0, param5, param2, param1);
      }
   }

   private static boolean isConnected(BlockState param0, Property<WallSide> param1) {
      return param0.getValue(param1) != WallSide.NONE;
   }

   private static boolean isCovered(VoxelShape param0, VoxelShape param1) {
      return !Shapes.joinIsNotEmpty(param1, param0, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(LevelReader param0, BlockState param1, BlockPos param2, BlockState param3) {
      boolean var5 = isConnected(param1, NORTH_WALL);
      boolean var6 = isConnected(param1, EAST_WALL);
      boolean var7 = isConnected(param1, SOUTH_WALL);
      boolean var8 = isConnected(param1, WEST_WALL);
      return this.updateShape(param0, param1, param2, param3, var5, var6, var7, var8);
   }

   private BlockState sideUpdate(LevelReader param0, BlockPos param1, BlockState param2, BlockPos param3, BlockState param4, Direction param5) {
      Direction var7 = param5.getOpposite();
      boolean var8 = param5 == Direction.NORTH ? this.connectsTo(param4, param4.isFaceSturdy(param0, param3, var7), var7) : isConnected(param2, NORTH_WALL);
      boolean var9 = param5 == Direction.EAST ? this.connectsTo(param4, param4.isFaceSturdy(param0, param3, var7), var7) : isConnected(param2, EAST_WALL);
      boolean var10 = param5 == Direction.SOUTH ? this.connectsTo(param4, param4.isFaceSturdy(param0, param3, var7), var7) : isConnected(param2, SOUTH_WALL);
      boolean var11 = param5 == Direction.WEST ? this.connectsTo(param4, param4.isFaceSturdy(param0, param3, var7), var7) : isConnected(param2, WEST_WALL);
      BlockPos var12 = param1.above();
      BlockState var13 = param0.getBlockState(var12);
      return this.updateShape(param0, param2, var12, var13, var8, var9, var10, var11);
   }

   private BlockState updateShape(
      LevelReader param0, BlockState param1, BlockPos param2, BlockState param3, boolean param4, boolean param5, boolean param6, boolean param7
   ) {
      VoxelShape var9 = param3.getCollisionShape(param0, param2).getFaceShape(Direction.DOWN);
      BlockState var10 = this.updateSides(param1, param4, param5, param6, param7, var9);
      return var10.setValue(UP, this.shouldRaisePost(var10, param3, var9));
   }

   private boolean shouldRaisePost(BlockState param0, BlockState param1, VoxelShape param2) {
      boolean var4 = param1.getBlock() instanceof WallBlock && param1.getValue(UP);
      if (var4) {
         return true;
      }

      WallSide var5 = param0.getValue(NORTH_WALL);
      WallSide var6 = param0.getValue(SOUTH_WALL);
      WallSide var7 = param0.getValue(EAST_WALL);
      WallSide var8 = param0.getValue(WEST_WALL);
      boolean var9 = var6 == WallSide.NONE;
      boolean var10 = var8 == WallSide.NONE;
      boolean var11 = var7 == WallSide.NONE;
      boolean var12 = var5 == WallSide.NONE;
      boolean var13 = var12 && var9 && var10 && var11 || var12 != var9 || var10 != var11;
      if (var13) {
         return true;
      }

      boolean var14 = var5 == WallSide.TALL && var6 == WallSide.TALL || var7 == WallSide.TALL && var8 == WallSide.TALL;
      return var14 ? false : param1.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(param2, POST_TEST);
   }

   private BlockState updateSides(BlockState param0, boolean param1, boolean param2, boolean param3, boolean param4, VoxelShape param5) {
      return param0.setValue(NORTH_WALL, this.makeWallState(param1, param5, NORTH_TEST))
         .setValue(EAST_WALL, this.makeWallState(param2, param5, EAST_TEST))
         .setValue(SOUTH_WALL, this.makeWallState(param3, param5, SOUTH_TEST))
         .setValue(WEST_WALL, this.makeWallState(param4, param5, WEST_TEST));
   }

   private WallSide makeWallState(boolean param0, VoxelShape param1, VoxelShape param2) {
      if (param0) {
         return isCovered(param1, param2) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public boolean propagatesSkylightDown(BlockState param0, BlockGetter param1, BlockPos param2) {
      return !param0.getValue(WATERLOGGED);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(UP, NORTH_WALL, EAST_WALL, WEST_WALL, SOUTH_WALL, WATERLOGGED);
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      switch (param1) {
         case CLOCKWISE_180:
            return param0.setValue(NORTH_WALL, param0.getValue(SOUTH_WALL))
               .setValue(EAST_WALL, param0.getValue(WEST_WALL))
               .setValue(SOUTH_WALL, param0.getValue(NORTH_WALL))
               .setValue(WEST_WALL, param0.getValue(EAST_WALL));
         case COUNTERCLOCKWISE_90:
            return param0.setValue(NORTH_WALL, param0.getValue(EAST_WALL))
               .setValue(EAST_WALL, param0.getValue(SOUTH_WALL))
               .setValue(SOUTH_WALL, param0.getValue(WEST_WALL))
               .setValue(WEST_WALL, param0.getValue(NORTH_WALL));
         case CLOCKWISE_90:
            return param0.setValue(NORTH_WALL, param0.getValue(WEST_WALL))
               .setValue(EAST_WALL, param0.getValue(NORTH_WALL))
               .setValue(SOUTH_WALL, param0.getValue(EAST_WALL))
               .setValue(WEST_WALL, param0.getValue(SOUTH_WALL));
         default:
            return param0;
      }
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      switch (param1) {
         case LEFT_RIGHT:
            return param0.setValue(NORTH_WALL, param0.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, param0.getValue(NORTH_WALL));
         case FRONT_BACK:
            return param0.setValue(EAST_WALL, param0.getValue(WEST_WALL)).setValue(WEST_WALL, param0.getValue(EAST_WALL));
         default:
            return super.mirror(param0, param1);
      }
   }
}
