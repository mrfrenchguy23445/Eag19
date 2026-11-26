package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnderChestBlock extends AbstractChestBlock<EnderChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");

   protected EnderChestBlock(BlockBehaviour.Properties param0) {
      super(param0, () -> BlockEntityType.ENDER_CHEST);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState param0, Level param1, BlockPos param2, boolean param3) {
      return DoubleBlockCombiner.Combiner::acceptNone;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      FluidState var2 = param0.getLevel().getFluidState(param0.getClickedPos());
      return this.defaultBlockState().setValue(FACING, param0.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, var2.getType() == Fluids.WATER);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      PlayerEnderChestContainer var7 = param3.getEnderChestInventory();
      BlockEntity var8 = param1.getBlockEntity(param2);
      if (var7 != null && var8 instanceof EnderChestBlockEntity) {
         BlockPos var9 = param2.above();
         if (param1.getBlockState(var9).isRedstoneConductor(param1, var9)) {
            return InteractionResult.sidedSuccess(param1.isClientSide);
         }

         if (param1.isClientSide) {
            return InteractionResult.SUCCESS;
         }

         EnderChestBlockEntity var10 = (EnderChestBlockEntity)var8;
         var7.setActiveChest(var10);
         param3.openMenu(new SimpleMenuProvider((l_param1, l_param2, l_param3) -> ChestMenu.threeRows(l_param1, l_param2, var7), CONTAINER_TITLE));
         param3.awardStat(Stats.OPEN_ENDERCHEST);
         PiglinAi.angerNearbyPiglins(param3, true);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.sidedSuccess(param1.isClientSide);
      }
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new EnderChestBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return param0.isClientSide ? createTickerHelper(param2, BlockEntityType.ENDER_CHEST, EnderChestBlockEntity::lidAnimateTick) : null;
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      for (int var5 = 0; var5 < 3; var5++) {
         int var6 = param3.nextInt(2) * 2 - 1;
         int var7 = param3.nextInt(2) * 2 - 1;
         double var8 = param2.getX() + 0.5 + 0.25 * var6;
         double var10 = param2.getY() + param3.nextFloat();
         double var12 = param2.getZ() + 0.5 + 0.25 * var7;
         double var14 = param3.nextFloat() * var6;
         double var16 = (param3.nextFloat() - 0.5) * 0.125;
         double var18 = param3.nextFloat() * var7;
         param1.addParticle(ParticleTypes.PORTAL, var8, var10, var12, var14, var16, var18);
      }
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
      param0.add(FACING, WATERLOGGED);
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      BlockEntity var5 = param1.getBlockEntity(param2);
      if (var5 instanceof EnderChestBlockEntity) {
         ((EnderChestBlockEntity)var5).recheckOpen();
      }
   }
}
