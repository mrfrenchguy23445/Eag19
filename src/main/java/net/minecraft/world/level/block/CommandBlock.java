package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
   private final boolean automatic;

   public CommandBlock(BlockBehaviour.Properties param0, boolean param1) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
      this.automatic = param1;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      CommandBlockEntity var3 = new CommandBlockEntity(param0, param1);
      var3.setAutomatic(this.automatic);
      return var3;
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      if (!param1.isClientSide) {
         if (param1.getBlockEntity(param2) instanceof CommandBlockEntity var8) {
            boolean var9 = param1.hasNeighborSignal(param2);
            boolean var10 = var8.isPowered();
            var8.setPowered(var9);
            if (!var10 && !var8.isAutomatic() && var8.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
               if (var9) {
                  var8.markConditionMet();
                  param1.scheduleTick(param2, this, 1);
               }
            }
         }
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.getBlockEntity(param2) instanceof CommandBlockEntity var6) {
         BaseCommandBlock var7 = var6.getCommandBlock();
         boolean var8 = !StringUtil.isNullOrEmpty(var7.getCommand());
         CommandBlockEntity.Mode var9 = var6.getMode();
         boolean var10 = var6.wasConditionMet();
         if (var9 == CommandBlockEntity.Mode.AUTO) {
            var6.markConditionMet();
            if (var10) {
               this.execute(param0, param1, param2, var7, var8);
            } else if (var6.isConditional()) {
               var7.setSuccessCount(0);
            }

            if (var6.isPowered() || var6.isAutomatic()) {
               param1.scheduleTick(param2, this, 1);
            }
         } else if (var9 == CommandBlockEntity.Mode.REDSTONE) {
            if (var10) {
               this.execute(param0, param1, param2, var7, var8);
            } else if (var6.isConditional()) {
               var7.setSuccessCount(0);
            }
         }

         param1.updateNeighbourForOutputSignal(param2, this);
      }
   }

   private void execute(BlockState param0, Level param1, BlockPos param2, BaseCommandBlock param3, boolean param4) {
      if (param4) {
         param3.performCommand(param1);
      } else {
         param3.setSuccessCount(0);
      }

      executeChain(param1, param2, param0.getValue(FACING));
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      BlockEntity var7 = param1.getBlockEntity(param2);
      if (var7 instanceof CommandBlockEntity && param3.canUseGameMasterBlocks()) {
         param3.openCommandBlock((CommandBlockEntity)var7);
         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      BlockEntity var4 = param1.getBlockEntity(param2);
      return var4 instanceof CommandBlockEntity ? ((CommandBlockEntity)var4).getCommandBlock().getSuccessCount() : 0;
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      if (param0.getBlockEntity(param1) instanceof CommandBlockEntity var7) {
         BaseCommandBlock var8 = var7.getCommandBlock();
         if (param4.hasCustomHoverName()) {
            var8.setName(param4.getHoverName());
         }

         if (!param0.isClientSide) {
            if (BlockItem.getBlockEntityData(param4) == null) {
               var8.setTrackOutput(param0.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
               var7.setAutomatic(this.automatic);
            }

            if (var7.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
               boolean var9 = param0.hasNeighborSignal(param1);
               var7.setPowered(var9);
            }
         }
      }
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
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
      param0.add(FACING, CONDITIONAL);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return this.defaultBlockState().setValue(FACING, param0.getNearestLookingDirection().getOpposite());
   }

   private static void executeChain(Level param0, BlockPos param1, Direction param2) {
      BlockPos.MutableBlockPos var3 = param1.mutable();
      GameRules var4 = param0.getGameRules();
      int var5 = var4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);

      while (var5-- > 0) {
         var3.move(param2);
         BlockState var6 = param0.getBlockState(var3);
         Block var7 = var6.getBlock();
         if (!var6.is(Blocks.CHAIN_COMMAND_BLOCK)
            || !(param0.getBlockEntity(var3) instanceof CommandBlockEntity var9)
            || var9.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            break;
         }

         if (var9.isPowered() || var9.isAutomatic()) {
            BaseCommandBlock var10 = var9.getCommandBlock();
            if (var9.markConditionMet()) {
               if (!var10.performCommand(param0)) {
                  break;
               }

               param0.updateNeighbourForOutputSignal(var3, var7);
            } else if (var9.isConditional()) {
               var10.setSuccessCount(0);
            }
         }

         param2 = var6.getValue(FACING);
      }

      if (var5 <= 0) {
         int var6 = Math.max(var4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
         LOGGER.warn("Command Block chain tried to execute more than {} steps!", var6);
      }
   }
}
