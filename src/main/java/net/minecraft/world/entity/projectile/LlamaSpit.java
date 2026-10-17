package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LlamaSpit extends Projectile {
   public LlamaSpit(EntityType<? extends LlamaSpit> param0, Level param1) {
      super(param0, param1);
   }

   public LlamaSpit(Level param0, Llama param1) {
      this(EntityType.LLAMA_SPIT, param0);
      this.setOwner(param1);
      this.setPos(
         param1.getX() - (param1.getBbWidth() + 1.0F) * 0.5 * Mth.sin(param1.yBodyRot * (float) (Math.PI / 180.0)),
         param1.getEyeY() - 0.1F,
         param1.getZ() + (param1.getBbWidth() + 1.0F) * 0.5 * Mth.cos(param1.yBodyRot * (float) (Math.PI / 180.0))
      );
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 var1 = this.getDeltaMovement();
      HitResult var2 = ProjectileUtil.getHitResult(this, this::canHitEntity);
      this.onHit(var2);
      double var3 = this.getX() + var1.x;
      double var5 = this.getY() + var1.y;
      double var7 = this.getZ() + var1.z;
      this.updateRotation();
      float var9 = 0.99F;
      float var10 = 0.06F;
      if (this.level.getBlockStates(this.getBoundingBox()).noneMatch(BlockBehaviour.BlockStateBase::isAir)) {
         this.discard();
      } else if (this.isInWaterOrBubble()) {
         this.discard();
      } else {
         this.setDeltaMovement(var1.scale(0.99F));
         if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06F, 0.0));
         }

         this.setPos(var3, var5, var7);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult param0) {
      super.onHitEntity(param0);
      if (this.getOwner() instanceof LivingEntity var2) {
         param0.getEntity().hurt(this.damageSources().mobProjectile(this, var2), 1.0F);
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult param0) {
      super.onHitBlock(param0);
      if (!this.level.isClientSide) {
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      double var2 = param0.getXa();
      double var4 = param0.getYa();
      double var6 = param0.getZa();

      for (int var8 = 0; var8 < 7; var8++) {
         double var9 = 0.4 + 0.1 * var8;
         this.level.addParticle(ParticleTypes.SPIT, this.getX(), this.getY(), this.getZ(), var2 * var9, var4, var6 * var9);
      }

      this.setDeltaMovement(var2, var4, var6);
   }
}
