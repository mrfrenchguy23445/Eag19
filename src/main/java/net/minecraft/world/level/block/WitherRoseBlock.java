package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WitherRoseBlock extends FlowerBlock {
   public WitherRoseBlock(MobEffect param0, BlockBehaviour.Properties param1) {
      super(param0, 8, param1);
   }

   @Override
   protected boolean mayPlaceOn(BlockState param0, BlockGetter param1, BlockPos param2) {
      return super.mayPlaceOn(param0, param1, param2) || param0.is(Blocks.NETHERRACK) || param0.is(Blocks.SOUL_SAND) || param0.is(Blocks.SOUL_SOIL);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      VoxelShape var5 = this.getShape(param0, param1, param2, CollisionContext.empty());
      Vec3 var6 = var5.bounds().getCenter();
      double var7 = param2.getX() + var6.x;
      double var9 = param2.getZ() + var6.z;

      for (int var11 = 0; var11 < 3; var11++) {
         if (param3.nextBoolean()) {
            param1.addParticle(
               ParticleTypes.SMOKE,
               var7 + param3.nextDouble() / 5.0,
               param2.getY() + (0.5 - param3.nextDouble()),
               var9 + param3.nextDouble() / 5.0,
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (!param1.isClientSide && param1.getDifficulty() != Difficulty.PEACEFUL) {
         if (param3 instanceof LivingEntity var5 && !var5.isInvulnerableTo(param1.damageSources().wither())) {
            var5.addEffect(new MobEffectInstance(MobEffects.WITHER, 40));
         }
      }
   }
}
