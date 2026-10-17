package net.minecraft.world.entity.monster;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Witch extends Raider implements RangedAttackMob {
   private static final UUID SPEED_MODIFIER_DRINKING_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
   private static final AttributeModifier SPEED_MODIFIER_DRINKING = new AttributeModifier(
      SPEED_MODIFIER_DRINKING_UUID, "Drinking speed penalty", -0.25, AttributeModifier.Operation.ADDITION
   );
   private static final EntityDataAccessor<Boolean> DATA_USING_ITEM = SynchedEntityData.defineId(Witch.class, EntityDataSerializers.BOOLEAN);
   private int usingTime;
   private NearestHealableRaiderTargetGoal<Raider> healRaidersGoal;
   private NearestAttackableWitchTargetGoal<Player> attackPlayersGoal;

   public Witch(EntityType<? extends Witch> param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.healRaidersGoal = new NearestHealableRaiderTargetGoal<>(
         this, Raider.class, true, l_param0 -> l_param0 != null && this.hasActiveRaid() && l_param0.getType() != EntityType.WITCH
      );
      this.attackPlayersGoal = new NearestAttackableWitchTargetGoal<>(this, Player.class, 10, true, false, null);
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 60, 10.0F));
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class));
      this.targetSelector.addGoal(2, this.healRaidersGoal);
      this.targetSelector.addGoal(3, this.attackPlayersGoal);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.getEntityData().define(DATA_USING_ITEM, false);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.WITCH_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.WITCH_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WITCH_DEATH;
   }

   public void setUsingItem(boolean param0) {
      this.getEntityData().set(DATA_USING_ITEM, param0);
   }

   public boolean isDrinkingPotion() {
      return this.getEntityData().get(DATA_USING_ITEM);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public void aiStep() {
      if (!this.level.isClientSide && this.isAlive()) {
         this.healRaidersGoal.decrementCooldown();
         if (this.healRaidersGoal.getCooldown() <= 0) {
            this.attackPlayersGoal.setCanAttack(true);
         } else {
            this.attackPlayersGoal.setCanAttack(false);
         }

         if (this.isDrinkingPotion()) {
            if (this.usingTime-- <= 0) {
               this.setUsingItem(false);
               ItemStack var1 = this.getMainHandItem();
               this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
               if (var1.is(Items.POTION)) {
                  List<MobEffectInstance> var2 = PotionUtils.getMobEffects(var1);
                  if (var2 != null) {
                     for (MobEffectInstance var4 : var2) {
                        this.addEffect(new MobEffectInstance(var4));
                     }
                  }
               }

               this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_DRINKING);
            }
         } else {
            Potion var1 = null;
            if (this.random.nextFloat() < 0.15F && this.isEyeInFluid(FluidTags.WATER) && !this.hasEffect(MobEffects.WATER_BREATHING)) {
               var1 = Potions.WATER_BREATHING;
            } else if (this.random.nextFloat() < 0.15F
               && (this.isOnFire() || this.getLastDamageSource() != null && this.getLastDamageSource().is(DamageTypeTags.IS_FIRE))
               && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
               var1 = Potions.FIRE_RESISTANCE;
            } else if (this.random.nextFloat() < 0.05F && this.getHealth() < this.getMaxHealth()) {
               var1 = Potions.HEALING;
            } else if (this.random.nextFloat() < 0.5F
               && this.getTarget() != null
               && !this.hasEffect(MobEffects.MOVEMENT_SPEED)
               && this.getTarget().distanceToSqr(this) > 121.0) {
               var1 = Potions.SWIFTNESS;
            }

            if (var1 != null) {
               this.setItemSlot(EquipmentSlot.MAINHAND, PotionUtils.setPotion(new ItemStack(Items.POTION), var1));
               this.usingTime = this.getMainHandItem().getUseDuration();
               this.setUsingItem(true);
               if (!this.isSilent()) {
                  this.level
                     .playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.WITCH_DRINK,
                        this.getSoundSource(),
                        1.0F,
                        0.8F + this.random.nextFloat() * 0.4F
                     );
               }

               AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
               var2.removeModifier(SPEED_MODIFIER_DRINKING);
               var2.addTransientModifier(SPEED_MODIFIER_DRINKING);
            }
         }

         if (this.random.nextFloat() < 7.5E-4F) {
            this.level.broadcastEntityEvent(this, (byte)15);
         }
      }

      super.aiStep();
   }

   @Override
   public SoundEvent getCelebrateSound() {
      return SoundEvents.WITCH_CELEBRATE;
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 15) {
         for (int var2 = 0; var2 < this.random.nextInt(35) + 10; var2++) {
            this.level
               .addParticle(
                  ParticleTypes.WITCH,
                  this.getX() + this.random.nextGaussian() * 0.13F,
                  this.getBoundingBox().maxY + 0.5 + this.random.nextGaussian() * 0.13F,
                  this.getZ() + this.random.nextGaussian() * 0.13F,
                  0.0,
                  0.0,
                  0.0
               );
         }
      } else {
         super.handleEntityEvent(param0);
      }
   }

   @Override
   protected float getDamageAfterMagicAbsorb(DamageSource param0, float param1) {
      param1 = super.getDamageAfterMagicAbsorb(param0, param1);
      if (param0.getEntity() == this) {
         param1 = 0.0F;
      }

      if (param0.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
         param1 *= 0.15F;
      }

      return param1;
   }

   @Override
   public void performRangedAttack(LivingEntity param0, float param1) {
      if (!this.isDrinkingPotion()) {
         Vec3 var3 = param0.getDeltaMovement();
         double var4 = param0.getX() + var3.x - this.getX();
         double var6 = param0.getEyeY() - 1.1F - this.getY();
         double var8 = param0.getZ() + var3.z - this.getZ();
         double var10 = Math.sqrt(var4 * var4 + var8 * var8);
         Potion var12 = Potions.HARMING;
         if (param0 instanceof Raider) {
            if (param0.getHealth() <= 4.0F) {
               var12 = Potions.HEALING;
            } else {
               var12 = Potions.REGENERATION;
            }

            this.setTarget(null);
         } else if (var10 >= 8.0 && !param0.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            var12 = Potions.SLOWNESS;
         } else if (param0.getHealth() >= 8.0F && !param0.hasEffect(MobEffects.POISON)) {
            var12 = Potions.POISON;
         } else if (var10 <= 3.0 && !param0.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
            var12 = Potions.WEAKNESS;
         }

         ThrownPotion var13 = new ThrownPotion(this.level, this);
         var13.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), var12));
         var13.setXRot(var13.getXRot() - -20.0F);
         var13.shoot(var4, var6 + var10 * 0.2, var8, 0.75F, 8.0F);
         if (!this.isSilent()) {
            this.level
               .playSound(
                  null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F
               );
         }

         this.level.addFreshEntity(var13);
      }
   }

   @Override
   protected float getStandingEyeHeight(Pose param0, EntityDimensions param1) {
      return 1.62F;
   }

   @Override
   public void applyRaidBuffs(int param0, boolean param1) {
   }

   @Override
   public boolean canBeLeader() {
      return false;
   }
}
