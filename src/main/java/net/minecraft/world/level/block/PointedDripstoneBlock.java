package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PointedDripstoneBlock extends Block implements Fallable, SimpleWaterloggedBlock {
   public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
   public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
   private static final int DELAY_BEFORE_FALLING = 2;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
   private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
   private static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
   private static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
   private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_DRIPSTONE = 0.6;
   private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
   private static final int STALACTITE_MAX_DAMAGE = 40;
   private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
   private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;
   private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
   private static final float AVERAGE_DAYS_PER_GROWTH = 5.0F;
   private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778F;
   private static final int MAX_GROWTH_LENGTH = 7;
   private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
   private static final float STALACTITE_DRIP_START_PIXEL = 0.6875F;
   private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
   private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
   private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
   private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
   private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
   private static final float MAX_HORIZONTAL_OFFSET = 0.125F;
   private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

   public PointedDripstoneBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(TIP_DIRECTION, Direction.UP).setValue(THICKNESS, DripstoneThickness.TIP).setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return isValidPointedDripstonePlacement(param1, param2, param0.getValue(TIP_DIRECTION));
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      if (param1 != Direction.UP && param1 != Direction.DOWN) {
         return param0;
      }

      Direction var7 = param0.getValue(TIP_DIRECTION);
      if (var7 == Direction.DOWN && param3.getBlockTicks().hasScheduledTick(param4, this)) {
         return param0;
      }

      if (param1 == var7.getOpposite() && !this.canSurvive(param0, param3, param4)) {
         if (var7 == Direction.DOWN) {
            param3.scheduleTick(param4, this, 2);
         } else {
            param3.scheduleTick(param4, this, 1);
         }

         return param0;
      } else {
         boolean var8 = param0.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
         DripstoneThickness var9 = calculateDripstoneThickness(param3, param4, var7, var8);
         return param0.setValue(THICKNESS, var9);
      }
   }

   @Override
   public void onProjectileHit(Level param0, BlockState param1, BlockHitResult param2, Projectile param3) {
      BlockPos var5 = param2.getBlockPos();
      if (!param0.isClientSide && param3.mayInteract(param0, var5) && param3 instanceof ThrownTrident && param3.getDeltaMovement().length() > 0.6) {
         param0.destroyBlock(var5, true);
      }
   }

   @Override
   public void fallOn(Level param0, BlockState param1, BlockPos param2, Entity param3, float param4) {
      if (param1.getValue(TIP_DIRECTION) == Direction.UP && param1.getValue(THICKNESS) == DripstoneThickness.TIP) {
         param3.causeFallDamage(param4 + 2.0F, 2.0F, param0.damageSources().stalagmite());
      } else {
         super.fallOn(param0, param1, param2, param3, param4);
      }
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (canDrip(param0)) {
         float var5 = param3.nextFloat();
         if (!(var5 > 0.12F)) {
            getFluidAboveStalactite(param1, param2, param0)
               .filter(l_param1 -> var5 < 0.02F || canFillCauldron(l_param1.fluid))
               .ifPresent(l_param3 -> spawnDripParticle(param1, param2, param0, l_param3.fluid));
         }
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (isStalagmite(param0) && !this.canSurvive(param0, param1, param2)) {
         param1.destroyBlock(param2, true);
      } else {
         spawnFallingStalactite(param0, param1, param2);
      }
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      maybeTransferFluid(param0, param1, param2, param3.nextFloat());
      if (param3.nextFloat() < 0.011377778F && isStalactiteStartPos(param0, param1, param2)) {
         growStalactiteOrStalagmiteIfPossible(param0, param1, param2, param3);
      }
   }

   @VisibleForTesting
   public static void maybeTransferFluid(BlockState param0, ServerLevel param1, BlockPos param2, float param3) {
      if (!(param3 > 0.17578125F) || !(param3 > 0.05859375F)) {
         if (isStalactiteStartPos(param0, param1, param2)) {
            Optional<PointedDripstoneBlock.FluidInfo> var4 = getFluidAboveStalactite(param1, param2, param0);
            if (!var4.isEmpty()) {
               Fluid var5 = var4.get().fluid;
               float var6;
               if (var5 == Fluids.WATER) {
                  var6 = 0.17578125F;
               } else {
                  if (var5 != Fluids.LAVA) {
                     return;
                  }

                  var6 = 0.05859375F;
               }

               if (!(param3 >= var6)) {
                  BlockPos var7 = findTip(param0, param1, param2, 11, false);
                  if (var7 != null) {
                     if (var4.get().sourceState.is(Blocks.MUD) && var5 == Fluids.WATER) {
                        BlockState var8 = Blocks.CLAY.defaultBlockState();
                        param1.setBlockAndUpdate(var4.get().pos, var8);
                        Block.pushEntitiesUp(var4.get().sourceState, var8, param1, var4.get().pos);
                        param1.gameEvent(GameEvent.BLOCK_CHANGE, var4.get().pos, GameEvent.Context.of(var8));
                        param1.levelEvent(1504, var7, 0);
                     } else {
                        BlockPos var8 = findFillableCauldronBelowStalactiteTip(param1, var7, var5);
                        if (var8 != null) {
                           param1.levelEvent(1504, var7, 0);
                           int var9 = var7.getY() - var8.getY();
                           int var10 = 50 + var9;
                           BlockState var11 = param1.getBlockState(var8);
                           param1.scheduleTick(var8, var11.getBlock(), var10);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      LevelAccessor var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      Direction var4 = param0.getNearestLookingVerticalDirection().getOpposite();
      Direction var5 = calculateTipDirection(var2, var3, var4);
      if (var5 == null) {
         return null;
      }

      boolean var6 = !param0.isSecondaryUseActive();
      DripstoneThickness var7 = calculateDripstoneThickness(var2, var3, var5, var6);
      return var7 == null
         ? null
         : this.defaultBlockState()
            .setValue(TIP_DIRECTION, var5)
            .setValue(THICKNESS, var7)
            .setValue(WATERLOGGED, var2.getFluidState(var3).getType() == Fluids.WATER);
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return Shapes.empty();
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      DripstoneThickness var6 = param0.getValue(THICKNESS);
      VoxelShape var5;
      if (var6 == DripstoneThickness.TIP_MERGE) {
         var5 = TIP_MERGE_SHAPE;
      } else if (var6 == DripstoneThickness.TIP) {
         if (param0.getValue(TIP_DIRECTION) == Direction.DOWN) {
            var5 = TIP_SHAPE_DOWN;
         } else {
            var5 = TIP_SHAPE_UP;
         }
      } else if (var6 == DripstoneThickness.FRUSTUM) {
         var5 = FRUSTUM_SHAPE;
      } else if (var6 == DripstoneThickness.MIDDLE) {
         var5 = MIDDLE_SHAPE;
      } else {
         var5 = BASE_SHAPE;
      }

      Vec3 var7 = param0.getOffset(param1, param2);
      return var5.move(var7.x, 0.0, var7.z);
   }

   @Override
   public boolean isCollisionShapeFullBlock(BlockState param0, BlockGetter param1, BlockPos param2) {
      return false;
   }

   @Override
   public float getMaxHorizontalOffset() {
      return 0.125F;
   }

   @Override
   public void onBrokenAfterFall(Level param0, BlockPos param1, FallingBlockEntity param2) {
      if (!param2.isSilent()) {
         param0.levelEvent(1045, param1, 0);
      }
   }

   @Override
   public DamageSource getFallDamageSource(Entity param0) {
      return param0.damageSources().fallingStalactite(param0);
   }

   @Override
   public Predicate<Entity> getHurtsEntitySelector() {
      return EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
   }

   private static void spawnFallingStalactite(BlockState param0, ServerLevel param1, BlockPos param2) {
      BlockPos.MutableBlockPos var3 = param2.mutable();
      BlockState var4 = param0;

      while (isStalactite(var4)) {
         FallingBlockEntity var5 = FallingBlockEntity.fall(param1, var3, var4);
         if (isTip(var4, true)) {
            int var6 = Math.max(1 + param2.getY() - var3.getY(), 6);
            float var7 = 1.0F * var6;
            var5.setHurtsEntities(var7, 40);
            break;
         }

         var3.move(Direction.DOWN);
         var4 = param1.getBlockState(var3);
      }
   }

   @VisibleForTesting
   public static void growStalactiteOrStalagmiteIfPossible(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      BlockState var4 = param1.getBlockState(param2.above(1));
      BlockState var5 = param1.getBlockState(param2.above(2));
      if (canGrow(var4, var5)) {
         BlockPos var6 = findTip(param0, param1, param2, 7, false);
         if (var6 != null) {
            BlockState var7 = param1.getBlockState(var6);
            if (canDrip(var7) && canTipGrow(var7, param1, var6)) {
               if (param3.nextBoolean()) {
                  grow(param1, var6, Direction.DOWN);
               } else {
                  growStalagmiteBelow(param1, var6);
               }
            }
         }
      }
   }

   private static void growStalagmiteBelow(ServerLevel param0, BlockPos param1) {
      BlockPos.MutableBlockPos var2 = param1.mutable();

      for (int var3 = 0; var3 < 10; var3++) {
         var2.move(Direction.DOWN);
         BlockState var4 = param0.getBlockState(var2);
         if (!var4.getFluidState().isEmpty()) {
            return;
         }

         if (isUnmergedTipWithDirection(var4, Direction.UP) && canTipGrow(var4, param0, var2)) {
            grow(param0, var2, Direction.UP);
            return;
         }

         if (isValidPointedDripstonePlacement(param0, var2, Direction.UP) && !param0.isWaterAt(var2.below())) {
            grow(param0, var2.below(), Direction.UP);
            return;
         }

         if (!canDripThrough(param0, var2, var4)) {
            return;
         }
      }
   }

   private static void grow(ServerLevel param0, BlockPos param1, Direction param2) {
      BlockPos var3 = param1.relative(param2);
      BlockState var4 = param0.getBlockState(var3);
      if (isUnmergedTipWithDirection(var4, param2.getOpposite())) {
         createMergedTips(var4, param0, var3);
      } else if (var4.isAir() || var4.is(Blocks.WATER)) {
         createDripstone(param0, var3, param2, DripstoneThickness.TIP);
      }
   }

   private static void createDripstone(LevelAccessor param0, BlockPos param1, Direction param2, DripstoneThickness param3) {
      BlockState var4 = Blocks.POINTED_DRIPSTONE
         .defaultBlockState()
         .setValue(TIP_DIRECTION, param2)
         .setValue(THICKNESS, param3)
         .setValue(WATERLOGGED, param0.getFluidState(param1).getType() == Fluids.WATER);
      param0.setBlock(param1, var4, 3);
   }

   private static void createMergedTips(BlockState param0, LevelAccessor param1, BlockPos param2) {
      BlockPos var3;
      BlockPos var4;
      if (param0.getValue(TIP_DIRECTION) == Direction.UP) {
         var4 = param2;
         var3 = param2.above();
      } else {
         var3 = param2;
         var4 = param2.below();
      }

      createDripstone(param1, var3, Direction.DOWN, DripstoneThickness.TIP_MERGE);
      createDripstone(param1, var4, Direction.UP, DripstoneThickness.TIP_MERGE);
   }

   public static void spawnDripParticle(Level param0, BlockPos param1, BlockState param2) {
      getFluidAboveStalactite(param0, param1, param2).ifPresent(l_param3 -> spawnDripParticle(param0, param1, param2, l_param3.fluid));
   }

   private static void spawnDripParticle(Level param0, BlockPos param1, BlockState param2, Fluid param3) {
      Vec3 var4 = param2.getOffset(param0, param1);
      double var5 = 0.0625;
      double var7 = param1.getX() + 0.5 + var4.x;
      double var9 = param1.getY() + 1 - 0.6875F - 0.0625;
      double var11 = param1.getZ() + 0.5 + var4.z;
      Fluid var13 = getDripFluid(param0, param3);
      ParticleOptions var14 = var13.is(FluidTags.LAVA) ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
      param0.addParticle(var14, var7, var9, var11, 0.0, 0.0, 0.0);
   }

   @Nullable
   private static BlockPos findTip(BlockState param0, LevelAccessor param1, BlockPos param2, int param3, boolean param4) {
      if (isTip(param0, param4)) {
         return param2;
      }

      Direction var5 = param0.getValue(TIP_DIRECTION);
      BiPredicate<BlockPos, BlockState> var6 = (l_param1, l_param2) -> l_param2.is(Blocks.POINTED_DRIPSTONE) && l_param2.getValue(TIP_DIRECTION) == var5;
      return findBlockVertical(param1, param2, var5.getAxisDirection(), var6, l_param1 -> isTip(l_param1, param4), param3).orElse(null);
   }

   @Nullable
   private static Direction calculateTipDirection(LevelReader param0, BlockPos param1, Direction param2) {
      Direction var3;
      if (isValidPointedDripstonePlacement(param0, param1, param2)) {
         var3 = param2;
      } else {
         if (!isValidPointedDripstonePlacement(param0, param1, param2.getOpposite())) {
            return null;
         }

         var3 = param2.getOpposite();
      }

      return var3;
   }

   private static DripstoneThickness calculateDripstoneThickness(LevelReader param0, BlockPos param1, Direction param2, boolean param3) {
      Direction var4 = param2.getOpposite();
      BlockState var5 = param0.getBlockState(param1.relative(param2));
      if (isPointedDripstoneWithDirection(var5, var4)) {
         return !param3 && var5.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
      } else if (!isPointedDripstoneWithDirection(var5, param2)) {
         return DripstoneThickness.TIP;
      } else {
         DripstoneThickness var6 = var5.getValue(THICKNESS);
         if (var6 != DripstoneThickness.TIP && var6 != DripstoneThickness.TIP_MERGE) {
            BlockState var7 = param0.getBlockState(param1.relative(var4));
            return !isPointedDripstoneWithDirection(var7, param2) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
         } else {
            return DripstoneThickness.FRUSTUM;
         }
      }
   }

   public static boolean canDrip(BlockState param0) {
      return isStalactite(param0) && param0.getValue(THICKNESS) == DripstoneThickness.TIP && !param0.getValue(WATERLOGGED);
   }

   private static boolean canTipGrow(BlockState param0, ServerLevel param1, BlockPos param2) {
      Direction var3 = param0.getValue(TIP_DIRECTION);
      BlockPos var4 = param2.relative(var3);
      BlockState var5 = param1.getBlockState(var4);
      if (!var5.getFluidState().isEmpty()) {
         return false;
      } else {
         return var5.isAir() ? true : isUnmergedTipWithDirection(var5, var3.getOpposite());
      }
   }

   private static Optional<BlockPos> findRootBlock(Level param0, BlockPos param1, BlockState param2, int param3) {
      Direction var4 = param2.getValue(TIP_DIRECTION);
      BiPredicate<BlockPos, BlockState> var5 = (l_param1, l_param2) -> l_param2.is(Blocks.POINTED_DRIPSTONE) && l_param2.getValue(TIP_DIRECTION) == var4;
      return findBlockVertical(param0, param1, var4.getOpposite().getAxisDirection(), var5, l_param0 -> !l_param0.is(Blocks.POINTED_DRIPSTONE), param3);
   }

   private static boolean isValidPointedDripstonePlacement(LevelReader param0, BlockPos param1, Direction param2) {
      BlockPos var3 = param1.relative(param2.getOpposite());
      BlockState var4 = param0.getBlockState(var3);
      return var4.isFaceSturdy(param0, var3, param2) || isPointedDripstoneWithDirection(var4, param2);
   }

   private static boolean isTip(BlockState param0, boolean param1) {
      if (!param0.is(Blocks.POINTED_DRIPSTONE)) {
         return false;
      }

      DripstoneThickness var2 = param0.getValue(THICKNESS);
      return var2 == DripstoneThickness.TIP || param1 && var2 == DripstoneThickness.TIP_MERGE;
   }

   private static boolean isUnmergedTipWithDirection(BlockState param0, Direction param1) {
      return isTip(param0, false) && param0.getValue(TIP_DIRECTION) == param1;
   }

   private static boolean isStalactite(BlockState param0) {
      return isPointedDripstoneWithDirection(param0, Direction.DOWN);
   }

   private static boolean isStalagmite(BlockState param0) {
      return isPointedDripstoneWithDirection(param0, Direction.UP);
   }

   private static boolean isStalactiteStartPos(BlockState param0, LevelReader param1, BlockPos param2) {
      return isStalactite(param0) && !param1.getBlockState(param2.above()).is(Blocks.POINTED_DRIPSTONE);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   private static boolean isPointedDripstoneWithDirection(BlockState param0, Direction param1) {
      return param0.is(Blocks.POINTED_DRIPSTONE) && param0.getValue(TIP_DIRECTION) == param1;
   }

   @Nullable
   private static BlockPos findFillableCauldronBelowStalactiteTip(Level param0, BlockPos param1, Fluid param2) {
      Predicate<BlockState> var3 = l_param1 -> l_param1.getBlock() instanceof AbstractCauldronBlock
         && ((AbstractCauldronBlock)l_param1.getBlock()).canReceiveStalactiteDrip(param2);
      BiPredicate<BlockPos, BlockState> var4 = (l_param1, l_param2) -> canDripThrough(param0, l_param1, l_param2);
      return findBlockVertical(param0, param1, Direction.DOWN.getAxisDirection(), var4, var3, 11).orElse(null);
   }

   @Nullable
   public static BlockPos findStalactiteTipAboveCauldron(Level param0, BlockPos param1) {
      BiPredicate<BlockPos, BlockState> var2 = (l_param1, l_param2) -> canDripThrough(param0, l_param1, l_param2);
      return findBlockVertical(param0, param1, Direction.UP.getAxisDirection(), var2, PointedDripstoneBlock::canDrip, 11).orElse(null);
   }

   public static Fluid getCauldronFillFluidType(ServerLevel param0, BlockPos param1) {
      return getFluidAboveStalactite(param0, param1, param0.getBlockState(param1))
         .map(l_param0 -> l_param0.fluid)
         .filter(PointedDripstoneBlock::canFillCauldron)
         .orElse(Fluids.EMPTY);
   }

   private static Optional<PointedDripstoneBlock.FluidInfo> getFluidAboveStalactite(Level param0, BlockPos param1, BlockState param2) {
      return !isStalactite(param2) ? Optional.empty() : findRootBlock(param0, param1, param2, 11).map(l_param1 -> {
         BlockPos l_var2 = l_param1.above();
         BlockState l_var3 = param0.getBlockState(l_var2);
         Fluid l_var4;
         if (l_var3.is(Blocks.MUD) && !param0.dimensionType().ultraWarm()) {
            l_var4 = Fluids.WATER;
         } else {
            l_var4 = param0.getFluidState(l_var2).getType();
         }

         return new PointedDripstoneBlock.FluidInfo(l_var2, l_var4, l_var3);
      });
   }

   private static boolean canFillCauldron(Fluid param0) {
      return param0 == Fluids.LAVA || param0 == Fluids.WATER;
   }

   private static boolean canGrow(BlockState param0, BlockState param1) {
      return param0.is(Blocks.DRIPSTONE_BLOCK) && param1.is(Blocks.WATER) && param1.getFluidState().isSource();
   }

   private static Fluid getDripFluid(Level param0, Fluid param1) {
      if (param1.isSame(Fluids.EMPTY)) {
         return param0.dimensionType().ultraWarm() ? Fluids.LAVA : Fluids.WATER;
      } else {
         return param1;
      }
   }

   private static Optional<BlockPos> findBlockVertical(
      LevelAccessor param0, BlockPos param1, Direction.AxisDirection param2, BiPredicate<BlockPos, BlockState> param3, Predicate<BlockState> param4, int param5
   ) {
      Direction var6 = Direction.get(param2, Direction.Axis.Y);
      BlockPos.MutableBlockPos var7 = param1.mutable();

      for (int var8 = 1; var8 < param5; var8++) {
         var7.move(var6);
         BlockState var9 = param0.getBlockState(var7);
         if (param4.test(var9)) {
            return Optional.of(var7.immutable());
         }

         if (param0.isOutsideBuildHeight(var7.getY()) || !param3.test(var7, var9)) {
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static boolean canDripThrough(BlockGetter param0, BlockPos param1, BlockState param2) {
      if (param2.isAir()) {
         return true;
      }

      if (param2.isSolidRender(param0, param1)) {
         return false;
      }

      if (!param2.getFluidState().isEmpty()) {
         return false;
      }

      VoxelShape var3 = param2.getCollisionShape(param0, param1);
      return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, var3, BooleanOp.AND);
   }

   record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
   }
}
