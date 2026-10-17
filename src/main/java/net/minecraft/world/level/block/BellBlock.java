package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BellBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape NORTH_SOUTH_FLOOR_SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape EAST_WEST_FLOOR_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   private static final VoxelShape BELL_TOP_SHAPE = Block.box(5.0, 6.0, 5.0, 11.0, 13.0, 11.0);
   private static final VoxelShape BELL_BOTTOM_SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 6.0, 12.0);
   private static final VoxelShape BELL_SHAPE = Shapes.or(BELL_BOTTOM_SHAPE, BELL_TOP_SHAPE);
   private static final VoxelShape NORTH_SOUTH_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 16.0));
   private static final VoxelShape EAST_WEST_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_WEST = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 13.0, 15.0, 9.0));
   private static final VoxelShape TO_EAST = Shapes.or(BELL_SHAPE, Block.box(3.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_NORTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 13.0));
   private static final VoxelShape TO_SOUTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 3.0, 9.0, 15.0, 16.0));
   private static final VoxelShape CEILING_SHAPE = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 7.0, 9.0, 16.0, 9.0));
   public static final int EVENT_BELL_RING = 1;

   public BellBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, false)
      );
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      boolean var7 = param1.hasNeighborSignal(param2);
      if (var7 != param0.getValue(POWERED)) {
         if (var7) {
            this.attemptToRing(param1, param2, null);
         }

         param1.setBlock(param2, param0.setValue(POWERED, var7), 3);
      }
   }

   @Override
   public void onProjectileHit(Level param0, BlockState param1, BlockHitResult param2, Projectile param3) {
      Entity var5 = param3.getOwner();
      Player var6 = var5 instanceof Player ? (Player)var5 : null;
      this.onHit(param0, param1, param2, var6, true);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      return this.onHit(param1, param0, param5, param3, true) ? InteractionResult.sidedSuccess(param1.isClientSide) : InteractionResult.PASS;
   }

   public boolean onHit(Level param0, BlockState param1, BlockHitResult param2, @Nullable Player param3, boolean param4) {
      Direction var6 = param2.getDirection();
      BlockPos var7 = param2.getBlockPos();
      boolean var8 = !param4 || this.isProperHit(param1, var6, param2.getLocation().y - var7.getY());
      if (var8) {
         boolean var9 = this.attemptToRing(param3, param0, var7, var6);
         if (var9 && param3 != null) {
            param3.awardStat(Stats.BELL_RING);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isProperHit(BlockState param0, Direction param1, double param2) {
      if (param1.getAxis() != Direction.Axis.Y && !(param2 > 0.8124F)) {
         Direction var5 = param0.getValue(FACING);
         BellAttachType var6 = param0.getValue(ATTACHMENT);
         switch (var6) {
            case FLOOR:
               return var5.getAxis() == param1.getAxis();
            case SINGLE_WALL:
            case DOUBLE_WALL:
               return var5.getAxis() != param1.getAxis();
            case CEILING:
               return true;
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public boolean attemptToRing(Level param0, BlockPos param1, @Nullable Direction param2) {
      return this.attemptToRing(null, param0, param1, param2);
   }

   public boolean attemptToRing(@Nullable Entity param0, Level param1, BlockPos param2, @Nullable Direction param3) {
      BlockEntity var5 = param1.getBlockEntity(param2);
      if (!param1.isClientSide && var5 instanceof BellBlockEntity) {
         if (param3 == null) {
            param3 = param1.getBlockState(param2).getValue(FACING);
         }

         ((BellBlockEntity)var5).onHit(param3);
         param1.playSound((Player)null, param2, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
         param1.gameEvent(param0, GameEvent.BLOCK_CHANGE, param2);
         return true;
      } else {
         return false;
      }
   }

   private VoxelShape getVoxelShape(BlockState param0) {
      Direction var2 = param0.getValue(FACING);
      BellAttachType var3 = param0.getValue(ATTACHMENT);
      if (var3 == BellAttachType.FLOOR) {
         return var2 != Direction.NORTH && var2 != Direction.SOUTH ? EAST_WEST_FLOOR_SHAPE : NORTH_SOUTH_FLOOR_SHAPE;
      } else if (var3 == BellAttachType.CEILING) {
         return CEILING_SHAPE;
      } else if (var3 == BellAttachType.DOUBLE_WALL) {
         return var2 != Direction.NORTH && var2 != Direction.SOUTH ? EAST_WEST_BETWEEN : NORTH_SOUTH_BETWEEN;
      } else if (var2 == Direction.NORTH) {
         return TO_NORTH;
      } else if (var2 == Direction.SOUTH) {
         return TO_SOUTH;
      } else {
         return var2 == Direction.EAST ? TO_EAST : TO_WEST;
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.getVoxelShape(param0);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.getVoxelShape(param0);
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      Direction var3 = param0.getClickedFace();
      BlockPos var4 = param0.getClickedPos();
      Level var5 = param0.getLevel();
      Direction.Axis var6 = var3.getAxis();
      if (var6 == Direction.Axis.Y) {
         BlockState var2 = this.defaultBlockState()
            .setValue(ATTACHMENT, var3 == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)
            .setValue(FACING, param0.getHorizontalDirection());
         if (var2.canSurvive(param0.getLevel(), var4)) {
            return var2;
         }
      } else {
         boolean var7 = var6 == Direction.Axis.X
               && var5.getBlockState(var4.west()).isFaceSturdy(var5, var4.west(), Direction.EAST)
               && var5.getBlockState(var4.east()).isFaceSturdy(var5, var4.east(), Direction.WEST)
            || var6 == Direction.Axis.Z
               && var5.getBlockState(var4.north()).isFaceSturdy(var5, var4.north(), Direction.SOUTH)
               && var5.getBlockState(var4.south()).isFaceSturdy(var5, var4.south(), Direction.NORTH);
         BlockState var2 = this.defaultBlockState()
            .setValue(FACING, var3.getOpposite())
            .setValue(ATTACHMENT, var7 ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
         if (var2.canSurvive(param0.getLevel(), param0.getClickedPos())) {
            return var2;
         }

         boolean var8 = var5.getBlockState(var4.below()).isFaceSturdy(var5, var4.below(), Direction.UP);
         var2 = var2.setValue(ATTACHMENT, var8 ? BellAttachType.FLOOR : BellAttachType.CEILING);
         if (var2.canSurvive(param0.getLevel(), param0.getClickedPos())) {
            return var2;
         }
      }

      return null;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      BellAttachType var7 = param0.getValue(ATTACHMENT);
      Direction var8 = getConnectedDirection(param0).getOpposite();
      if (var8 == param1 && !param0.canSurvive(param3, param4) && var7 != BellAttachType.DOUBLE_WALL) {
         return Blocks.AIR.defaultBlockState();
      }

      if (param1.getAxis() == param0.getValue(FACING).getAxis()) {
         if (var7 == BellAttachType.DOUBLE_WALL && !param2.isFaceSturdy(param3, param5, param1)) {
            return param0.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL).setValue(FACING, param1.getOpposite());
         }

         if (var7 == BellAttachType.SINGLE_WALL && var8.getOpposite() == param1 && param2.isFaceSturdy(param3, param5, param0.getValue(FACING))) {
            return param0.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
         }
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      Direction var4 = getConnectedDirection(param0).getOpposite();
      return var4 == Direction.UP
         ? Block.canSupportCenter(param1, param2.above(), Direction.DOWN)
         : FaceAttachedHorizontalDirectionalBlock.canAttach(param1, param2, var4);
   }

   private static Direction getConnectedDirection(BlockState param0) {
      switch ((BellAttachType)param0.getValue(ATTACHMENT)) {
         case FLOOR:
            return Direction.UP;
         case CEILING:
            return Direction.DOWN;
         default:
            return param0.getValue(FACING).getOpposite();
      }
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, ATTACHMENT, POWERED);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new BellBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return createTickerHelper(param2, BlockEntityType.BELL, param0.isClientSide ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
