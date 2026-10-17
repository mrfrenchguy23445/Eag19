package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
   private static final int SECONDS_ON_FIRE = 8;
   private final float fireDamage;
   protected static final float AABB_OFFSET = 1.0F;
   protected static final VoxelShape DOWN_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

   public BaseFireBlock(BlockBehaviour.Properties param0, float param1) {
      super(param0);
      this.fireDamage = param1;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      return getState(param0.getLevel(), param0.getClickedPos());
   }

   public static BlockState getState(BlockGetter param0, BlockPos param1) {
      BlockPos var2 = param1.below();
      BlockState var3 = param0.getBlockState(var2);
      return SoulFireBlock.canSurviveOnBlock(var3) ? Blocks.SOUL_FIRE.defaultBlockState() : ((FireBlock)Blocks.FIRE).getStateForPlacement(param0, param1);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return DOWN_AABB;
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param3.nextInt(24) == 0) {
         param1.playLocalSound(
            param2.getX() + 0.5,
            param2.getY() + 0.5,
            param2.getZ() + 0.5,
            SoundEvents.FIRE_AMBIENT,
            SoundSource.BLOCKS,
            1.0F + param3.nextFloat(),
            param3.nextFloat() * 0.7F + 0.3F,
            false
         );
      }

      BlockPos var5 = param2.below();
      BlockState var6 = param1.getBlockState(var5);
      if (!this.canBurn(var6) && !var6.isFaceSturdy(param1, var5, Direction.UP)) {
         if (this.canBurn(param1.getBlockState(param2.west()))) {
            for (int var7 = 0; var7 < 2; var7++) {
               double var8 = param2.getX() + param3.nextDouble() * 0.1F;
               double var10 = param2.getY() + param3.nextDouble();
               double var12 = param2.getZ() + param3.nextDouble();
               param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(param1.getBlockState(param2.east()))) {
            for (int var7 = 0; var7 < 2; var7++) {
               double var8 = param2.getX() + 1 - param3.nextDouble() * 0.1F;
               double var10 = param2.getY() + param3.nextDouble();
               double var12 = param2.getZ() + param3.nextDouble();
               param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(param1.getBlockState(param2.north()))) {
            for (int var7 = 0; var7 < 2; var7++) {
               double var8 = param2.getX() + param3.nextDouble();
               double var10 = param2.getY() + param3.nextDouble();
               double var12 = param2.getZ() + param3.nextDouble() * 0.1F;
               param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(param1.getBlockState(param2.south()))) {
            for (int var7 = 0; var7 < 2; var7++) {
               double var8 = param2.getX() + param3.nextDouble();
               double var10 = param2.getY() + param3.nextDouble();
               double var12 = param2.getZ() + 1 - param3.nextDouble() * 0.1F;
               param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
         }

         if (this.canBurn(param1.getBlockState(param2.above()))) {
            for (int var7 = 0; var7 < 2; var7++) {
               double var8 = param2.getX() + param3.nextDouble();
               double var10 = param2.getY() + 1 - param3.nextDouble() * 0.1F;
               double var12 = param2.getZ() + param3.nextDouble();
               param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
         }
      } else {
         for (int var7 = 0; var7 < 3; var7++) {
            double var8 = param2.getX() + param3.nextDouble();
            double var10 = param2.getY() + param3.nextDouble() * 0.5 + 0.5;
            double var12 = param2.getZ() + param3.nextDouble();
            param1.addParticle(ParticleTypes.LARGE_SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
         }
      }
   }

   protected abstract boolean canBurn(BlockState var1);

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (!param3.fireImmune()) {
         param3.setRemainingFireTicks(param3.getRemainingFireTicks() + 1);
         if (param3.getRemainingFireTicks() == 0) {
            param3.setSecondsOnFire(8);
         }
      }

      param3.hurt(param1.damageSources().inFire(), this.fireDamage);
      super.entityInside(param0, param1, param2, param3);
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (!param3.is(param0.getBlock())) {
         if (inPortalDimension(param1)) {
            Optional<PortalShape> var6 = PortalShape.findEmptyPortalShape(param1, param2, Direction.Axis.X);
            if (var6.isPresent()) {
               var6.get().createPortalBlocks();
               return;
            }
         }

         if (!param0.canSurvive(param1, param2)) {
            param1.removeBlock(param2, false);
         }
      }
   }

   private static boolean inPortalDimension(Level param0) {
      return param0.dimension() == Level.OVERWORLD || param0.dimension() == Level.NETHER;
   }

   @Override
   protected void spawnDestroyParticles(Level param0, Player param1, BlockPos param2, BlockState param3) {
   }

   @Override
   public void playerWillDestroy(Level param0, BlockPos param1, BlockState param2, Player param3) {
      if (!param0.isClientSide()) {
         param0.levelEvent(null, 1009, param1, 0);
      }

      super.playerWillDestroy(param0, param1, param2, param3);
   }

   public static boolean canBePlacedAt(Level param0, BlockPos param1, Direction param2) {
      BlockState var3 = param0.getBlockState(param1);
      return !var3.isAir() ? false : getState(param0, param1).canSurvive(param0, param1) || isPortal(param0, param1, param2);
   }

   private static boolean isPortal(Level param0, BlockPos param1, Direction param2) {
      if (!inPortalDimension(param0)) {
         return false;
      }

      BlockPos.MutableBlockPos var3 = param1.mutable();
      boolean var4 = false;

      for (Direction var8 : Direction.values()) {
         if (param0.getBlockState(var3.set(param1).move(var8)).is(Blocks.OBSIDIAN)) {
            var4 = true;
            break;
         }
      }

      if (!var4) {
         return false;
      }

      Direction.Axis var5 = param2.getAxis().isHorizontal() ? param2.getCounterClockWise().getAxis() : Direction.Plane.HORIZONTAL.getRandomAxis(param0.random);
      return PortalShape.findEmptyPortalShape(param0, param1, var5).isPresent();
   }
}
