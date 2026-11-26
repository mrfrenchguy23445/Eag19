package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChestBlock extends AbstractChestBlock<ChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final int EVENT_SET_OPEN_COUNT = 1;
   protected static final int AABB_OFFSET = 1;
   protected static final int AABB_HEIGHT = 14;
   protected static final VoxelShape NORTH_AABB = Block.box(1.0, 0.0, 0.0, 15.0, 14.0, 15.0);
   protected static final VoxelShape SOUTH_AABB = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 16.0);
   protected static final VoxelShape WEST_AABB = Block.box(0.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   protected static final VoxelShape EAST_AABB = Block.box(1.0, 0.0, 1.0, 16.0, 14.0, 15.0);
   protected static final VoxelShape AABB = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>> CHEST_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>>() {
      public Optional<Container> acceptDouble(ChestBlockEntity param0, ChestBlockEntity param1) {
         return Optional.of(new CompoundContainer(param0, param1));
      }

      public Optional<Container> acceptSingle(ChestBlockEntity param0) {
         return Optional.of(param0);
      }

      public Optional<Container> acceptNone() {
         return Optional.empty();
      }
   };
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {
      public Optional<MenuProvider> acceptDouble(final ChestBlockEntity param0, final ChestBlockEntity param1) {
         final Container var3 = new CompoundContainer(param0, param1);
         return Optional.of(new MenuProvider() {
            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int l_param0, Inventory l_param1, Player l_param2) {
               if (param0.canOpen(l_param2) && param1.canOpen(l_param2)) {
                  param0.unpackLootTable(l_param1.player);
                  param1.unpackLootTable(l_param1.player);
                  return ChestMenu.sixRows(l_param0, l_param1, var3);
               } else {
                  return null;
               }
            }

            @Override
            public Component getDisplayName() {
               if (param0.hasCustomName()) {
                  return param0.getDisplayName();
               } else {
                  return param1.hasCustomName() ? param1.getDisplayName() : Component.translatable("container.chestDouble");
               }
            }
         });
      }

      public Optional<MenuProvider> acceptSingle(ChestBlockEntity param0) {
         return Optional.of(param0);
      }

      public Optional<MenuProvider> acceptNone() {
         return Optional.empty();
      }
   };

   protected ChestBlock(BlockBehaviour.Properties param0, Supplier<BlockEntityType<? extends ChestBlockEntity>> param1) {
      super(param0, param1);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false));
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState param0) {
      ChestType var1 = param0.getValue(TYPE);
      if (var1 == ChestType.SINGLE) {
         return DoubleBlockCombiner.BlockType.SINGLE;
      } else {
         return var1 == ChestType.RIGHT ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
      }
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      if (param2.is(this) && param1.getAxis().isHorizontal()) {
         ChestType var7 = param2.getValue(TYPE);
         if (param0.getValue(TYPE) == ChestType.SINGLE
            && var7 != ChestType.SINGLE
            && param0.getValue(FACING) == param2.getValue(FACING)
            && getConnectedDirection(param2) == param1.getOpposite()) {
            return param0.setValue(TYPE, var7.getOpposite());
         }
      } else if (getConnectedDirection(param0) == param1) {
         return param0.setValue(TYPE, ChestType.SINGLE);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      if (param0.getValue(TYPE) == ChestType.SINGLE) {
         return AABB;
      }

      switch (getConnectedDirection(param0)) {
         case NORTH:
         default:
            return NORTH_AABB;
         case SOUTH:
            return SOUTH_AABB;
         case WEST:
            return WEST_AABB;
         case EAST:
            return EAST_AABB;
      }
   }

   public static Direction getConnectedDirection(BlockState param0) {
      Direction var1 = param0.getValue(FACING);
      return param0.getValue(TYPE) == ChestType.LEFT ? var1.getClockWise() : var1.getCounterClockWise();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      ChestType var2 = ChestType.SINGLE;
      Direction var3 = param0.getHorizontalDirection().getOpposite();
      FluidState var4 = param0.getLevel().getFluidState(param0.getClickedPos());
      boolean var5 = param0.isSecondaryUseActive();
      Direction var6 = param0.getClickedFace();
      if (var6.getAxis().isHorizontal() && var5) {
         Direction var7 = this.candidatePartnerFacing(param0, var6.getOpposite());
         if (var7 != null && var7.getAxis() != var6.getAxis()) {
            var3 = var7;
            var2 = var3.getCounterClockWise() == var6.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
         }
      }

      if (var2 == ChestType.SINGLE && !var5) {
         if (var3 == this.candidatePartnerFacing(param0, var3.getClockWise())) {
            var2 = ChestType.LEFT;
         } else if (var3 == this.candidatePartnerFacing(param0, var3.getCounterClockWise())) {
            var2 = ChestType.RIGHT;
         }
      }

      return this.defaultBlockState().setValue(FACING, var3).setValue(TYPE, var2).setValue(WATERLOGGED, var4.getType() == Fluids.WATER);
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Nullable
   private Direction candidatePartnerFacing(BlockPlaceContext param0, Direction param1) {
      BlockState var3 = param0.getLevel().getBlockState(param0.getClickedPos().relative(param1));
      return var3.is(this) && var3.getValue(TYPE) == ChestType.SINGLE ? var3.getValue(FACING) : null;
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      if (param4.hasCustomHoverName()) {
         BlockEntity var6 = param0.getBlockEntity(param1);
         if (var6 instanceof ChestBlockEntity) {
            ((ChestBlockEntity)var6).setCustomName(param4.getHoverName());
         }
      }
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param0.is(param3.getBlock())) {
         BlockEntity var6 = param1.getBlockEntity(param2);
         if (var6 instanceof Container) {
            Containers.dropContents(param1, param2, (Container)var6);
            param1.updateNeighbourForOutputSignal(param2, this);
         }

         super.onRemove(param0, param1, param2, param3, param4);
      }
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.isClientSide) {
         return InteractionResult.SUCCESS;
      }

      MenuProvider var7 = this.getMenuProvider(param0, param1, param2);
      if (var7 != null) {
         param3.openMenu(var7);
         param3.awardStat(this.getOpenChestStat());
         PiglinAi.angerNearbyPiglins(param3, true);
      }

      return InteractionResult.CONSUME;
   }

   protected Stat<ResourceLocation> getOpenChestStat() {
      return Stats.CUSTOM.get(Stats.OPEN_CHEST);
   }

   public BlockEntityType<? extends ChestBlockEntity> blockEntityType() {
      return this.blockEntityType.get();
   }

   @Nullable
   public static Container getContainer(ChestBlock param0, BlockState param1, Level param2, BlockPos param3, boolean param4) {
      return param0.combine(param1, param2, param3, param4).apply(CHEST_COMBINER).orElse(null);
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState param0, Level param1, BlockPos param2, boolean param3) {
      BiPredicate<LevelAccessor, BlockPos> var5;
      if (param3) {
         var5 = (l_param0, l_param1) -> false;
      } else {
         var5 = ChestBlock::isChestBlockedAt;
      }

      return DoubleBlockCombiner.combineWithNeigbour(
         this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, param0, param1, param2, var5
      );
   }

   @Nullable
   @Override
   public MenuProvider getMenuProvider(BlockState param0, Level param1, BlockPos param2) {
      return this.combine(param0, param1, param2, false).apply(MENU_PROVIDER_COMBINER).orElse(null);
   }

   public static DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction> opennessCombiner(final LidBlockEntity param0) {
      return new DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction>() {
         public Float2FloatFunction acceptDouble(ChestBlockEntity l_param0, ChestBlockEntity l_param1) {
            return l2_param2 -> Math.max(l_param0.getOpenNess(l2_param2), l_param1.getOpenNess(l2_param2));
         }

         public Float2FloatFunction acceptSingle(ChestBlockEntity l_param0) {
            return l_param0::getOpenNess;
         }

         public Float2FloatFunction acceptNone() {
            return param0::getOpenNess;
         }
      };
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new ChestBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return param0.isClientSide ? createTickerHelper(param2, this.blockEntityType(), ChestBlockEntity::lidAnimateTick) : null;
   }

   public static boolean isChestBlockedAt(LevelAccessor param0, BlockPos param1) {
      return isBlockedChestByBlock(param0, param1) || isCatSittingOnChest(param0, param1);
   }

   private static boolean isBlockedChestByBlock(BlockGetter param0, BlockPos param1) {
      BlockPos var2 = param1.above();
      return param0.getBlockState(var2).isRedstoneConductor(param0, var2);
   }

   private static boolean isCatSittingOnChest(LevelAccessor param0, BlockPos param1) {
      List<Cat> var2 = param0.getEntitiesOfClass(
         Cat.class, new AABB(param1.getX(), param1.getY() + 1, param1.getZ(), param1.getX() + 1, param1.getY() + 2, param1.getZ() + 1)
      );
      if (!var2.isEmpty()) {
         for (Cat var4 : var2) {
            if (var4.isInSittingPose()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(this, param0, param1, param2, false));
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
      param0.add(FACING, TYPE, WATERLOGGED);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      BlockEntity var5 = param1.getBlockEntity(param2);
      if (var5 instanceof ChestBlockEntity) {
         ((ChestBlockEntity)var5).recheckOpen();
      }
   }
}
