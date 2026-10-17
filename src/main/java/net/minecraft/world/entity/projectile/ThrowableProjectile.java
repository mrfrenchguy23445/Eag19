package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class ThrowableProjectile extends Projectile {
   protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> param0, Level param1) {
      super(param0, param1);
   }

   protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> param0, double param1, double param3, double param5, Level param7) {
      this(param0, param7);
      this.setPos(param1, param3, param5);
   }

   protected ThrowableProjectile(EntityType<? extends ThrowableProjectile> param0, LivingEntity param1, Level param2) {
      this(param0, param1.getX(), param1.getEyeY() - 0.1F, param1.getZ(), param2);
      this.setOwner(param1);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double param0) {
      double var3 = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN(var3)) {
         var3 = 4.0;
      }

      var3 *= 64.0;
      return param0 < var3 * var3;
   }

   @Override
   public void tick() {
      super.tick();
      HitResult var1 = ProjectileUtil.getHitResult(this, this::canHitEntity);
      boolean var2 = false;
      if (var1.getType() == HitResult.Type.BLOCK) {
         BlockPos var3 = ((BlockHitResult)var1).getBlockPos();
         BlockState var4 = this.level.getBlockState(var3);
         if (var4.is(Blocks.NETHER_PORTAL)) {
            this.handleInsidePortal(var3);
            var2 = true;
         } else if (var4.is(Blocks.END_GATEWAY)) {
            BlockEntity var5 = this.level.getBlockEntity(var3);
            if (var5 instanceof TheEndGatewayBlockEntity && TheEndGatewayBlockEntity.canEntityTeleport(this)) {
               TheEndGatewayBlockEntity.teleportEntity(this.level, var3, var4, this, (TheEndGatewayBlockEntity)var5);
            }

            var2 = true;
         }
      }

      if (var1.getType() != HitResult.Type.MISS && !var2) {
         this.onHit(var1);
      }

      this.checkInsideBlocks();
      Vec3 var3 = this.getDeltaMovement();
      double var4 = this.getX() + var3.x;
      double var6 = this.getY() + var3.y;
      double var8 = this.getZ() + var3.z;
      this.updateRotation();
      float var10;
      if (this.isInWater()) {
         for (int var11 = 0; var11 < 4; var11++) {
            float var12 = 0.25F;
            this.level.addParticle(ParticleTypes.BUBBLE, var4 - var3.x * 0.25, var6 - var3.y * 0.25, var8 - var3.z * 0.25, var3.x, var3.y, var3.z);
         }

         var10 = 0.8F;
      } else {
         var10 = 0.99F;
      }

      this.setDeltaMovement(var3.scale(var10));
      if (!this.isNoGravity()) {
         Vec3 var11 = this.getDeltaMovement();
         this.setDeltaMovement(var11.x, var11.y - this.getGravity(), var11.z);
      }

      this.setPos(var4, var6, var8);
   }

   protected float getGravity() {
      return 0.03F;
   }
}
