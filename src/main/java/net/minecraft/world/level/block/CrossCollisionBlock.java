package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrossCollisionBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
      .entrySet()
      .stream()
      .filter(param0 -> param0.getKey().getAxis().isHorizontal())
      .collect(Util.toMap());
   protected final VoxelShape[] collisionShapeByIndex;
   protected final VoxelShape[] shapeByIndex;
   private final Object2IntMap<BlockState> stateToIndex = new Object2IntOpenHashMap<>();

   protected CrossCollisionBlock(float param0, float param1, float param2, float param3, float param4, BlockBehaviour.Properties param5) {
      super(param5);
      this.collisionShapeByIndex = this.makeShapes(param0, param1, param4, 0.0F, param4);
      this.shapeByIndex = this.makeShapes(param0, param1, param2, 0.0F, param3);

      for (BlockState var8 : this.stateDefinition.getPossibleStates()) {
         this.getAABBIndex(var8);
      }
   }

   protected VoxelShape[] makeShapes(float param0, float param1, float param2, float param3, float param4) {
      float var6 = 8.0F - param0;
      float var7 = 8.0F + param0;
      float var8 = 8.0F - param1;
      float var9 = 8.0F + param1;
      VoxelShape var10 = Block.box(var6, 0.0, var6, var7, param2, var7);
      VoxelShape var11 = Block.box(var8, param3, 0.0, var9, param4, var9);
      VoxelShape var12 = Block.box(var8, param3, var8, var9, param4, 16.0);
      VoxelShape var13 = Block.box(0.0, param3, var8, var9, param4, var9);
      VoxelShape var14 = Block.box(var8, param3, var8, 16.0, param4, var9);
      VoxelShape var15 = Shapes.or(var11, var14);
      VoxelShape var16 = Shapes.or(var12, var13);
      VoxelShape[] var17 = new VoxelShape[]{
         Shapes.empty(),
         var12,
         var13,
         var16,
         var11,
         Shapes.or(var12, var11),
         Shapes.or(var13, var11),
         Shapes.or(var16, var11),
         var14,
         Shapes.or(var12, var14),
         Shapes.or(var13, var14),
         Shapes.or(var16, var14),
         var15,
         Shapes.or(var12, var15),
         Shapes.or(var13, var15),
         Shapes.or(var16, var15)
      };

      for (int var18 = 0; var18 < 16; var18++) {
         var17[var18] = Shapes.or(var10, var17[var18]);
      }

      return var17;
   }

   @Override
   public boolean propagatesSkylightDown(BlockState param0, BlockGetter param1, BlockPos param2) {
      return !param0.getValue(WATERLOGGED);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.shapeByIndex[this.getAABBIndex(param0)];
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.collisionShapeByIndex[this.getAABBIndex(param0)];
   }

   private static int indexFor(Direction param0) {
      return 1 << param0.get2DDataValue();
   }

   protected int getAABBIndex(BlockState param0) {
      return this.stateToIndex.computeIntIfAbsent(param0, l_param0 -> {
         int l_var1 = 0;
         if (l_param0.getValue(NORTH)) {
            l_var1 |= indexFor(Direction.NORTH);
         }

         if (l_param0.getValue(EAST)) {
            l_var1 |= indexFor(Direction.EAST);
         }

         if (l_param0.getValue(SOUTH)) {
            l_var1 |= indexFor(Direction.SOUTH);
         }

         if (l_param0.getValue(WEST)) {
            l_var1 |= indexFor(Direction.WEST);
         }

         return l_var1;
      });
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      switch (param1) {
         case CLOCKWISE_180:
            return param0.setValue(NORTH, param0.getValue(SOUTH))
               .setValue(EAST, param0.getValue(WEST))
               .setValue(SOUTH, param0.getValue(NORTH))
               .setValue(WEST, param0.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return param0.setValue(NORTH, param0.getValue(EAST))
               .setValue(EAST, param0.getValue(SOUTH))
               .setValue(SOUTH, param0.getValue(WEST))
               .setValue(WEST, param0.getValue(NORTH));
         case CLOCKWISE_90:
            return param0.setValue(NORTH, param0.getValue(WEST))
               .setValue(EAST, param0.getValue(NORTH))
               .setValue(SOUTH, param0.getValue(EAST))
               .setValue(WEST, param0.getValue(SOUTH));
         default:
            return param0;
      }
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      switch (param1) {
         case LEFT_RIGHT:
            return param0.setValue(NORTH, param0.getValue(SOUTH)).setValue(SOUTH, param0.getValue(NORTH));
         case FRONT_BACK:
            return param0.setValue(EAST, param0.getValue(WEST)).setValue(WEST, param0.getValue(EAST));
         default:
            return super.mirror(param0, param1);
      }
   }
}
