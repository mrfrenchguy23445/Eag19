package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WetSpongeBlock extends Block {
   protected WetSpongeBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public void onPlace(BlockState param0, Level param1, BlockPos param2, BlockState param3, boolean param4) {
      if (param1.dimensionType().ultraWarm()) {
         param1.setBlock(param2, Blocks.SPONGE.defaultBlockState(), 3);
         param1.levelEvent(2009, param2, 0);
         param1.playSound((Player)null, param2, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, (1.0F + param1.getRandom().nextFloat() * 0.2F) * 0.7F);
      }
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      Direction var5 = Direction.getRandom(param3);
      if (var5 != Direction.UP) {
         BlockPos var6 = param2.relative(var5);
         BlockState var7 = param1.getBlockState(var6);
         if (!param0.canOcclude() || !var7.isFaceSturdy(param1, var6, var5.getOpposite())) {
            double var8 = param2.getX();
            double var10 = param2.getY();
            double var12 = param2.getZ();
            if (var5 == Direction.DOWN) {
               var10 -= 0.05;
               var8 += param3.nextDouble();
               var12 += param3.nextDouble();
            } else {
               var10 += param3.nextDouble() * 0.8;
               if (var5.getAxis() == Direction.Axis.X) {
                  var12 += param3.nextDouble();
                  if (var5 == Direction.EAST) {
                     var8 += 1.1;
                  } else {
                     var8 += 0.05;
                  }
               } else {
                  var8 += param3.nextDouble();
                  if (var5 == Direction.SOUTH) {
                     var12 += 1.1;
                  } else {
                     var12 += 0.05;
                  }
               }
            }

            param1.addParticle(ParticleTypes.DRIPPING_WATER, var8, var10, var12, 0.0, 0.0, 0.0);
         }
      }
   }
}
