package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
   public static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
   public static final VoxelShape SHAPE_POST = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
   public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
   public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0, 15.0, 0.0, 16.0, 15.0, 16.0);
   public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
   public static final VoxelShape SHAPE_WEST = Shapes.or(
      Block.box(1.0, 10.0, 0.0, 5.333333, 14.0, 16.0),
      Block.box(5.333333, 12.0, 0.0, 9.666667, 16.0, 16.0),
      Block.box(9.666667, 14.0, 0.0, 14.0, 18.0, 16.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_NORTH = Shapes.or(
      Block.box(0.0, 10.0, 1.0, 16.0, 14.0, 5.333333),
      Block.box(0.0, 12.0, 5.333333, 16.0, 16.0, 9.666667),
      Block.box(0.0, 14.0, 9.666667, 16.0, 18.0, 14.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_EAST = Shapes.or(
      Block.box(10.666667, 10.0, 0.0, 15.0, 14.0, 16.0),
      Block.box(6.333333, 12.0, 0.0, 10.666667, 16.0, 16.0),
      Block.box(2.0, 14.0, 0.0, 6.333333, 18.0, 16.0),
      SHAPE_COMMON
   );
   public static final VoxelShape SHAPE_SOUTH = Shapes.or(
      Block.box(0.0, 10.0, 10.666667, 16.0, 14.0, 15.0),
      Block.box(0.0, 12.0, 6.333333, 16.0, 16.0, 10.666667),
      Block.box(0.0, 14.0, 2.0, 16.0, 18.0, 6.333333),
      SHAPE_COMMON
   );
   private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

   protected LecternBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(HAS_BOOK, false));
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return SHAPE_COMMON;
   }

   @Override
   public boolean useShapeForLightOcclusion(BlockState param0) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      Level var2 = param0.getLevel();
      ItemStack var3 = param0.getItemInHand();
      Player var4 = param0.getPlayer();
      boolean var5 = false;
      if (!var2.isClientSide && var4 != null && var4.canUseGameMasterBlocks()) {
         CompoundTag var6 = BlockItem.getBlockEntityData(var3);
         if (var6 != null && var6.contains("Book")) {
            var5 = true;
         }
      }

      return this.defaultBlockState().setValue(FACING, param0.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, var5);
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE_COLLISION;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      switch ((Direction)param0.getValue(FACING)) {
         case NORTH:
            return SHAPE_NORTH;
         case SOUTH:
            return SHAPE_SOUTH;
         case EAST:
            return SHAPE_EAST;
         case WEST:
            return SHAPE_WEST;
         default:
            return SHAPE_COMMON;
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
      param0.add(FACING, POWERED, HAS_BOOK);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new LecternBlockEntity(param0, param1);
   }

   public static boolean tryPlaceBook(@Nullable Entity param0, Level param1, BlockPos param2, BlockState param3, ItemStack param4) {
      if (!param3.getValue(HAS_BOOK)) {
         if (!param1.isClientSide) {
            placeBook(param0, param1, param2, param3, param4);
         }

         return true;
      } else {
         return false;
      }
   }

   private static void placeBook(@Nullable Entity param0, Level param1, BlockPos param2, BlockState param3, ItemStack param4) {
      if (param1.getBlockEntity(param2) instanceof LecternBlockEntity var6) {
         var6.setBook(param4.split(1));
         resetBookState(param0, param1, param2, param3, true);
         param1.playSound((Player)null, param2, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public static void resetBookState(@Nullable Entity param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      BlockState var5 = param3.setValue(POWERED, false).setValue(HAS_BOOK, param4);
      param1.setBlock(param2, var5, 3);
      param1.gameEvent(GameEvent.BLOCK_CHANGE, param2, GameEvent.Context.of(param0, var5));
      updateBelow(param1, param2, param3);
   }

   public static void signalPageChange(Level param0, BlockPos param1, BlockState param2) {
      changePowered(param0, param1, param2, true);
      param0.scheduleTick(param1, param2.getBlock(), 2);
      param0.levelEvent(1043, param1, 0);
   }

   private static void changePowered(Level param0, BlockPos param1, BlockState param2, boolean param3) {
      param0.setBlock(param1, param2.setValue(POWERED, param3), 3);
      updateBelow(param0, param1, param2);
   }

   private static void updateBelow(Level param0, BlockPos param1, BlockState param2) {
      param0.updateNeighborsAt(param1.below(), param2.getBlock());
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      changePowered(param1, param2, param0, false);
   }

   @Override
   public void onRemove(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param0.is(param3.getBlock())) {
         if (param0.getValue(HAS_BOOK)) {
            this.popBook(param0, param1, param2);
         }

         if (param0.getValue(POWERED)) {
            param1.updateNeighborsAt(param2.below(), this);
         }

         super.onRemove(param0, param1, param2, param3, param4);
      }
   }

   private void popBook(BlockState param0, Level param1, BlockPos param2) {
      if (param1.getBlockEntity(param2) instanceof LecternBlockEntity var5) {
         Direction var6 = param0.getValue(FACING);
         ItemStack var7 = var5.getBook().copy();
         float var8 = 0.25F * var6.getStepX();
         float var9 = 0.25F * var6.getStepZ();
         ItemEntity var10 = new ItemEntity(param1, param2.getX() + 0.5 + var8, param2.getY() + 1, param2.getZ() + 0.5 + var9, var7);
         var10.setDefaultPickUpDelay();
         param1.addFreshEntity(var10);
         var5.clearContent();
      }
   }

   @Override
   public boolean isSignalSource(BlockState param0) {
      return true;
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getValue(POWERED) ? 15 : 0;
   }

   @Override
   public int getDirectSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param3 == Direction.UP && param0.getValue(POWERED) ? 15 : 0;
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      if (param0.getValue(HAS_BOOK)) {
         BlockEntity var4 = param1.getBlockEntity(param2);
         if (var4 instanceof LecternBlockEntity) {
            return ((LecternBlockEntity)var4).getRedstoneSignal();
         }
      }

      return 0;
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param0.getValue(HAS_BOOK)) {
         if (!param1.isClientSide) {
            this.openScreen(param1, param2, param3);
         }

         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         ItemStack var7 = param3.getItemInHand(param4);
         return !var7.isEmpty() && !var7.is(ItemTags.LECTERN_BOOKS) ? InteractionResult.CONSUME : InteractionResult.PASS;
      }
   }

   @Nullable
   @Override
   public MenuProvider getMenuProvider(BlockState param0, Level param1, BlockPos param2) {
      return !param0.getValue(HAS_BOOK) ? null : super.getMenuProvider(param0, param1, param2);
   }

   private void openScreen(Level param0, BlockPos param1, Player param2) {
      BlockEntity var4 = param0.getBlockEntity(param1);
      if (var4 instanceof LecternBlockEntity) {
         param2.openMenu((LecternBlockEntity)var4);
         param2.awardStat(Stats.INTERACT_WITH_LECTERN);
      }
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
