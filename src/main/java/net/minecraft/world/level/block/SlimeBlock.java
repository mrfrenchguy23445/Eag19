package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SlimeBlock extends HalfTransparentBlock {
   public SlimeBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   public void fallOn(Level param0, BlockState param1, BlockPos param2, Entity param3, float param4) {
      if (param3.isSuppressingBounce()) {
         super.fallOn(param0, param1, param2, param3, param4);
      } else {
         param3.causeFallDamage(param4, 0.0F, param0.damageSources().fall());
      }
   }

   @Override
   public void updateEntityAfterFallOn(BlockGetter param0, Entity param1) {
      if (param1.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(param0, param1);
      } else {
         this.bounceUp(param1);
      }
   }

   private void bounceUp(Entity param0) {
      Vec3 var2 = param0.getDeltaMovement();
      if (var2.y < 0.0) {
         double var3 = param0 instanceof LivingEntity ? 1.0 : 0.8;
         param0.setDeltaMovement(var2.x, -var2.y * var3, var2.z);
      }
   }

   @Override
   public void stepOn(Level param0, BlockPos param1, BlockState param2, Entity param3) {
      double var5 = Math.abs(param3.getDeltaMovement().y);
      if (var5 < 0.1 && !param3.isSteppingCarefully()) {
         double var7 = 0.4 + var5 * 0.2;
         param3.setDeltaMovement(param3.getDeltaMovement().multiply(var7, 1.0, var7));
      }

      super.stepOn(param0, param1, param2, param3);
   }
}
