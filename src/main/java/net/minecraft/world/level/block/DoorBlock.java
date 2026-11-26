package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DoorBlock extends Block {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   protected static final float AABB_DOOR_THICKNESS = 3.0F;
   protected static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
   protected static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape WEST_AABB = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
   private final BlockSetType type;

   protected DoorBlock(BlockBehaviour.Properties param0, BlockSetType param1) {
      super(param0.sound(param1.soundType()));
      this.type = param1;
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(HINGE, DoorHingeSide.LEFT)
            .setValue(POWERED, false)
            .setValue(HALF, DoubleBlockHalf.LOWER)
      );
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      Direction var5 = param0.getValue(FACING);
      boolean var6 = !param0.getValue(OPEN);
      boolean var7 = param0.getValue(HINGE) == DoorHingeSide.RIGHT;
      switch (var5) {
         case EAST:
         default:
            return var6 ? EAST_AABB : (var7 ? NORTH_AABB : SOUTH_AABB);
         case SOUTH:
            return var6 ? SOUTH_AABB : (var7 ? EAST_AABB : WEST_AABB);
         case WEST:
            return var6 ? WEST_AABB : (var7 ? SOUTH_AABB : NORTH_AABB);
         case NORTH:
            return var6 ? NORTH_AABB : (var7 ? WEST_AABB : EAST_AABB);
      }
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      DoubleBlockHalf var7 = param0.getValue(HALF);
      if (param1.getAxis() != Direction.Axis.Y || var7 == DoubleBlockHalf.LOWER != (param1 == Direction.UP)) {
         return var7 == DoubleBlockHalf.LOWER && param1 == Direction.DOWN && !param0.canSurvive(param3, param4)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(param0, param1, param2, param3, param4, param5);
      } else {
         return param2.is(this) && param2.getValue(HALF) != var7
            ? param0.setValue(FACING, param2.getValue(FACING))
               .setValue(OPEN, param2.getValue(OPEN))
               .setValue(HINGE, param2.getValue(HINGE))
               .setValue(POWERED, param2.getValue(POWERED))
            : Blocks.AIR.defaultBlockState();
      }
   }

   @Override
   public void playerWillDestroy(Level param0, BlockPos param1, BlockState param2, Player param3) {
      if (!param0.isClientSide && param3.isCreative()) {
         DoublePlantBlock.preventCreativeDropFromBottomPart(param0, param1, param2, param3);
      }

      super.playerWillDestroy(param0, param1, param2, param3);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      switch (param3) {
         case LAND:
            return param0.getValue(OPEN);
         case WATER:
            return false;
         case AIR:
            return param0.getValue(OPEN);
         default:
            return false;
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockPos var2 = param0.getClickedPos();
      Level var3 = param0.getLevel();
      if (var2.getY() < var3.getMaxBuildHeight() - 1 && var3.getBlockState(var2.above()).canBeReplaced(param0)) {
         boolean var4 = var3.hasNeighborSignal(var2) || var3.hasNeighborSignal(var2.above());
         return this.defaultBlockState()
            .setValue(FACING, param0.getHorizontalDirection())
            .setValue(HINGE, this.getHinge(param0))
            .setValue(POWERED, var4)
            .setValue(OPEN, var4)
            .setValue(HALF, DoubleBlockHalf.LOWER);
      } else {
         return null;
      }
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, LivingEntity param3, ItemStack param4) {
      param0.setBlock(param1.above(), param2.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   private DoorHingeSide getHinge(BlockPlaceContext param0) {
      BlockGetter var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      Direction var4 = param0.getHorizontalDirection();
      BlockPos var5 = var3.above();
      Direction var6 = var4.getCounterClockWise();
      BlockPos var7 = var3.relative(var6);
      BlockState var8 = var2.getBlockState(var7);
      BlockPos var9 = var5.relative(var6);
      BlockState var10 = var2.getBlockState(var9);
      Direction var11 = var4.getClockWise();
      BlockPos var12 = var3.relative(var11);
      BlockState var13 = var2.getBlockState(var12);
      BlockPos var14 = var5.relative(var11);
      BlockState var15 = var2.getBlockState(var14);
      int var16 = (var8.isCollisionShapeFullBlock(var2, var7) ? -1 : 0)
         + (var10.isCollisionShapeFullBlock(var2, var9) ? -1 : 0)
         + (var13.isCollisionShapeFullBlock(var2, var12) ? 1 : 0)
         + (var15.isCollisionShapeFullBlock(var2, var14) ? 1 : 0);
      boolean var17 = var8.is(this) && var8.getValue(HALF) == DoubleBlockHalf.LOWER;
      boolean var18 = var13.is(this) && var13.getValue(HALF) == DoubleBlockHalf.LOWER;
      if ((!var17 || var18) && var16 <= 0) {
         if ((!var18 || var17) && var16 >= 0) {
            int var19 = var4.getStepX();
            int var20 = var4.getStepZ();
            Vec3 var21 = param0.getClickLocation();
            double var22 = var21.x - var3.getX();
            double var24 = var21.z - var3.getZ();
            return (var19 >= 0 || !(var24 < 0.5)) && (var19 <= 0 || !(var24 > 0.5)) && (var20 >= 0 || !(var22 > 0.5)) && (var20 <= 0 || !(var22 < 0.5))
               ? DoorHingeSide.LEFT
               : DoorHingeSide.RIGHT;
         } else {
            return DoorHingeSide.LEFT;
         }
      } else {
         return DoorHingeSide.RIGHT;
      }
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (this.material == Material.METAL) {
         return InteractionResult.PASS;
      }

      param0 = param0.cycle(OPEN);
      param1.setBlock(param2, param0, 10);
      this.playSound(param3, param1, param2, param0.getValue(OPEN));
      param1.gameEvent(param3, this.isOpen(param0) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, param2);
      return InteractionResult.sidedSuccess(param1.isClientSide);
   }

   public boolean isOpen(BlockState param0) {
      return param0.getValue(OPEN);
   }

   public void setOpen(@Nullable Entity param0, Level param1, BlockState param2, BlockPos param3, boolean param4) {
      if (param2.is(this) && param2.getValue(OPEN) != param4) {
         param1.setBlock(param3, param2.setValue(OPEN, param4), 10);
         this.playSound(param0, param1, param3, param4);
         param1.gameEvent(param0, param4 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, param3);
      }
   }

   @Override
   public void neighborChanged(BlockState param0, Level param1, BlockPos param2, Block param3, BlockPos param4, boolean param5) {
      boolean var7 = param1.hasNeighborSignal(param2)
         || param1.hasNeighborSignal(param2.relative(param0.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
      if (!this.defaultBlockState().is(param3) && var7 != param0.getValue(POWERED)) {
         if (var7 != param0.getValue(OPEN)) {
            this.playSound(null, param1, param2, var7);
            param1.gameEvent((Entity)null, var7 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, param2);
         }

         param1.setBlock(param2, param0.setValue(POWERED, var7).setValue(OPEN, var7), 2);
      }
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var4 = param2.below();
      BlockState var5 = param1.getBlockState(var4);
      return param0.getValue(HALF) == DoubleBlockHalf.LOWER ? var5.isFaceSturdy(param1, var4, Direction.UP) : var5.is(this);
   }

   private void playSound(@Nullable Entity param0, Level param1, BlockPos param2, boolean param3) {
      param1.playSound(
         param0, param2, param3 ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, param1.getRandom().nextFloat() * 0.1F + 0.9F
      );
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      return param0.setValue(FACING, param1.rotate(param0.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      return param1 == Mirror.NONE ? param0 : param0.rotate(param1.getRotation(param0.getValue(FACING))).cycle(HINGE);
   }

   @Override
   public long getSeed(BlockState param0, BlockPos param1) {
      return Mth.getSeed(param1.getX(), param1.below(param0.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), param1.getZ());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(HALF, FACING, OPEN, HINGE, POWERED);
   }

   public static boolean isWoodenDoor(Level param0, BlockPos param1) {
      return isWoodenDoor(param0.getBlockState(param1));
   }

   public static boolean isWoodenDoor(BlockState param0) {
      return param0.getBlock() instanceof DoorBlock && (param0.getMaterial() == Material.WOOD || param0.getMaterial() == Material.NETHER_WOOD);
   }
}
