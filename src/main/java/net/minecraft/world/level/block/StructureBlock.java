package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.BlockHitResult;

public class StructureBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

   protected StructureBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new StructureBlockEntity(param0, param1);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      BlockEntity var7 = param1.getBlockEntity(param2);
      if (var7 instanceof StructureBlockEntity) {
         return ((StructureBlockEntity)var7).usedBy(param3) ? InteractionResult.sidedSuccess(param1.isClientSide) : InteractionResult.PASS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, @Nullable LivingEntity param3, ItemStack param4) {
      if (!param0.isClientSide) {
         if (param3 != null) {
            BlockEntity var6 = param0.getBlockEntity(param1);
            if (var6 instanceof StructureBlockEntity) {
               ((StructureBlockEntity)var6).createdBy(param3);
            }
         }
      }
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(MODE);
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (param1 instanceof ServerLevel) {
         if (param1.getBlockEntity(param2) instanceof StructureBlockEntity var8) {
            boolean var9 = param1.hasNeighborSignal(param2);
            boolean var10 = var8.isPowered();
            if (var9 && !var10) {
               var8.setPowered(true);
               this.trigger((ServerLevel)param1, var8);
            } else if (!var9 && var10) {
               var8.setPowered(false);
            }
         }
      }
   }

   private void trigger(ServerLevel param0, StructureBlockEntity param1) {
      switch (param1.getMode()) {
         case SAVE:
            param1.saveStructure(false);
            break;
         case LOAD:
            param1.loadStructure(param0, false);
            break;
         case CORNER:
            param1.unloadStructure();
         case DATA:
      }
   }
}
