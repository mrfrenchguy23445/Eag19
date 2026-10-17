package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class EndGatewayBlock extends BaseEntityBlock {
   protected EndGatewayBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new TheEndGatewayBlockEntity(param0, param1);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level param0, BlockState param1, BlockEntityType<T> param2) {
      return createTickerHelper(
         param2, BlockEntityType.END_GATEWAY, param0.isClientSide ? TheEndGatewayBlockEntity::beamAnimationTick : TheEndGatewayBlockEntity::teleportTick
      );
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      BlockEntity var5 = param1.getBlockEntity(param2);
      if (var5 instanceof TheEndGatewayBlockEntity) {
         int var6 = ((TheEndGatewayBlockEntity)var5).getParticleAmount();

         for (int var7 = 0; var7 < var6; var7++) {
            double var8 = param2.getX() + param3.nextDouble();
            double var10 = param2.getY() + param3.nextDouble();
            double var12 = param2.getZ() + param3.nextDouble();
            double var14 = (param3.nextDouble() - 0.5) * 0.5;
            double var16 = (param3.nextDouble() - 0.5) * 0.5;
            double var18 = (param3.nextDouble() - 0.5) * 0.5;
            int var20 = param3.nextInt(2) * 2 - 1;
            if (param3.nextBoolean()) {
               var12 = param2.getZ() + 0.5 + 0.25 * var20;
               var18 = param3.nextFloat() * 2.0F * var20;
            } else {
               var8 = param2.getX() + 0.5 + 0.25 * var20;
               var14 = param3.nextFloat() * 2.0F * var20;
            }

            param1.addParticle(ParticleTypes.PORTAL, var8, var10, var12, var14, var16, var18);
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter param0, BlockPos param1, BlockState param2) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean canBeReplaced(BlockState param0, Fluid param1) {
      return false;
   }
}
