package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireHookBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   protected static final int WIRE_DIST_MIN = 1;
   protected static final int WIRE_DIST_MAX = 42;
   private static final int RECHECK_PERIOD = 10;
   protected static final int AABB_OFFSET = 3;
   protected static final VoxelShape NORTH_AABB = Block.box(5.0, 0.0, 10.0, 11.0, 10.0, 16.0);
   protected static final VoxelShape SOUTH_AABB = Block.box(5.0, 0.0, 0.0, 11.0, 10.0, 6.0);
   protected static final VoxelShape WEST_AABB = Block.box(10.0, 0.0, 5.0, 16.0, 10.0, 11.0);
   protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 5.0, 6.0, 10.0, 11.0);

   public TripWireHookBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(ATTACHED, false));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      switch ((Direction)param0.getValue(FACING)) {
         case EAST:
         default:
            return EAST_AABB;
         case WEST:
            return WEST_AABB;
         case SOUTH:
            return SOUTH_AABB;
         case NORTH:
            return NORTH_AABB;
      }
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      Direction var4 = param0.getValue(FACING);
      BlockPos var5 = param2.relative(var4.getOpposite());
      BlockState var6 = param1.getBlockState(var5);
      return var4.getAxis().isHorizontal() && var6.isFaceSturdy(param1, var5, var4);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return param1.getOpposite() == param0.getValue(FACING) && !param0.canSurvive(param3, param4)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = this.defaultBlockState().setValue(POWERED, false).setValue(ATTACHED, false);
      LevelReader var3 = param0.getLevel();
      BlockPos var4 = param0.getClickedPos();
      Direction[] var5 = param0.getNearestLookingDirections();

      for (Direction var9 : var5) {
         if (var9.getAxis().isHorizontal()) {
            Direction var10 = var9.getOpposite();
            var2 = var2.setValue(FACING, var10);
            if (var2.canSurvive(var3, var4)) {
               return var2;
            }
         }
      }

      return null;
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      this.calculateState(param0, param1, param2, false, false, -1, null);
   }

   public void calculateState(Level param0, BlockPos param1, BlockState param2, boolean param3, boolean param4, int param5, @Nullable BlockState param6) {
      Direction var8 = param2.getValue(FACING);
      boolean var9 = param2.getValue(ATTACHED);
      boolean var10 = param2.getValue(POWERED);
      boolean var11 = !param3;
      boolean var12 = false;
      int var13 = 0;
      BlockState[] var14 = new BlockState[42];

      for (int var15 = 1; var15 < 42; var15++) {
         BlockPos var16 = param1.relative(var8, var15);
         BlockState var17 = param0.getBlockState(var16);
         if (var17.is(Blocks.TRIPWIRE_HOOK)) {
            if (var17.getValue(FACING) == var8.getOpposite()) {
               var13 = var15;
            }
            break;
         }

         if (!var17.is(Blocks.TRIPWIRE) && var15 != param5) {
            var14[var15] = null;
            var11 = false;
         } else {
            if (var15 == param5) {
               var17 = MoreObjects.firstNonNull(param6, var17);
            }

            boolean var18 = !var17.getValue(TripWireBlock.DISARMED);
            boolean var19 = var17.getValue(TripWireBlock.POWERED);
            var12 |= var18 && var19;
            var14[var15] = var17;
            if (var15 == param5) {
               param0.scheduleTick(param1, this, 10);
               var11 &= var18;
            }
         }
      }

      var11 &= var13 > 1;
      var12 &= var11;
      BlockState var15 = this.defaultBlockState().setValue(ATTACHED, var11).setValue(POWERED, var12);
      if (var13 > 0) {
         BlockPos var16 = param1.relative(var8, var13);
         Direction var17 = var8.getOpposite();
         param0.setBlock(var16, var15.setValue(FACING, var17), 3);
         this.notifyNeighbors(param0, var16, var17);
         this.emitState(param0, var16, var11, var12, var9, var10);
      }

      this.emitState(param0, param1, var11, var12, var9, var10);
      if (!param3) {
         param0.setBlock(param1, var15.setValue(FACING, var8), 3);
         if (param4) {
            this.notifyNeighbors(param0, param1, var8);
         }
      }

      if (var9 != var11) {
         for (int var16 = 1; var16 < var13; var16++) {
            BlockPos var17 = param1.relative(var8, var16);
            BlockState var18 = var14[var16];
            if (var18 != null) {
               param0.setBlock(var17, var18.setValue(ATTACHED, var11), 3);
               if (!param0.getBlockState(var17).isAir()) {
               }
            }
         }
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      this.calculateState(param1, param2, param0, false, true, -1, null);
   }

   private void emitState(Level param0, BlockPos param1, boolean param2, boolean param3, boolean param4, boolean param5) {
      if (param3 && !param5) {
         param0.playSound((Player)null, param1, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
         param0.gameEvent((Entity)null, GameEvent.BLOCK_ACTIVATE, param1);
      } else if (!param3 && param5) {
         param0.playSound((Player)null, param1, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
         param0.gameEvent((Entity)null, GameEvent.BLOCK_DEACTIVATE, param1);
      } else if (param2 && !param4) {
         param0.playSound((Player)null, param1, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
         param0.gameEvent((Entity)null, GameEvent.BLOCK_ATTACH, param1);
      } else if (!param2 && param4) {
         param0.playSound((Player)null, param1, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (param0.random.nextFloat() * 0.2F + 0.9F));
         param0.gameEvent((Entity)null, GameEvent.BLOCK_DETACH, param1);
      }
   }

   private void notifyNeighbors(Level param0, BlockPos param1, Direction param2) {
      param0.updateNeighborsAt(param1, this);
      param0.updateNeighborsAt(param1.relative(param2.getOpposite()), this);
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param4 && !param0.is(param3.getBlock())) {
         boolean var6 = param0.getValue(ATTACHED);
         boolean var7 = param0.getValue(POWERED);
         if (var6 || var7) {
            this.calculateState(param1, param2, param0, true, false, -1, null);
         }

         if (var7) {
            param1.updateNeighborsAt(param2, this);
            param1.updateNeighborsAt(param2.relative(param0.getValue(FACING).getOpposite()), this);
         }

         super.onRemove(param0, param1, param2, param3, param4);
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
         return param0.getValue(FACING) == param3 ? 15 : 0;
      }
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
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
      param0.add(FACING, POWERED, ATTACHED);
   }
}
