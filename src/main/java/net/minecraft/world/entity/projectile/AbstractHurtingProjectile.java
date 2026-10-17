package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractHurtingProjectile extends Projectile {
   public double xPower;
   public double yPower;
   public double zPower;

   protected AbstractHurtingProjectile(EntityType<? extends AbstractHurtingProjectile> param0, Level param1) {
      super(param0, param1);
   }

   public AbstractHurtingProjectile(
      EntityType<? extends AbstractHurtingProjectile> param0,
      double param1,
      double param3,
      double param5,
      double param7,
      double param9,
      double param11,
      Level param13
   ) {
      this(param0, param13);
      this.moveTo(param1, param3, param5, this.getYRot(), this.getXRot());
      this.reapplyPosition();
      double var15 = Math.sqrt(param7 * param7 + param9 * param9 + param11 * param11);
      if (var15 != 0.0) {
         this.xPower = param7 / var15 * 0.1;
         this.yPower = param9 / var15 * 0.1;
         this.zPower = param11 / var15 * 0.1;
      }
   }

   public AbstractHurtingProjectile(
      EntityType<? extends AbstractHurtingProjectile> param0, LivingEntity param1, double param2, double param4, double param6, Level param8
   ) {
      this(param0, param1.getX(), param1.getY(), param1.getZ(), param2, param4, param6, param8);
      this.setOwner(param1);
      this.setRot(param1.getYRot(), param1.getXRot());
   }

   @Override
   protected void defineSynchedData() {
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
      Entity var1 = this.getOwner();
      if (this.level.isClientSide || (var1 == null || !var1.isRemoved()) && this.level.hasChunkAt(this.blockPosition())) {
         super.tick();
         if (this.shouldBurn()) {
            this.setSecondsOnFire(1);
         }

         HitResult var2 = ProjectileUtil.getHitResult(this, this::canHitEntity);
         if (var2.getType() != HitResult.Type.MISS) {
            this.onHit(var2);
         }

         this.checkInsideBlocks();
         Vec3 var3 = this.getDeltaMovement();
         double var4 = this.getX() + var3.x;
         double var6 = this.getY() + var3.y;
         double var8 = this.getZ() + var3.z;
         ProjectileUtil.rotateTowardsMovement(this, 0.2F);
         float var10 = this.getInertia();
         if (this.isInWater()) {
            for (int var11 = 0; var11 < 4; var11++) {
               float var12 = 0.25F;
               this.level.addParticle(ParticleTypes.BUBBLE, var4 - var3.x * 0.25, var6 - var3.y * 0.25, var8 - var3.z * 0.25, var3.x, var3.y, var3.z);
            }

            var10 = 0.8F;
         }

         this.setDeltaMovement(var3.add(this.xPower, this.yPower, this.zPower).scale(var10));
         this.level.addParticle(this.getTrailParticle(), var4, var6 + 0.5, var8, 0.0, 0.0, 0.0);
         this.setPos(var4, var6, var8);
      } else {
         this.discard();
      }
   }

   @Override
   protected boolean canHitEntity(Entity param0) {
      return super.canHitEntity(param0) && !param0.noPhysics;
   }

   protected boolean shouldBurn() {
      return true;
   }

   protected ParticleOptions getTrailParticle() {
      return ParticleTypes.SMOKE;
   }

   protected float getInertia() {
      return 0.95F;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.put("power", this.newDoubleList(this.xPower, this.yPower, this.zPower));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("power", 9)) {
         ListTag var2 = param0.getList("power", 6);
         if (var2.size() == 3) {
            this.xPower = var2.getDouble(0);
            this.yPower = var2.getDouble(1);
            this.zPower = var2.getDouble(2);
         }
      }
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public float getPickRadius() {
      return 1.0F;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      this.markHurt();
      Entity var3 = param0.getEntity();
      if (var3 != null) {
         if (!this.level.isClientSide) {
            Vec3 var4 = var3.getLookAngle();
            this.setDeltaMovement(var4);
            this.xPower = var4.x * 0.1;
            this.yPower = var4.y * 0.1;
            this.zPower = var4.z * 0.1;
            this.setOwner(var3);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      Entity var1 = this.getOwner();
      int var2 = var1 == null ? 0 : var1.getId();
      return new ClientboundAddEntityPacket(
         this.getId(),
         this.getUUID(),
         this.getX(),
         this.getY(),
         this.getZ(),
         this.getXRot(),
         this.getYRot(),
         this.getType(),
         var2,
         new Vec3(this.xPower, this.yPower, this.zPower),
         0.0
      );
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      super.recreateFromPacket(param0);
      double var2 = param0.getXa();
      double var4 = param0.getYa();
      double var6 = param0.getZa();
      double var8 = Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
      if (var8 != 0.0) {
         this.xPower = var2 / var8 * 0.1;
         this.yPower = var4 / var8 * 0.1;
         this.zPower = var6 / var8 * 0.1;
      }
   }
}
