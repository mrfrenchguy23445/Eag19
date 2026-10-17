package net.minecraft.world.level.block;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

public class DetectorRailBlock extends BaseRailBlock {
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int PRESSED_CHECK_PERIOD = 20;

   public DetectorRailBlock(BlockBehaviour.Properties param0) {
      super(true, param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false).setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (!param1.isClientSide) {
         if (!param0.getValue(POWERED)) {
            this.checkPressed(param1, param2, param0);
         }
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(POWERED)) {
         this.checkPressed(param1, param2, param0);
      }
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getValue(POWERED) ? 15 : 0;
   }

   @Override
   public int getDirectSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      if (!param0.getValue(POWERED)) {
         return 0;
      } else {
         return param3 == Direction.UP ? 15 : 0;
      }
   }

   private void checkPressed(Level param0, BlockPos param1, BlockState param2) {
      if (this.canSurvive(param2, param0, param1)) {
         boolean var4 = param2.getValue(POWERED);
         boolean var5 = false;
         List<AbstractMinecart> var6 = this.getInteractingMinecartOfType(param0, param1, AbstractMinecart.class, l_param0 -> true);
         if (!var6.isEmpty()) {
            var5 = true;
         }

         if (var5 && !var4) {
            BlockState var7 = param2.setValue(POWERED, true);
            param0.setBlock(param1, var7, 3);
            this.updatePowerToConnected(param0, param1, var7, true);
            param0.updateNeighborsAt(param1, this);
            param0.updateNeighborsAt(param1.below(), this);
            param0.setBlocksDirty(param1, param2, var7);
         }

         if (!var5 && var4) {
            BlockState var7 = param2.setValue(POWERED, false);
            param0.setBlock(param1, var7, 3);
            this.updatePowerToConnected(param0, param1, var7, false);
            param0.updateNeighborsAt(param1, this);
            param0.updateNeighborsAt(param1.below(), this);
            param0.setBlocksDirty(param1, param2, var7);
         }

         if (var5) {
            param0.scheduleTick(param1, this, 20);
         }

         param0.updateNeighbourForOutputSignal(param1, this);
      }
   }

   protected void updatePowerToConnected(Level param0, BlockPos param1, BlockState param2, boolean param3) {
      RailState var5 = new RailState(param0, param1, param2);

      for (BlockPos var8 : var5.getConnections()) {
         BlockState var9 = param0.getBlockState(var8);
         param0.neighborChanged(var9, var8, var9.getBlock(), param1, false);
      }
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock())) {
         BlockState var6 = this.updateState(param0, param1, param2, param4);
         this.checkPressed(param1, param2, var6);
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      if (param0.getValue(POWERED)) {
         List<MinecartCommandBlock> var4 = this.getInteractingMinecartOfType(param1, param2, MinecartCommandBlock.class, l_param0 -> true);
         if (!var4.isEmpty()) {
            return var4.get(0).getCommandBlock().getSuccessCount();
         }

         List<AbstractMinecart> var5 = this.getInteractingMinecartOfType(param1, param2, AbstractMinecart.class, EntitySelector.CONTAINER_ENTITY_SELECTOR);
         if (!var5.isEmpty()) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)var5.get(0));
         }
      }

      return 0;
   }

   private <T extends AbstractMinecart> List<T> getInteractingMinecartOfType(Level param0, BlockPos param1, Class<T> param2, Predicate<Entity> param3) {
      return param0.getEntitiesOfClass(param2, this.getSearchBB(param1), param3);
   }

   private AABB getSearchBB(BlockPos param0) {
      double var2 = 0.2;
      return new AABB(param0.getX() + 0.2, param0.getY(), param0.getZ() + 0.2, param0.getX() + 1 - 0.2, param0.getY() + 1 - 0.2, param0.getZ() + 1 - 0.2);
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      switch (param1) {
         case CLOCKWISE_180:
            switch ((RailShape)param0.getValue(SHAPE)) {
               case ASCENDING_EAST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_WEST);
               case ASCENDING_WEST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_EAST);
               case ASCENDING_NORTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
               case ASCENDING_SOUTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_NORTH);
               case SOUTH_EAST:
                  return param0.setValue(SHAPE, RailShape.NORTH_WEST);
               case SOUTH_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_EAST);
               case NORTH_WEST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_EAST);
               case NORTH_EAST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_WEST);
            }
         case COUNTERCLOCKWISE_90:
            switch ((RailShape)param0.getValue(SHAPE)) {
               case ASCENDING_EAST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_NORTH);
               case ASCENDING_WEST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
               case ASCENDING_NORTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_WEST);
               case ASCENDING_SOUTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_EAST);
               case SOUTH_EAST:
                  return param0.setValue(SHAPE, RailShape.NORTH_EAST);
               case SOUTH_WEST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_EAST);
               case NORTH_WEST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_WEST);
               case NORTH_EAST:
                  return param0.setValue(SHAPE, RailShape.NORTH_WEST);
               case NORTH_SOUTH:
                  return param0.setValue(SHAPE, RailShape.EAST_WEST);
               case EAST_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_SOUTH);
            }
         case CLOCKWISE_90:
            switch ((RailShape)param0.getValue(SHAPE)) {
               case ASCENDING_EAST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
               case ASCENDING_WEST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_NORTH);
               case ASCENDING_NORTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_EAST);
               case ASCENDING_SOUTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_WEST);
               case SOUTH_EAST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_WEST);
               case SOUTH_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_WEST);
               case NORTH_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_EAST);
               case NORTH_EAST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_EAST);
               case NORTH_SOUTH:
                  return param0.setValue(SHAPE, RailShape.EAST_WEST);
               case EAST_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_SOUTH);
            }
         default:
            return param0;
      }
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      RailShape var3 = param0.getValue(SHAPE);
      switch (param1) {
         case LEFT_RIGHT:
            switch (var3) {
               case ASCENDING_NORTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
               case ASCENDING_SOUTH:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_NORTH);
               case SOUTH_EAST:
                  return param0.setValue(SHAPE, RailShape.NORTH_EAST);
               case SOUTH_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_WEST);
               case NORTH_WEST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_WEST);
               case NORTH_EAST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_EAST);
               default:
                  return super.mirror(param0, param1);
            }
         case FRONT_BACK:
            switch (var3) {
               case ASCENDING_EAST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_WEST);
               case ASCENDING_WEST:
                  return param0.setValue(SHAPE, RailShape.ASCENDING_EAST);
               case ASCENDING_NORTH:
               case ASCENDING_SOUTH:
               default:
                  break;
               case SOUTH_EAST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_WEST);
               case SOUTH_WEST:
                  return param0.setValue(SHAPE, RailShape.SOUTH_EAST);
               case NORTH_WEST:
                  return param0.setValue(SHAPE, RailShape.NORTH_EAST);
               case NORTH_EAST:
                  return param0.setValue(SHAPE, RailShape.NORTH_WEST);
            }
      }

      return super.mirror(param0, param1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(SHAPE, POWERED, WATERLOGGED);
   }
}
