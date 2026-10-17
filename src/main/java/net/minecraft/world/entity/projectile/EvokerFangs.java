package net.minecraft.world.entity.projectile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;

public class EvokerFangs extends Entity implements TraceableEntity {
   public static final int ATTACK_DURATION = 20;
   public static final int LIFE_OFFSET = 2;
   public static final int ATTACK_TRIGGER_TICKS = 14;
   private int warmupDelayTicks;
   private boolean sentSpikeEvent;
   private int lifeTicks = 22;
   private boolean clientSideAttackStarted;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUUID;

   public EvokerFangs(EntityType<? extends EvokerFangs> param0, Level param1) {
      super(param0, param1);
   }

   public EvokerFangs(Level param0, double param1, double param3, double param5, float param7, int param8, LivingEntity param9) {
      this(EntityType.EVOKER_FANGS, param0);
      this.warmupDelayTicks = param8;
      this.setOwner(param9);
      this.setYRot(param7 * (180.0F / (float)Math.PI));
      this.setPos(param1, param3, param5);
   }

   @Override
   protected void defineSynchedData() {
   }

   public void setOwner(@Nullable LivingEntity param0) {
      this.owner = param0;
      this.ownerUUID = param0 == null ? null : param0.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUUID != null && this.level instanceof ServerLevel) {
         Entity var1 = ((ServerLevel)this.level).getEntity(this.ownerUUID);
         if (var1 instanceof LivingEntity) {
            this.owner = (LivingEntity)var1;
         }
      }

      return this.owner;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag param0) {
      this.warmupDelayTicks = param0.getInt("Warmup");
      if (param0.hasUUID("Owner")) {
         this.ownerUUID = param0.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag param0) {
      param0.putInt("Warmup", this.warmupDelayTicks);
      if (this.ownerUUID != null) {
         param0.putUUID("Owner", this.ownerUUID);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         if (this.clientSideAttackStarted) {
            this.lifeTicks--;
            if (this.lifeTicks == 14) {
               for (int var1 = 0; var1 < 12; var1++) {
                  double var2 = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double var4 = this.getY() + 0.05 + this.random.nextDouble();
                  double var6 = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double var8 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  double var10 = 0.3 + this.random.nextDouble() * 0.3;
                  double var12 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  this.level.addParticle(ParticleTypes.CRIT, var2, var4 + 1.0, var6, var8, var10, var12);
               }
            }
         }
      } else if (--this.warmupDelayTicks < 0) {
         if (this.warmupDelayTicks == -8) {
            for (LivingEntity var3 : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
               this.dealDamageTo(var3);
            }
         }

         if (!this.sentSpikeEvent) {
            this.level.broadcastEntityEvent(this, (byte)4);
            this.sentSpikeEvent = true;
         }

         if (--this.lifeTicks < 0) {
            this.discard();
         }
      }
   }

   private void dealDamageTo(LivingEntity param0) {
      LivingEntity var2 = this.getOwner();
      if (param0.isAlive() && !param0.isInvulnerable() && param0 != var2) {
         if (var2 == null) {
            param0.hurt(this.damageSources().magic(), 6.0F);
         } else {
            if (var2.isAlliedTo(param0)) {
               return;
            }

            param0.hurt(this.damageSources().indirectMagic(this, var2), 6.0F);
         }
      }
   }

   @Override
   public void handleEntityEvent(byte param0) {
      super.handleEntityEvent(param0);
      if (param0 == 4) {
         this.clientSideAttackStarted = true;
         if (!this.isSilent()) {
            this.level
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.EVOKER_FANGS_ATTACK,
                  this.getSoundSource(),
                  1.0F,
                  this.random.nextFloat() * 0.2F + 0.85F,
                  false
               );
         }
      }
   }

   public float getAnimationProgress(float param0) {
      if (!this.clientSideAttackStarted) {
         return 0.0F;
      }

      int var2 = this.lifeTicks - 2;
      return var2 <= 0 ? 1.0F : 1.0F - (var2 - param0) / 20.0F;
   }
}
