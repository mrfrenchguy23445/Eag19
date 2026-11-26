package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class JukeboxBlock extends BaseEntityBlock {
   public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

   protected JukeboxBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(HAS_RECORD, false));
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, @Nullable LivingEntity param3, ItemStack param4) {
      super.setPlacedBy(param0, param1, param2, param3, param4);
      CompoundTag var6 = BlockItem.getBlockEntityData(param4);
      if (var6 != null && var6.contains("RecordItem")) {
         param0.setBlock(param1, param2.setValue(HAS_RECORD, true), 2);
      }
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param0.getValue(HAS_RECORD) && param1.getBlockEntity(param2) instanceof JukeboxBlockEntity var7) {
         var7.popOutRecord();
         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param0.is(param3.getBlock())) {
         if (param1.getBlockEntity(param2) instanceof JukeboxBlockEntity var6) {
            var6.popOutRecord();
         }

         super.onRemove(param0, param1, param2, param3, param4);
      }
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new JukeboxBlockEntity(param0, param1);
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param1.getBlockEntity(param2) instanceof JukeboxBlockEntity var5 && var5.isRecordPlaying() ? 15 : 0;
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      return param1.getBlockEntity(param2) instanceof JukeboxBlockEntity var4 && var4.getFirstItem().getItem() instanceof RecordItem var5
         ? var5.getAnalogOutput()
         : 0;
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(HAS_RECORD);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return param1.getValue(HAS_RECORD) ? createTickerHelper(param2, BlockEntityType.JUKEBOX, JukeboxBlockEntity::playRecordTick) : null;
   }
}
