package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PipeBlock extends Block {
   private static final Direction[] DIRECTIONS = Direction.values();
   public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
   public static final BooleanProperty EAST = BlockStateProperties.EAST;
   public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
   public static final BooleanProperty WEST = BlockStateProperties.WEST;
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), param0 -> {
      param0.put(Direction.NORTH, NORTH);
      param0.put(Direction.EAST, EAST);
      param0.put(Direction.SOUTH, SOUTH);
      param0.put(Direction.WEST, WEST);
      param0.put(Direction.UP, UP);
      param0.put(Direction.DOWN, DOWN);
   }));
   protected final VoxelShape[] shapeByIndex;

   protected PipeBlock(float param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.shapeByIndex = this.makeShapes(param0);
   }

   private VoxelShape[] makeShapes(float param0) {
      float var2 = 0.5F - param0;
      float var3 = 0.5F + param0;
      VoxelShape var4 = Block.box(var2 * 16.0F, var2 * 16.0F, var2 * 16.0F, var3 * 16.0F, var3 * 16.0F, var3 * 16.0F);
      VoxelShape[] var5 = new VoxelShape[DIRECTIONS.length];

      for (int var6 = 0; var6 < DIRECTIONS.length; var6++) {
         Direction var7 = DIRECTIONS[var6];
         var5[var6] = Shapes.box(
            0.5 + Math.min(-param0, var7.getStepX() * 0.5),
            0.5 + Math.min(-param0, var7.getStepY() * 0.5),
            0.5 + Math.min(-param0, var7.getStepZ() * 0.5),
            0.5 + Math.max(param0, var7.getStepX() * 0.5),
            0.5 + Math.max(param0, var7.getStepY() * 0.5),
            0.5 + Math.max(param0, var7.getStepZ() * 0.5)
         );
      }

      VoxelShape[] var6 = new VoxelShape[64];

      for (int var7 = 0; var7 < 64; var7++) {
         VoxelShape var8 = var4;

         for (int var9 = 0; var9 < DIRECTIONS.length; var9++) {
            if ((var7 & 1 << var9) != 0) {
               var8 = Shapes.or(var8, var5[var9]);
            }
         }

         var6[var7] = var8;
      }

      return var6;
   }

   @Override
   public boolean propagatesSkylightDown(BlockState param0, BlockGetter param1, BlockPos param2) {
      return false;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.shapeByIndex[this.getAABBIndex(param0)];
   }

   protected int getAABBIndex(BlockState param0) {
      int var2 = 0;

      for (int var3 = 0; var3 < DIRECTIONS.length; var3++) {
         if (param0.getValue(PROPERTY_BY_DIRECTION.get(DIRECTIONS[var3]))) {
            var2 |= 1 << var3;
         }
      }

      return var2;
   }
}
