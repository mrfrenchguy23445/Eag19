package net.minecraft.world.level.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HoneyBlock extends HalfTransparentBlock {
   private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
   private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08;
   private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
   private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

   public HoneyBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   private static boolean doesEntityDoHoneyBlockSlideEffects(Entity param0) {
      return param0 instanceof LivingEntity || param0 instanceof AbstractMinecart || param0 instanceof PrimedTnt || param0 instanceof Boat;
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public void fallOn(Level param0, BlockState param1, BlockPos param2, Entity param3, float param4) {
      param3.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
      if (!param0.isClientSide) {
         param0.broadcastEntityEvent(param3, (byte)54);
      }

      if (param3.causeFallDamage(param4, 0.2F, param0.damageSources().fall())) {
         param3.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
      }
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (this.isSlidingDown(param2, param3)) {
         this.maybeDoSlideAchievement(param3, param2);
         this.doSlideMovement(param3);
         this.maybeDoSlideEffects(param1, param3);
      }

      super.entityInside(param0, param1, param2, param3);
   }

   private boolean isSlidingDown(BlockPos param0, Entity param1) {
      if (param1.isOnGround()) {
         return false;
      }

      if (param1.getY() > param0.getY() + 0.9375 - 1.0E-7) {
         return false;
      }

      if (param1.getDeltaMovement().y >= -0.08) {
         return false;
      }

      double var3 = Math.abs(param0.getX() + 0.5 - param1.getX());
      double var5 = Math.abs(param0.getZ() + 0.5 - param1.getZ());
      double var7 = 0.4375 + param1.getBbWidth() / 2.0F;
      return var3 + 1.0E-7 > var7 || var5 + 1.0E-7 > var7;
   }

   private void maybeDoSlideAchievement(Entity param0, BlockPos param1) {
      if (param0 instanceof ServerPlayer && param0.level.getGameTime() % 20L == 0L) {
         CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger((ServerPlayer)param0, param0.level.getBlockState(param1));
      }
   }

   private void doSlideMovement(Entity param0) {
      Vec3 var2 = param0.getDeltaMovement();
      if (var2.y < -0.13) {
         double var3 = -0.05 / var2.y;
         param0.setDeltaMovement(new Vec3(var2.x * var3, -0.05, var2.z * var3));
      } else {
         param0.setDeltaMovement(new Vec3(var2.x, -0.05, var2.z));
      }

      param0.resetFallDistance();
   }

   private void maybeDoSlideEffects(Level param0, Entity param1) {
      if (doesEntityDoHoneyBlockSlideEffects(param1)) {
         if (param0.random.nextInt(5) == 0) {
            param1.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
         }

         if (!param0.isClientSide && param0.random.nextInt(5) == 0) {
            param0.broadcastEntityEvent(param1, (byte)53);
         }
      }
   }

   public static void showSlideParticles(Entity param0) {
      showParticles(param0, 5);
   }

   public static void showJumpParticles(Entity param0) {
      showParticles(param0, 10);
   }

   private static void showParticles(Entity param0, int param1) {
      if (param0.level.isClientSide) {
         BlockState var2 = Blocks.HONEY_BLOCK.defaultBlockState();

         for (int var3 = 0; var3 < param1; var3++) {
            param0.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, var2), param0.getX(), param0.getY(), param0.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }
}
