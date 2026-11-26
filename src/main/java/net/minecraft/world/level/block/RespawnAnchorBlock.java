package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RespawnAnchorBlock extends Block {
   public static final int MIN_CHARGES = 0;
   public static final int MAX_CHARGES = 4;
   public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
   private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(
      new Vec3i(0, 0, -1),
      new Vec3i(-1, 0, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(-1, 0, -1),
      new Vec3i(1, 0, -1),
      new Vec3i(-1, 0, 1),
      new Vec3i(1, 0, 1)
   );
   private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new Builder<Vec3i>()
      .addAll(RESPAWN_HORIZONTAL_OFFSETS)
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
      .add(new Vec3i(0, 1, 0))
      .build();

   public RespawnAnchorBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      ItemStack var7 = param3.getItemInHand(param4);
      if (param4 == InteractionHand.MAIN_HAND && !isRespawnFuel(var7) && isRespawnFuel(param3.getItemInHand(InteractionHand.OFF_HAND))) {
         return InteractionResult.PASS;
      }

      if (isRespawnFuel(var7) && canBeCharged(param0)) {
         charge(param3, param1, param2, param0);
         if (!param3.getAbilities().instabuild) {
            var7.shrink(1);
         }

         return InteractionResult.sidedSuccess(param1.isClientSide);
      } else {
         if (param0.getValue(CHARGE) == 0) {
            return InteractionResult.PASS;
         }

         if (!canSetSpawn(param1)) {
            if (!param1.isClientSide) {
               this.explode(param0, param1, param2);
            }

            return InteractionResult.sidedSuccess(param1.isClientSide);
         } else {
            if (!param1.isClientSide) {
               ServerPlayer var8 = (ServerPlayer)param3;
               if (var8.getRespawnDimension() != param1.dimension() || !param2.equals(var8.getRespawnPosition())) {
                  var8.setRespawnPosition(param1.dimension(), param2, 0.0F, false, true);
                  param1.playSound(
                     null, param2.getX() + 0.5, param2.getY() + 0.5, param2.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F
                  );
                  return InteractionResult.SUCCESS;
               }
            }

            return InteractionResult.CONSUME;
         }
      }
   }

   private static boolean isRespawnFuel(ItemStack param0) {
      return param0.is(Items.GLOWSTONE);
   }

   private static boolean canBeCharged(BlockState param0) {
      return param0.getValue(CHARGE) < 4;
   }

   private static boolean isWaterThatWouldFlow(BlockPos param0, Level param1) {
      FluidState var2 = param1.getFluidState(param0);
      if (!var2.is(FluidTags.WATER)) {
         return false;
      }

      if (var2.isSource()) {
         return true;
      }

      float var3 = var2.getAmount();
      if (var3 < 2.0F) {
         return false;
      }

      FluidState var4 = param1.getFluidState(param0.below());
      return !var4.is(FluidTags.WATER);
   }

   private void explode(BlockState param0, Level param1, final BlockPos param2) {
      param1.removeBlock(param2, false);
      boolean var4 = Direction.Plane.HORIZONTAL.stream().map(param2::relative).anyMatch(l_param1 -> isWaterThatWouldFlow(l_param1, param1));
      final boolean var5 = var4 || param1.getFluidState(param2.above()).is(FluidTags.WATER);
      ExplosionDamageCalculator var6 = new ExplosionDamageCalculator() {
         @Override
         public Optional<Float> getBlockExplosionResistance(
            Explosion l_param0, BlockGetter l_param1, BlockPos l_param2, BlockState l_param3, FluidState l_param4
         ) {
            return l_param2.equals(param2) && var5
               ? Optional.of(Blocks.WATER.getExplosionResistance())
               : super.getBlockExplosionResistance(l_param0, l_param1, l_param2, l_param3, l_param4);
         }
      };
      Vec3 var7 = param2.getCenter();
      param1.explode(null, param1.damageSources().badRespawnPointExplosion(var7), var6, var7, 5.0F, true, Level.ExplosionInteraction.BLOCK);
   }

   public static boolean canSetSpawn(Level param0) {
      return param0.dimensionType().respawnAnchorWorks();
   }

   public static void charge(@Nullable Entity param0, Level param1, BlockPos param2, BlockState param3) {
      BlockState var4 = param3.setValue(CHARGE, param3.getValue(CHARGE) + 1);
      param1.setBlock(param2, var4, 3);
      param1.gameEvent(GameEvent.BLOCK_CHANGE, param2, GameEvent.Context.of(param0, var4));
      param1.playSound(null, param2.getX() + 0.5, param2.getY() + 0.5, param2.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(CHARGE) != 0) {
         if (param3.nextInt(100) == 0) {
            param1.playSound(
               null, param2.getX() + 0.5, param2.getY() + 0.5, param2.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F
            );
         }

         double var5 = param2.getX() + 0.5 + (0.5 - param3.nextDouble());
         double var7 = param2.getY() + 1.0;
         double var9 = param2.getZ() + 0.5 + (0.5 - param3.nextDouble());
         double var11 = param3.nextFloat() * 0.04;
         param1.addParticle(ParticleTypes.REVERSE_PORTAL, var5, var7, var9, 0.0, var11, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(CHARGE);
   }

   @Override
   public boolean hasAnalogOutputSignal(BlockState param0) {
      return true;
   }

   public static int getScaledChargeLevel(BlockState param0, int param1) {
      return Mth.floor((param0.getValue(CHARGE) - 0) / 4.0F * param1);
   }

   @Override
   public int getAnalogOutputSignal(BlockState param0, Level param1, BlockPos param2) {
      return getScaledChargeLevel(param0, 15);
   }

   public static Optional<Vec3> findStandUpPosition(EntityType<?> param0, CollisionGetter param1, BlockPos param2) {
      Optional<Vec3> var3 = findStandUpPosition(param0, param1, param2, true);
      return var3.isPresent() ? var3 : findStandUpPosition(param0, param1, param2, false);
   }

   private static Optional<Vec3> findStandUpPosition(EntityType<?> param0, CollisionGetter param1, BlockPos param2, boolean param3) {
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();

      for (Vec3i var6 : RESPAWN_OFFSETS) {
         var4.set(param2).move(var6);
         Vec3 var7 = DismountHelper.findSafeDismountLocation(param0, param1, var4, param3);
         if (var7 != null) {
            return Optional.of(var7);
         }
      }

      return Optional.empty();
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
