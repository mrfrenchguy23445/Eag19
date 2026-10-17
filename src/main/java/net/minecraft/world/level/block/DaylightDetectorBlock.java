package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DaylightDetectorBlock extends BaseEntityBlock {
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
   protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

   public DaylightDetectorBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0).setValue(INVERTED, false));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public boolean useShapeForLightOcclusion(BlockState param0) {
      return true;
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getValue(POWER);
   }

   private static void updateSignalStrength(BlockState param0, Level param1, BlockPos param2) {
      int var3 = param1.getBrightness(LightLayer.SKY, param2) - param1.getSkyDarken();
      float var4 = param1.getSunAngle(1.0F);
      boolean var5 = param0.getValue(INVERTED);
      if (var5) {
         var3 = 15 - var3;
      } else if (var3 > 0) {
         float var6 = var4 < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
         var4 += (var6 - var4) * 0.2F;
         var3 = Math.round(var3 * Mth.cos(var4));
      }

      var3 = Mth.clamp(var3, 0, 15);
      if (param0.getValue(POWER) != var3) {
         param1.setBlock(param2, param0.setValue(POWER, var3), 3);
      }
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param3.mayBuild()) {
         if (param1.isClientSide) {
            return InteractionResult.SUCCESS;
         }

         BlockState var7 = param0.cycle(INVERTED);
         param1.setBlock(param2, var7, 4);
         param1.gameEvent(GameEvent.BLOCK_CHANGE, param2, GameEvent.Context.of(param3, var7));
         updateSignalStrength(var7, param1, param2);
         return InteractionResult.CONSUME;
      } else {
         return super.use(param0, param1, param2, param3, param4, param5);
      }
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new DaylightDetectorBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return !param0.isClientSide && param0.dimensionType().hasSkyLight()
         ? createTickerHelper(param2, BlockEntityType.DAYLIGHT_DETECTOR, DaylightDetectorBlock::tickEntity)
         : null;
   }

   private static void tickEntity(Level param0, BlockPos param1, BlockState param2, DaylightDetectorBlockEntity param3) {
      if (param0.getGameTime() % 20L == 0L) {
         updateSignalStrength(param2, param0, param1);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(POWER, INVERTED);
   }
}
