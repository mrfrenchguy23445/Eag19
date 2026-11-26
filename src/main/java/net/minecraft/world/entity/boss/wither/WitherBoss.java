package net.minecraft.world.entity.boss.wither;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WitherBoss extends Monster implements PowerableMob, RangedAttackMob {
   private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TARGET_B = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TARGET_C = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
   private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
   private static final int INVULNERABLE_TICKS = 220;
   private final float[] xRotHeads = new float[2];
   private final float[] yRotHeads = new float[2];
   private final float[] xRotOHeads = new float[2];
   private final float[] yRotOHeads = new float[2];
   private final int[] nextHeadUpdate = new int[2];
   private final int[] idleHeadUpdates = new int[2];
   private int destroyBlocksTick;
   private final ServerBossEvent bossEvent = (ServerBossEvent)new ServerBossEvent(
         this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS
      )
      .setDarkenScreen(true);
   private static final Predicate<LivingEntity> LIVING_ENTITY_SELECTOR = param0 -> param0.getMobType() != MobType.UNDEAD && param0.attackable();
   private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(20.0).selector(LIVING_ENTITY_SELECTOR);

   public WitherBoss(EntityType<? extends WitherBoss> param0, Level param1) {
      super(param0, param1);
      this.moveControl = new FlyingMoveControl(this, 10, false);
      this.setHealth(this.getMaxHealth());
      this.xpReward = 50;
   }

   @Override
   protected PathNavigation createNavigation(Level param0) {
      FlyingPathNavigation var2 = new FlyingPathNavigation(this, param0);
      var2.setCanOpenDoors(false);
      var2.setCanFloat(true);
      var2.setCanPassDoors(true);
      return var2;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new WitherBoss.WitherDoNothingGoal());
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 20.0F));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_TARGET_A, 0);
      this.entityData.define(DATA_TARGET_B, 0);
      this.entityData.define(DATA_TARGET_C, 0);
      this.entityData.define(DATA_ID_INV, 0);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putInt("Invul", this.getInvulnerableTicks());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      this.setInvulnerableTicks(param0.getInt("Invul"));
      if (this.hasCustomName()) {
         this.bossEvent.setName(this.getDisplayName());
      }
   }

   @Override
   public void setCustomName(@Nullable Component param0) {
      super.setCustomName(param0);
      this.bossEvent.setName(this.getDisplayName());
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.WITHER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.WITHER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WITHER_DEATH;
   }

   @Override
   public void aiStep() {
      Vec3 var1 = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
      if (!this.level.isClientSide && this.getAlternativeTarget(0) > 0) {
         Entity var2 = this.level.getEntity(this.getAlternativeTarget(0));
         if (var2 != null) {
            double var3 = var1.y;
            if (this.getY() < var2.getY() || !this.isPowered() && this.getY() < var2.getY() + 5.0) {
               var3 = Math.max(0.0, var3);
               var3 += 0.3 - var3 * 0.6F;
            }

            var1 = new Vec3(var1.x, var3, var1.z);
            Vec3 var5 = new Vec3(var2.getX() - this.getX(), 0.0, var2.getZ() - this.getZ());
            if (var5.horizontalDistanceSqr() > 9.0) {
               Vec3 var6 = var5.normalize();
               var1 = var1.add(var6.x * 0.3 - var1.x * 0.6, 0.0, var6.z * 0.3 - var1.z * 0.6);
            }
         }
      }

      this.setDeltaMovement(var1);
      if (var1.horizontalDistanceSqr() > 0.05) {
         this.setYRot((float)Mth.atan2(var1.z, var1.x) * (180.0F / (float)Math.PI) - 90.0F);
      }

      super.aiStep();

      for (int var2 = 0; var2 < 2; var2++) {
         this.yRotOHeads[var2] = this.yRotHeads[var2];
         this.xRotOHeads[var2] = this.xRotHeads[var2];
      }

      for (int var2 = 0; var2 < 2; var2++) {
         int var3 = this.getAlternativeTarget(var2 + 1);
         Entity var4 = null;
         if (var3 > 0) {
            var4 = this.level.getEntity(var3);
         }

         if (var4 != null) {
            double var5 = this.getHeadX(var2 + 1);
            double var7 = this.getHeadY(var2 + 1);
            double var9 = this.getHeadZ(var2 + 1);
            double var11 = var4.getX() - var5;
            double var13 = var4.getEyeY() - var7;
            double var15 = var4.getZ() - var9;
            double var17 = Math.sqrt(var11 * var11 + var15 * var15);
            float var19 = (float)(Mth.atan2(var15, var11) * 180.0F / (float)Math.PI) - 90.0F;
            float var20 = (float)(-(Mth.atan2(var13, var17) * 180.0F / (float)Math.PI));
            this.xRotHeads[var2] = this.rotlerp(this.xRotHeads[var2], var20, 40.0F);
            this.yRotHeads[var2] = this.rotlerp(this.yRotHeads[var2], var19, 10.0F);
         } else {
            this.yRotHeads[var2] = this.rotlerp(this.yRotHeads[var2], this.yBodyRot, 10.0F);
         }
      }

      boolean var2 = this.isPowered();

      for (int var3 = 0; var3 < 3; var3++) {
         double var4 = this.getHeadX(var3);
         double var6 = this.getHeadY(var3);
         double var8 = this.getHeadZ(var3);
         this.level
            .addParticle(
               ParticleTypes.SMOKE,
               var4 + this.random.nextGaussian() * 0.3F,
               var6 + this.random.nextGaussian() * 0.3F,
               var8 + this.random.nextGaussian() * 0.3F,
               0.0,
               0.0,
               0.0
            );
         if (var2 && this.level.random.nextInt(4) == 0) {
            this.level
               .addParticle(
                  ParticleTypes.ENTITY_EFFECT,
                  var4 + this.random.nextGaussian() * 0.3F,
                  var6 + this.random.nextGaussian() * 0.3F,
                  var8 + this.random.nextGaussian() * 0.3F,
                  0.7F,
                  0.7F,
                  0.5
               );
         }
      }

      if (this.getInvulnerableTicks() > 0) {
         for (int var3 = 0; var3 < 3; var3++) {
            this.level
               .addParticle(
                  ParticleTypes.ENTITY_EFFECT,
                  this.getX() + this.random.nextGaussian(),
                  this.getY() + this.random.nextFloat() * 3.3F,
                  this.getZ() + this.random.nextGaussian(),
                  0.7F,
                  0.7F,
                  0.9F
               );
         }
      }
   }

   @Override
   protected void customServerAiStep() {
      if (this.getInvulnerableTicks() > 0) {
         int var1 = this.getInvulnerableTicks() - 1;
         this.bossEvent.setProgress(1.0F - var1 / 220.0F);
         if (var1 <= 0) {
            this.level.explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
            if (!this.isSilent()) {
               this.level.globalLevelEvent(1023, this.blockPosition(), 0);
            }
         }

         this.setInvulnerableTicks(var1);
         if (this.tickCount % 10 == 0) {
            this.heal(10.0F);
         }
      } else {
         super.customServerAiStep();

         for (int var1 = 1; var1 < 3; var1++) {
            if (this.tickCount >= this.nextHeadUpdate[var1 - 1]) {
               this.nextHeadUpdate[var1 - 1] = this.tickCount + 10 + this.random.nextInt(10);
               if ((this.level.getDifficulty() == Difficulty.NORMAL || this.level.getDifficulty() == Difficulty.HARD) && this.idleHeadUpdates[var1 - 1]++ > 15) {
                  float var2 = 10.0F;
                  float var3 = 5.0F;
                  double var4 = Mth.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                  double var6 = Mth.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                  double var8 = Mth.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                  this.performRangedAttack(var1 + 1, var4, var6, var8, true);
                  this.idleHeadUpdates[var1 - 1] = 0;
               }

               int var2 = this.getAlternativeTarget(var1);
               if (var2 > 0) {
                  LivingEntity var3 = (LivingEntity)this.level.getEntity(var2);
                  if (var3 != null && this.canAttack(var3) && !(this.distanceToSqr(var3) > 900.0) && this.hasLineOfSight(var3)) {
                     this.performRangedAttack(var1 + 1, var3);
                     this.nextHeadUpdate[var1 - 1] = this.tickCount + 40 + this.random.nextInt(20);
                     this.idleHeadUpdates[var1 - 1] = 0;
                  } else {
                     this.setAlternativeTarget(var1, 0);
                  }
               } else {
                  List<LivingEntity> var3 = this.level
                     .getNearbyEntities(LivingEntity.class, TARGETING_CONDITIONS, this, this.getBoundingBox().inflate(20.0, 8.0, 20.0));
                  if (!var3.isEmpty()) {
                     LivingEntity var4 = var3.get(this.random.nextInt(var3.size()));
                     this.setAlternativeTarget(var1, var4.getId());
                  }
               }
            }
         }

         if (this.getTarget() != null) {
            this.setAlternativeTarget(0, this.getTarget().getId());
         } else {
            this.setAlternativeTarget(0, 0);
         }

         if (this.destroyBlocksTick > 0) {
            this.destroyBlocksTick--;
            if (this.destroyBlocksTick == 0 && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
               int var1 = Mth.floor(this.getY());
               int var2 = Mth.floor(this.getX());
               int var3 = Mth.floor(this.getZ());
               boolean var4 = false;

               for (int var5 = -1; var5 <= 1; var5++) {
                  for (int var6 = -1; var6 <= 1; var6++) {
                     for (int var7 = 0; var7 <= 3; var7++) {
                        int var8 = var2 + var5;
                        int var9 = var1 + var7;
                        int var10 = var3 + var6;
                        BlockPos var11 = new BlockPos(var8, var9, var10);
                        BlockState var12 = this.level.getBlockState(var11);
                        if (canDestroy(var12)) {
                           var4 = this.level.destroyBlock(var11, true, this) || var4;
                        }
                     }
                  }
               }

               if (var4) {
                  this.level.levelEvent(null, 1022, this.blockPosition(), 0);
               }
            }
         }

         if (this.tickCount % 20 == 0) {
            this.heal(1.0F);
         }

         this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
      }
   }

   public static boolean canDestroy(BlockState param0) {
      return !param0.isAir() && !param0.is(BlockTags.WITHER_IMMUNE);
   }

   public void makeInvulnerable() {
      this.setInvulnerableTicks(220);
      this.bossEvent.setProgress(0.0F);
      this.setHealth(this.getMaxHealth() / 3.0F);
   }

   @Override
   public void makeStuckInBlock(BlockState param0, Vec3 param1) {
   }

   @Override
   public void startSeenByPlayer(ServerPlayer param0) {
      super.startSeenByPlayer(param0);
      this.bossEvent.addPlayer(param0);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer param0) {
      super.stopSeenByPlayer(param0);
      this.bossEvent.removePlayer(param0);
   }

   private double getHeadX(int param0) {
      if (param0 <= 0) {
         return this.getX();
      }

      float var2 = (this.yBodyRot + 180 * (param0 - 1)) * (float) (Math.PI / 180.0);
      float var3 = Mth.cos(var2);
      return this.getX() + var3 * 1.3;
   }

   private double getHeadY(int param0) {
      return param0 <= 0 ? this.getY() + 3.0 : this.getY() + 2.2;
   }

   private double getHeadZ(int param0) {
      if (param0 <= 0) {
         return this.getZ();
      }

      float var2 = (this.yBodyRot + 180 * (param0 - 1)) * (float) (Math.PI / 180.0);
      float var3 = Mth.sin(var2);
      return this.getZ() + var3 * 1.3;
   }

   private float rotlerp(float param0, float param1, float param2) {
      float var4 = Mth.wrapDegrees(param1 - param0);
      if (var4 > param2) {
         var4 = param2;
      }

      if (var4 < -param2) {
         var4 = -param2;
      }

      return param0 + var4;
   }

   private void performRangedAttack(int param0, LivingEntity param1) {
      this.performRangedAttack(
         param0, param1.getX(), param1.getY() + param1.getEyeHeight() * 0.5, param1.getZ(), param0 == 0 && this.random.nextFloat() < 0.001F
      );
   }

   private void performRangedAttack(int param0, double param1, double param3, double param5, boolean param7) {
      if (!this.isSilent()) {
         this.level.levelEvent(null, 1024, this.blockPosition(), 0);
      }

      double var9 = this.getHeadX(param0);
      double var11 = this.getHeadY(param0);
      double var13 = this.getHeadZ(param0);
      double var15 = param1 - var9;
      double var17 = param3 - var11;
      double var19 = param5 - var13;
      WitherSkull var21 = new WitherSkull(this.level, this, var15, var17, var19);
      var21.setOwner(this);
      if (param7) {
         var21.setDangerous(true);
      }

      var21.setPosRaw(var9, var11, var13);
      this.level.addFreshEntity(var21);
   }

   @Override
   public void performRangedAttack(LivingEntity param0, float param1) {
      this.performRangedAttack(0, param0);
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (param0.is(DamageTypeTags.WITHER_IMMUNE_TO) || param0.getEntity() instanceof WitherBoss) {
         return false;
      }

      if (this.getInvulnerableTicks() > 0 && !param0.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      if (this.isPowered()) {
         Entity var3 = param0.getDirectEntity();
         if (var3 instanceof AbstractArrow) {
            return false;
         }
      }

      Entity var3 = param0.getEntity();
      if (var3 != null && !(var3 instanceof Player) && var3 instanceof LivingEntity && ((LivingEntity)var3).getMobType() == this.getMobType()) {
         return false;
      }

      if (this.destroyBlocksTick <= 0) {
         this.destroyBlocksTick = 20;
      }

      for (int var4 = 0; var4 < this.idleHeadUpdates.length; var4++) {
         this.idleHeadUpdates[var4] = this.idleHeadUpdates[var4] + 3;
      }

      return super.hurt(param0, param1);
   }

   @Override
   protected void dropCustomDeathLoot(DamageSource param0, int param1, boolean param2) {
      super.dropCustomDeathLoot(param0, param1, param2);
      ItemEntity var4 = this.spawnAtLocation(Items.NETHER_STAR);
      if (var4 != null) {
         var4.setExtendedLifetime();
      }
   }

   @Override
   public void checkDespawn() {
      if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
         this.discard();
      } else {
         this.noActionTime = 0;
      }
   }

   @Override
   public boolean addEffect(MobEffectInstance param0, @Nullable Entity param1) {
      return false;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 300.0)
         .add(Attributes.MOVEMENT_SPEED, 0.6F)
         .add(Attributes.FLYING_SPEED, 0.6F)
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.ARMOR, 4.0);
   }

   public float getHeadYRot(int param0) {
      return this.yRotHeads[param0];
   }

   public float getHeadXRot(int param0) {
      return this.xRotHeads[param0];
   }

   public int getInvulnerableTicks() {
      return this.entityData.get(DATA_ID_INV);
   }

   public void setInvulnerableTicks(int param0) {
      this.entityData.set(DATA_ID_INV, param0);
   }

   public int getAlternativeTarget(int param0) {
      return this.entityData.get(DATA_TARGETS.get(param0));
   }

   public void setAlternativeTarget(int param0, int param1) {
      this.entityData.set(DATA_TARGETS.get(param0), param1);
   }

   @Override
   public boolean isPowered() {
      return this.getHealth() <= this.getMaxHealth() / 2.0F;
   }

   @Override
   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   @Override
   protected boolean canRide(Entity param0) {
      return false;
   }

   @Override
   public boolean canChangeDimensions() {
      return false;
   }

   @Override
   public boolean canBeAffected(MobEffectInstance param0) {
      return param0.getEffect() == MobEffects.WITHER ? false : super.canBeAffected(param0);
   }

   class WitherDoNothingGoal extends Goal {
      public WitherDoNothingGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return WitherBoss.this.getInvulnerableTicks() > 0;
      }
   }
}
