package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RiderShieldingMount;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Camel extends AbstractHorse implements PlayerRideableJumping, RiderShieldingMount, Saddleable {
   public static final Ingredient TEMPTATION_ITEM = Ingredient.of(Items.CACTUS);
   public static final int DASH_COOLDOWN_TICKS = 55;
   public static final int MAX_HEAD_Y_ROT = 30;
   private static final float RUNNING_SPEED_BONUS = 0.1F;
   private static final float DASH_VERTICAL_MOMENTUM = 1.4285F;
   private static final float DASH_HORIZONTAL_MOMENTUM = 22.2222F;
   private static final int SITDOWN_DURATION_TICKS = 40;
   private static final int STANDUP_DURATION_TICKS = 52;
   private static final int IDLE_MINIMAL_DURATION_TICKS = 80;
   private static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;
   public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
   public final AnimationState sitAnimationState = new AnimationState();
   public final AnimationState sitPoseAnimationState = new AnimationState();
   public final AnimationState sitUpAnimationState = new AnimationState();
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState dashAnimationState = new AnimationState();
   private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(EntityType.CAMEL.getWidth(), EntityType.CAMEL.getHeight() - 1.43F);
   private int dashCooldown = 0;
   private int idleAnimationTimeout = 0;

   public Camel(EntityType<? extends Camel> param0, Level param1) {
      super(param0, param1);
      this.setMaxUpStep(1.5F);
      this.moveControl = new Camel.CamelMoveControl();
      GroundPathNavigation var3 = (GroundPathNavigation)this.getNavigation();
      var3.setCanFloat(true);
      var3.setCanWalkOverFences(true);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putLong("LastPoseTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      long var2 = param0.getLong("LastPoseTick");
      if (var2 < 0L) {
         this.setPose(Pose.SITTING);
      }

      this.resetLastPoseChangeTick(var2);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.09F).add(Attributes.JUMP_STRENGTH, 0.42F);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DASH, false);
      this.entityData.define(LAST_POSE_CHANGE_TICK, 0L);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor param0, DifficultyInstance param1, MobSpawnType param2, @Nullable SpawnGroupData param3, @Nullable CompoundTag param4
   ) {
      CamelAi.initMemories(this, param0.getRandom());
      this.resetLastPoseChangeTickToFullStand(param0.getLevel().getGameTime());
      return super.finalizeSpawn(param0, param1, param2, param3, param4);
   }

   @Override
   protected Brain.Provider<Camel> brainProvider() {
      return CamelAi.brainProvider();
   }

   @Override
   protected void registerGoals() {
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> param0) {
      return CamelAi.makeBrain(this.brainProvider().makeBrain(param0));
   }

   @Override
   public EntityDimensions getDimensions(Pose param0) {
      return param0 == Pose.SITTING ? SITTING_DIMENSIONS.scale(this.getScale()) : super.getDimensions(param0);
   }

   @Override
   protected float getStandingEyeHeight(Pose param0, EntityDimensions param1) {
      return param1.height - 0.1F;
   }

   @Override
   public double getRiderShieldingHeight() {
      return 0.5;
   }

   @Override
   protected void customServerAiStep() {
      this.level.getProfiler().push("camelBrain");
      Brain<?> var1 = this.getBrain();
      ((Brain<Camel>)var1).tick((ServerLevel)this.level, this);
      this.level.getProfiler().pop();
      this.level.getProfiler().push("camelActivityUpdate");
      CamelAi.updateActivity(this);
      this.level.getProfiler().pop();
      super.customServerAiStep();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isDashing() && this.dashCooldown < 55 && (this.onGround || this.isInWater())) {
         this.setDashing(false);
      }

      if (this.dashCooldown > 0) {
         this.dashCooldown--;
         if (this.dashCooldown == 0) {
            this.level.playSound((Player)null, this.blockPosition(), SoundEvents.CAMEL_DASH_READY, SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      }

      if (this.level.isClientSide()) {
         this.setupAnimationStates();
      }

      if (this.refuseToMove()) {
         this.clampHeadRotationToBody(this, 30.0F);
      }
   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0) {
         this.idleAnimationTimeout = this.random.nextInt(40) + 80;
         this.idleAnimationState.start(this.tickCount);
      } else {
         this.idleAnimationTimeout--;
      }

      if (this.isCamelVisuallySitting()) {
         this.sitUpAnimationState.stop();
         this.dashAnimationState.stop();
         if (this.isVisuallySittingDown()) {
            this.sitAnimationState.startIfStopped(this.tickCount);
            this.sitPoseAnimationState.stop();
         } else {
            this.sitAnimationState.stop();
            this.sitPoseAnimationState.startIfStopped(this.tickCount);
         }
      } else {
         this.sitAnimationState.stop();
         this.sitPoseAnimationState.stop();
         this.dashAnimationState.animateWhen(this.isDashing(), this.tickCount);
         this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
      }
   }

   @Override
   protected void updateWalkAnimation(float param0) {
      float var2;
      if (this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted()) {
         var2 = Math.min(param0 * 6.0F, 1.0F);
      } else {
         var2 = 0.0F;
      }

      this.walkAnimation.update(var2, 0.2F);
   }

   @Override
   public void travel(Vec3 param0) {
      if (this.refuseToMove() && this.isOnGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
         param0 = param0.multiply(0.0, 1.0, 0.0);
      }

      super.travel(param0);
   }

   @Override
   protected void tickRidden(LivingEntity param0, Vec3 param1) {
      super.tickRidden(param0, param1);
      if (param0.zza > 0.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }
   }

   public boolean refuseToMove() {
      return this.isCamelSitting() || this.isInPoseTransition();
   }

   @Override
   protected float getRiddenSpeed(LivingEntity param0) {
      float var2 = param0.isSprinting() && this.getJumpCooldown() == 0 ? 0.1F : 0.0F;
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + var2;
   }

   @Override
   protected Vec2 getRiddenRotation(LivingEntity param0) {
      return this.refuseToMove() ? new Vec2(this.getXRot(), this.getYRot()) : super.getRiddenRotation(param0);
   }

   @Override
   protected Vec3 getRiddenInput(LivingEntity param0, Vec3 param1) {
      return this.refuseToMove() ? Vec3.ZERO : super.getRiddenInput(param0, param1);
   }

   @Override
   public boolean canJump() {
      return !this.refuseToMove() && super.canJump();
   }

   @Override
   public void onPlayerJump(int param0) {
      if (this.isSaddled() && this.dashCooldown <= 0 && this.isOnGround()) {
         super.onPlayerJump(param0);
      }
   }

   @Override
   public boolean canSprint() {
      return true;
   }

   @Override
   protected void executeRidersJump(float param0, Vec3 param1) {
      double var3 = this.getAttributeValue(Attributes.JUMP_STRENGTH) * this.getBlockJumpFactor() + this.getJumpBoostPower();
      this.addDeltaMovement(
         this.getLookAngle()
            .multiply(1.0, 0.0, 1.0)
            .normalize()
            .scale(22.2222F * param0 * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
            .add(0.0, 1.4285F * param0 * var3, 0.0)
      );
      this.dashCooldown = 55;
      this.setDashing(true);
      this.hasImpulse = true;
   }

   public boolean isDashing() {
      return this.entityData.get(DASH);
   }

   public void setDashing(boolean param0) {
      this.entityData.set(DASH, param0);
   }

   public boolean isPanicking() {
      return this.getBrain().checkMemory(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT);
   }

   @Override
   public void handleStartJump(int param0) {
      this.playSound(SoundEvents.CAMEL_DASH, 1.0F, 1.0F);
      this.setDashing(true);
   }

   @Override
   public void handleStopJump() {
   }

   @Override
   public int getJumpCooldown() {
      return this.dashCooldown;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CAMEL_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CAMEL_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.CAMEL_HURT;
   }

   @Override
   protected void playStepSound(BlockPos param0, BlockState param1) {
      if (param1.getSoundType() == SoundType.SAND) {
         this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0F, 1.0F);
      } else {
         this.playSound(SoundEvents.CAMEL_STEP, 1.0F, 1.0F);
      }
   }

   @Override
   public boolean isFood(ItemStack param0) {
      return TEMPTATION_ITEM.test(param0);
   }

   @Override
   public InteractionResult mobInteract(Player param0, InteractionHand param1) {
      ItemStack var3 = param0.getItemInHand(param1);
      if (param0.isSecondaryUseActive()) {
         this.openCustomInventoryScreen(param0);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      }

      InteractionResult var4 = var3.interactLivingEntity(param0, this, param1);
      if (var4.consumesAction()) {
         return var4;
      }

      if (this.isFood(var3)) {
         return this.fedFood(param0, var3);
      }

      if (this.getPassengers().size() < 2 && !this.isBaby()) {
         this.doPlayerRide(param0);
      }

      return InteractionResult.sidedSuccess(this.level.isClientSide);
   }

   @Override
   protected void onLeashDistance(float param0) {
      if (param0 > 6.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }
   }

   @Override
   protected boolean handleEating(Player param0, ItemStack param1) {
      if (!this.isFood(param1)) {
         return false;
      }

      boolean var3 = this.getHealth() < this.getMaxHealth();
      if (var3) {
         this.heal(2.0F);
      }

      boolean var4 = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
      if (var4) {
         this.setInLove(param0);
      }

      boolean var5 = this.isBaby();
      if (var5) {
         this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
         if (!this.level.isClientSide) {
            this.ageUp(10);
         }
      }

      if (!var3 && !var4 && !var5) {
         return false;
      }

      if (!this.isSilent()) {
         SoundEvent var6 = this.getEatingSound();
         if (var6 != null) {
            this.level
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  var6,
                  this.getSoundSource(),
                  1.0F,
                  1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
               );
         }
      }

      return true;
   }

   @Override
   protected boolean canPerformRearing() {
      return false;
   }

   @Override
   public boolean canMate(Animal param0) {
      return param0 != this && param0 instanceof Camel var2 && this.canParent() && var2.canParent();
   }

   @Nullable
   public Camel getBreedOffspring(ServerLevel param0, AgeableMob param1) {
      return EntityType.CAMEL.create(param0);
   }

   @Nullable
   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.CAMEL_EAT;
   }

   @Override
   protected void actuallyHurt(DamageSource param0, float param1) {
      this.standUpPanic();
      super.actuallyHurt(param0, param1);
   }

   @Override
   public void positionRider(Entity param0) {
      int var2 = this.getPassengers().indexOf(param0);
      if (var2 >= 0) {
         boolean var3 = var2 == 0;
         float var4 = 0.5F;
         float var5 = (float)(this.isRemoved() ? 0.01F : this.getBodyAnchorAnimationYOffset(var3, 0.0F) + param0.getMyRidingOffset());
         if (this.getPassengers().size() > 1) {
            if (!var3) {
               var4 = -0.7F;
            }

            if (param0 instanceof Animal) {
               var4 += 0.2F;
            }
         }

         Vec3 var6 = new Vec3(0.0, 0.0, var4).yRot(-this.yBodyRot * (float) (Math.PI / 180.0));
         param0.setPos(this.getX() + var6.x, this.getY() + var5, this.getZ() + var6.z);
         this.clampRotation(param0);
      }
   }

   private double getBodyAnchorAnimationYOffset(boolean param0, float param1) {
      double var3 = this.getPassengersRidingOffset();
      float var5 = this.getScale() * 1.43F;
      float var6 = var5 - this.getScale() * 0.2F;
      float var7 = var5 - var6;
      boolean var8 = this.isInPoseTransition();
      boolean var9 = this.isCamelSitting();
      if (var8) {
         int var10 = var9 ? 40 : 52;
         int var11;
         float var12;
         if (var9) {
            var11 = 28;
            var12 = param0 ? 0.5F : 0.1F;
         } else {
            var11 = param0 ? 24 : 32;
            var12 = param0 ? 0.6F : 0.35F;
         }

         float var13 = Mth.clamp((float)this.getPoseTime() + param1, 0.0F, var10);
         boolean var14 = var13 < var11;
         float var15 = var14 ? var13 / var11 : (var13 - var11) / (var10 - var11);
         float var16 = var5 - var12 * var6;
         var3 += var9
            ? Mth.lerp(var15, var14 ? var5 : var16, var14 ? var16 : var7)
            : Mth.lerp(var15, var14 ? var7 - var5 : var7 - var16, var14 ? var7 - var16 : 0.0F);
      }

      if (var9 && !var8) {
         var3 += var7;
      }

      return var3;
   }

   @Override
   public Vec3 getLeashOffset(float param0) {
      return new Vec3(0.0, this.getBodyAnchorAnimationYOffset(true, param0) - 0.2F * this.getScale(), this.getBbWidth() * 0.56F);
   }

   @Override
   public double getPassengersRidingOffset() {
      return this.getDimensions(this.isCamelSitting() ? Pose.SITTING : Pose.STANDING).height - (this.isBaby() ? 0.35F : 0.6F);
   }

   @Override
   public void onPassengerTurned(Entity param0) {
      if (this.getControllingPassenger() != param0) {
         this.clampRotation(param0);
      }
   }

   private void clampRotation(Entity param0) {
      param0.setYBodyRot(this.getYRot());
      float var2 = param0.getYRot();
      float var3 = Mth.wrapDegrees(var2 - this.getYRot());
      float var4 = Mth.clamp(var3, -160.0F, 160.0F);
      param0.yRotO += var4 - var3;
      float var5 = var2 + var4 - var3;
      param0.setYRot(var5);
      param0.setYHeadRot(var5);
   }

   private void clampHeadRotationToBody(Entity param0, float param1) {
      float var3 = param0.getYHeadRot();
      float var4 = Mth.wrapDegrees(this.yBodyRot - var3);
      float var5 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - var3), -param1, param1);
      float var6 = var3 + var4 - var5;
      param0.setYHeadRot(var6);
   }

   @Override
   public int getMaxHeadYRot() {
      return 30;
   }

   @Override
   protected boolean canAddPassenger(Entity param0) {
      return this.getPassengers().size() <= 2;
   }

   @Nullable
   @Override
   public LivingEntity getControllingPassenger() {
      if (!this.getPassengers().isEmpty() && this.isSaddled()) {
         Entity var1 = this.getPassengers().get(0);
         if (var1 instanceof LivingEntity var2) {
            return var2;
         }
      }

      return null;
   }

   @Override
   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   public boolean isCamelSitting() {
      return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
   }

   public boolean isCamelVisuallySitting() {
      return this.getPoseTime() < 0L != this.isCamelSitting();
   }

   public boolean isInPoseTransition() {
      long var1 = this.getPoseTime();
      return var1 < (this.isCamelSitting() ? 40 : 52);
   }

   private boolean isVisuallySittingDown() {
      return this.isCamelSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
   }

   public void sitDown() {
      if (!this.isCamelSitting()) {
         this.playSound(SoundEvents.CAMEL_SIT, 1.0F, 1.0F);
         this.setPose(Pose.SITTING);
         this.resetLastPoseChangeTick(-this.level.getGameTime());
      }
   }

   public void standUp() {
      if (this.isCamelSitting()) {
         this.playSound(SoundEvents.CAMEL_STAND, 1.0F, 1.0F);
         this.setPose(Pose.STANDING);
         this.resetLastPoseChangeTick(this.level.getGameTime());
      }
   }

   public void standUpPanic() {
      this.setPose(Pose.STANDING);
      this.resetLastPoseChangeTickToFullStand(this.level.getGameTime());
   }

   @VisibleForTesting
   public void resetLastPoseChangeTick(long param0) {
      this.entityData.set(LAST_POSE_CHANGE_TICK, param0);
   }

   private void resetLastPoseChangeTickToFullStand(long param0) {
      this.resetLastPoseChangeTick(Math.max(0L, param0 - 52L - 1L));
   }

   public long getPoseTime() {
      return this.level.getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   @Override
   public SoundEvent getSaddleSoundEvent() {
      return SoundEvents.CAMEL_SADDLE;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      if (!this.firstTick && DASH.equals(param0)) {
         this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(param0);
   }

   @Override
   protected BodyRotationControl createBodyControl() {
      return new Camel.CamelBodyRotationControl(this);
   }

   @Override
   public boolean isTamed() {
      return true;
   }

   @Override
   public void openCustomInventoryScreen(Player param0) {
      if (!this.level.isClientSide) {
         param0.openHorseInventory(this, this.inventory);
      }
   }

   class CamelBodyRotationControl extends BodyRotationControl {
      public CamelBodyRotationControl(Camel param1) {
         super(param1);
      }

      @Override
      public void clientTick() {
         if (!Camel.this.refuseToMove()) {
            super.clientTick();
         }
      }
   }

   class CamelMoveControl extends MoveControl {
      public CamelMoveControl() {
         super(Camel.this);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO && !Camel.this.isLeashed() && Camel.this.isCamelSitting() && !Camel.this.isInPoseTransition()) {
            Camel.this.standUp();
         }

         super.tick();
      }
   }
}
