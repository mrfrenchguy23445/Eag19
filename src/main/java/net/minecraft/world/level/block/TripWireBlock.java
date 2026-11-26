package net.minecraft.world.level.block;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireBlock extends Block {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   public static final BooleanProperty DISARMED = BlockStateProperties.DISARMED;
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
   protected static final VoxelShape AABB = Block.box(0.0, 1.0, 0.0, 16.0, 2.5, 16.0);
   protected static final VoxelShape NOT_ATTACHED_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
   private static final int RECHECK_PERIOD = 10;
   private final TripWireHookBlock hook;

   public TripWireBlock(TripWireHookBlock param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(POWERED, false)
            .setValue(ATTACHED, false)
            .setValue(DISARMED, false)
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
      );
      this.hook = param0;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return param0.getValue(ATTACHED) ? AABB : NOT_ATTACHED_AABB;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockGetter var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      return this.defaultBlockState()
         .setValue(NORTH, this.shouldConnectTo(var2.getBlockState(var3.north()), Direction.NORTH))
         .setValue(EAST, this.shouldConnectTo(var2.getBlockState(var3.east()), Direction.EAST))
         .setValue(SOUTH, this.shouldConnectTo(var2.getBlockState(var3.south()), Direction.SOUTH))
         .setValue(WEST, this.shouldConnectTo(var2.getBlockState(var3.west()), Direction.WEST));
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1.getAxis().isHorizontal()
         ? param0.setValue(PROPERTY_BY_DIRECTION.get(param1), this.shouldConnectTo(param2, param1))
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock())) {
         this.updateSource(param1, param2, param0);
      }
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param4 && !param0.is(param3.getBlock())) {
         this.updateSource(param1, param2, param0.setValue(POWERED, true));
      }
   }

   @Override
   public void playerWillDestroy(Level param0, BlockPos param1, BlockState param2, Player param3) {
      if (!param0.isClientSide && !param3.getMainHandItem().isEmpty() && param3.getMainHandItem().is(Items.SHEARS)) {
         param0.setBlock(param1, param2.setValue(DISARMED, true), 4);
         param0.gameEvent(param3, GameEvent.SHEAR, param1);
      }

      super.playerWillDestroy(param0, param1, param2, param3);
   }

   private void updateSource(Level param0, BlockPos param1, BlockState param2) {
      for (Direction var7 : new Direction[]{Direction.SOUTH, Direction.WEST}) {
         for (int var8 = 1; var8 < 42; var8++) {
            BlockPos var9 = param1.relative(var7, var8);
            BlockState var10 = param0.getBlockState(var9);
            if (var10.is(this.hook)) {
               if (var10.getValue(TripWireHookBlock.FACING) == var7.getOpposite()) {
                  this.hook.calculateState(param0, var9, var10, false, true, var8, param2);
               }
               break;
            }

            if (!var10.is(this)) {
               break;
            }
         }
      }
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (!param1.isClientSide) {
         if (!param0.getValue(POWERED)) {
            this.checkPressed(param1, param2);
         }
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.getBlockState(param2).getValue(POWERED)) {
         this.checkPressed(param1, param2);
      }
   }

   private void checkPressed(Level param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      boolean var4 = var3.getValue(POWERED);
      boolean var5 = false;
      List<? extends Entity> var6 = param0.getEntities(null, var3.getShape(param0, param1).bounds().move(param1));
      if (!var6.isEmpty()) {
         for (Entity var8 : var6) {
            if (!var8.isIgnoringBlockTriggers()) {
               var5 = true;
               break;
            }
         }
      }

      if (var5 != var4) {
         var3 = var3.setValue(POWERED, var5);
         param0.setBlock(param1, var3, 3);
         this.updateSource(param0, param1, var3);
      }

      if (var5) {
         param0.scheduleTick(new BlockPos(param1), this, 10);
      }
   }

   public boolean shouldConnectTo(BlockState param0, Direction param1) {
      return param0.is(this.hook) ? param0.getValue(TripWireHookBlock.FACING) == param1.getOpposite() : param0.is(this);
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

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
   }
}
