package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

public class ComparatorBlock extends DiodeBlock implements EntityBlock {
   public static final EnumProperty<ComparatorMode> MODE = BlockStateProperties.MODE_COMPARATOR;

   public ComparatorBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(MODE, ComparatorMode.COMPARE));
   }

   @Override
   protected int getDelay(BlockState param0) {
      return 2;
   }

   @Override
   protected int getOutputSignal(BlockGetter param0, BlockPos param1, BlockState param2) {
      BlockEntity var4 = param0.getBlockEntity(param1);
      return var4 instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)var4).getOutputSignal() : 0;
   }

   private int calculateOutputSignal(Level param0, BlockPos param1, BlockState param2) {
      int var4 = this.getInputSignal(param0, param1, param2);
      if (var4 == 0) {
         return 0;
      } else {
         int var5 = this.getAlternateSignal(param0, param1, param2);
         if (var5 > var4) {
            return 0;
         } else {
            return param2.getValue(MODE) == ComparatorMode.SUBTRACT ? var4 - var5 : var4;
         }
      }
   }

   @Override
   protected boolean shouldTurnOn(Level param0, BlockPos param1, BlockState param2) {
      int var4 = this.getInputSignal(param0, param1, param2);
      if (var4 == 0) {
         return false;
      }

      int var5 = this.getAlternateSignal(param0, param1, param2);
      return var4 > var5 ? true : var4 == var5 && param2.getValue(MODE) == ComparatorMode.COMPARE;
   }

   @Override
   protected int getInputSignal(Level param0, BlockPos param1, BlockState param2) {
      int var4 = super.getInputSignal(param0, param1, param2);
      Direction var5 = param2.getValue(FACING);
      BlockPos var6 = param1.relative(var5);
      BlockState var7 = param0.getBlockState(var6);
      if (var7.hasAnalogOutputSignal()) {
         var4 = var7.getAnalogOutputSignal(param0, var6);
      } else if (var4 < 15 && var7.isRedstoneConductor(param0, var6)) {
         var6 = var6.relative(var5);
         var7 = param0.getBlockState(var6);
         ItemFrame var8 = this.getItemFrame(param0, var5, var6);
         int var9 = Math.max(
            var8 == null ? Integer.MIN_VALUE : var8.getAnalogOutput(),
            var7.hasAnalogOutputSignal() ? var7.getAnalogOutputSignal(param0, var6) : Integer.MIN_VALUE
         );
         if (var9 != Integer.MIN_VALUE) {
            var4 = var9;
         }
      }

      return var4;
   }

   @Nullable
   private ItemFrame getItemFrame(Level param0, Direction param1, BlockPos param2) {
      List<ItemFrame> var4 = param0.getEntitiesOfClass(
         ItemFrame.class,
         new AABB(param2.getX(), param2.getY(), param2.getZ(), param2.getX() + 1, param2.getY() + 1, param2.getZ() + 1),
         l_param1 -> l_param1 != null && l_param1.getDirection() == param1
      );
      return var4.size() == 1 ? var4.get(0) : null;
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (!param3.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      }

      param0 = param0.cycle(MODE);
      float var7 = param0.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
      param1.playSound(param3, param2, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, var7);
      param1.setBlock(param2, param0, 2);
      this.refreshOutputState(param1, param2, param0);
      return InteractionResult.sidedSuccess(param1.isClientSide);
   }

   @Override
   protected void checkTickOnNeighbor(Level param0, BlockPos param1, BlockState param2) {
      if (!param0.getBlockTicks().willTickThisTick(param1, this)) {
         int var4 = this.calculateOutputSignal(param0, param1, param2);
         BlockEntity var5 = param0.getBlockEntity(param1);
         int var6 = var5 instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)var5).getOutputSignal() : 0;
         if (var4 != var6 || param2.getValue(POWERED) != this.shouldTurnOn(param0, param1, param2)) {
            TickPriority var7 = this.shouldPrioritize(param0, param1, param2) ? TickPriority.HIGH : TickPriority.NORMAL;
            param0.scheduleTick(param1, this, 2, var7);
         }
      }
   }

   private void refreshOutputState(Level param0, BlockPos param1, BlockState param2) {
      int var4 = this.calculateOutputSignal(param0, param1, param2);
      BlockEntity var5 = param0.getBlockEntity(param1);
      int var6 = 0;
      if (var5 instanceof ComparatorBlockEntity var7) {
         var6 = var7.getOutputSignal();
         var7.setOutputSignal(var4);
      }

      if (var6 != var4 || param2.getValue(MODE) == ComparatorMode.COMPARE) {
         boolean var7 = this.shouldTurnOn(param0, param1, param2);
         boolean var8 = param2.getValue(POWERED);
         if (var8 && !var7) {
            param0.setBlock(param1, param2.setValue(POWERED, false), 2);
         } else if (!var8 && var7) {
            param0.setBlock(param1, param2.setValue(POWERED, true), 2);
         }

         this.updateNeighborsInFront(param0, param1, param2);
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      this.refreshOutputState(param1, param2, param0);
   }

   @Override
   public boolean triggerEvent(BlockState param0, Level param1, BlockPos param2, int param3, int param4) {
      super.triggerEvent(param0, param1, param2, param3, param4);
      BlockEntity var6 = param1.getBlockEntity(param2);
      return var6 != null && var6.triggerEvent(param3, param4);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new ComparatorBlockEntity(param0, param1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, MODE, POWERED);
   }
}
