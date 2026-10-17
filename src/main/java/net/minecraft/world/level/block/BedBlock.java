package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;

public class BedBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
   protected static final int HEIGHT = 9;
   protected static final VoxelShape BASE = Block.box(0.0, 3.0, 0.0, 16.0, 9.0, 16.0);
   private static final int LEG_WIDTH = 3;
   protected static final VoxelShape LEG_NORTH_WEST = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_WEST = Block.box(0.0, 0.0, 13.0, 3.0, 3.0, 16.0);
   protected static final VoxelShape LEG_NORTH_EAST = Block.box(13.0, 0.0, 0.0, 16.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_EAST = Block.box(13.0, 0.0, 13.0, 16.0, 3.0, 16.0);
   protected static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_NORTH_EAST);
   protected static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, LEG_SOUTH_WEST, LEG_SOUTH_EAST);
   protected static final VoxelShape WEST_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_SOUTH_WEST);
   protected static final VoxelShape EAST_SHAPE = Shapes.or(BASE, LEG_NORTH_EAST, LEG_SOUTH_EAST);
   private final DyeColor color;

   public BedBlock(DyeColor param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.color = param0;
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
   }

   @Nullable
   public static Direction getBedOrientation(BlockGetter param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      return var2.getBlock() instanceof BedBlock ? var2.getValue(FACING) : null;
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.isClientSide) {
         return InteractionResult.CONSUME;
      }

      if (param0.getValue(PART) != BedPart.HEAD) {
         param2 = param2.relative(param0.getValue(FACING));
         param0 = param1.getBlockState(param2);
         if (!param0.is(this)) {
            return InteractionResult.CONSUME;
         }
      }

      if (!canSetSpawn(param1)) {
         param1.removeBlock(param2, false);
         BlockPos var7 = param2.relative(param0.getValue(FACING).getOpposite());
         if (param1.getBlockState(var7).is(this)) {
            param1.removeBlock(var7, false);
         }

         Vec3 var8 = param2.getCenter();
         param1.explode(null, param1.damageSources().badRespawnPointExplosion(var8), null, var8, 5.0F, true, Level.ExplosionInteraction.BLOCK);
         return InteractionResult.SUCCESS;
      } else if (param0.getValue(OCCUPIED)) {
         if (!this.kickVillagerOutOfBed(param1, param2)) {
            param3.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
         }

         return InteractionResult.SUCCESS;
      } else {
         param3.startSleepInBed(param2).ifLeft(l_param1 -> {
            if (l_param1.getMessage() != null) {
               param3.displayClientMessage(l_param1.getMessage(), true);
            }
         });
         return InteractionResult.SUCCESS;
      }
   }

   public static boolean canSetSpawn(Level param0) {
      return param0.dimensionType().bedWorks();
   }

   private boolean kickVillagerOutOfBed(Level param0, BlockPos param1) {
      List<Villager> var3 = param0.getEntitiesOfClass(Villager.class, new AABB(param1), LivingEntity::isSleeping);
      if (var3.isEmpty()) {
         return false;
      }

      var3.get(0).stopSleeping();
      return true;
   }

   @Override
   public void fallOn(Level param0, BlockState param1, BlockPos param2, Entity param3, float param4) {
      super.fallOn(param0, param1, param2, param3, param4 * 0.5F);
   }

   @Override
   public void updateEntityAfterFallOn(BlockGetter param0, Entity param1) {
      if (param1.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(param0, param1);
      } else {
         this.bounceUp(param1);
      }
   }

   private void bounceUp(Entity param0) {
      Vec3 var2 = param0.getDeltaMovement();
      if (var2.y < 0.0) {
         double var3 = param0 instanceof LivingEntity ? 1.0 : 0.8;
         param0.setDeltaMovement(var2.x, -var2.y * 0.66F * var3, var2.z);
      }
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param1 == getNeighbourDirection(param0.getValue(PART), param0.getValue(FACING))) {
         return param2.is(this) && param2.getValue(PART) != param0.getValue(PART)
            ? param0.setValue(OCCUPIED, param2.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      } else {
         return super.updateShape(param0, param1, param2, param3, param4, param5);
      }
   }

   private static Direction getNeighbourDirection(BedPart param0, Direction param1) {
      return param0 == BedPart.FOOT ? param1 : param1.getOpposite();
   }

   @Override
   public void playerWillDestroy(Level param0, BlockPos param1, BlockState param2, Player param3) {
      if (!param0.isClientSide && param3.isCreative()) {
         BedPart var5 = param2.getValue(PART);
         if (var5 == BedPart.FOOT) {
            BlockPos var6 = param1.relative(getNeighbourDirection(var5, param2.getValue(FACING)));
            BlockState var7 = param0.getBlockState(var6);
            if (var7.is(this) && var7.getValue(PART) == BedPart.HEAD) {
               param0.setBlock(var6, Blocks.AIR.defaultBlockState(), 35);
               param0.levelEvent(param3, 2001, var6, Block.getId(var7));
            }
         }
      }

      super.playerWillDestroy(param0, param1, param2, param3);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      Direction var2 = param0.getHorizontalDirection();
      BlockPos var3 = param0.getClickedPos();
      BlockPos var4 = var3.relative(var2);
      Level var5 = param0.getLevel();
      return var5.getBlockState(var4).canBeReplaced(param0) && var5.getWorldBorder().isWithinBounds(var4)
         ? this.defaultBlockState().setValue(FACING, var2)
         : null;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      Direction var5 = getConnectedDirection(param0).getOpposite();
      switch (var5) {
         case NORTH:
            return NORTH_SHAPE;
         case SOUTH:
            return SOUTH_SHAPE;
         case WEST:
            return WEST_SHAPE;
         default:
            return EAST_SHAPE;
      }
   }

   public static Direction getConnectedDirection(BlockState param0) {
      Direction var1 = param0.getValue(FACING);
      return param0.getValue(PART) == BedPart.HEAD ? var1.getOpposite() : var1;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState param0) {
      BedPart var1 = param0.getValue(PART);
      return var1 == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(BlockGetter param0, BlockPos param1) {
      return param0.getBlockState(param1.below()).getBlock() instanceof BedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(EntityType<?> param0, CollisionGetter param1, BlockPos param2, Direction param3, float param4) {
      Direction var5 = param3.getClockWise();
      Direction var6 = var5.isFacingAngle(param4) ? var5.getOpposite() : var5;
      if (isBunkBed(param1, param2)) {
         return findBunkBedStandUpPosition(param0, param1, param2, param3, var6);
      }

      int[][] var7 = bedStandUpOffsets(param3, var6);
      Optional<Vec3> var8 = findStandUpPositionAtOffset(param0, param1, param2, var7, true);
      return var8.isPresent() ? var8 : findStandUpPositionAtOffset(param0, param1, param2, var7, false);
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(EntityType<?> param0, CollisionGetter param1, BlockPos param2, Direction param3, Direction param4) {
      int[][] var5 = bedSurroundStandUpOffsets(param3, param4);
      Optional<Vec3> var6 = findStandUpPositionAtOffset(param0, param1, param2, var5, true);
      if (var6.isPresent()) {
         return var6;
      }

      BlockPos var7 = param2.below();
      Optional<Vec3> var8 = findStandUpPositionAtOffset(param0, param1, var7, var5, true);
      if (var8.isPresent()) {
         return var8;
      }

      int[][] var9 = bedAboveStandUpOffsets(param3);
      Optional<Vec3> var10 = findStandUpPositionAtOffset(param0, param1, param2, var9, true);
      if (var10.isPresent()) {
         return var10;
      }

      Optional<Vec3> var11 = findStandUpPositionAtOffset(param0, param1, param2, var5, false);
      if (var11.isPresent()) {
         return var11;
      }

      Optional<Vec3> var12 = findStandUpPositionAtOffset(param0, param1, var7, var5, false);
      return var12.isPresent() ? var12 : findStandUpPositionAtOffset(param0, param1, param2, var9, false);
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(EntityType<?> param0, CollisionGetter param1, BlockPos param2, int[][] param3, boolean param4) {
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int[] var9 : param3) {
         var5.set(param2.getX() + var9[0], param2.getY(), param2.getZ() + var9[1]);
         Vec3 var10 = DismountHelper.findSafeDismountLocation(param0, param1, var5, param4);
         if (var10 != null) {
            return Optional.of(var10);
         }
      }

      return Optional.empty();
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   @Override
   public RenderShape getRenderShape(BlockState param0) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, PART, OCCUPIED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new BedBlockEntity(param0, param1, this.color);
   }

   @Override
   public void setPlacedBy(Level param0, BlockPos param1, BlockState param2, @Nullable LivingEntity param3, ItemStack param4) {
      super.setPlacedBy(param0, param1, param2, param3, param4);
      if (!param0.isClientSide) {
         BlockPos var6 = param1.relative(param2.getValue(FACING));
         param0.setBlock(var6, param2.setValue(PART, BedPart.HEAD), 3);
         param0.blockUpdated(param1, Blocks.AIR);
         param2.updateNeighbourShapes(param0, param1, 3);
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   @Override
   public long getSeed(BlockState param0, BlockPos param1) {
      BlockPos var3 = param1.relative(param0.getValue(FACING), param0.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed(var3.getX(), param1.getY(), var3.getZ());
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   private static int[][] bedStandUpOffsets(Direction param0, Direction param1) {
      return ArrayUtils.addAll(bedSurroundStandUpOffsets(param0, param1), bedAboveStandUpOffsets(param0));
   }

   private static int[][] bedSurroundStandUpOffsets(Direction param0, Direction param1) {
      return new int[][]{
         {param1.getStepX(), param1.getStepZ()},
         {param1.getStepX() - param0.getStepX(), param1.getStepZ() - param0.getStepZ()},
         {param1.getStepX() - param0.getStepX() * 2, param1.getStepZ() - param0.getStepZ() * 2},
         {-param0.getStepX() * 2, -param0.getStepZ() * 2},
         {-param1.getStepX() - param0.getStepX() * 2, -param1.getStepZ() - param0.getStepZ() * 2},
         {-param1.getStepX() - param0.getStepX(), -param1.getStepZ() - param0.getStepZ()},
         {-param1.getStepX(), -param1.getStepZ()},
         {-param1.getStepX() + param0.getStepX(), -param1.getStepZ() + param0.getStepZ()},
         {param0.getStepX(), param0.getStepZ()},
         {param1.getStepX() + param0.getStepX(), param1.getStepZ() + param0.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(Direction param0) {
      return new int[][]{{0, 0}, {-param0.getStepX(), -param0.getStepZ()}};
   }
}
