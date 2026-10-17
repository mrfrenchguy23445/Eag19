package net.minecraft.world.entity;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final UUID SPEED_MODIFIER_SPRINTING_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
   private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
   private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
   private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
      SPEED_MODIFIER_SPRINTING_UUID, "Sprinting speed boost", 0.3F, AttributeModifier.Operation.MULTIPLY_TOTAL
   );
   public static final int HAND_SLOTS = 2;
   public static final int ARMOR_SLOTS = 4;
   public static final int EQUIPMENT_SLOT_OFFSET = 98;
   public static final int ARMOR_SLOT_OFFSET = 100;
   public static final int SWING_DURATION = 6;
   public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
   private static final int DAMAGE_SOURCE_TIMEOUT = 40;
   public static final double MIN_MOVEMENT_DISTANCE = 0.003;
   public static final double DEFAULT_BASE_GRAVITY = 0.08;
   public static final int DEATH_DURATION = 20;
   private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
   private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
   private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
   public static final int USE_ITEM_INTERVAL = 4;
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
   protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
   protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
   protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
   protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
      LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
   );
   protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
   protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F);
   public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
   private static final int MAX_HEAD_ROTATION_RELATIVE_TO_BODY = 50;
   private final AttributeMap attributes;
   private final CombatTracker combatTracker = new CombatTracker(this);
   private final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();
   private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
   private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
   public boolean swinging;
   private boolean discardFriction = false;
   public InteractionHand swingingArm;
   public int swingTime;
   public int removeArrowTime;
   public int removeStingerTime;
   public int hurtTime;
   public int hurtDuration;
   public int deathTime;
   public float oAttackAnim;
   public float attackAnim;
   protected int attackStrengthTicker;
   public final WalkAnimationState walkAnimation = new WalkAnimationState();
   public final int invulnerableDuration = 20;
   public final float timeOffs;
   public final float rotA;
   public float yBodyRot;
   public float yBodyRotO;
   public float yHeadRot;
   public float yHeadRotO;
   @Nullable
   protected Player lastHurtByPlayer;
   protected int lastHurtByPlayerTime;
   protected boolean dead;
   protected int noActionTime;
   protected float oRun;
   protected float run;
   protected float animStep;
   protected float animStepO;
   protected float rotOffs;
   protected int deathScore;
   protected float lastHurt;
   protected boolean jumping;
   public float xxa;
   public float yya;
   public float zza;
   protected int lerpSteps;
   protected double lerpX;
   protected double lerpY;
   protected double lerpZ;
   protected double lerpYRot;
   protected double lerpXRot;
   protected double lyHeadRot;
   protected int lerpHeadSteps;
   private boolean effectsDirty = true;
   @Nullable
   private LivingEntity lastHurtByMob;
   private int lastHurtByMobTimestamp;
   private LivingEntity lastHurtMob;
   private int lastHurtMobTimestamp;
   private float speed;
   private int noJumpDelay;
   private float absorptionAmount;
   protected ItemStack useItem = ItemStack.EMPTY;
   protected int useItemRemaining;
   protected int fallFlyTicks;
   private BlockPos lastPos;
   private Optional<BlockPos> lastClimbablePos = Optional.empty();
   @Nullable
   private DamageSource lastDamageSource;
   private long lastDamageStamp;
   protected int autoSpinAttackTicks;
   private float swimAmount;
   private float swimAmountO;
   protected Brain<?> brain;
   private boolean skipDropExperience;

   protected LivingEntity(EntityType<? extends LivingEntity> param0, Level param1) {
      super(param0, param1);
      this.attributes = new AttributeMap(DefaultAttributes.getSupplier(param0));
      this.setHealth(this.getMaxHealth());
      this.blocksBuilding = true;
      this.rotA = (float)((Math.random() + 1.0) * 0.01F);
      this.reapplyPosition();
      this.timeOffs = (float)Math.random() * 12398.0F;
      this.setYRot((float)(Math.random() * (float) (Math.PI * 2)));
      this.yHeadRot = this.getYRot();
      this.setMaxUpStep(0.6F);
      NbtOps var3 = NbtOps.INSTANCE;
      this.brain = this.makeBrain(new Dynamic<>(var3, var3.createMap(ImmutableMap.of(var3.createString("memories"), var3.emptyMap()))));
   }

   public Brain<?> getBrain() {
      return this.brain;
   }

   protected Brain.Provider<?> brainProvider() {
      return Brain.provider(ImmutableList.of(), ImmutableList.of());
   }

   protected Brain<?> makeBrain(Dynamic<?> param0) {
      return this.brainProvider().makeBrain(param0);
   }

   @Override
   public void kill() {
      this.hurt(this.damageSources().outOfWorld(), Float.MAX_VALUE);
   }

   public boolean canAttackType(EntityType<?> param0) {
      return true;
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
      this.entityData.define(DATA_EFFECT_COLOR_ID, 0);
      this.entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.define(DATA_ARROW_COUNT_ID, 0);
      this.entityData.define(DATA_STINGER_COUNT_ID, 0);
      this.entityData.define(DATA_HEALTH_ID, 1.0F);
      this.entityData.define(SLEEPING_POS_ID, Optional.empty());
   }

   public static AttributeSupplier.Builder createLivingAttributes() {
      return AttributeSupplier.builder()
         .add(Attributes.MAX_HEALTH)
         .add(Attributes.KNOCKBACK_RESISTANCE)
         .add(Attributes.MOVEMENT_SPEED)
         .add(Attributes.ARMOR)
         .add(Attributes.ARMOR_TOUGHNESS);
   }

   @Override
   protected void checkFallDamage(double param0, boolean param2, BlockState param3, BlockPos param4) {
      if (!this.isInWater()) {
         this.updateInWaterStateAndDoWaterCurrentPushing();
      }

      if (!this.level.isClientSide && param2 && this.fallDistance > 0.0F) {
         this.removeSoulSpeed();
         this.tryAddSoulSpeed();
      }

      if (!this.level.isClientSide && this.fallDistance > 3.0F && param2) {
         float var6 = Mth.ceil(this.fallDistance - 3.0F);
         if (!param3.isAir()) {
            double var7 = Math.min(0.2F + var6 / 15.0F, 2.5);
            int var9 = (int)(150.0 * var7);
            ((ServerLevel)this.level)
               .sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, param3), this.getX(), this.getY(), this.getZ(), var9, 0.0, 0.0, 0.0, 0.15F);
         }
      }

      super.checkFallDamage(param0, param2, param3, param4);
   }

   public boolean canBreatheUnderwater() {
      return this.getMobType() == MobType.UNDEAD;
   }

   public float getSwimAmount(float param0) {
      return Mth.lerp(param0, this.swimAmountO, this.swimAmount);
   }

   @Override
   public void baseTick() {
      this.oAttackAnim = this.attackAnim;
      if (this.firstTick) {
         this.getSleepingPos().ifPresent(this::setPosToBed);
      }

      if (this.canSpawnSoulSpeedParticle()) {
         this.spawnSoulSpeedParticle();
      }

      super.baseTick();
      this.level.getProfiler().push("livingEntityBaseTick");
      if (this.fireImmune() || this.level.isClientSide) {
         this.clearFire();
      }

      if (this.isAlive()) {
         boolean var1 = this instanceof Player;
         if (!this.level.isClientSide) {
            if (this.isInWall()) {
               this.hurt(this.damageSources().inWall(), 1.0F);
            } else if (var1 && !this.level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
               double var2 = this.level.getWorldBorder().getDistanceToBorder(this) + this.level.getWorldBorder().getDamageSafeZone();
               if (var2 < 0.0) {
                  double var4 = this.level.getWorldBorder().getDamagePerBlock();
                  if (var4 > 0.0) {
                     this.hurt(this.damageSources().inWall(), Math.max(1, Mth.floor(-var2 * var4)));
                  }
               }
            }
         }

         if (this.isEyeInFluid(FluidTags.WATER)
            && !this.level.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            boolean var2 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!var1 || !((Player)this).getAbilities().invulnerable);
            if (var2) {
               this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
               if (this.getAirSupply() == -20) {
                  this.setAirSupply(0);
                  Vec3 var3 = this.getDeltaMovement();

                  for (int var4 = 0; var4 < 8; var4++) {
                     double var5 = this.random.nextDouble() - this.random.nextDouble();
                     double var7 = this.random.nextDouble() - this.random.nextDouble();
                     double var9 = this.random.nextDouble() - this.random.nextDouble();
                     this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + var5, this.getY() + var7, this.getZ() + var9, var3.x, var3.y, var3.z);
                  }

                  this.hurt(this.damageSources().drown(), 2.0F);
               }
            }

            if (!this.level.isClientSide && this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
               this.stopRiding();
            }
         } else if (this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
         }

         if (!this.level.isClientSide) {
            BlockPos var2 = this.blockPosition();
            if (!Objects.equal(this.lastPos, var2)) {
               this.lastPos = var2;
               this.onChangedBlock(var2);
            }
         }
      }

      if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
         this.extinguishFire();
      }

      if (this.hurtTime > 0) {
         this.hurtTime--;
      }

      if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
         this.invulnerableTime--;
      }

      if (this.isDeadOrDying() && this.level.shouldTickDeath(this)) {
         this.tickDeath();
      }

      if (this.lastHurtByPlayerTime > 0) {
         this.lastHurtByPlayerTime--;
      } else {
         this.lastHurtByPlayer = null;
      }

      if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
         this.lastHurtMob = null;
      }

      if (this.lastHurtByMob != null) {
         if (!this.lastHurtByMob.isAlive()) {
            this.setLastHurtByMob(null);
         } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
            this.setLastHurtByMob(null);
         }
      }

      this.tickEffects();
      this.animStepO = this.animStep;
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.level.getProfiler().pop();
   }

   public boolean canSpawnSoulSpeedParticle() {
      return this.tickCount % 5 == 0
         && this.getDeltaMovement().x != 0.0
         && this.getDeltaMovement().z != 0.0
         && !this.isSpectator()
         && EnchantmentHelper.hasSoulSpeed(this)
         && this.onSoulSpeedBlock();
   }

   protected void spawnSoulSpeedParticle() {
      Vec3 var1 = this.getDeltaMovement();
      this.level
         .addParticle(
            ParticleTypes.SOUL,
            this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
            this.getY() + 0.1,
            this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
            var1.x * -0.2,
            0.1,
            var1.z * -0.2
         );
      float var2 = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;
      this.playSound(SoundEvents.SOUL_ESCAPE, var2, 0.6F + this.random.nextFloat() * 0.4F);
   }

   protected boolean onSoulSpeedBlock() {
      return this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.SOUL_SPEED_BLOCKS);
   }

   @Override
   protected float getBlockSpeedFactor() {
      return this.onSoulSpeedBlock() && EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
   }

   protected boolean shouldRemoveSoulSpeed(BlockState param0) {
      return !param0.isAir() || this.isFallFlying();
   }

   protected void removeSoulSpeed() {
      AttributeInstance var1 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var1 != null) {
         if (var1.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
            var1.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
         }
      }
   }

   protected void tryAddSoulSpeed() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int var1 = EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);
         if (var1 > 0 && this.onSoulSpeedBlock()) {
            AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (var2 == null) {
               return;
            }

            var2.addTransientModifier(
               new AttributeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", 0.03F * (1.0F + var1 * 0.35F), AttributeModifier.Operation.ADDITION)
            );
            if (this.getRandom().nextFloat() < 0.04F) {
               ItemStack var3 = this.getItemBySlot(EquipmentSlot.FEET);
               var3.hurtAndBreak(1, this, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.FEET));
            }
         }
      }
   }

   protected void removeFrost() {
      AttributeInstance var1 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var1 != null) {
         if (var1.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
            var1.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
         }
      }
   }

   protected void tryAddFrost() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int var1 = this.getTicksFrozen();
         if (var1 > 0) {
            AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (var2 == null) {
               return;
            }

            float var3 = -0.05F * this.getPercentFrozen();
            var2.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", var3, AttributeModifier.Operation.ADDITION));
         }
      }
   }

   protected void onChangedBlock(BlockPos param0) {
      int var2 = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, this);
      if (var2 > 0) {
         FrostWalkerEnchantment.onEntityMoved(this, this.level, param0, var2);
      }

      if (this.shouldRemoveSoulSpeed(this.getBlockStateOnLegacy())) {
         this.removeSoulSpeed();
      }

      this.tryAddSoulSpeed();
   }

   public boolean isBaby() {
      return false;
   }

   public float getScale() {
      return this.isBaby() ? 0.5F : 1.0F;
   }

   protected boolean isAffectedByFluids() {
      return true;
   }

   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime >= 20 && !this.level.isClientSide() && !this.isRemoved()) {
         this.level.broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   public boolean shouldDropExperience() {
      return !this.isBaby();
   }

   protected boolean shouldDropLoot() {
      return !this.isBaby();
   }

   protected int decreaseAirSupply(int param0) {
      int var2 = EnchantmentHelper.getRespiration(this);
      return var2 > 0 && this.random.nextInt(var2 + 1) > 0 ? param0 : param0 - 1;
   }

   protected int increaseAirSupply(int param0) {
      return Math.min(param0 + 4, this.getMaxAirSupply());
   }

   public int getExperienceReward() {
      return 0;
   }

   protected boolean isAlwaysExperienceDropper() {
      return false;
   }

   public RandomSource getRandom() {
      return this.random;
   }

   @Nullable
   public LivingEntity getLastHurtByMob() {
      return this.lastHurtByMob;
   }

   @Override
   public LivingEntity getLastAttacker() {
      return this.getLastHurtByMob();
   }

   public int getLastHurtByMobTimestamp() {
      return this.lastHurtByMobTimestamp;
   }

   public void setLastHurtByPlayer(@Nullable Player param0) {
      this.lastHurtByPlayer = param0;
      this.lastHurtByPlayerTime = this.tickCount;
   }

   public void setLastHurtByMob(@Nullable LivingEntity param0) {
      this.lastHurtByMob = param0;
      this.lastHurtByMobTimestamp = this.tickCount;
   }

   @Nullable
   public LivingEntity getLastHurtMob() {
      return this.lastHurtMob;
   }

   public int getLastHurtMobTimestamp() {
      return this.lastHurtMobTimestamp;
   }

   public void setLastHurtMob(Entity param0) {
      if (param0 instanceof LivingEntity) {
         this.lastHurtMob = (LivingEntity)param0;
      } else {
         this.lastHurtMob = null;
      }

      this.lastHurtMobTimestamp = this.tickCount;
   }

   public int getNoActionTime() {
      return this.noActionTime;
   }

   public void setNoActionTime(int param0) {
      this.noActionTime = param0;
   }

   public boolean shouldDiscardFriction() {
      return this.discardFriction;
   }

   public void setDiscardFriction(boolean param0) {
      this.discardFriction = param0;
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot param0) {
      return true;
   }

   public void onEquipItem(EquipmentSlot param0, ItemStack param1, ItemStack param2) {
      boolean var4 = param2.isEmpty() && param1.isEmpty();
      if (!var4 && !ItemStack.isSameItemSameTags(param1, param2) && !this.firstTick) {
         Equipable var5 = Equipable.get(param2);
         if (var5 != null && !this.isSpectator() && var5.getEquipmentSlot() == param0) {
            if (!this.level.isClientSide() && !this.isSilent()) {
               this.level.playSound(null, this.getX(), this.getY(), this.getZ(), var5.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F);
            }

            if (this.doesEmitEquipEvent(param0)) {
               this.gameEvent(GameEvent.EQUIP);
            }
         }
      }
   }

   @Override
   public void remove(Entity.RemovalReason param0) {
      super.remove(param0);
      this.brain.clearMemories();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      param0.putFloat("Health", this.getHealth());
      param0.putShort("HurtTime", (short)this.hurtTime);
      param0.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
      param0.putShort("DeathTime", (short)this.deathTime);
      param0.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
      param0.put("Attributes", this.getAttributes().save());
      if (!this.activeEffects.isEmpty()) {
         ListTag var2 = new ListTag();

         for (MobEffectInstance var4 : this.activeEffects.values()) {
            var2.add(var4.save(new CompoundTag()));
         }

         param0.put("ActiveEffects", var2);
      }

      param0.putBoolean("FallFlying", this.isFallFlying());
      this.getSleepingPos().ifPresent(l_param1 -> {
         param0.putInt("SleepingX", l_param1.getX());
         param0.putInt("SleepingY", l_param1.getY());
         param0.putInt("SleepingZ", l_param1.getZ());
      });
      DataResult<Tag> var2 = this.brain.serializeStart(NbtOps.INSTANCE);
      var2.resultOrPartial(LOGGER::error).ifPresent(l_param1 -> param0.put("Brain", l_param1));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      this.setAbsorptionAmount(param0.getFloat("AbsorptionAmount"));
      if (param0.contains("Attributes", 9) && this.level != null && !this.level.isClientSide) {
         this.getAttributes().load(param0.getList("Attributes", 10));
      }

      if (param0.contains("ActiveEffects", 9)) {
         ListTag var2 = param0.getList("ActiveEffects", 10);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            CompoundTag var4 = var2.getCompound(var3);
            MobEffectInstance var5 = MobEffectInstance.load(var4);
            if (var5 != null) {
               this.activeEffects.put(var5.getEffect(), var5);
            }
         }
      }

      if (param0.contains("Health", 99)) {
         this.setHealth(param0.getFloat("Health"));
      }

      this.hurtTime = param0.getShort("HurtTime");
      this.deathTime = param0.getShort("DeathTime");
      this.lastHurtByMobTimestamp = param0.getInt("HurtByTimestamp");
      if (param0.contains("Team", 8)) {
         String var2 = param0.getString("Team");
         PlayerTeam var3 = this.level.getScoreboard().getPlayerTeam(var2);
         boolean var4 = var3 != null && this.level.getScoreboard().addPlayerToTeam(this.getStringUUID(), var3);
         if (!var4) {
            LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", var2);
         }
      }

      if (param0.getBoolean("FallFlying")) {
         this.setSharedFlag(7, true);
      }

      if (param0.contains("SleepingX", 99) && param0.contains("SleepingY", 99) && param0.contains("SleepingZ", 99)) {
         BlockPos var2 = new BlockPos(param0.getInt("SleepingX"), param0.getInt("SleepingY"), param0.getInt("SleepingZ"));
         this.setSleepingPos(var2);
         this.entityData.set(DATA_POSE, Pose.SLEEPING);
         if (!this.firstTick) {
            this.setPosToBed(var2);
         }
      }

      if (param0.contains("Brain", 10)) {
         this.brain = this.makeBrain(new Dynamic<>(NbtOps.INSTANCE, param0.get("Brain")));
      }
   }

   protected void tickEffects() {
      Iterator<MobEffect> var1 = this.activeEffects.keySet().iterator();

      try {
         while (var1.hasNext()) {
            MobEffect var2 = var1.next();
            MobEffectInstance var3 = this.activeEffects.get(var2);
            if (!var3.tick(this, () -> this.onEffectUpdated(var3, true, null))) {
               if (!this.level.isClientSide) {
                  var1.remove();
                  this.onEffectRemoved(var3);
               }
            } else if (var3.getDuration() % 600 == 0) {
               this.onEffectUpdated(var3, false, null);
            }
         }
      } catch (ConcurrentModificationException var11) {
      }

      if (this.effectsDirty) {
         if (!this.level.isClientSide) {
            this.updateInvisibilityStatus();
            this.updateGlowingStatus();
         }

         this.effectsDirty = false;
      }

      int var2 = this.entityData.get(DATA_EFFECT_COLOR_ID);
      boolean var3 = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
      if (var2 > 0) {
         boolean var4;
         if (this.isInvisible()) {
            var4 = this.random.nextInt(15) == 0;
         } else {
            var4 = this.random.nextBoolean();
         }

         if (var3) {
            var4 &= this.random.nextInt(5) == 0;
         }

         if (var4 && var2 > 0) {
            double var5 = (var2 >> 16 & 0xFF) / 255.0;
            double var7 = (var2 >> 8 & 0xFF) / 255.0;
            double var9 = (var2 >> 0 & 0xFF) / 255.0;
            this.level
               .addParticle(
                  var3 ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT,
                  this.getRandomX(0.5),
                  this.getRandomY(),
                  this.getRandomZ(0.5),
                  var5,
                  var7,
                  var9
               );
         }
      }
   }

   protected void updateInvisibilityStatus() {
      if (this.activeEffects.isEmpty()) {
         this.removeEffectParticles();
         this.setInvisible(false);
      } else {
         Collection<MobEffectInstance> var1 = this.activeEffects.values();
         this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(var1));
         this.entityData.set(DATA_EFFECT_COLOR_ID, PotionUtils.getColor(var1));
         this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
      }
   }

   private void updateGlowingStatus() {
      boolean var1 = this.isCurrentlyGlowing();
      if (this.getSharedFlag(6) != var1) {
         this.setSharedFlag(6, var1);
      }
   }

   public double getVisibilityPercent(@Nullable Entity param0) {
      double var2 = 1.0;
      if (this.isDiscrete()) {
         var2 *= 0.8;
      }

      if (this.isInvisible()) {
         float var4 = this.getArmorCoverPercentage();
         if (var4 < 0.1F) {
            var4 = 0.1F;
         }

         var2 *= 0.7 * var4;
      }

      if (param0 != null) {
         ItemStack var4 = this.getItemBySlot(EquipmentSlot.HEAD);
         EntityType<?> var5 = param0.getType();
         if (var5 == EntityType.SKELETON && var4.is(Items.SKELETON_SKULL)
            || var5 == EntityType.ZOMBIE && var4.is(Items.ZOMBIE_HEAD)
            || var5 == EntityType.PIGLIN && var4.is(Items.PIGLIN_HEAD)
            || var5 == EntityType.PIGLIN_BRUTE && var4.is(Items.PIGLIN_HEAD)
            || var5 == EntityType.CREEPER && var4.is(Items.CREEPER_HEAD)) {
            var2 *= 0.5;
         }
      }

      return var2;
   }

   public boolean canAttack(LivingEntity param0) {
      return param0 instanceof Player && this.level.getDifficulty() == Difficulty.PEACEFUL ? false : param0.canBeSeenAsEnemy();
   }

   public boolean canAttack(LivingEntity param0, TargetingConditions param1) {
      return param1.test(this, param0);
   }

   public boolean canBeSeenAsEnemy() {
      return !this.isInvulnerable() && this.canBeSeenByAnyone();
   }

   public boolean canBeSeenByAnyone() {
      return !this.isSpectator() && this.isAlive();
   }

   public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> param0) {
      for (MobEffectInstance var2 : param0) {
         if (var2.isVisible() && !var2.isAmbient()) {
            return false;
         }
      }

      return true;
   }

   protected void removeEffectParticles() {
      this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
   }

   public boolean removeAllEffects() {
      if (this.level.isClientSide) {
         return false;
      }

      Iterator<MobEffectInstance> var1 = this.activeEffects.values().iterator();

      boolean var2;
      for (var2 = false; var1.hasNext(); var2 = true) {
         this.onEffectRemoved(var1.next());
         var1.remove();
      }

      return var2;
   }

   public Collection<MobEffectInstance> getActiveEffects() {
      return this.activeEffects.values();
   }

   public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
      return this.activeEffects;
   }

   public boolean hasEffect(MobEffect param0) {
      return this.activeEffects.containsKey(param0);
   }

   @Nullable
   public MobEffectInstance getEffect(MobEffect param0) {
      return this.activeEffects.get(param0);
   }

   public final boolean addEffect(MobEffectInstance param0) {
      return this.addEffect(param0, null);
   }

   public boolean addEffect(MobEffectInstance param0, @Nullable Entity param1) {
      if (!this.canBeAffected(param0)) {
         return false;
      } else {
         MobEffectInstance var3 = this.activeEffects.get(param0.getEffect());
         if (var3 == null) {
            this.activeEffects.put(param0.getEffect(), param0);
            this.onEffectAdded(param0, param1);
            return true;
         } else if (var3.update(param0)) {
            this.onEffectUpdated(var3, true, param1);
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean canBeAffected(MobEffectInstance param0) {
      if (this.getMobType() == MobType.UNDEAD) {
         MobEffect var2 = param0.getEffect();
         if (var2 == MobEffects.REGENERATION || var2 == MobEffects.POISON) {
            return false;
         }
      }

      return true;
   }

   public void forceAddEffect(MobEffectInstance param0, @Nullable Entity param1) {
      if (this.canBeAffected(param0)) {
         MobEffectInstance var3 = this.activeEffects.put(param0.getEffect(), param0);
         if (var3 == null) {
            this.onEffectAdded(param0, param1);
         } else {
            this.onEffectUpdated(param0, true, param1);
         }
      }
   }

   public boolean isInvertedHealAndHarm() {
      return this.getMobType() == MobType.UNDEAD;
   }

   @Nullable
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect param0) {
      return this.activeEffects.remove(param0);
   }

   public boolean removeEffect(MobEffect param0) {
      MobEffectInstance var2 = this.removeEffectNoUpdate(param0);
      if (var2 != null) {
         this.onEffectRemoved(var2);
         return true;
      } else {
         return false;
      }
   }

   protected void onEffectAdded(MobEffectInstance param0, @Nullable Entity param1) {
      this.effectsDirty = true;
      if (!this.level.isClientSide) {
         param0.getEffect().addAttributeModifiers(this, this.getAttributes(), param0.getAmplifier());
      }
   }

   protected void onEffectUpdated(MobEffectInstance param0, boolean param1, @Nullable Entity param2) {
      this.effectsDirty = true;
      if (param1 && !this.level.isClientSide) {
         MobEffect var4 = param0.getEffect();
         var4.removeAttributeModifiers(this, this.getAttributes(), param0.getAmplifier());
         var4.addAttributeModifiers(this, this.getAttributes(), param0.getAmplifier());
      }
   }

   protected void onEffectRemoved(MobEffectInstance param0) {
      this.effectsDirty = true;
      if (!this.level.isClientSide) {
         param0.getEffect().removeAttributeModifiers(this, this.getAttributes(), param0.getAmplifier());
      }
   }

   public void heal(float param0) {
      float var2 = this.getHealth();
      if (var2 > 0.0F) {
         this.setHealth(var2 + param0);
      }
   }

   public float getHealth() {
      return this.entityData.get(DATA_HEALTH_ID);
   }

   public void setHealth(float param0) {
      this.entityData.set(DATA_HEALTH_ID, Mth.clamp(param0, 0.0F, this.getMaxHealth()));
   }

   public boolean isDeadOrDying() {
      return this.getHealth() <= 0.0F;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.isInvulnerableTo(param0)) {
         return false;
      }

      if (this.level.isClientSide) {
         return false;
      }

      if (this.isDeadOrDying()) {
         return false;
      }

      if (param0.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         return false;
      }

      if (this.isSleeping() && !this.level.isClientSide) {
         this.stopSleeping();
      }

      this.noActionTime = 0;
      float var3 = param1;
      boolean var4 = false;
      float var5 = 0.0F;
      if (param1 > 0.0F && this.isDamageSourceBlocked(param0)) {
         this.hurtCurrentlyUsedShield(param1);
         var5 = param1;
         param1 = 0.0F;
         if (!param0.is(DamageTypeTags.IS_PROJECTILE) && param0.getDirectEntity() instanceof LivingEntity var7) {
            this.blockUsingShield(var7);
         }

         var4 = true;
      }

      if (param0.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
         param1 *= 5.0F;
      }

      this.walkAnimation.setSpeed(1.5F);
      boolean var6 = true;
      if (this.invulnerableTime > 10.0F && !param0.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
         if (param1 <= this.lastHurt) {
            return false;
         }

         this.actuallyHurt(param0, param1 - this.lastHurt);
         this.lastHurt = param1;
         var6 = false;
      } else {
         this.lastHurt = param1;
         this.invulnerableTime = 20;
         this.actuallyHurt(param0, param1);
         this.hurtDuration = 10;
         this.hurtTime = this.hurtDuration;
      }

      if (param0.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         this.hurtHelmet(param0, param1);
         param1 *= 0.75F;
      }

      Entity var7 = param0.getEntity();
      if (var7 != null) {
         if (var7 instanceof LivingEntity var8 && !param0.is(DamageTypeTags.NO_ANGER)) {
            this.setLastHurtByMob(var8);
         }

         if (var7 instanceof Player var8) {
            this.lastHurtByPlayerTime = 100;
            this.lastHurtByPlayer = var8;
         } else if (var7 instanceof Wolf var9 && var9.isTame()) {
            this.lastHurtByPlayerTime = 100;
            if (var9.getOwner() instanceof Player var10) {
               this.lastHurtByPlayer = var10;
            } else {
               this.lastHurtByPlayer = null;
            }
         }
      }

      if (var6) {
         if (var4) {
            this.level.broadcastEntityEvent(this, (byte)29);
         } else {
            this.level.broadcastDamageEvent(this, param0);
         }

         if (!param0.is(DamageTypeTags.NO_IMPACT) && (!var4 || param1 > 0.0F)) {
            this.markHurt();
         }

         if (var7 != null && !param0.is(DamageTypeTags.IS_EXPLOSION)) {
            double var8 = var7.getX() - this.getX();

            double var10;
            for (var10 = var7.getZ() - this.getZ(); var8 * var8 + var10 * var10 < 1.0E-4; var10 = (Math.random() - Math.random()) * 0.01) {
               var8 = (Math.random() - Math.random()) * 0.01;
            }

            this.knockback(0.4F, var8, var10);
            if (!var4) {
               this.indicateDamage(var8, var10);
            }
         }
      }

      if (this.isDeadOrDying()) {
         if (!this.checkTotemDeathProtection(param0)) {
            SoundEvent var8 = this.getDeathSound();
            if (var6 && var8 != null) {
               this.playSound(var8, this.getSoundVolume(), this.getVoicePitch());
            }

            this.die(param0);
         }
      } else if (var6) {
         this.playHurtSound(param0);
      }

      boolean var8 = !var4 || param1 > 0.0F;
      if (var8) {
         this.lastDamageSource = param0;
         this.lastDamageStamp = this.level.getGameTime();
      }

      if (this instanceof ServerPlayer) {
         CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer)this, param0, var3, param1, var4);
         if (var5 > 0.0F && var5 < 3.4028235E37F) {
            ((ServerPlayer)this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(var5 * 10.0F));
         }
      }

      if (var7 instanceof ServerPlayer) {
         CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)var7, this, param0, var3, param1, var4);
      }

      return var8;
   }

   protected void blockUsingShield(LivingEntity param0) {
      param0.blockedByShield(this);
   }

   protected void blockedByShield(LivingEntity param0) {
      param0.knockback(0.5, param0.getX() - this.getX(), param0.getZ() - this.getZ());
   }

   private boolean checkTotemDeathProtection(DamageSource param0) {
      if (param0.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      ItemStack var2 = null;

      for (InteractionHand var7 : InteractionHand.values()) {
         ItemStack var3 = this.getItemInHand(var7);
         if (var3.is(Items.TOTEM_OF_UNDYING)) {
            var2 = var3.copy();
            var3.shrink(1);
            break;
         }
      }

      if (var2 != null) {
         if (this instanceof ServerPlayer var4) {
            var4.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
            CriteriaTriggers.USED_TOTEM.trigger(var4, var2);
         }

         this.setHealth(1.0F);
         this.removeAllEffects();
         this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
         this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
         this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
         this.level.broadcastEntityEvent(this, (byte)35);
      }

      return var2 != null;
   }

   @Nullable
   public DamageSource getLastDamageSource() {
      if (this.level.getGameTime() - this.lastDamageStamp > 40L) {
         this.lastDamageSource = null;
      }

      return this.lastDamageSource;
   }

   protected void playHurtSound(DamageSource param0) {
      SoundEvent var2 = this.getHurtSound(param0);
      if (var2 != null) {
         this.playSound(var2, this.getSoundVolume(), this.getVoicePitch());
      }
   }

   public boolean isDamageSourceBlocked(DamageSource param0) {
      Entity var2 = param0.getDirectEntity();
      boolean var3 = false;
      if (var2 instanceof AbstractArrow var4 && var4.getPierceLevel() > 0) {
         var3 = true;
      }

      if (!param0.is(DamageTypeTags.BYPASSES_SHIELD) && this.isBlocking() && !var3) {
         Vec3 var4 = param0.getSourcePosition();
         if (var4 != null) {
            Vec3 var5 = this.getViewVector(1.0F);
            Vec3 var6 = var4.vectorTo(this.position()).normalize();
            var6 = new Vec3(var6.x, 0.0, var6.z);
            if (var6.dot(var5) < 0.0) {
               return true;
            }
         }
      }

      return false;
   }

   private void breakItem(ItemStack param0) {
      if (!param0.isEmpty()) {
         if (!this.isSilent()) {
            this.level
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.ITEM_BREAK,
                  this.getSoundSource(),
                  0.8F,
                  0.8F + this.level.random.nextFloat() * 0.4F,
                  false
               );
         }

         this.spawnItemParticles(param0, 5);
      }
   }

   public void die(DamageSource param0) {
      if (!this.isRemoved() && !this.dead) {
         Entity var2 = param0.getEntity();
         LivingEntity var3 = this.getKillCredit();
         if (this.deathScore >= 0 && var3 != null) {
            var3.awardKillScore(this, this.deathScore, param0);
         }

         if (this.isSleeping()) {
            this.stopSleeping();
         }

         if (!this.level.isClientSide && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
         }

         this.dead = true;
         this.getCombatTracker().recheckStatus();
         if (this.level instanceof ServerLevel) {
            if (var2 == null || var2.wasKilled((ServerLevel)this.level, this)) {
               this.gameEvent(GameEvent.ENTITY_DIE);
               this.dropAllDeathLoot(param0);
               this.createWitherRose(var3);
            }

            this.level.broadcastEntityEvent(this, (byte)3);
         }

         this.setPose(Pose.DYING);
      }
   }

   protected void createWitherRose(@Nullable LivingEntity param0) {
      if (!this.level.isClientSide) {
         boolean var2 = false;
         if (param0 instanceof WitherBoss) {
            if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
               BlockPos var3 = this.blockPosition();
               BlockState var4 = Blocks.WITHER_ROSE.defaultBlockState();
               if (this.level.getBlockState(var3).isAir() && var4.canSurvive(this.level, var3)) {
                  this.level.setBlock(var3, var4, 3);
                  var2 = true;
               }
            }

            if (!var2) {
               ItemEntity var3 = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
               this.level.addFreshEntity(var3);
            }
         }
      }
   }

   protected void dropAllDeathLoot(DamageSource param0) {
      Entity var2 = param0.getEntity();
      int var3;
      if (var2 instanceof Player) {
         var3 = EnchantmentHelper.getMobLooting((LivingEntity)var2);
      } else {
         var3 = 0;
      }

      boolean var4 = this.lastHurtByPlayerTime > 0;
      if (this.shouldDropLoot() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         this.dropFromLootTable(param0, var4);
         this.dropCustomDeathLoot(param0, var3, var4);
      }

      this.dropEquipment();
      this.dropExperience();
   }

   protected void dropEquipment() {
   }

   protected void dropExperience() {
      if (this.level instanceof ServerLevel
         && !this.wasExperienceConsumed()
         && (
            this.isAlwaysExperienceDropper()
               || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
         )) {
         ExperienceOrb.award((ServerLevel)this.level, this.position(), this.getExperienceReward());
      }
   }

   protected void dropCustomDeathLoot(DamageSource param0, int param1, boolean param2) {
   }

   public ResourceLocation getLootTable() {
      return this.getType().getDefaultLootTable();
   }

   protected void dropFromLootTable(DamageSource param0, boolean param1) {
      ResourceLocation var3 = this.getLootTable();
      LootTable var4 = this.level.getServer().getLootTables().get(var3);
      LootContext.Builder var5 = this.createLootContext(param1, param0);
      var4.getRandomItems(var5.create(LootContextParamSets.ENTITY), this::spawnAtLocation);
   }

   protected LootContext.Builder createLootContext(boolean param0, DamageSource param1) {
      LootContext.Builder var3 = new LootContext.Builder((ServerLevel)this.level)
         .withRandom(this.random)
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, param1)
         .withOptionalParameter(LootContextParams.KILLER_ENTITY, param1.getEntity())
         .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, param1.getDirectEntity());
      if (param0 && this.lastHurtByPlayer != null) {
         var3 = var3.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
      }

      return var3;
   }

   public void knockback(double param0, double param2, double param4) {
      param0 *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      if (!(param0 <= 0.0)) {
         this.hasImpulse = true;
         Vec3 var7 = this.getDeltaMovement();
         Vec3 var8 = new Vec3(param2, 0.0, param4).normalize().scale(param0);
         this.setDeltaMovement(var7.x / 2.0 - var8.x, this.onGround ? Math.min(0.4, var7.y / 2.0 + param0) : var7.y, var7.z / 2.0 - var8.z);
      }
   }

   public void indicateDamage(double param0, double param2) {
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource param0) {
      return SoundEvents.GENERIC_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.GENERIC_DEATH;
   }

   private SoundEvent getFallDamageSound(int param0) {
      return param0 > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
   }

   public void skipDropExperience() {
      this.skipDropExperience = true;
   }

   public boolean wasExperienceConsumed() {
      return this.skipDropExperience;
   }

   protected Vec3 getMeleeAttackReferencePosition() {
      return this.getVehicle() instanceof RiderShieldingMount var1 ? this.position().add(0.0, var1.getRiderShieldingHeight(), 0.0) : this.position();
   }

   public float getHurtDir() {
      return 0.0F;
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
   }

   protected SoundEvent getDrinkingSound(ItemStack param0) {
      return param0.getDrinkingSound();
   }

   public SoundEvent getEatingSound(ItemStack param0) {
      return param0.getEatingSound();
   }

   @Override
   public void setOnGround(boolean param0) {
      super.setOnGround(param0);
      if (param0) {
         this.lastClimbablePos = Optional.empty();
      }
   }

   public Optional<BlockPos> getLastClimbablePos() {
      return this.lastClimbablePos;
   }

   public boolean onClimbable() {
      if (this.isSpectator()) {
         return false;
      } else {
         BlockPos var1 = this.blockPosition();
         BlockState var2 = this.getFeetBlockState();
         if (var2.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(var1);
            return true;
         } else if (var2.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(var1, var2)) {
            this.lastClimbablePos = Optional.of(var1);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean trapdoorUsableAsLadder(BlockPos param0, BlockState param1) {
      if (param1.getValue(TrapDoorBlock.OPEN)) {
         BlockState var3 = this.level.getBlockState(param0.below());
         if (var3.is(Blocks.LADDER) && var3.getValue(LadderBlock.FACING) == param1.getValue(TrapDoorBlock.FACING)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isAlive() {
      return !this.isRemoved() && this.getHealth() > 0.0F;
   }

   @Override
   public boolean causeFallDamage(float param0, float param1, DamageSource param2) {
      boolean var4 = super.causeFallDamage(param0, param1, param2);
      int var5 = this.calculateFallDamage(param0, param1);
      if (var5 > 0) {
         this.playSound(this.getFallDamageSound(var5), 1.0F, 1.0F);
         this.playBlockFallSound();
         this.hurt(param2, var5);
         return true;
      } else {
         return var4;
      }
   }

   protected int calculateFallDamage(float param0, float param1) {
      if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      }

      MobEffectInstance var3 = this.getEffect(MobEffects.JUMP);
      float var4 = var3 == null ? 0.0F : var3.getAmplifier() + 1;
      return Mth.ceil((param0 - 3.0F - var4) * param1);
   }

   protected void playBlockFallSound() {
      if (!this.isSilent()) {
         int var1 = Mth.floor(this.getX());
         int var2 = Mth.floor(this.getY() - 0.2F);
         int var3 = Mth.floor(this.getZ());
         BlockState var4 = this.level.getBlockState(new BlockPos(var1, var2, var3));
         if (!var4.isAir()) {
            SoundType var5 = var4.getSoundType();
            this.playSound(var5.getFallSound(), var5.getVolume() * 0.5F, var5.getPitch() * 0.75F);
         }
      }
   }

   @Override
   public void animateHurt(float param0) {
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
   }

   public int getArmorValue() {
      return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
   }

   protected void hurtArmor(DamageSource param0, float param1) {
   }

   protected void hurtHelmet(DamageSource param0, float param1) {
   }

   protected void hurtCurrentlyUsedShield(float param0) {
   }

   protected float getDamageAfterArmorAbsorb(DamageSource param0, float param1) {
      if (!param0.is(DamageTypeTags.BYPASSES_ARMOR)) {
         this.hurtArmor(param0, param1);
         param1 = CombatRules.getDamageAfterAbsorb(param1, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      }

      return param1;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource param0, float param1) {
      if (param0.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return param1;
      }

      if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !param0.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
         int var3 = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
         int var4 = 25 - var3;
         float var5 = param1 * var4;
         float var6 = param1;
         param1 = Math.max(var5 / 25.0F, 0.0F);
         float var7 = var6 - param1;
         if (var7 > 0.0F && var7 < 3.4028235E37F) {
            if (this instanceof ServerPlayer) {
               ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(var7 * 10.0F));
            } else if (param0.getEntity() instanceof ServerPlayer) {
               ((ServerPlayer)param0.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(var7 * 10.0F));
            }
         }
      }

      if (param1 <= 0.0F) {
         return 0.0F;
      }

      if (param0.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
         return param1;
      }

      int var3 = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), param0);
      if (var3 > 0) {
         param1 = CombatRules.getDamageAfterMagicAbsorb(param1, var3);
      }

      return param1;
   }

   protected void actuallyHurt(DamageSource param0, float param1) {
      if (!this.isInvulnerableTo(param0)) {
         param1 = this.getDamageAfterArmorAbsorb(param0, param1);
         param1 = this.getDamageAfterMagicAbsorb(param0, param1);
         float var3 = param1;
         param1 = Math.max(param1 - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (var3 - param1));
         float var4 = var3 - param1;
         if (var4 > 0.0F && var4 < 3.4028235E37F && param0.getEntity() instanceof ServerPlayer var5) {
            var5.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(var4 * 10.0F));
         }

         if (param1 != 0.0F) {
            float var5 = this.getHealth();
            this.getCombatTracker().recordDamage(param0, var5, param1);
            this.setHealth(var5 - param1);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - param1);
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public CombatTracker getCombatTracker() {
      return this.combatTracker;
   }

   @Nullable
   public LivingEntity getKillCredit() {
      if (this.combatTracker.getKiller() != null) {
         return this.combatTracker.getKiller();
      } else if (this.lastHurtByPlayer != null) {
         return this.lastHurtByPlayer;
      } else {
         return this.lastHurtByMob != null ? this.lastHurtByMob : null;
      }
   }

   public final float getMaxHealth() {
      return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
   }

   public final int getArrowCount() {
      return this.entityData.get(DATA_ARROW_COUNT_ID);
   }

   public final void setArrowCount(int param0) {
      this.entityData.set(DATA_ARROW_COUNT_ID, param0);
   }

   public final int getStingerCount() {
      return this.entityData.get(DATA_STINGER_COUNT_ID);
   }

   public final void setStingerCount(int param0) {
      this.entityData.set(DATA_STINGER_COUNT_ID, param0);
   }

   private int getCurrentSwingDuration() {
      if (MobEffectUtil.hasDigSpeed(this)) {
         return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
      } else {
         return this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
      }
   }

   public void swing(InteractionHand param0) {
      this.swing(param0, false);
   }

   public void swing(InteractionHand param0, boolean param1) {
      if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
         this.swingTime = -1;
         this.swinging = true;
         this.swingingArm = param0;
         if (this.level instanceof ServerLevel) {
            ClientboundAnimatePacket var3 = new ClientboundAnimatePacket(this, param0 == InteractionHand.MAIN_HAND ? 0 : 3);
            ServerChunkCache var4 = ((ServerLevel)this.level).getChunkSource();
            if (param1) {
               var4.broadcastAndSend(this, var3);
            } else {
               var4.broadcast(this, var3);
            }
         }
      }
   }

   @Override
   public void handleDamageEvent(DamageSource param0) {
      this.walkAnimation.setSpeed(1.5F);
      this.invulnerableTime = 20;
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
      SoundEvent var2 = this.getHurtSound(param0);
      if (var2 != null) {
         this.playSound(var2, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }

      this.hurt(this.damageSources().generic(), 0.0F);
      this.lastDamageSource = param0;
      this.lastDamageStamp = this.level.getGameTime();
   }

   @Override
   public void handleEntityEvent(byte param0) {
      switch (param0) {
         case 3:
            SoundEvent var2 = this.getDeathSound();
            if (var2 != null) {
               this.playSound(var2, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof Player)) {
               this.setHealth(0.0F);
               this.die(this.damageSources().generic());
            }
            break;
         case 29:
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
         case 30:
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            break;
         case 46:
            int var2_1 = 128;

            for (int var3 = 0; var3 < 128; var3++) {
               double var4 = var3 / 127.0;
               float var6 = (this.random.nextFloat() - 0.5F) * 0.2F;
               float var7 = (this.random.nextFloat() - 0.5F) * 0.2F;
               float var8 = (this.random.nextFloat() - 0.5F) * 0.2F;
               double var9 = Mth.lerp(var4, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               double var11 = Mth.lerp(var4, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
               double var13 = Mth.lerp(var4, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               this.level.addParticle(ParticleTypes.PORTAL, var9, var11, var13, var6, var7, var8);
            }
            break;
         case 47:
            this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            break;
         case 48:
            this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
            break;
         case 49:
            this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
            break;
         case 50:
            this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
            break;
         case 51:
            this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
            break;
         case 52:
            this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
            break;
         case 54:
            HoneyBlock.showJumpParticles(this);
            break;
         case 55:
            this.swapHandItems();
            break;
         case 60:
            this.makePoofParticles();
            break;
         default:
            super.handleEntityEvent(param0);
      }
   }

   private void makePoofParticles() {
      for (int var1 = 0; var1 < 20; var1++) {
         double var2 = this.random.nextGaussian() * 0.02;
         double var4 = this.random.nextGaussian() * 0.02;
         double var6 = this.random.nextGaussian() * 0.02;
         this.level.addParticle(ParticleTypes.POOF, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), var2, var4, var6);
      }
   }

   private void swapHandItems() {
      ItemStack var1 = this.getItemBySlot(EquipmentSlot.OFFHAND);
      this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
      this.setItemSlot(EquipmentSlot.MAINHAND, var1);
   }

   @Override
   protected void outOfWorld() {
      this.hurt(this.damageSources().outOfWorld(), 4.0F);
   }

   protected void updateSwingTime() {
      int var1 = this.getCurrentSwingDuration();
      if (this.swinging) {
         this.swingTime++;
         if (this.swingTime >= var1) {
            this.swingTime = 0;
            this.swinging = false;
         }
      } else {
         this.swingTime = 0;
      }

      this.attackAnim = (float)this.swingTime / var1;
   }

   @Nullable
   public AttributeInstance getAttribute(Attribute param0) {
      return this.getAttributes().getInstance(param0);
   }

   public double getAttributeValue(Holder<Attribute> param0) {
      return this.getAttributeValue(param0.value());
   }

   public double getAttributeValue(Attribute param0) {
      return this.getAttributes().getValue(param0);
   }

   public double getAttributeBaseValue(Holder<Attribute> param0) {
      return this.getAttributeBaseValue(param0.value());
   }

   public double getAttributeBaseValue(Attribute param0) {
      return this.getAttributes().getBaseValue(param0);
   }

   public AttributeMap getAttributes() {
      return this.attributes;
   }

   public MobType getMobType() {
      return MobType.UNDEFINED;
   }

   public ItemStack getMainHandItem() {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
   }

   public ItemStack getOffhandItem() {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
   }

   public boolean isHolding(Item param0) {
      return this.isHolding(l_param1 -> l_param1.is(param0));
   }

   public boolean isHolding(Predicate<ItemStack> param0) {
      return param0.test(this.getMainHandItem()) || param0.test(this.getOffhandItem());
   }

   public ItemStack getItemInHand(InteractionHand param0) {
      if (param0 == InteractionHand.MAIN_HAND) {
         return this.getItemBySlot(EquipmentSlot.MAINHAND);
      } else if (param0 == InteractionHand.OFF_HAND) {
         return this.getItemBySlot(EquipmentSlot.OFFHAND);
      } else {
         throw new IllegalArgumentException("Invalid hand " + param0);
      }
   }

   public void setItemInHand(InteractionHand param0, ItemStack param1) {
      if (param0 == InteractionHand.MAIN_HAND) {
         this.setItemSlot(EquipmentSlot.MAINHAND, param1);
      } else {
         if (param0 != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("Invalid hand " + param0);
         }

         this.setItemSlot(EquipmentSlot.OFFHAND, param1);
      }
   }

   public boolean hasItemInSlot(EquipmentSlot param0) {
      return !this.getItemBySlot(param0).isEmpty();
   }

   @Override
   public abstract Iterable<ItemStack> getArmorSlots();

   public abstract ItemStack getItemBySlot(EquipmentSlot var1);

   @Override
   public abstract void setItemSlot(EquipmentSlot var1, ItemStack var2);

   protected void verifyEquippedItem(ItemStack param0) {
      CompoundTag var2 = param0.getTag();
      if (var2 != null) {
         param0.getItem().verifyTagAfterLoad(var2);
      }
   }

   public float getArmorCoverPercentage() {
      Iterable<ItemStack> var1 = this.getArmorSlots();
      int var2 = 0;
      int var3 = 0;

      for (ItemStack var5 : var1) {
         if (!var5.isEmpty()) {
            var3++;
         }

         var2++;
      }

      return var2 > 0 ? (float)var3 / var2 : 0.0F;
   }

   @Override
   public void setSprinting(boolean param0) {
      super.setSprinting(param0);
      AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var2.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) {
         var2.removeModifier(SPEED_MODIFIER_SPRINTING);
      }

      if (param0) {
         var2.addTransientModifier(SPEED_MODIFIER_SPRINTING);
      }
   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return this.isBaby()
         ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
         : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   protected boolean isImmobile() {
      return this.isDeadOrDying();
   }

   @Override
   public void push(Entity param0) {
      if (!this.isSleeping()) {
         super.push(param0);
      }
   }

   private void dismountVehicle(Entity param0) {
      Vec3 var2;
      if (this.isRemoved()) {
         var2 = this.position();
      } else if (!param0.isRemoved() && !this.level.getBlockState(param0.blockPosition()).is(BlockTags.PORTALS)) {
         var2 = param0.getDismountLocationForPassenger(this);
      } else {
         double var3 = Math.max(this.getY(), param0.getY());
         var2 = new Vec3(this.getX(), var3, this.getZ());
      }

      this.dismountTo(var2.x, var2.y, var2.z);
   }

   @Override
   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   protected float getJumpPower() {
      return 0.42F * this.getBlockJumpFactor();
   }

   public double getJumpBoostPower() {
      return this.hasEffect(MobEffects.JUMP) ? 0.1F * (this.getEffect(MobEffects.JUMP).getAmplifier() + 1) : 0.0;
   }

   protected void jumpFromGround() {
      double var1 = this.getJumpPower() + this.getJumpBoostPower();
      Vec3 var3 = this.getDeltaMovement();
      this.setDeltaMovement(var3.x, var1, var3.z);
      if (this.isSprinting()) {
         float var4 = this.getYRot() * (float) (Math.PI / 180.0);
         this.setDeltaMovement(this.getDeltaMovement().add(-Mth.sin(var4) * 0.2F, 0.0, Mth.cos(var4) * 0.2F));
      }

      this.hasImpulse = true;
   }

   protected void goDownInWater() {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04F, 0.0));
   }

   protected void jumpInLiquid(TagKey<Fluid> param0) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
   }

   protected float getWaterSlowDown() {
      return 0.8F;
   }

   public boolean canStandOnFluid(FluidState param0) {
      return false;
   }

   public void travel(Vec3 param0) {
      if (this.isControlledByLocalInstance()) {
         double var2 = 0.08;
         boolean var4 = this.getDeltaMovement().y <= 0.0;
         if (var4 && this.hasEffect(MobEffects.SLOW_FALLING)) {
            var2 = 0.01;
            this.resetFallDistance();
         }

         FluidState var5 = this.level.getFluidState(this.blockPosition());
         if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(var5)) {
            double var6 = this.getY();
            float var8 = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
            float var9 = 0.02F;
            float var10 = EnchantmentHelper.getDepthStrider(this);
            if (var10 > 3.0F) {
               var10 = 3.0F;
            }

            if (!this.onGround) {
               var10 *= 0.5F;
            }

            if (var10 > 0.0F) {
               var8 += (0.54600006F - var8) * var10 / 3.0F;
               var9 += (this.getSpeed() - var9) * var10 / 3.0F;
            }

            if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
               var8 = 0.96F;
            }

            this.moveRelative(var9, param0);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 var11 = this.getDeltaMovement();
            if (this.horizontalCollision && this.onClimbable()) {
               var11 = new Vec3(var11.x, 0.2, var11.z);
            }

            this.setDeltaMovement(var11.multiply(var8, 0.8F, var8));
            Vec3 var12 = this.getFluidFallingAdjustedMovement(var2, var4, this.getDeltaMovement());
            this.setDeltaMovement(var12);
            if (this.horizontalCollision && this.isFree(var12.x, var12.y + 0.6F - this.getY() + var6, var12.z)) {
               this.setDeltaMovement(var12.x, 0.3F, var12.z);
            }
         } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(var5)) {
            double var6 = this.getY();
            this.moveRelative(0.02F, param0);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
               Vec3 var8 = this.getFluidFallingAdjustedMovement(var2, var4, this.getDeltaMovement());
               this.setDeltaMovement(var8);
            } else {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            }

            if (!this.isNoGravity()) {
               this.setDeltaMovement(this.getDeltaMovement().add(0.0, -var2 / 4.0, 0.0));
            }

            Vec3 var8 = this.getDeltaMovement();
            if (this.horizontalCollision && this.isFree(var8.x, var8.y + 0.6F - this.getY() + var6, var8.z)) {
               this.setDeltaMovement(var8.x, 0.3F, var8.z);
            }
         } else if (this.isFallFlying()) {
            this.checkSlowFallDistance();
            Vec3 var6 = this.getDeltaMovement();
            Vec3 var7 = this.getLookAngle();
            float var8 = this.getXRot() * (float) (Math.PI / 180.0);
            double var9 = Math.sqrt(var7.x * var7.x + var7.z * var7.z);
            double var11 = var6.horizontalDistance();
            double var13 = var7.length();
            double var15 = Math.cos(var8);
            var15 = var15 * var15 * Math.min(1.0, var13 / 0.4);
            var6 = this.getDeltaMovement().add(0.0, var2 * (-1.0 + var15 * 0.75), 0.0);
            if (var6.y < 0.0 && var9 > 0.0) {
               double var17 = var6.y * -0.1 * var15;
               var6 = var6.add(var7.x * var17 / var9, var17, var7.z * var17 / var9);
            }

            if (var8 < 0.0F && var9 > 0.0) {
               double var17 = var11 * -Mth.sin(var8) * 0.04;
               var6 = var6.add(-var7.x * var17 / var9, var17 * 3.2, -var7.z * var17 / var9);
            }

            if (var9 > 0.0) {
               var6 = var6.add((var7.x / var9 * var11 - var6.x) * 0.1, 0.0, (var7.z / var9 * var11 - var6.z) * 0.1);
            }

            this.setDeltaMovement(var6.multiply(0.99F, 0.98F, 0.99F));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.horizontalCollision && !this.level.isClientSide) {
               double var17 = this.getDeltaMovement().horizontalDistance();
               double var19 = var11 - var17;
               float var21 = (float)(var19 * 10.0 - 3.0);
               if (var21 > 0.0F) {
                  this.playSound(this.getFallDamageSound((int)var21), 1.0F, 1.0F);
                  this.hurt(this.damageSources().flyIntoWall(), var21);
               }
            }

            if (this.onGround && !this.level.isClientSide) {
               this.setSharedFlag(7, false);
            }
         } else {
            BlockPos var6 = this.getBlockPosBelowThatAffectsMyMovement();
            float var7 = this.level.getBlockState(var6).getBlock().getFriction();
            float var8 = this.onGround ? var7 * 0.91F : 0.91F;
            Vec3 var9 = this.handleRelativeFrictionAndCalculateMovement(param0, var7);
            double var10 = var9.y;
            if (this.hasEffect(MobEffects.LEVITATION)) {
               var10 += (0.05 * (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - var9.y) * 0.2;
               this.resetFallDistance();
            } else if (this.level.isClientSide && !this.level.hasChunkAt(var6)) {
               if (this.getY() > this.level.getMinBuildHeight()) {
                  var10 = -0.1;
               } else {
                  var10 = 0.0;
               }
            } else if (!this.isNoGravity()) {
               var10 -= var2;
            }

            if (this.shouldDiscardFriction()) {
               this.setDeltaMovement(var9.x, var10, var9.z);
            } else {
               this.setDeltaMovement(var9.x * var8, var10 * 0.98F, var9.z * var8);
            }
         }
      }

      this.calculateEntityAnimation(this instanceof FlyingAnimal);
   }

   private void travelRidden(LivingEntity param0, Vec3 param1) {
      Vec3 var3 = this.getRiddenInput(param0, param1);
      this.tickRidden(param0, var3);
      if (this.isControlledByLocalInstance()) {
         this.setSpeed(this.getRiddenSpeed(param0));
         this.travel(var3);
      } else {
         this.calculateEntityAnimation(false);
         this.setDeltaMovement(Vec3.ZERO);
         this.tryCheckInsideBlocks();
      }
   }

   protected void tickRidden(LivingEntity param0, Vec3 param1) {
   }

   protected Vec3 getRiddenInput(LivingEntity param0, Vec3 param1) {
      return param1;
   }

   protected float getRiddenSpeed(LivingEntity param0) {
      return this.getSpeed();
   }

   public void calculateEntityAnimation(boolean param0) {
      float var2 = (float)Mth.length(this.getX() - this.xo, param0 ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
      this.updateWalkAnimation(var2);
   }

   protected void updateWalkAnimation(float param0) {
      float var2 = Math.min(param0 * 4.0F, 1.0F);
      this.walkAnimation.update(var2, 0.4F);
   }

   public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 param0, float param1) {
      this.moveRelative(this.getFrictionInfluencedSpeed(param1), param0);
      this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 var3 = this.getDeltaMovement();
      if ((this.horizontalCollision || this.jumping)
         && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
         var3 = new Vec3(var3.x, 0.2, var3.z);
      }

      return var3;
   }

   public Vec3 getFluidFallingAdjustedMovement(double param0, boolean param2, Vec3 param3) {
      if (!this.isNoGravity() && !this.isSprinting()) {
         double var5;
         if (param2 && Math.abs(param3.y - 0.005) >= 0.003 && Math.abs(param3.y - param0 / 16.0) < 0.003) {
            var5 = -0.003;
         } else {
            var5 = param3.y - param0 / 16.0;
         }

         return new Vec3(param3.x, var5, param3.z);
      } else {
         return param3;
      }
   }

   private Vec3 handleOnClimbable(Vec3 param0) {
      if (this.onClimbable()) {
         this.resetFallDistance();
         float var2 = 0.15F;
         double var3 = Mth.clamp(param0.x, -0.15F, 0.15F);
         double var5 = Mth.clamp(param0.z, -0.15F, 0.15F);
         double var7 = Math.max(param0.y, -0.15F);
         if (var7 < 0.0 && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
            var7 = 0.0;
         }

         param0 = new Vec3(var3, var7, var5);
      }

      return param0;
   }

   private float getFrictionInfluencedSpeed(float param0) {
      return this.onGround ? this.getSpeed() * (0.21600002F / (param0 * param0 * param0)) : this.getFlyingSpeed();
   }

   protected float getFlyingSpeed() {
      return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float param0) {
      this.speed = param0;
   }

   public boolean doHurtTarget(Entity param0) {
      this.setLastHurtMob(param0);
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      this.updatingUsingItem();
      this.updateSwimAmount();
      if (!this.level.isClientSide) {
         int var1 = this.getArrowCount();
         if (var1 > 0) {
            if (this.removeArrowTime <= 0) {
               this.removeArrowTime = 20 * (30 - var1);
            }

            this.removeArrowTime--;
            if (this.removeArrowTime <= 0) {
               this.setArrowCount(var1 - 1);
            }
         }

         int var2 = this.getStingerCount();
         if (var2 > 0) {
            if (this.removeStingerTime <= 0) {
               this.removeStingerTime = 20 * (30 - var2);
            }

            this.removeStingerTime--;
            if (this.removeStingerTime <= 0) {
               this.setStingerCount(var2 - 1);
            }
         }

         this.detectEquipmentUpdates();
         if (this.tickCount % 20 == 0) {
            this.getCombatTracker().recheckStatus();
         }

         if (this.isSleeping() && !this.checkBedExists()) {
            this.stopSleeping();
         }
      }

      if (!this.isRemoved()) {
         this.aiStep();
      }

      double var1 = this.getX() - this.xo;
      double var3 = this.getZ() - this.zo;
      float var5 = (float)(var1 * var1 + var3 * var3);
      float var6 = this.yBodyRot;
      float var7 = 0.0F;
      this.oRun = this.run;
      float var8 = 0.0F;
      if (var5 > 0.0025000002F) {
         var8 = 1.0F;
         var7 = (float)Math.sqrt(var5) * 3.0F;
         float var9 = (float)Mth.atan2(var3, var1) * (180.0F / (float)Math.PI) - 90.0F;
         float var10 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - var9);
         if (95.0F < var10 && var10 < 265.0F) {
            var6 = var9 - 180.0F;
         } else {
            var6 = var9;
         }
      }

      if (this.attackAnim > 0.0F) {
         var6 = this.getYRot();
      }

      if (!this.onGround) {
         var8 = 0.0F;
      }

      this.run = this.run + (var8 - this.run) * 0.3F;
      this.level.getProfiler().push("headTurn");
      var7 = this.tickHeadTurn(var6, var7);
      this.level.getProfiler().pop();
      this.level.getProfiler().push("rangeChecks");

      while (this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while (this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO < -180.0F) {
         this.yBodyRotO -= 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
         this.yBodyRotO += 360.0F;
      }

      while (this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while (this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO < -180.0F) {
         this.yHeadRotO -= 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
         this.yHeadRotO += 360.0F;
      }

      this.level.getProfiler().pop();
      this.animStep += var7;
      if (this.isFallFlying()) {
         this.fallFlyTicks++;
      } else {
         this.fallFlyTicks = 0;
      }

      if (this.isSleeping()) {
         this.setXRot(0.0F);
      }
   }

   private void detectEquipmentUpdates() {
      Map<EquipmentSlot, ItemStack> var1 = this.collectEquipmentChanges();
      if (var1 != null) {
         this.handleHandSwap(var1);
         if (!var1.isEmpty()) {
            this.handleEquipmentChanges(var1);
         }
      }
   }

   @Nullable
   private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
      Map<EquipmentSlot, ItemStack> var1 = null;

      for (EquipmentSlot var5 : EquipmentSlot.values()) {
         ItemStack var6;
         switch (var5.getType()) {
            case HAND:
               var6 = this.getLastHandItem(var5);
               break;
            case ARMOR:
               var6 = this.getLastArmorItem(var5);
               break;
            default:
               continue;
         }

         ItemStack var7 = this.getItemBySlot(var5);
         if (this.equipmentHasChanged(var6, var7)) {
            if (var1 == null) {
               var1 = Maps.newEnumMap(EquipmentSlot.class);
            }

            var1.put(var5, var7);
            if (!var6.isEmpty()) {
               this.getAttributes().removeAttributeModifiers(var6.getAttributeModifiers(var5));
            }

            if (!var7.isEmpty()) {
               this.getAttributes().addTransientAttributeModifiers(var7.getAttributeModifiers(var5));
            }
         }
      }

      return var1;
   }

   public boolean equipmentHasChanged(ItemStack param0, ItemStack param1) {
      return !ItemStack.matches(param1, param0);
   }

   private void handleHandSwap(Map<EquipmentSlot, ItemStack> param0) {
      ItemStack var2 = param0.get(EquipmentSlot.MAINHAND);
      ItemStack var3 = param0.get(EquipmentSlot.OFFHAND);
      if (var2 != null
         && var3 != null
         && ItemStack.matches(var2, this.getLastHandItem(EquipmentSlot.OFFHAND))
         && ItemStack.matches(var3, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
         ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
         param0.remove(EquipmentSlot.MAINHAND);
         param0.remove(EquipmentSlot.OFFHAND);
         this.setLastHandItem(EquipmentSlot.MAINHAND, var2.copy());
         this.setLastHandItem(EquipmentSlot.OFFHAND, var3.copy());
      }
   }

   private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> param0) {
      List<Pair<EquipmentSlot, ItemStack>> var2 = Lists.newArrayListWithCapacity(param0.size());
      param0.forEach((l_param1, l_param2) -> {
         ItemStack l_var4 = l_param2.copy();
         var2.add(Pair.of(l_param1, l_var4));
         switch (l_param1.getType()) {
            case HAND:
               this.setLastHandItem(l_param1, l_var4);
               break;
            case ARMOR:
               this.setLastArmorItem(l_param1, l_var4);
         }
      });
      ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), var2));
   }

   private ItemStack getLastArmorItem(EquipmentSlot param0) {
      return this.lastArmorItemStacks.get(param0.getIndex());
   }

   private void setLastArmorItem(EquipmentSlot param0, ItemStack param1) {
      this.lastArmorItemStacks.set(param0.getIndex(), param1);
   }

   private ItemStack getLastHandItem(EquipmentSlot param0) {
      return this.lastHandItemStacks.get(param0.getIndex());
   }

   private void setLastHandItem(EquipmentSlot param0, ItemStack param1) {
      this.lastHandItemStacks.set(param0.getIndex(), param1);
   }

   protected float tickHeadTurn(float param0, float param1) {
      float var3 = Mth.wrapDegrees(param0 - this.yBodyRot);
      this.yBodyRot += var3 * 0.3F;
      float var4 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
      if (Math.abs(var4) > 50.0F) {
         this.yBodyRot = this.yBodyRot + (var4 - Mth.sign(var4) * 50);
      }

      boolean var5 = var4 < -90.0F || var4 >= 90.0F;
      if (var5) {
         param1 *= -1.0F;
      }

      return param1;
   }

   public void aiStep() {
      if (this.noJumpDelay > 0) {
         this.noJumpDelay--;
      }

      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         double var1 = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
         double var3 = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
         double var5 = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
         double var7 = Mth.wrapDegrees(this.lerpYRot - this.getYRot());
         this.setYRot(this.getYRot() + (float)var7 / this.lerpSteps);
         this.setXRot(this.getXRot() + (float)(this.lerpXRot - this.getXRot()) / this.lerpSteps);
         this.lerpSteps--;
         this.setPos(var1, var3, var5);
         this.setRot(this.getYRot(), this.getXRot());
      } else if (!this.isEffectiveAi()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      }

      if (this.lerpHeadSteps > 0) {
         this.yHeadRot = this.yHeadRot + (float)Mth.wrapDegrees(this.lyHeadRot - this.yHeadRot) / this.lerpHeadSteps;
         this.lerpHeadSteps--;
      }

      Vec3 var1 = this.getDeltaMovement();
      double var2 = var1.x;
      double var4 = var1.y;
      double var6 = var1.z;
      if (Math.abs(var1.x) < 0.003) {
         var2 = 0.0;
      }

      if (Math.abs(var1.y) < 0.003) {
         var4 = 0.0;
      }

      if (Math.abs(var1.z) < 0.003) {
         var6 = 0.0;
      }

      this.setDeltaMovement(var2, var4, var6);
      this.level.getProfiler().push("ai");
      if (this.isImmobile()) {
         this.jumping = false;
         this.xxa = 0.0F;
         this.zza = 0.0F;
      } else if (this.isEffectiveAi()) {
         this.level.getProfiler().push("newAi");
         this.serverAiStep();
         this.level.getProfiler().pop();
      }

      this.level.getProfiler().pop();
      this.level.getProfiler().push("jump");
      if (this.jumping && this.isAffectedByFluids()) {
         double var8;
         if (this.isInLava()) {
            var8 = this.getFluidHeight(FluidTags.LAVA);
         } else {
            var8 = this.getFluidHeight(FluidTags.WATER);
         }

         boolean var10 = this.isInWater() && var8 > 0.0;
         double var11 = this.getFluidJumpThreshold();
         if (!var10 || this.onGround && !(var8 > var11)) {
            if (!this.isInLava() || this.onGround && !(var8 > var11)) {
               if ((this.onGround || var10 && var8 <= var11) && this.noJumpDelay == 0) {
                  this.jumpFromGround();
                  this.noJumpDelay = 10;
               }
            } else {
               this.jumpInLiquid(FluidTags.LAVA);
            }
         } else {
            this.jumpInLiquid(FluidTags.WATER);
         }
      } else {
         this.noJumpDelay = 0;
      }

      this.level.getProfiler().pop();
      this.level.getProfiler().push("travel");
      this.xxa *= 0.98F;
      this.zza *= 0.98F;
      this.updateFallFlying();
      AABB var8 = this.getBoundingBox();
      LivingEntity var9 = this.getControllingPassenger();
      Vec3 var10 = new Vec3(this.xxa, this.yya, this.zza);
      if (var9 != null && this.isAlive()) {
         this.travelRidden(var9, var10);
      } else {
         this.travel(var10);
      }

      this.level.getProfiler().pop();
      this.level.getProfiler().push("freezing");
      if (!this.level.isClientSide && !this.isDeadOrDying()) {
         int var11 = this.getTicksFrozen();
         if (this.isInPowderSnow && this.canFreeze()) {
            this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), var11 + 1));
         } else {
            this.setTicksFrozen(Math.max(0, var11 - 2));
         }
      }

      this.removeFrost();
      this.tryAddFrost();
      if (!this.level.isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
         this.hurt(this.damageSources().freeze(), 1.0F);
      }

      this.level.getProfiler().pop();
      this.level.getProfiler().push("push");
      if (this.autoSpinAttackTicks > 0) {
         this.autoSpinAttackTicks--;
         this.checkAutoSpinAttack(var8, this.getBoundingBox());
      }

      this.pushEntities();
      this.level.getProfiler().pop();
      if (!this.level.isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
         this.hurt(this.damageSources().drown(), 1.0F);
      }
   }

   public boolean isSensitiveToWater() {
      return false;
   }

   private void updateFallFlying() {
      boolean var1 = this.getSharedFlag(7);
      if (var1 && !this.onGround && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
         ItemStack var2 = this.getItemBySlot(EquipmentSlot.CHEST);
         if (var2.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(var2)) {
            var1 = true;
            int var3 = this.fallFlyTicks + 1;
            if (!this.level.isClientSide && var3 % 10 == 0) {
               int var4 = var3 / 10;
               if (var4 % 2 == 0) {
                  var2.hurtAndBreak(1, this, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.CHEST));
               }

               this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
         } else {
            var1 = false;
         }
      } else {
         var1 = false;
      }

      if (!this.level.isClientSide) {
         this.setSharedFlag(7, var1);
      }
   }

   protected void serverAiStep() {
   }

   protected void pushEntities() {
      if (this.level.isClientSide()) {
         this.level.getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
      } else {
         List<Entity> var1 = this.level.getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
         if (!var1.isEmpty()) {
            int var2 = this.level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (var2 > 0 && var1.size() > var2 - 1 && this.random.nextInt(4) == 0) {
               int var3 = 0;

               for (int var4 = 0; var4 < var1.size(); var4++) {
                  if (!var1.get(var4).isPassenger()) {
                     var3++;
                  }
               }

               if (var3 > var2 - 1) {
                  this.hurt(this.damageSources().cramming(), 6.0F);
               }
            }

            for (int var3 = 0; var3 < var1.size(); var3++) {
               Entity var4 = var1.get(var3);
               this.doPush(var4);
            }
         }
      }
   }

   protected void checkAutoSpinAttack(AABB param0, AABB param1) {
      AABB var3 = param0.minmax(param1);
      List<Entity> var4 = this.level.getEntities(this, var3);
      if (!var4.isEmpty()) {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            Entity var6 = var4.get(var5);
            if (var6 instanceof LivingEntity) {
               this.doAutoAttackOnTouch((LivingEntity)var6);
               this.autoSpinAttackTicks = 0;
               this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
               break;
            }
         }
      } else if (this.horizontalCollision) {
         this.autoSpinAttackTicks = 0;
      }

      if (!this.level.isClientSide && this.autoSpinAttackTicks <= 0) {
         this.setLivingEntityFlag(4, false);
      }
   }

   protected void doPush(Entity param0) {
      param0.push(this);
   }

   protected void doAutoAttackOnTouch(LivingEntity param0) {
   }

   public boolean isAutoSpinAttack() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
   }

   @Override
   public void stopRiding() {
      Entity var1 = this.getVehicle();
      super.stopRiding();
      if (var1 != null && var1 != this.getVehicle() && !this.level.isClientSide) {
         this.dismountVehicle(var1);
      }
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.oRun = this.run;
      this.run = 0.0F;
      this.resetFallDistance();
   }

   @Override
   public void lerpTo(double param0, double param2, double param4, float param6, float param7, int param8, boolean param9) {
      this.lerpX = param0;
      this.lerpY = param2;
      this.lerpZ = param4;
      this.lerpYRot = param6;
      this.lerpXRot = param7;
      this.lerpSteps = param8;
   }

   @Override
   public void lerpHeadTo(float param0, int param1) {
      this.lyHeadRot = param0;
      this.lerpHeadSteps = param1;
   }

   public void setJumping(boolean param0) {
      this.jumping = param0;
   }

   public void onItemPickup(ItemEntity param0) {
      Entity var2 = param0.getOwner();
      if (var2 instanceof ServerPlayer) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)var2, param0.getItem(), this);
      }
   }

   public void take(Entity param0, int param1) {
      if (!param0.isRemoved()
         && !this.level.isClientSide
         && (param0 instanceof ItemEntity || param0 instanceof AbstractArrow || param0 instanceof ExperienceOrb)) {
         ((ServerLevel)this.level).getChunkSource().broadcast(param0, new ClientboundTakeItemEntityPacket(param0.getId(), this.getId(), param1));
      }
   }

   public boolean hasLineOfSight(Entity param0) {
      if (param0.level != this.level) {
         return false;
      }

      Vec3 var2 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
      Vec3 var3 = new Vec3(param0.getX(), param0.getEyeY(), param0.getZ());
      return var3.distanceTo(var2) > 128.0
         ? false
         : this.level.clip(new ClipContext(var2, var3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
   }

   @Override
   public float getViewYRot(float param0) {
      return param0 == 1.0F ? this.yHeadRot : Mth.lerp(param0, this.yHeadRotO, this.yHeadRot);
   }

   public float getAttackAnim(float param0) {
      float var2 = this.attackAnim - this.oAttackAnim;
      if (var2 < 0.0F) {
         var2++;
      }

      return this.oAttackAnim + var2 * param0;
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.onClimbable();
   }

   @Override
   public float getYHeadRot() {
      return this.yHeadRot;
   }

   @Override
   public void setYHeadRot(float param0) {
      this.yHeadRot = param0;
   }

   @Override
   public void setYBodyRot(float param0) {
      this.yBodyRot = param0;
   }

   @Override
   protected Vec3 getRelativePortalPosition(Direction.Axis param0, BlockUtil.FoundRectangle param1) {
      return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(param0, param1));
   }

   public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 param0) {
      return new Vec3(param0.x, param0.y, 0.0);
   }

   public float getAbsorptionAmount() {
      return this.absorptionAmount;
   }

   public void setAbsorptionAmount(float param0) {
      if (param0 < 0.0F) {
         param0 = 0.0F;
      }

      this.absorptionAmount = param0;
   }

   public void onEnterCombat() {
   }

   public void onLeaveCombat() {
   }

   protected void updateEffectVisibility() {
      this.effectsDirty = true;
   }

   public abstract HumanoidArm getMainArm();

   public boolean isUsingItem() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
   }

   public InteractionHand getUsedItemHand() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private void updatingUsingItem() {
      if (this.isUsingItem()) {
         if (ItemStack.isSame(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            this.updateUsingItem(this.useItem);
         } else {
            this.stopUsingItem();
         }
      }
   }

   protected void updateUsingItem(ItemStack param0) {
      param0.onUseTick(this.level, this, this.getUseItemRemainingTicks());
      if (this.shouldTriggerItemUseEffects()) {
         this.triggerItemUseEffects(param0, 5);
      }

      if (--this.useItemRemaining == 0 && !this.level.isClientSide && !param0.useOnRelease()) {
         this.completeUsingItem();
      }
   }

   private boolean shouldTriggerItemUseEffects() {
      int var1 = this.getUseItemRemainingTicks();
      FoodProperties var2 = this.useItem.getItem().getFoodProperties();
      boolean var3 = var2 != null && var2.isFastFood();
      var3 |= var1 <= this.useItem.getUseDuration() - 7;
      return var3 && var1 % 4 == 0;
   }

   private void updateSwimAmount() {
      this.swimAmountO = this.swimAmount;
      if (this.isVisuallySwimming()) {
         this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
      } else {
         this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
      }
   }

   protected void setLivingEntityFlag(int param0, boolean param1) {
      int var3 = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
      if (param1) {
         var3 |= param0;
      } else {
         var3 &= ~param0;
      }

      this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)var3);
   }

   public void startUsingItem(InteractionHand param0) {
      ItemStack var2 = this.getItemInHand(param0);
      if (!var2.isEmpty() && !this.isUsingItem()) {
         this.useItem = var2;
         this.useItemRemaining = var2.getUseDuration();
         if (!this.level.isClientSide) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, param0 == InteractionHand.OFF_HAND);
            this.gameEvent(GameEvent.ITEM_INTERACT_START);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      super.onSyncedDataUpdated(param0);
      if (SLEEPING_POS_ID.equals(param0)) {
         if (this.level.isClientSide) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
         }
      } else if (DATA_LIVING_ENTITY_FLAGS.equals(param0) && this.level.isClientSide) {
         if (this.isUsingItem() && this.useItem.isEmpty()) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            if (!this.useItem.isEmpty()) {
               this.useItemRemaining = this.useItem.getUseDuration();
            }
         } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
            this.useItem = ItemStack.EMPTY;
            this.useItemRemaining = 0;
         }
      }
   }

   @Override
   public void lookAt(EntityAnchorArgument.Anchor param0, Vec3 param1) {
      super.lookAt(param0, param1);
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
   }

   protected void triggerItemUseEffects(ItemStack param0, int param1) {
      if (!param0.isEmpty() && this.isUsingItem()) {
         if (param0.getUseAnimation() == UseAnim.DRINK) {
            this.playSound(this.getDrinkingSound(param0), 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
         }

         if (param0.getUseAnimation() == UseAnim.EAT) {
            this.spawnItemParticles(param0, param1);
            this.playSound(this.getEatingSound(param0), 0.5F + 0.5F * this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
         }
      }
   }

   private void spawnItemParticles(ItemStack param0, int param1) {
      for (int var3 = 0; var3 < param1; var3++) {
         Vec3 var4 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
         var4 = var4.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         var4 = var4.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         double var5 = -this.random.nextFloat() * 0.6 - 0.3;
         Vec3 var7 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, var5, 0.6);
         var7 = var7.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         var7 = var7.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         var7 = var7.add(this.getX(), this.getEyeY(), this.getZ());
         this.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, param0), var7.x, var7.y, var7.z, var4.x, var4.y + 0.05, var4.z);
      }
   }

   protected void completeUsingItem() {
      if (!this.level.isClientSide || this.isUsingItem()) {
         InteractionHand var1 = this.getUsedItemHand();
         if (!this.useItem.equals(this.getItemInHand(var1))) {
            this.releaseUsingItem();
         } else {
            if (!this.useItem.isEmpty() && this.isUsingItem()) {
               this.triggerItemUseEffects(this.useItem, 16);
               ItemStack var2 = this.useItem.finishUsingItem(this.level, this);
               if (var2 != this.useItem) {
                  this.setItemInHand(var1, var2);
               }

               this.stopUsingItem();
            }
         }
      }
   }

   public ItemStack getUseItem() {
      return this.useItem;
   }

   public int getUseItemRemainingTicks() {
      return this.useItemRemaining;
   }

   public int getTicksUsingItem() {
      return this.isUsingItem() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
   }

   public void releaseUsingItem() {
      if (!this.useItem.isEmpty()) {
         this.useItem.releaseUsing(this.level, this, this.getUseItemRemainingTicks());
         if (this.useItem.useOnRelease()) {
            this.updatingUsingItem();
         }
      }

      this.stopUsingItem();
   }

   public void stopUsingItem() {
      if (!this.level.isClientSide) {
         boolean var1 = this.isUsingItem();
         this.setLivingEntityFlag(1, false);
         if (var1) {
            this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
         }
      }

      this.useItem = ItemStack.EMPTY;
      this.useItemRemaining = 0;
   }

   public boolean isBlocking() {
      if (this.isUsingItem() && !this.useItem.isEmpty()) {
         Item var1 = this.useItem.getItem();
         return var1.getUseAnimation(this.useItem) != UseAnim.BLOCK ? false : var1.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
      } else {
         return false;
      }
   }

   public boolean isSuppressingSlidingDownLadder() {
      return this.isShiftKeyDown();
   }

   public boolean isFallFlying() {
      return this.getSharedFlag(7);
   }

   @Override
   public boolean isVisuallySwimming() {
      return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
   }

   public int getFallFlyingTicks() {
      return this.fallFlyTicks;
   }

   public boolean randomTeleport(double param0, double param2, double param4, boolean param6) {
      double var8 = this.getX();
      double var10 = this.getY();
      double var12 = this.getZ();
      double var14 = param2;
      boolean var16 = false;
      BlockPos var17 = BlockPos.containing(param0, var14, param4);
      Level var18 = this.level;
      if (var18.hasChunkAt(var17)) {
         boolean var19 = false;

         while (!var19 && var17.getY() > var18.getMinBuildHeight()) {
            BlockPos var20 = var17.below();
            BlockState var21 = var18.getBlockState(var20);
            if (var21.getMaterial().blocksMotion()) {
               var19 = true;
            } else {
               var14--;
               var17 = var20;
            }
         }

         if (var19) {
            this.teleportTo(param0, var14, param4);
            if (var18.noCollision(this) && !var18.containsAnyLiquid(this.getBoundingBox())) {
               var16 = true;
            }
         }
      }

      if (!var16) {
         this.teleportTo(var8, var10, var12);
         return false;
      }

      if (param6) {
         var18.broadcastEntityEvent(this, (byte)46);
      }

      if (this instanceof PathfinderMob) {
         ((PathfinderMob)this).getNavigation().stop();
      }

      return true;
   }

   public boolean isAffectedByPotions() {
      return true;
   }

   public boolean attackable() {
      return true;
   }

   public void setRecordPlayingNearby(BlockPos param0, boolean param1) {
   }

   public boolean canTakeItem(ItemStack param0) {
      return false;
   }

   @Override
   public EntityDimensions getDimensions(Pose param0) {
      return param0 == Pose.SLEEPING ? SLEEPING_DIMENSIONS : super.getDimensions(param0).scale(this.getScale());
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING);
   }

   public AABB getLocalBoundsForPose(Pose param0) {
      EntityDimensions var2 = this.getDimensions(param0);
      return new AABB(-var2.width / 2.0F, 0.0, -var2.width / 2.0F, var2.width / 2.0F, var2.height, var2.width / 2.0F);
   }

   @Override
   public boolean canChangeDimensions() {
      return super.canChangeDimensions() && !this.isSleeping();
   }

   public Optional<BlockPos> getSleepingPos() {
      return this.entityData.get(SLEEPING_POS_ID);
   }

   public void setSleepingPos(BlockPos param0) {
      this.entityData.set(SLEEPING_POS_ID, Optional.of(param0));
   }

   public void clearSleepingPos() {
      this.entityData.set(SLEEPING_POS_ID, Optional.empty());
   }

   public boolean isSleeping() {
      return this.getSleepingPos().isPresent();
   }

   public void startSleeping(BlockPos param0) {
      if (this.isPassenger()) {
         this.stopRiding();
      }

      BlockState var2 = this.level.getBlockState(param0);
      if (var2.getBlock() instanceof BedBlock) {
         this.level.setBlock(param0, var2.setValue(BedBlock.OCCUPIED, true), 3);
      }

      this.setPose(Pose.SLEEPING);
      this.setPosToBed(param0);
      this.setSleepingPos(param0);
      this.setDeltaMovement(Vec3.ZERO);
      this.hasImpulse = true;
   }

   private void setPosToBed(BlockPos param0) {
      this.setPos(param0.getX() + 0.5, param0.getY() + 0.6875, param0.getZ() + 0.5);
   }

   private boolean checkBedExists() {
      return this.getSleepingPos().map(l_param0 -> this.level.getBlockState(l_param0).getBlock() instanceof BedBlock).orElse(false);
   }

   public void stopSleeping() {
      this.getSleepingPos().filter(this.level::hasChunkAt).ifPresent(l_param0 -> {
         BlockState l_var2 = this.level.getBlockState(l_param0);
         if (l_var2.getBlock() instanceof BedBlock) {
            Direction l_var3 = l_var2.getValue(BedBlock.FACING);
            this.level.setBlock(l_param0, l_var2.setValue(BedBlock.OCCUPIED, false), 3);
            Vec3 l_var4 = BedBlock.findStandUpPosition(this.getType(), this.level, l_param0, l_var3, this.getYRot()).orElseGet(() -> {
               BlockPos l2_var1 = l_param0.above();
               return new Vec3(l2_var1.getX() + 0.5, l2_var1.getY() + 0.1, l2_var1.getZ() + 0.5);
            });
            Vec3 l_var5 = Vec3.atBottomCenterOf(l_param0).subtract(l_var4).normalize();
            float l_var6 = (float)Mth.wrapDegrees(Mth.atan2(l_var5.z, l_var5.x) * 180.0F / (float)Math.PI - 90.0);
            this.setPos(l_var4.x, l_var4.y, l_var4.z);
            this.setYRot(l_var6);
            this.setXRot(0.0F);
         }
      });
      Vec3 var1 = this.position();
      this.setPose(Pose.STANDING);
      this.setPos(var1.x, var1.y, var1.z);
      this.clearSleepingPos();
   }

   @Nullable
   public Direction getBedOrientation() {
      BlockPos var1 = this.getSleepingPos().orElse(null);
      return var1 != null ? BedBlock.getBedOrientation(this.level, var1) : null;
   }

   @Override
   public boolean isInWall() {
      return !this.isSleeping() && super.isInWall();
   }

   @Override
   protected final float getEyeHeight(Pose param0, EntityDimensions param1) {
      return param0 == Pose.SLEEPING ? 0.2F : this.getStandingEyeHeight(param0, param1);
   }

   protected float getStandingEyeHeight(Pose param0, EntityDimensions param1) {
      return super.getEyeHeight(param0, param1);
   }

   public ItemStack getProjectile(ItemStack param0) {
      return ItemStack.EMPTY;
   }

   public ItemStack eat(Level param0, ItemStack param1) {
      if (param1.isEdible()) {
         param0.playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            this.getEatingSound(param1),
            SoundSource.NEUTRAL,
            1.0F,
            1.0F + (param0.random.nextFloat() - param0.random.nextFloat()) * 0.4F
         );
         this.addEatEffect(param1, param0, this);
         if (!(this instanceof Player) || !((Player)this).getAbilities().instabuild) {
            param1.shrink(1);
         }

         this.gameEvent(GameEvent.EAT);
      }

      return param1;
   }

   private void addEatEffect(ItemStack param0, Level param1, LivingEntity param2) {
      Item var4 = param0.getItem();
      if (var4.isEdible()) {
         for (Pair<MobEffectInstance, Float> var7 : var4.getFoodProperties().getEffects()) {
            if (!param1.isClientSide && var7.getFirst() != null && param1.random.nextFloat() < var7.getSecond()) {
               param2.addEffect(new MobEffectInstance(var7.getFirst()));
            }
         }
      }
   }

   private static byte entityEventForEquipmentBreak(EquipmentSlot param0) {
      switch (param0) {
         case MAINHAND:
            return 47;
         case OFFHAND:
            return 48;
         case HEAD:
            return 49;
         case CHEST:
            return 50;
         case FEET:
            return 52;
         case LEGS:
            return 51;
         default:
            return 47;
      }
   }

   public void broadcastBreakEvent(EquipmentSlot param0) {
      this.level.broadcastEntityEvent(this, entityEventForEquipmentBreak(param0));
   }

   public void broadcastBreakEvent(InteractionHand param0) {
      this.broadcastBreakEvent(param0 == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
         float var1 = 0.5F;
         return this.getBoundingBox().inflate(0.5, 0.5, 0.5);
      } else {
         return super.getBoundingBoxForCulling();
      }
   }

   public static EquipmentSlot getEquipmentSlotForItem(ItemStack param0) {
      Equipable var1 = Equipable.get(param0);
      return var1 != null ? var1.getEquipmentSlot() : EquipmentSlot.MAINHAND;
   }

   private static SlotAccess createEquipmentSlotAccess(LivingEntity param0, EquipmentSlot param1) {
      return param1 != EquipmentSlot.HEAD && param1 != EquipmentSlot.MAINHAND && param1 != EquipmentSlot.OFFHAND
         ? SlotAccess.forEquipmentSlot(param0, param1, l_param1 -> l_param1.isEmpty() || Mob.getEquipmentSlotForItem(l_param1) == param1)
         : SlotAccess.forEquipmentSlot(param0, param1);
   }

   @Nullable
   private static EquipmentSlot getEquipmentSlot(int param0) {
      if (param0 == 100 + EquipmentSlot.HEAD.getIndex()) {
         return EquipmentSlot.HEAD;
      } else if (param0 == 100 + EquipmentSlot.CHEST.getIndex()) {
         return EquipmentSlot.CHEST;
      } else if (param0 == 100 + EquipmentSlot.LEGS.getIndex()) {
         return EquipmentSlot.LEGS;
      } else if (param0 == 100 + EquipmentSlot.FEET.getIndex()) {
         return EquipmentSlot.FEET;
      } else if (param0 == 98) {
         return EquipmentSlot.MAINHAND;
      } else {
         return param0 == 99 ? EquipmentSlot.OFFHAND : null;
      }
   }

   @Override
   public SlotAccess getSlot(int param0) {
      EquipmentSlot var2 = getEquipmentSlot(param0);
      return var2 != null ? createEquipmentSlotAccess(this, var2) : super.getSlot(param0);
   }

   @Override
   public boolean canFreeze() {
      if (this.isSpectator()) {
         return false;
      }

      boolean var1 = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
         && !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
         && !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
         && !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
      return var1 && super.canFreeze();
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.level.isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.yBodyRot;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket param0) {
      double var2 = param0.getX();
      double var4 = param0.getY();
      double var6 = param0.getZ();
      float var8 = param0.getYRot();
      float var9 = param0.getXRot();
      this.syncPacketPositionCodec(var2, var4, var6);
      this.yBodyRot = param0.getYHeadRot();
      this.yHeadRot = param0.getYHeadRot();
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.setId(param0.getId());
      this.setUUID(param0.getUUID());
      this.absMoveTo(var2, var4, var6, var8, var9);
      this.setDeltaMovement(param0.getXa(), param0.getYa(), param0.getZa());
   }

   public boolean canDisableShield() {
      return this.getMainHandItem().getItem() instanceof AxeItem;
   }

   @Override
   public float maxUpStep() {
      float var1 = super.maxUpStep();
      return this.getControllingPassenger() instanceof Player ? Math.max(var1, 1.0F) : var1;
   }

   public record Fallsounds(SoundEvent small, SoundEvent big) {
   }
}
