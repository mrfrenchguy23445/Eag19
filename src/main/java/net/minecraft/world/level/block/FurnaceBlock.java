package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceBlock extends AbstractFurnaceBlock {
   protected FurnaceBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new FurnaceBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return createFurnaceTicker(param0, param2, BlockEntityType.FURNACE);
   }

   @Override
   protected void openContainer(Level param0, BlockPos param1, Player param2) {
      BlockEntity var4 = param0.getBlockEntity(param1);
      if (var4 instanceof FurnaceBlockEntity) {
         param2.openMenu((MenuProvider)var4);
         param2.awardStat(Stats.INTERACT_WITH_FURNACE);
      }
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         double var5 = param2.getX() + 0.5;
         double var7 = param2.getY();
         double var9 = param2.getZ() + 0.5;
         if (param3.nextDouble() < 0.1) {
            param1.playLocalSound(var5, var7, var9, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction var11 = param0.getValue(FACING);
         Direction.Axis var12 = var11.getAxis();
         double var13 = 0.52;
         double var15 = param3.nextDouble() * 0.6 - 0.3;
         double var17 = var12 == Direction.Axis.X ? var11.getStepX() * 0.52 : var15;
         double var19 = param3.nextDouble() * 6.0 / 16.0;
         double var21 = var12 == Direction.Axis.Z ? var11.getStepZ() * 0.52 : var15;
         param1.addParticle(ParticleTypes.SMOKE, var5 + var17, var7 + var19, var9 + var21, 0.0, 0.0, 0.0);
         param1.addParticle(ParticleTypes.FLAME, var5 + var17, var7 + var19, var9 + var21, 0.0, 0.0, 0.0);
      }
   }
}
