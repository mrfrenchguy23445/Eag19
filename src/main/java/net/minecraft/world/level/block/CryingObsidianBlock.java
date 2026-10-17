package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CryingObsidianBlock extends Block {
   public CryingObsidianBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param3.nextInt(5) == 0) {
         Direction var5 = Direction.getRandom(param3);
         if (var5 != Direction.UP) {
            BlockPos var6 = param2.relative(var5);
            BlockState var7 = param1.getBlockState(var6);
            if (!param0.canOcclude() || !var7.isFaceSturdy(param1, var6, var5.getOpposite())) {
               double var8 = var5.getStepX() == 0 ? param3.nextDouble() : 0.5 + var5.getStepX() * 0.6;
               double var10 = var5.getStepY() == 0 ? param3.nextDouble() : 0.5 + var5.getStepY() * 0.6;
               double var12 = var5.getStepZ() == 0 ? param3.nextDouble() : 0.5 + var5.getStepZ() * 0.6;
               param1.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, param2.getX() + var8, param2.getY() + var10, param2.getZ() + var12, 0.0, 0.0, 0.0);
            }
         }
      }
   }
}
