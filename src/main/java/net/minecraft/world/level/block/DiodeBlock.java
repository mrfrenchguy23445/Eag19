package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public abstract class DiodeBlock extends HorizontalDirectionalBlock {
   protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   protected DiodeBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return canSupportRigidBlock(param1, param2.below());
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!this.isLocked(param1, param2, param0)) {
         boolean var5 = param0.getValue(POWERED);
         boolean var6 = this.shouldTurnOn(param1, param2, param0);
         if (var5 && !var6) {
            param1.setBlock(param2, param0.setValue(POWERED, false), 2);
         } else if (!var5) {
            param1.setBlock(param2, param0.setValue(POWERED, true), 2);
            if (!var6) {
               param1.scheduleTick(param2, this, this.getDelay(param0), TickPriority.VERY_HIGH);
            }
         }
      }
   }

   @Override
   public int getDirectSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getSignal(param1, param2, param3);
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      if (!param0.getValue(POWERED)) {
         return 0;
      } else {
         return param0.getValue(FACING) == param3 ? this.getOutputSignal(param1, param2, param0) : 0;
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (param0.canSurvive(param1, param2)) {
         this.checkTickOnNeighbor(param1, param2, param0);
      } else {
         BlockEntity var7 = param0.hasBlockEntity() ? param1.getBlockEntity(param2) : null;
         dropResources(param0, param1, param2, var7);
         param1.removeBlock(param2, false);

         for (Direction var11 : Direction.values()) {
            param1.updateNeighborsAt(param2.relative(var11), this);
         }
      }
   }

   protected void checkTickOnNeighbor(Level param0, BlockPos param1, BlockState param2) {
      if (!this.isLocked(param0, param1, param2)) {
         boolean var4 = param2.getValue(POWERED);
         boolean var5 = this.shouldTurnOn(param0, param1, param2);
         if (var4 != var5 && !param0.getBlockTicks().willTickThisTick(param1, this)) {
            TickPriority var6 = TickPriority.HIGH;
            if (this.shouldPrioritize(param0, param1, param2)) {
               var6 = TickPriority.EXTREMELY_HIGH;
            } else if (var4) {
               var6 = TickPriority.VERY_HIGH;
            }

            param0.scheduleTick(param1, this, this.getDelay(param2), var6);
         }
      }
   }

   public boolean isLocked(LevelReader param0, BlockPos param1, BlockState param2) {
      return false;
   }

   protected boolean shouldTurnOn(Level param0, BlockPos param1, BlockState param2) {
      return this.getInputSignal(param0, param1, param2) > 0;
   }

   protected int getInputSignal(Level param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING);
      BlockPos var5 = param1.relative(var4);
      int var6 = param0.getSignal(var5, var4);
      if (var6 >= 15) {
         return var6;
      }

      BlockState var7 = param0.getBlockState(var5);
      return Math.max(var6, var7.is(Blocks.REDSTONE_WIRE) ? var7.getValue(RedStoneWireBlock.POWER) : 0);
   }

   protected int getAlternateSignal(LevelReader param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING);
      Direction var5 = var4.getClockWise();
      Direction var6 = var4.getCounterClockWise();
      return Math.max(this.getAlternateSignalAt(param0, param1.relative(var5), var5), this.getAlternateSignalAt(param0, param1.relative(var6), var6));
   }

   protected int getAlternateSignalAt(LevelReader param0, BlockPos param1, Direction param2) {
      BlockState var4 = param0.getBlockState(param1);
      if (this.isAlternateInput(var4)) {
         if (var4.is(Blocks.REDSTONE_BLOCK)) {
            return 15;
         } else {
            return var4.is(Blocks.REDSTONE_WIRE) ? var4.getValue(RedStoneWireBlock.POWER) : param0.getDirectSignal(param1, param2);
         }
      } else {
         return 0;
      }
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.defaultBlockState().setValue(FACING, param0.getHorizontalDirection().getOpposite());
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      if (this.shouldTurnOn(param0, param1, param2)) {
         param0.scheduleTick(param1, this, 1);
      }
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      this.updateNeighborsInFront(param1, param2, param0);
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param4 && !param0.is(param3.getBlock())) {
         super.onRemove(param0, param1, param2, param3, param4);
         this.updateNeighborsInFront(param1, param2, param0);
      }
   }

   protected void updateNeighborsInFront(Level param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING);
      BlockPos var5 = param1.relative(var4.getOpposite());
      param0.neighborChanged(var5, this, param1);
      param0.updateNeighborsAtExceptFromFacing(var5, this, var4);
   }

   protected boolean isAlternateInput(BlockState param0) {
      return param0.isSignalSource();
   }

   protected int getOutputSignal(BlockGetter param0, BlockPos param1, BlockState param2) {
      return 15;
   }

   public static boolean isDiode(BlockState param0) {
      return param0.getBlock() instanceof DiodeBlock;
   }

   public boolean shouldPrioritize(BlockGetter param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING).getOpposite();
      BlockState var5 = param0.getBlockState(param1.relative(var4));
      return isDiode(var5) && var5.getValue(FACING) != var4;
   }

   protected abstract int getDelay(BlockState var1);
}
