package net.minecraft.world.entity.monster;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class Creeper extends Monster implements PowerableMob {
   private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private int oldSwell;
   private int swell;
   private int maxSwell = 30;
   private int explosionRadius = 3;
   private int droppedSkulls;

   public Creeper(EntityType<? extends Creeper> param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new SwellGoal(this));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public int getMaxFallDistance() {
      return this.getTarget() == null ? 3 : 3 + (int)(this.getHealth() - 1.0F);
   }

   @Override
   public boolean causeFallDamage(float param0, float param1, DamageSource param2) {
      boolean var4 = super.causeFallDamage(param0, param1, param2);
      this.swell += (int)(param0 * 1.5F);
      if (this.swell > this.maxSwell - 5) {
         this.swell = this.maxSwell - 5;
      }

      return var4;
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_SWELL_DIR, -1);
      this.entityData.define(DATA_IS_POWERED, false);
      this.entityData.define(DATA_IS_IGNITED, false);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      if (this.entityData.get(DATA_IS_POWERED)) {
         param0.putBoolean("powered", true);
      }

      param0.putShort("Fuse", (short)this.maxSwell);
      param0.putByte("ExplosionRadius", (byte)this.explosionRadius);
      param0.putBoolean("ignited", this.isIgnited());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      this.entityData.set(DATA_IS_POWERED, param0.getBoolean("powered"));
      if (param0.contains("Fuse", 99)) {
         this.maxSwell = param0.getShort("Fuse");
      }

      if (param0.contains("ExplosionRadius", 99)) {
         this.explosionRadius = param0.getByte("ExplosionRadius");
      }

      if (param0.getBoolean("ignited")) {
         this.ignite();
      }
   }

   @Override
   public void tick() {
      if (this.isAlive()) {
         this.oldSwell = this.swell;
         if (this.isIgnited()) {
            this.setSwellDir(1);
         }

         int var1 = this.getSwellDir();
         if (var1 > 0 && this.swell == 0) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            this.gameEvent(GameEvent.PRIME_FUSE);
         }

         this.swell += var1;
         if (this.swell < 0) {
            this.swell = 0;
         }

         if (this.swell >= this.maxSwell) {
            this.swell = this.maxSwell;
            this.explodeCreeper();
         }
      }

      super.tick();
   }

   @Override
   public void setTarget(@Nullable LivingEntity param0) {
      if (!(param0 instanceof Goat)) {
         super.setTarget(param0);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.CREEPER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CREEPER_DEATH;
   }

   @Override
   protected void dropCustomDeathLoot(DamageSource param0, int param1, boolean param2) {
      super.dropCustomDeathLoot(param0, param1, param2);
      Entity var4 = param0.getEntity();
      if (var4 != this && var4 instanceof Creeper var5 && var5.canDropMobsSkull()) {
         var5.increaseDroppedSkulls();
         this.spawnAtLocation(Items.CREEPER_HEAD);
      }
   }

   @Override
   public boolean doHurtTarget(Entity param0) {
      return true;
   }

   @Override
   public boolean isPowered() {
      return this.entityData.get(DATA_IS_POWERED);
   }

   public float getSwelling(float param0) {
      return Mth.lerp(param0, this.oldSwell, this.swell) / (this.maxSwell - 2);
   }

   public int getSwellDir() {
      return this.entityData.get(DATA_SWELL_DIR);
   }

   public void setSwellDir(int param0) {
      this.entityData.set(DATA_SWELL_DIR, param0);
   }

   @Override
   public void thunderHit(ServerLevel param0, LightningBolt param1) {
      super.thunderHit(param0, param1);
      this.entityData.set(DATA_IS_POWERED, true);
   }

   @Override
   protected InteractionResult mobInteract(Player param0, InteractionHand param1) {
      ItemStack var3 = param0.getItemInHand(param1);
      if (var3.is(ItemTags.CREEPER_IGNITERS)) {
         SoundEvent var4 = var3.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
         this.level.playSound(param0, this.getX(), this.getY(), this.getZ(), var4, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
         if (!this.level.isClientSide) {
            this.ignite();
            if (!var3.isDamageableItem()) {
               var3.shrink(1);
            } else {
               var3.hurtAndBreak(1, param0, l_param1 -> l_param1.broadcastBreakEvent(param1));
            }
         }

         return InteractionResult.sidedSuccess(this.level.isClientSide);
      } else {
         return super.mobInteract(param0, param1);
      }
   }

   private void explodeCreeper() {
      if (!this.level.isClientSide) {
         float var1 = this.isPowered() ? 2.0F : 1.0F;
         this.dead = true;
         this.level.explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius * var1, Level.ExplosionInteraction.MOB);
         this.discard();
         this.spawnLingeringCloud();
      }
   }

   private void spawnLingeringCloud() {
      Collection<MobEffectInstance> var1 = this.getActiveEffects();
      if (!var1.isEmpty()) {
         AreaEffectCloud var2 = new AreaEffectCloud(this.level, this.getX(), this.getY(), this.getZ());
         var2.setRadius(2.5F);
         var2.setRadiusOnUse(-0.5F);
         var2.setWaitTime(10);
         var2.setDuration(var2.getDuration() / 2);
         var2.setRadiusPerTick(-var2.getRadius() / var2.getDuration());

         for (MobEffectInstance var4 : var1) {
            var2.addEffect(new MobEffectInstance(var4));
         }

         this.level.addFreshEntity(var2);
      }
   }

   public boolean isIgnited() {
      return this.entityData.get(DATA_IS_IGNITED);
   }

   public void ignite() {
      this.entityData.set(DATA_IS_IGNITED, true);
   }

   public boolean canDropMobsSkull() {
      return this.isPowered() && this.droppedSkulls < 1;
   }

   public void increaseDroppedSkulls() {
      this.droppedSkulls++;
   }
}
