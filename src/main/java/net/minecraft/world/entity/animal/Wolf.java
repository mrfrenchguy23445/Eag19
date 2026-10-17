package net.minecraft.world.entity.animal;

import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class Wolf extends TamableAnimal implements NeutralMob {
   private static final EntityDataAccessor<Boolean> DATA_INTERESTED_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
   public static final Predicate<LivingEntity> PREY_SELECTOR = param0 -> {
      EntityType<?> var1 = param0.getType();
      return var1 == EntityType.SHEEP || var1 == EntityType.RABBIT || var1 == EntityType.FOX;
   };
   private static final float START_HEALTH = 8.0F;
   private static final float TAME_HEALTH = 20.0F;
   private float interestedAngle;
   private float interestedAngleO;
   private boolean isWet;
   private boolean isShaking;
   private float shakeAnim;
   private float shakeAnimO;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   @Nullable
   private UUID persistentAngerTarget;

   public Wolf(EntityType<? extends Wolf> param0, Level param1) {
      super(param0, param1);
      this.setTame(false);
      this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, -1.0F);
      this.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new Wolf.WolfPanicGoal(1.5));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(3, new Wolf.WolfAvoidEntityGoal<>(this, Llama.class, 24.0F, 1.5, 1.5));
      this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
      this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F, false));
      this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(9, new BegGoal(this, 8.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Animal.class, false, PREY_SELECTOR));
      this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
      this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
      this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_INTERESTED_ID, false);
      this.entityData.define(DATA_COLLAR_COLOR, DyeColor.RED.getId());
      this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
   }

   @Override
   protected void playStepSound(BlockPos param0, BlockState param1) {
      this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putByte("CollarColor", (byte)this.getCollarColor().getId());
      this.addPersistentAngerSaveData(param0);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("CollarColor", 99)) {
         this.setCollarColor(DyeColor.byId(param0.getInt("CollarColor")));
      }

      this.readPersistentAngerSaveData(this.level, param0);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isAngry()) {
         return SoundEvents.WOLF_GROWL;
      } else if (this.random.nextInt(3) == 0) {
         return this.isTame() && this.getHealth() < 10.0F ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_PANT;
      } else {
         return SoundEvents.WOLF_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.WOLF_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WOLF_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level.isClientSide && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround) {
         this.isShaking = true;
         this.shakeAnim = 0.0F;
         this.shakeAnimO = 0.0F;
         this.level.broadcastEntityEvent(this, (byte)8);
      }

      if (!this.level.isClientSide) {
         this.updatePersistentAnger((ServerLevel)this.level, true);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isAlive()) {
         this.interestedAngleO = this.interestedAngle;
         if (this.isInterested()) {
            this.interestedAngle = this.interestedAngle + (1.0F - this.interestedAngle) * 0.4F;
         } else {
            this.interestedAngle = this.interestedAngle + (0.0F - this.interestedAngle) * 0.4F;
         }

         if (this.isInWaterRainOrBubble()) {
            this.isWet = true;
            if (this.isShaking && !this.level.isClientSide) {
               this.level.broadcastEntityEvent(this, (byte)56);
               this.cancelShake();
            }
         } else if ((this.isWet || this.isShaking) && this.isShaking) {
            if (this.shakeAnim == 0.0F) {
               this.playSound(SoundEvents.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
               this.gameEvent(GameEvent.ENTITY_SHAKE);
            }

            this.shakeAnimO = this.shakeAnim;
            this.shakeAnim += 0.05F;
            if (this.shakeAnimO >= 2.0F) {
               this.isWet = false;
               this.isShaking = false;
               this.shakeAnimO = 0.0F;
               this.shakeAnim = 0.0F;
            }

            if (this.shakeAnim > 0.4F) {
               float var1 = (float)this.getY();
               int var2 = (int)(Mth.sin((this.shakeAnim - 0.4F) * (float) Math.PI) * 7.0F);
               Vec3 var3 = this.getDeltaMovement();

               for (int var4 = 0; var4 < var2; var4++) {
                  float var5 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                  float var6 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                  this.level.addParticle(ParticleTypes.SPLASH, this.getX() + var5, var1 + 0.8F, this.getZ() + var6, var3.x, var3.y, var3.z);
               }
            }
         }
      }
   }

   private void cancelShake() {
      this.isShaking = false;
      this.shakeAnim = 0.0F;
      this.shakeAnimO = 0.0F;
   }

   @Override
   public void die(DamageSource param0) {
      this.isWet = false;
      this.isShaking = false;
      this.shakeAnimO = 0.0F;
      this.shakeAnim = 0.0F;
      super.die(param0);
   }

   public boolean isWet() {
      return this.isWet;
   }

   public float getWetShade(float param0) {
      return Math.min(0.5F + Mth.lerp(param0, this.shakeAnimO, this.shakeAnim) / 2.0F * 0.5F, 1.0F);
   }

   public float getBodyRollAngle(float param0, float param1) {
      float var3 = (Mth.lerp(param0, this.shakeAnimO, this.shakeAnim) + param1) / 1.8F;
      if (var3 < 0.0F) {
         var3 = 0.0F;
      } else if (var3 > 1.0F) {
         var3 = 1.0F;
      }

      return Mth.sin(var3 * (float) Math.PI) * Mth.sin(var3 * (float) Math.PI * 11.0F) * 0.15F * (float) Math.PI;
   }

   public float getHeadRollAngle(float param0) {
      return Mth.lerp(param0, this.interestedAngleO, this.interestedAngle) * 0.15F * (float) Math.PI;
   }

   @Override
   protected float getStandingEyeHeight(Pose param0, EntityDimensions param1) {
      return param1.height * 0.8F;
   }

   @Override
   public int getMaxHeadXRot() {
      return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      Entity var3 = param0.getEntity();
      if (!this.level.isClientSide) {
         this.setOrderedToSit(false);
      }

      if (var3 != null && !(var3 instanceof Player) && !(var3 instanceof AbstractArrow)) {
         param1 = (param1 + 1.0F) / 2.0F;
      }

      return super.hurt(param0, param1);
   }

   @Override
   public boolean doHurtTarget(Entity param0) {
      boolean var2 = param0.hurt(this.damageSources().mobAttack(this), (int)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
      if (var2) {
         this.doEnchantDamageEffects(this, param0);
      }

      return var2;
   }

   @Override
   public void setTame(boolean param0) {
      super.setTame(param0);
      if (param0) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
         this.setHealth(20.0F);
      } else {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
      }

      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
   }

   @Override
   public InteractionResult mobInteract(Player param0, InteractionHand param1) {
      ItemStack var3 = param0.getItemInHand(param1);
      Item var4 = var3.getItem();
      if (this.level.isClientSide) {
         boolean var5 = this.isOwnedBy(param0) || this.isTame() || var3.is(Items.BONE) && !this.isTame() && !this.isAngry();
         return var5 ? InteractionResult.CONSUME : InteractionResult.PASS;
      }

      if (this.isTame()) {
         if (this.isFood(var3) && this.getHealth() < this.getMaxHealth()) {
            if (!param0.getAbilities().instabuild) {
               var3.shrink(1);
            }

            this.heal(var4.getFoodProperties().getNutrition());
            return InteractionResult.SUCCESS;
         }

         if (!(var4 instanceof DyeItem)) {
            InteractionResult var5 = super.mobInteract(param0, param1);
            if ((!var5.consumesAction() || this.isBaby()) && this.isOwnedBy(param0)) {
               this.setOrderedToSit(!this.isOrderedToSit());
               this.jumping = false;
               this.navigation.stop();
               this.setTarget(null);
               return InteractionResult.SUCCESS;
            }

            return var5;
         }

         DyeColor var5 = ((DyeItem)var4).getDyeColor();
         if (var5 != this.getCollarColor()) {
            this.setCollarColor(var5);
            if (!param0.getAbilities().instabuild) {
               var3.shrink(1);
            }

            return InteractionResult.SUCCESS;
         }
      } else if (var3.is(Items.BONE) && !this.isAngry()) {
         if (!param0.getAbilities().instabuild) {
            var3.shrink(1);
         }

         if (this.random.nextInt(3) == 0) {
            this.tame(param0);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level.broadcastEntityEvent(this, (byte)7);
         } else {
            this.level.broadcastEntityEvent(this, (byte)6);
         }

         return InteractionResult.SUCCESS;
      }

      return super.mobInteract(param0, param1);
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 8) {
         this.isShaking = true;
         this.shakeAnim = 0.0F;
         this.shakeAnimO = 0.0F;
      } else if (param0 == 56) {
         this.cancelShake();
      } else {
         super.handleEntityEvent(param0);
      }
   }

   public float getTailAngle() {
      if (this.isAngry()) {
         return 1.5393804F;
      } else {
         return this.isTame() ? (0.55F - (this.getMaxHealth() - this.getHealth()) * 0.02F) * (float) Math.PI : (float) (Math.PI / 5);
      }
   }

   @Override
   public boolean isFood(ItemStack param0) {
      Item var2 = param0.getItem();
      return var2.isEdible() && var2.getFoodProperties().isMeat();
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 8;
   }

   @Override
   public int getRemainingPersistentAngerTime() {
      return this.entityData.get(DATA_REMAINING_ANGER_TIME);
   }

   @Override
   public void setRemainingPersistentAngerTime(int param0) {
      this.entityData.set(DATA_REMAINING_ANGER_TIME, param0);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Nullable
   @Override
   public UUID getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable UUID param0) {
      this.persistentAngerTarget = param0;
   }

   public DyeColor getCollarColor() {
      return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
   }

   public void setCollarColor(DyeColor param0) {
      this.entityData.set(DATA_COLLAR_COLOR, param0.getId());
   }

   @Nullable
   public Wolf getBreedOffspring(ServerLevel param0, AgeableMob param1) {
      Wolf var3 = EntityType.WOLF.create(param0);
      if (var3 != null) {
         UUID var4 = this.getOwnerUUID();
         if (var4 != null) {
            var3.setOwnerUUID(var4);
            var3.setTame(true);
         }
      }

      return var3;
   }

   public void setIsInterested(boolean param0) {
      this.entityData.set(DATA_INTERESTED_ID, param0);
   }

   @Override
   public boolean canMate(Animal param0) {
      if (param0 == this) {
         return false;
      } else if (!this.isTame()) {
         return false;
      } else if (!(param0 instanceof Wolf var2)) {
         return false;
      } else if (!var2.isTame()) {
         return false;
      } else {
         return var2.isInSittingPose() ? false : this.isInLove() && var2.isInLove();
      }
   }

   public boolean isInterested() {
      return this.entityData.get(DATA_INTERESTED_ID);
   }

   @Override
   public boolean wantsToAttack(LivingEntity param0, LivingEntity param1) {
      if (param0 instanceof Creeper || param0 instanceof Ghast) {
         return false;
      } else if (param0 instanceof Wolf var3) {
         return !var3.isTame() || var3.getOwner() != param1;
      } else if (param0 instanceof Player && param1 instanceof Player && !((Player)param1).canHarmPlayer((Player)param0)) {
         return false;
      } else {
         return param0 instanceof AbstractHorse && ((AbstractHorse)param0).isTamed()
            ? false
            : !(param0 instanceof TamableAnimal) || !((TamableAnimal)param0).isTame();
      }
   }

   @Override
   public boolean canBeLeashed(Player param0) {
      return !this.isAngry() && super.canBeLeashed(param0);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   public static boolean checkWolfSpawnRules(EntityType<Wolf> param0, LevelAccessor param1, MobSpawnType param2, BlockPos param3, RandomSource param4) {
      return param1.getBlockState(param3.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(param1, param3);
   }

   class WolfAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Wolf wolf;

      public WolfAvoidEntityGoal(Wolf param1, Class<T> param2, float param3, double param4, double param6) {
         super(param1, param2, param3, param4, param6);
         this.wolf = param1;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.toAvoid instanceof Llama ? !this.wolf.isTame() && this.avoidLlama((Llama)this.toAvoid) : false;
      }

      private boolean avoidLlama(Llama param0) {
         return param0.getStrength() >= Wolf.this.random.nextInt(5);
      }

      @Override
      public void start() {
         Wolf.this.setTarget(null);
         super.start();
      }

      @Override
      public void tick() {
         Wolf.this.setTarget(null);
         super.tick();
      }
   }

   class WolfPanicGoal extends PanicGoal {
      public WolfPanicGoal(double param1) {
         super(Wolf.this, param1);
      }

      @Override
      protected boolean shouldPanic() {
         return this.mob.isFreezing() || this.mob.isOnFire();
      }
   }
}
