package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DragonEggBlock extends FallingBlock {
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

   public DragonEggBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      this.teleport(param0, param1, param2);
      return InteractionResult.sidedSuccess(param1.isClientSide);
   }

   @Override
   public void attack(BlockState param0, Level param1, BlockPos param2, Player param3) {
      this.teleport(param0, param1, param2);
   }

   private void teleport(BlockState param0, Level param1, BlockPos param2) {
      WorldBorder var4 = param1.getWorldBorder();

      for (int var5 = 0; var5 < 1000; var5++) {
         BlockPos var6 = param2.offset(
            param1.random.nextInt(16) - param1.random.nextInt(16),
            param1.random.nextInt(8) - param1.random.nextInt(8),
            param1.random.nextInt(16) - param1.random.nextInt(16)
         );
         if (param1.getBlockState(var6).isAir() && var4.isWithinBounds(var6)) {
            if (param1.isClientSide) {
               for (int var7 = 0; var7 < 128; var7++) {
                  double var8 = param1.random.nextDouble();
                  float var10 = (param1.random.nextFloat() - 0.5F) * 0.2F;
                  float var11 = (param1.random.nextFloat() - 0.5F) * 0.2F;
                  float var12 = (param1.random.nextFloat() - 0.5F) * 0.2F;
                  double var13 = Mth.lerp(var8, var6.getX(), param2.getX()) + (param1.random.nextDouble() - 0.5) + 0.5;
                  double var15 = Mth.lerp(var8, var6.getY(), param2.getY()) + param1.random.nextDouble() - 0.5;
                  double var17 = Mth.lerp(var8, var6.getZ(), param2.getZ()) + (param1.random.nextDouble() - 0.5) + 0.5;
                  param1.addParticle(ParticleTypes.PORTAL, var13, var15, var17, var10, var11, var12);
               }
            } else {
               param1.setBlock(var6, param0, 2);
               param1.removeBlock(param2, false);
            }

            return;
         }
      }
   }

   @Override
   protected int getDelayAfterPlace() {
      return 5;
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}
