package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class Squid extends WaterAnimal {
   public float xBodyRot;
   public float xBodyRotO;
   public float zBodyRot;
   public float zBodyRotO;
   public float tentacleMovement;
   public float oldTentacleMovement;
   public float tentacleAngle;
   public float oldTentacleAngle;
   private float speed;
   private float tentacleSpeed;
   private float rotateSpeed;
   private float tx;
   private float ty;
   private float tz;

   public Squid(EntityType<? extends Squid> param0, Level param1) {
      super(param0, param1);
      this.random.setSeed(this.getId());
      this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Squid.SquidRandomMovementGoal(this));
      this.goalSelector.addGoal(1, new Squid.SquidFleeGoal());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0);
   }

   @Override
   protected float getStandingEyeHeight(Pose param0, EntityDimensions param1) {
      return param1.height * 0.5F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SQUID_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.SQUID_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SQUID_DEATH;
   }

   protected SoundEvent getSquirtSound() {
      return SoundEvents.SQUID_SQUIRT;
   }

   @Override
   public boolean canBeLeashed(Player param0) {
      return !this.isLeashed();
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.xBodyRotO = this.xBodyRot;
      this.zBodyRotO = this.zBodyRot;
      this.oldTentacleMovement = this.tentacleMovement;
      this.oldTentacleAngle = this.tentacleAngle;
      this.tentacleMovement = this.tentacleMovement + this.tentacleSpeed;
      if (this.tentacleMovement > Math.PI * 2) {
         if (this.level.isClientSide) {
            this.tentacleMovement = (float) (Math.PI * 2);
         } else {
            this.tentacleMovement -= (float) (Math.PI * 2);
            if (this.random.nextInt(10) == 0) {
               this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }

            this.level.broadcastEntityEvent(this, (byte)19);
         }
      }

      if (this.isInWaterOrBubble()) {
         if (this.tentacleMovement < (float) Math.PI) {
            float var1 = this.tentacleMovement / (float) Math.PI;
            this.tentacleAngle = Mth.sin(var1 * var1 * (float) Math.PI) * (float) Math.PI * 0.25F;
            if (var1 > 0.75) {
               this.speed = 1.0F;
               this.rotateSpeed = 1.0F;
            } else {
               this.rotateSpeed *= 0.8F;
            }
         } else {
            this.tentacleAngle = 0.0F;
            this.speed *= 0.9F;
            this.rotateSpeed *= 0.99F;
         }

         if (!this.level.isClientSide) {
            this.setDeltaMovement(this.tx * this.speed, this.ty * this.speed, this.tz * this.speed);
         }

         Vec3 var1 = this.getDeltaMovement();
         double var2 = var1.horizontalDistance();
         this.yBodyRot = this.yBodyRot + (-((float)Mth.atan2(var1.x, var1.z)) * (180.0F / (float)Math.PI) - this.yBodyRot) * 0.1F;
         this.setYRot(this.yBodyRot);
         this.zBodyRot = this.zBodyRot + (float) Math.PI * this.rotateSpeed * 1.5F;
         this.xBodyRot = this.xBodyRot + (-((float)Mth.atan2(var2, var1.y)) * (180.0F / (float)Math.PI) - this.xBodyRot) * 0.1F;
      } else {
         this.tentacleAngle = Mth.abs(Mth.sin(this.tentacleMovement)) * (float) Math.PI * 0.25F;
         if (!this.level.isClientSide) {
            double var1 = this.getDeltaMovement().y;
            if (this.hasEffect(MobEffects.LEVITATION)) {
               var1 = 0.05 * (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
            } else if (!this.isNoGravity()) {
               var1 -= 0.08;
            }

            this.setDeltaMovement(0.0, var1 * 0.98F, 0.0);
         }

         this.xBodyRot = this.xBodyRot + (-90.0F - this.xBodyRot) * 0.02F;
      }
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (super.hurt(param0, param1) && this.getLastHurtByMob() != null) {
         if (!this.level.isClientSide) {
            this.spawnInk();
         }

         return true;
      } else {
         return false;
      }
   }

   private Vec3 rotateVector(Vec3 param0) {
      Vec3 var2 = param0.xRot(this.xBodyRotO * (float) (Math.PI / 180.0));
      return var2.yRot(-this.yBodyRotO * (float) (Math.PI / 180.0));
   }

   private void spawnInk() {
      this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());
      Vec3 var1 = this.rotateVector(new Vec3(0.0, -1.0, 0.0)).add(this.getX(), this.getY(), this.getZ());

      for (int var2 = 0; var2 < 30; var2++) {
         Vec3 var3 = this.rotateVector(new Vec3(this.random.nextFloat() * 0.6 - 0.3, -1.0, this.random.nextFloat() * 0.6 - 0.3));
         Vec3 var4 = var3.scale(0.3 + this.random.nextFloat() * 2.0F);
         ((ServerLevel)this.level).sendParticles(this.getInkParticle(), var1.x, var1.y + 0.5, var1.z, 0, var4.x, var4.y, var4.z, 0.1F);
      }
   }

   protected ParticleOptions getInkParticle() {
      return ParticleTypes.SQUID_INK;
   }

   @Override
   public void travel(Vec3 param0) {
      this.move(MoverType.SELF, this.getDeltaMovement());
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 19) {
         this.tentacleMovement = 0.0F;
      } else {
         super.handleEntityEvent(param0);
      }
   }

   public void setMovementVector(float param0, float param1, float param2) {
      this.tx = param0;
      this.ty = param1;
      this.tz = param2;
   }

   public boolean hasMovementVector() {
      return this.tx != 0.0F || this.ty != 0.0F || this.tz != 0.0F;
   }

   class SquidFleeGoal extends Goal {
      private static final float SQUID_FLEE_SPEED = 3.0F;
      private static final float SQUID_FLEE_MIN_DISTANCE = 5.0F;
      private static final float SQUID_FLEE_MAX_DISTANCE = 10.0F;
      private int fleeTicks;

      @Override
      public boolean canUse() {
         LivingEntity var1 = Squid.this.getLastHurtByMob();
         return Squid.this.isInWater() && var1 != null ? Squid.this.distanceToSqr(var1) < 100.0 : false;
      }

      @Override
      public void start() {
         this.fleeTicks = 0;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         this.fleeTicks++;
         LivingEntity var1 = Squid.this.getLastHurtByMob();
         if (var1 != null) {
            Vec3 var2 = new Vec3(Squid.this.getX() - var1.getX(), Squid.this.getY() - var1.getY(), Squid.this.getZ() - var1.getZ());
            BlockState var3 = Squid.this.level
               .getBlockState(BlockPos.containing(Squid.this.getX() + var2.x, Squid.this.getY() + var2.y, Squid.this.getZ() + var2.z));
            FluidState var4 = Squid.this.level
               .getFluidState(BlockPos.containing(Squid.this.getX() + var2.x, Squid.this.getY() + var2.y, Squid.this.getZ() + var2.z));
            if (var4.is(FluidTags.WATER) || var3.isAir()) {
               double var5 = var2.length();
               if (var5 > 0.0) {
                  var2.normalize();
                  double var7 = 3.0;
                  if (var5 > 5.0) {
                     var7 -= (var5 - 5.0) / 5.0;
                  }

                  if (var7 > 0.0) {
                     var2 = var2.scale(var7);
                  }
               }

               if (var3.isAir()) {
                  var2 = var2.subtract(0.0, var2.y, 0.0);
               }

               Squid.this.setMovementVector((float)var2.x / 20.0F, (float)var2.y / 20.0F, (float)var2.z / 20.0F);
            }

            if (this.fleeTicks % 10 == 5) {
               Squid.this.level.addParticle(ParticleTypes.BUBBLE, Squid.this.getX(), Squid.this.getY(), Squid.this.getZ(), 0.0, 0.0, 0.0);
            }
         }
      }
   }

   class SquidRandomMovementGoal extends Goal {
      private final Squid squid;

      public SquidRandomMovementGoal(Squid param1) {
         this.squid = param1;
      }

      @Override
      public boolean canUse() {
         return true;
      }

      @Override
      public void tick() {
         int var1 = this.squid.getNoActionTime();
         if (var1 > 100) {
            this.squid.setMovementVector(0.0F, 0.0F, 0.0F);
         } else if (this.squid.getRandom().nextInt(reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
            float var2 = this.squid.getRandom().nextFloat() * (float) (Math.PI * 2);
            float var3 = Mth.cos(var2) * 0.2F;
            float var4 = -0.1F + this.squid.getRandom().nextFloat() * 0.2F;
            float var5 = Mth.sin(var2) * 0.2F;
            this.squid.setMovementVector(var3, var4, var5);
         }
      }
   }
}
