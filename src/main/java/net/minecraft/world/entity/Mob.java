package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class Mob extends LivingEntity implements Targeting {
   private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
   private static final int MOB_FLAG_NO_AI = 1;
   private static final int MOB_FLAG_LEFTHANDED = 2;
   private static final int MOB_FLAG_AGGRESSIVE = 4;
   protected static final int PICKUP_REACH = 1;
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
   public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
   public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
   public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
   public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
   public static final String LEASH_TAG = "Leash";
   public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
   public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
   public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
   public int ambientSoundTime;
   protected int xpReward;
   protected LookControl lookControl;
   protected MoveControl moveControl;
   protected JumpControl jumpControl;
   private final BodyRotationControl bodyRotationControl;
   protected PathNavigation navigation;
   protected final GoalSelector goalSelector;
   protected final GoalSelector targetSelector;
   @Nullable
   private LivingEntity target;
   private final Sensing sensing;
   private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
   protected final float[] handDropChances = new float[2];
   private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
   protected final float[] armorDropChances = new float[4];
   private boolean canPickUpLoot;
   private boolean persistenceRequired;
   private final Map<BlockPathTypes, Float> pathfindingMalus = Maps.newEnumMap(BlockPathTypes.class);
   @Nullable
   private ResourceLocation lootTable;
   private long lootTableSeed;
   @Nullable
   private Entity leashHolder;
   private int delayedLeashHolderId;
   @Nullable
   private CompoundTag leashInfoTag;
   private BlockPos restrictCenter = BlockPos.ZERO;
   private float restrictRadius = -1.0F;

   protected Mob(EntityType<? extends Mob> param0, Level param1) {
      super(param0, param1);
      this.goalSelector = new GoalSelector(param1.getProfilerSupplier());
      this.targetSelector = new GoalSelector(param1.getProfilerSupplier());
      this.lookControl = new LookControl(this);
      this.moveControl = new MoveControl(this);
      this.jumpControl = new JumpControl(this);
      this.bodyRotationControl = this.createBodyControl();
      this.navigation = this.createNavigation(param1);
      this.sensing = new Sensing(this);
      Arrays.fill(this.armorDropChances, 0.085F);
      Arrays.fill(this.handDropChances, 0.085F);
      if (param1 != null && !param1.isClientSide) {
         this.registerGoals();
      }
   }

   protected void registerGoals() {
   }

   public static AttributeSupplier.Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.ATTACK_KNOCKBACK);
   }

   protected PathNavigation createNavigation(Level param0) {
      return new GroundPathNavigation(this, param0);
   }

   protected boolean shouldPassengersInheritMalus() {
      return false;
   }

   public float getPathfindingMalus(BlockPathTypes param0) {
      Mob var2;
      if (this.getControlledVehicle() instanceof Mob var3 && var3.shouldPassengersInheritMalus()) {
         var2 = var3;
      } else {
         var2 = this;
      }

      Float var3 = var2.pathfindingMalus.get(param0);
      return var3 == null ? param0.getMalus() : var3;
   }

   public void setPathfindingMalus(BlockPathTypes param0, float param1) {
      this.pathfindingMalus.put(param0, param1);
   }

   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this);
   }

   public LookControl getLookControl() {
      return this.lookControl;
   }

   public MoveControl getMoveControl() {
      return this.getControlledVehicle() instanceof Mob var1 ? var1.getMoveControl() : this.moveControl;
   }

   public JumpControl getJumpControl() {
      return this.jumpControl;
   }

   public PathNavigation getNavigation() {
      return this.getControlledVehicle() instanceof Mob var1 ? var1.getNavigation() : this.navigation;
   }

   @Nullable
   @Override
   public LivingEntity getControllingPassenger() {
      return !this.isNoAi() && this.getFirstPassenger() instanceof Mob var1 ? var1 : null;
   }

   public Sensing getSensing() {
      return this.sensing;
   }

   @Nullable
   @Override
   public LivingEntity getTarget() {
      return this.target;
   }

   public void setTarget(@Nullable LivingEntity param0) {
      this.target = param0;
   }

   @Override
   public boolean canAttackType(EntityType<?> param0) {
      return param0 != EntityType.GHAST;
   }

   public boolean canFireProjectileWeapon(ProjectileWeaponItem param0) {
      return false;
   }

   public void ate() {
      this.gameEvent(GameEvent.EAT);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_MOB_FLAGS_ID, (byte)0);
   }

   public int getAmbientSoundInterval() {
      return 80;
   }

   public void playAmbientSound() {
      SoundEvent var1 = this.getAmbientSound();
      if (var1 != null) {
         this.playSound(var1, this.getSoundVolume(), this.getVoicePitch());
      }
   }

   @Override
   public void baseTick() {
      super.baseTick();
      this.level.getProfiler().push("mobBaseTick");
      if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
         this.resetAmbientSoundTime();
         this.playAmbientSound();
      }

      this.level.getProfiler().pop();
   }

   @Override
   protected void playHurtSound(DamageSource param0) {
      this.resetAmbientSoundTime();
      super.playHurtSound(param0);
   }

   private void resetAmbientSoundTime() {
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   @Override
   public int getExperienceReward() {
      if (this.xpReward > 0) {
         int var1 = this.xpReward;

         for (int var2 = 0; var2 < this.armorItems.size(); var2++) {
            if (!this.armorItems.get(var2).isEmpty() && this.armorDropChances[var2] <= 1.0F) {
               var1 += 1 + this.random.nextInt(3);
            }
         }

         for (int var2 = 0; var2 < this.handItems.size(); var2++) {
            if (!this.handItems.get(var2).isEmpty() && this.handDropChances[var2] <= 1.0F) {
               var1 += 1 + this.random.nextInt(3);
            }
         }

         return var1;
      } else {
         return this.xpReward;
      }
   }

   public void spawnAnim() {
      if (this.level.isClientSide) {
         for (int var1 = 0; var1 < 20; var1++) {
            double var2 = this.random.nextGaussian() * 0.02;
            double var4 = this.random.nextGaussian() * 0.02;
            double var6 = this.random.nextGaussian() * 0.02;
            double var8 = 10.0;
            this.level
               .addParticle(
                  ParticleTypes.POOF, this.getX(1.0) - var2 * 10.0, this.getRandomY() - var4 * 10.0, this.getRandomZ(1.0) - var6 * 10.0, var2, var4, var6
               );
         }
      } else {
         this.level.broadcastEntityEvent(this, (byte)20);
      }
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 == 20) {
         this.spawnAnim();
      } else {
         super.handleEntityEvent(param0);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         this.tickLeash();
         if (this.tickCount % 5 == 0) {
            this.updateControlFlags();
         }
      }
   }

   protected void updateControlFlags() {
      boolean var1 = !(this.getControllingPassenger() instanceof Mob);
      boolean var2 = !(this.getVehicle() instanceof Boat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, var1);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, var1 && var2);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, var1);
   }

   @Override
   protected float tickHeadTurn(float param0, float param1) {
      this.bodyRotationControl.clientTick();
      return param1;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putBoolean("CanPickUpLoot", this.canPickUpLoot());
      param0.putBoolean("PersistenceRequired", this.persistenceRequired);
      ListTag var2 = new ListTag();

      for (ItemStack var4 : this.armorItems) {
         CompoundTag var5 = new CompoundTag();
         if (!var4.isEmpty()) {
            var4.save(var5);
         }

         var2.add(var5);
      }

      param0.put("ArmorItems", var2);
      ListTag var3 = new ListTag();

      for (ItemStack var5 : this.handItems) {
         CompoundTag var6 = new CompoundTag();
         if (!var5.isEmpty()) {
            var5.save(var6);
         }

         var3.add(var6);
      }

      param0.put("HandItems", var3);
      ListTag var4 = new ListTag();

      for (float var8 : this.armorDropChances) {
         var4.add(FloatTag.valueOf(var8));
      }

      param0.put("ArmorDropChances", var4);
      ListTag var5 = new ListTag();

      for (float var9 : this.handDropChances) {
         var5.add(FloatTag.valueOf(var9));
      }

      param0.put("HandDropChances", var5);
      if (this.leashHolder != null) {
         CompoundTag var6 = new CompoundTag();
         if (this.leashHolder instanceof LivingEntity) {
            UUID var7 = this.leashHolder.getUUID();
            var6.putUUID("UUID", var7);
         } else if (this.leashHolder instanceof HangingEntity) {
            BlockPos var7 = ((HangingEntity)this.leashHolder).getPos();
            var6.putInt("X", var7.getX());
            var6.putInt("Y", var7.getY());
            var6.putInt("Z", var7.getZ());
         }

         param0.put("Leash", var6);
      } else if (this.leashInfoTag != null) {
         param0.put("Leash", this.leashInfoTag.copy());
      }

      param0.putBoolean("LeftHanded", this.isLeftHanded());
      if (this.lootTable != null) {
         param0.putString("DeathLootTable", this.lootTable.toString());
         if (this.lootTableSeed != 0L) {
            param0.putLong("DeathLootTableSeed", this.lootTableSeed);
         }
      }

      if (this.isNoAi()) {
         param0.putBoolean("NoAI", this.isNoAi());
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      if (param0.contains("CanPickUpLoot", 1)) {
         this.setCanPickUpLoot(param0.getBoolean("CanPickUpLoot"));
      }

      this.persistenceRequired = param0.getBoolean("PersistenceRequired");
      if (param0.contains("ArmorItems", 9)) {
         ListTag var2 = param0.getList("ArmorItems", 10);

         for (int var3 = 0; var3 < this.armorItems.size(); var3++) {
            this.armorItems.set(var3, ItemStack.of(var2.getCompound(var3)));
         }
      }

      if (param0.contains("HandItems", 9)) {
         ListTag var2 = param0.getList("HandItems", 10);

         for (int var3 = 0; var3 < this.handItems.size(); var3++) {
            this.handItems.set(var3, ItemStack.of(var2.getCompound(var3)));
         }
      }

      if (param0.contains("ArmorDropChances", 9)) {
         ListTag var2 = param0.getList("ArmorDropChances", 5);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            this.armorDropChances[var3] = var2.getFloat(var3);
         }
      }

      if (param0.contains("HandDropChances", 9)) {
         ListTag var2 = param0.getList("HandDropChances", 5);

         for (int var3 = 0; var3 < var2.size(); var3++) {
            this.handDropChances[var3] = var2.getFloat(var3);
         }
      }

      if (param0.contains("Leash", 10)) {
         this.leashInfoTag = param0.getCompound("Leash");
      }

      this.setLeftHanded(param0.getBoolean("LeftHanded"));
      if (param0.contains("DeathLootTable", 8)) {
         this.lootTable = new ResourceLocation(param0.getString("DeathLootTable"));
         this.lootTableSeed = param0.getLong("DeathLootTableSeed");
      }

      this.setNoAi(param0.getBoolean("NoAI"));
   }

   @Override
   protected void dropFromLootTable(DamageSource param0, boolean param1) {
      super.dropFromLootTable(param0, param1);
      this.lootTable = null;
   }

   @Override
   protected LootContext.Builder createLootContext(boolean param0, DamageSource param1) {
      return super.createLootContext(param0, param1).withOptionalRandomSeed(this.lootTableSeed, this.random);
   }

   @Override
   public final ResourceLocation getLootTable() {
      return this.lootTable == null ? this.getDefaultLootTable() : this.lootTable;
   }

   protected ResourceLocation getDefaultLootTable() {
      return super.getLootTable();
   }

   public void setZza(float param0) {
      this.zza = param0;
   }

   public void setYya(float param0) {
      this.yya = param0;
   }

   public void setXxa(float param0) {
      this.xxa = param0;
   }

   @Override
   public void setSpeed(float param0) {
      super.setSpeed(param0);
      this.setZza(param0);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.level.getProfiler().push("looting");
      if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         Vec3i var1 = this.getPickupReach();

         for (ItemEntity var4 : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(var1.getX(), var1.getY(), var1.getZ()))) {
            if (!var4.isRemoved() && !var4.getItem().isEmpty() && !var4.hasPickUpDelay() && this.wantsToPickUp(var4.getItem())) {
               this.pickUpItem(var4);
            }
         }
      }

      this.level.getProfiler().pop();
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   protected void pickUpItem(ItemEntity param0) {
      ItemStack var2 = param0.getItem();
      ItemStack var3 = this.equipItemIfPossible(var2.copy());
      if (!var3.isEmpty()) {
         this.onItemPickup(param0);
         this.take(param0, var3.getCount());
         var2.shrink(var3.getCount());
         if (var2.isEmpty()) {
            param0.discard();
         }
      }
   }

   public ItemStack equipItemIfPossible(ItemStack param0) {
      EquipmentSlot var2 = getEquipmentSlotForItem(param0);
      ItemStack var3 = this.getItemBySlot(var2);
      boolean var4 = this.canReplaceCurrentItem(param0, var3);
      if (var2.isArmor() && !var4) {
         var2 = EquipmentSlot.MAINHAND;
         var3 = this.getItemBySlot(var2);
         var4 = this.canReplaceCurrentItem(param0, var3);
      }

      if (var4 && this.canHoldItem(param0)) {
         double var5 = this.getEquipmentDropChance(var2);
         if (!var3.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < var5) {
            this.spawnAtLocation(var3);
         }

         if (var2.isArmor() && param0.getCount() > 1) {
            ItemStack var7 = param0.copyWithCount(1);
            this.setItemSlotAndDropWhenKilled(var2, var7);
            return var7;
         } else {
            this.setItemSlotAndDropWhenKilled(var2, param0);
            return param0;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   protected void setItemSlotAndDropWhenKilled(EquipmentSlot param0, ItemStack param1) {
      this.setItemSlot(param0, param1);
      this.setGuaranteedDrop(param0);
      this.persistenceRequired = true;
   }

   public void setGuaranteedDrop(EquipmentSlot param0) {
      switch (param0.getType()) {
         case HAND:
            this.handDropChances[param0.getIndex()] = 2.0F;
            break;
         case ARMOR:
            this.armorDropChances[param0.getIndex()] = 2.0F;
      }
   }

   protected boolean canReplaceCurrentItem(ItemStack param0, ItemStack param1) {
      if (param1.isEmpty()) {
         return true;
      }

      if (param0.getItem() instanceof SwordItem) {
         if (!(param1.getItem() instanceof SwordItem)) {
            return true;
         }

         SwordItem var3 = (SwordItem)param0.getItem();
         SwordItem var4 = (SwordItem)param1.getItem();
         return var3.getDamage() != var4.getDamage() ? var3.getDamage() > var4.getDamage() : this.canReplaceEqualItem(param0, param1);
      } else {
         if (param0.getItem() instanceof BowItem && param1.getItem() instanceof BowItem) {
            return this.canReplaceEqualItem(param0, param1);
         }

         if (param0.getItem() instanceof CrossbowItem && param1.getItem() instanceof CrossbowItem) {
            return this.canReplaceEqualItem(param0, param1);
         }

         if (param0.getItem() instanceof ArmorItem) {
            if (EnchantmentHelper.hasBindingCurse(param1)) {
               return false;
            } else if (!(param1.getItem() instanceof ArmorItem)) {
               return true;
            } else {
               ArmorItem var3 = (ArmorItem)param0.getItem();
               ArmorItem var4 = (ArmorItem)param1.getItem();
               if (var3.getDefense() != var4.getDefense()) {
                  return var3.getDefense() > var4.getDefense();
               } else {
                  return var3.getToughness() != var4.getToughness() ? var3.getToughness() > var4.getToughness() : this.canReplaceEqualItem(param0, param1);
               }
            }
         } else {
            if (param0.getItem() instanceof DiggerItem) {
               if (param1.getItem() instanceof BlockItem) {
                  return true;
               }

               if (param1.getItem() instanceof DiggerItem) {
                  DiggerItem var3 = (DiggerItem)param0.getItem();
                  DiggerItem var4 = (DiggerItem)param1.getItem();
                  if (var3.getAttackDamage() != var4.getAttackDamage()) {
                     return var3.getAttackDamage() > var4.getAttackDamage();
                  }

                  return this.canReplaceEqualItem(param0, param1);
               }
            }

            return false;
         }
      }
   }

   public boolean canReplaceEqualItem(ItemStack param0, ItemStack param1) {
      if (param0.getDamageValue() >= param1.getDamageValue() && (!param0.hasTag() || param1.hasTag())) {
         return param0.hasTag() && param1.hasTag()
            ? param0.getTag().getAllKeys().stream().anyMatch(l_param0 -> !l_param0.equals("Damage"))
               && !param1.getTag().getAllKeys().stream().anyMatch(l_param0 -> !l_param0.equals("Damage"))
            : false;
      } else {
         return true;
      }
   }

   public boolean canHoldItem(ItemStack param0) {
      return true;
   }

   public boolean wantsToPickUp(ItemStack param0) {
      return this.canHoldItem(param0);
   }

   public boolean removeWhenFarAway(double param0) {
      return true;
   }

   public boolean requiresCustomPersistence() {
      return this.isPassenger();
   }

   protected boolean shouldDespawnInPeaceful() {
      return false;
   }

   @Override
   public void checkDespawn() {
      if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
         this.discard();
      } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
         Entity var1 = this.level.getNearestPlayer(this, -1.0);
         if (var1 != null) {
            double var2 = var1.distanceToSqr(this);
            int var4 = this.getType().getCategory().getDespawnDistance();
            int var5 = var4 * var4;
            if (var2 > var5 && this.removeWhenFarAway(var2)) {
               this.discard();
            }

            int var6 = this.getType().getCategory().getNoDespawnDistance();
            int var7 = var6 * var6;
            if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && var2 > var7 && this.removeWhenFarAway(var2)) {
               this.discard();
            } else if (var2 < var7) {
               this.noActionTime = 0;
            }
         }
      } else {
         this.noActionTime = 0;
      }
   }

   @Override
   protected final void serverAiStep() {
      this.noActionTime++;
      this.level.getProfiler().push("sensing");
      this.sensing.tick();
      this.level.getProfiler().pop();
      int var1 = this.level.getServer().getTickCount() + this.getId();
      if (var1 % 2 != 0 && this.tickCount > 1) {
         this.level.getProfiler().push("targetSelector");
         this.targetSelector.tickRunningGoals(false);
         this.level.getProfiler().pop();
         this.level.getProfiler().push("goalSelector");
         this.goalSelector.tickRunningGoals(false);
         this.level.getProfiler().pop();
      } else {
         this.level.getProfiler().push("targetSelector");
         this.targetSelector.tick();
         this.level.getProfiler().pop();
         this.level.getProfiler().push("goalSelector");
         this.goalSelector.tick();
         this.level.getProfiler().pop();
      }

      this.level.getProfiler().push("navigation");
      this.navigation.tick();
      this.level.getProfiler().pop();
      this.level.getProfiler().push("mob tick");
      this.customServerAiStep();
      this.level.getProfiler().pop();
      this.level.getProfiler().push("controls");
      this.level.getProfiler().push("move");
      this.moveControl.tick();
      this.level.getProfiler().popPush("look");
      this.lookControl.tick();
      this.level.getProfiler().popPush("jump");
      this.jumpControl.tick();
      this.level.getProfiler().pop();
      this.level.getProfiler().pop();
      this.sendDebugPackets();
   }

   protected void sendDebugPackets() {
      DebugPackets.sendGoalSelector(this.level, this, this.goalSelector);
   }

   protected void customServerAiStep() {
   }

   public int getMaxHeadXRot() {
      return 40;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   public int getHeadRotSpeed() {
      return 10;
   }

   public void lookAt(Entity param0, float param1, float param2) {
      double var4 = param0.getX() - this.getX();
      double var8 = param0.getZ() - this.getZ();
      double var6;
      if (param0 instanceof LivingEntity var10) {
         var6 = var10.getEyeY() - this.getEyeY();
      } else {
         var6 = (param0.getBoundingBox().minY + param0.getBoundingBox().maxY) / 2.0 - this.getEyeY();
      }

      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)(Mth.atan2(var8, var4) * 180.0F / (float)Math.PI) - 90.0F;
      float var13 = (float)(-(Mth.atan2(var6, var10) * 180.0F / (float)Math.PI));
      this.setXRot(this.rotlerp(this.getXRot(), var13, param2));
      this.setYRot(this.rotlerp(this.getYRot(), var12, param1));
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

   public static boolean checkMobSpawnRules(EntityType<? extends Mob> param0, LevelAccessor param1, MobSpawnType param2, BlockPos param3, RandomSource param4) {
      BlockPos var5 = param3.below();
      return param2 == MobSpawnType.SPAWNER || param1.getBlockState(var5).isValidSpawn(param1, var5, param0);
   }

   public boolean checkSpawnRules(LevelAccessor param0, MobSpawnType param1) {
      return true;
   }

   public boolean checkSpawnObstruction(LevelReader param0) {
      return !param0.containsAnyLiquid(this.getBoundingBox()) && param0.isUnobstructed(this);
   }

   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isMaxGroupSizeReached(int param0) {
      return false;
   }

   @Override
   public int getMaxFallDistance() {
      if (this.getTarget() == null) {
         return 3;
      }

      int var1 = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
      var1 -= (3 - this.level.getDifficulty().getId()) * 4;
      if (var1 < 0) {
         var1 = 0;
      }

      return var1 + 3;
   }

   @Override
   public Iterable<ItemStack> getHandSlots() {
      return this.handItems;
   }

   @Override
   public Iterable<ItemStack> getArmorSlots() {
      return this.armorItems;
   }

   @Override
   public ItemStack getItemBySlot(EquipmentSlot param0) {
      switch (param0.getType()) {
         case HAND:
            return this.handItems.get(param0.getIndex());
         case ARMOR:
            return this.armorItems.get(param0.getIndex());
         default:
            return ItemStack.EMPTY;
      }
   }

   @Override
   public void setItemSlot(EquipmentSlot param0, ItemStack param1) {
      this.verifyEquippedItem(param1);
      switch (param0.getType()) {
         case HAND:
            this.onEquipItem(param0, this.handItems.set(param0.getIndex(), param1), param1);
            break;
         case ARMOR:
            this.onEquipItem(param0, this.armorItems.set(param0.getIndex(), param1), param1);
      }
   }

   @Override
   protected void dropCustomDeathLoot(DamageSource param0, int param1, boolean param2) {
      super.dropCustomDeathLoot(param0, param1, param2);

      for (EquipmentSlot var7 : EquipmentSlot.values()) {
         ItemStack var8 = this.getItemBySlot(var7);
         float var9 = this.getEquipmentDropChance(var7);
         boolean var10 = var9 > 1.0F;
         if (!var8.isEmpty()
            && !EnchantmentHelper.hasVanishingCurse(var8)
            && (param2 || var10)
            && Math.max(this.random.nextFloat() - param1 * 0.01F, 0.0F) < var9) {
            if (!var10 && var8.isDamageableItem()) {
               var8.setDamageValue(var8.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(var8.getMaxDamage() - 3, 1))));
            }

            this.spawnAtLocation(var8);
            this.setItemSlot(var7, ItemStack.EMPTY);
         }
      }
   }

   protected float getEquipmentDropChance(EquipmentSlot param0) {
      return switch (param0.getType()) {
         case HAND -> this.handDropChances[param0.getIndex()];
         case ARMOR -> this.armorDropChances[param0.getIndex()];
         default -> 0.0F;
      };
   }

   protected void populateDefaultEquipmentSlots(RandomSource param0, DifficultyInstance param1) {
      if (param0.nextFloat() < 0.15F * param1.getSpecialMultiplier()) {
         int var3 = param0.nextInt(2);
         float var4 = this.level.getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
         if (param0.nextFloat() < 0.095F) {
            var3++;
         }

         if (param0.nextFloat() < 0.095F) {
            var3++;
         }

         if (param0.nextFloat() < 0.095F) {
            var3++;
         }

         boolean var5 = true;

         for (EquipmentSlot var9 : EquipmentSlot.values()) {
            if (var9.getType() == EquipmentSlot.Type.ARMOR) {
               ItemStack var10 = this.getItemBySlot(var9);
               if (!var5 && param0.nextFloat() < var4) {
                  break;
               }

               var5 = false;
               if (var10.isEmpty()) {
                  Item var11 = getEquipmentForSlot(var9, var3);
                  if (var11 != null) {
                     this.setItemSlot(var9, new ItemStack(var11));
                  }
               }
            }
         }
      }
   }

   @Nullable
   public static Item getEquipmentForSlot(EquipmentSlot param0, int param1) {
      switch (param0) {
         case HEAD:
            if (param1 == 0) {
               return Items.LEATHER_HELMET;
            } else if (param1 == 1) {
               return Items.GOLDEN_HELMET;
            } else if (param1 == 2) {
               return Items.CHAINMAIL_HELMET;
            } else if (param1 == 3) {
               return Items.IRON_HELMET;
            } else if (param1 == 4) {
               return Items.DIAMOND_HELMET;
            }
         case CHEST:
            if (param1 == 0) {
               return Items.LEATHER_CHESTPLATE;
            } else if (param1 == 1) {
               return Items.GOLDEN_CHESTPLATE;
            } else if (param1 == 2) {
               return Items.CHAINMAIL_CHESTPLATE;
            } else if (param1 == 3) {
               return Items.IRON_CHESTPLATE;
            } else if (param1 == 4) {
               return Items.DIAMOND_CHESTPLATE;
            }
         case LEGS:
            if (param1 == 0) {
               return Items.LEATHER_LEGGINGS;
            } else if (param1 == 1) {
               return Items.GOLDEN_LEGGINGS;
            } else if (param1 == 2) {
               return Items.CHAINMAIL_LEGGINGS;
            } else if (param1 == 3) {
               return Items.IRON_LEGGINGS;
            } else if (param1 == 4) {
               return Items.DIAMOND_LEGGINGS;
            }
         case FEET:
            if (param1 == 0) {
               return Items.LEATHER_BOOTS;
            } else if (param1 == 1) {
               return Items.GOLDEN_BOOTS;
            } else if (param1 == 2) {
               return Items.CHAINMAIL_BOOTS;
            } else if (param1 == 3) {
               return Items.IRON_BOOTS;
            } else if (param1 == 4) {
               return Items.DIAMOND_BOOTS;
            }
         default:
            return null;
      }
   }

   protected void populateDefaultEquipmentEnchantments(RandomSource param0, DifficultyInstance param1) {
      float var3 = param1.getSpecialMultiplier();
      this.enchantSpawnedWeapon(param0, var3);

      for (EquipmentSlot var7 : EquipmentSlot.values()) {
         if (var7.getType() == EquipmentSlot.Type.ARMOR) {
            this.enchantSpawnedArmor(param0, var3, var7);
         }
      }
   }

   protected void enchantSpawnedWeapon(RandomSource param0, float param1) {
      if (!this.getMainHandItem().isEmpty() && param0.nextFloat() < 0.25F * param1) {
         this.setItemSlot(
            EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(param0, this.getMainHandItem(), (int)(5.0F + param1 * param0.nextInt(18)), false)
         );
      }
   }

   protected void enchantSpawnedArmor(RandomSource param0, float param1, EquipmentSlot param2) {
      ItemStack var4 = this.getItemBySlot(param2);
      if (!var4.isEmpty() && param0.nextFloat() < 0.5F * param1) {
         this.setItemSlot(param2, EnchantmentHelper.enchantItem(param0, var4, (int)(5.0F + param1 * param0.nextInt(18)), false));
      }
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor param0, DifficultyInstance param1, MobSpawnType param2, @Nullable SpawnGroupData param3, @Nullable CompoundTag param4
   ) {
      RandomSource var6 = param0.getRandom();
      this.getAttribute(Attributes.FOLLOW_RANGE)
         .addPermanentModifier(new AttributeModifier("Random spawn bonus", var6.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.MULTIPLY_BASE));
      if (var6.nextFloat() < 0.05F) {
         this.setLeftHanded(true);
      } else {
         this.setLeftHanded(false);
      }

      return param3;
   }

   public void setPersistenceRequired() {
      this.persistenceRequired = true;
   }

   public void setDropChance(EquipmentSlot param0, float param1) {
      switch (param0.getType()) {
         case HAND:
            this.handDropChances[param0.getIndex()] = param1;
            break;
         case ARMOR:
            this.armorDropChances[param0.getIndex()] = param1;
      }
   }

   public boolean canPickUpLoot() {
      return this.canPickUpLoot;
   }

   public void setCanPickUpLoot(boolean param0) {
      this.canPickUpLoot = param0;
   }

   @Override
   public boolean canTakeItem(ItemStack param0) {
      EquipmentSlot var2 = getEquipmentSlotForItem(param0);
      return this.getItemBySlot(var2).isEmpty() && this.canPickUpLoot();
   }

   public boolean isPersistenceRequired() {
      return this.persistenceRequired;
   }

   @Override
   public final InteractionResult interact(Player param0, InteractionHand param1) {
      if (!this.isAlive()) {
         return InteractionResult.PASS;
      } else if (this.getLeashHolder() == param0) {
         this.dropLeash(true, !param0.getAbilities().instabuild);
         this.gameEvent(GameEvent.ENTITY_INTERACT, param0);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      } else {
         InteractionResult var3 = this.checkAndHandleImportantInteractions(param0, param1);
         if (var3.consumesAction()) {
            this.gameEvent(GameEvent.ENTITY_INTERACT, param0);
            return var3;
         } else {
            var3 = this.mobInteract(param0, param1);
            if (var3.consumesAction()) {
               this.gameEvent(GameEvent.ENTITY_INTERACT, param0);
               return var3;
            } else {
               return super.interact(param0, param1);
            }
         }
      }
   }

   private InteractionResult checkAndHandleImportantInteractions(Player param0, InteractionHand param1) {
      ItemStack var3 = param0.getItemInHand(param1);
      if (var3.is(Items.LEAD) && this.canBeLeashed(param0)) {
         this.setLeashedTo(param0, true);
         var3.shrink(1);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      }

      if (var3.is(Items.NAME_TAG)) {
         InteractionResult var4 = var3.interactLivingEntity(param0, this, param1);
         if (var4.consumesAction()) {
            return var4;
         }
      }

      if (var3.getItem() instanceof SpawnEggItem) {
         if (this.level instanceof ServerLevel) {
            SpawnEggItem var4 = (SpawnEggItem)var3.getItem();
            Optional<Mob> var5 = var4.spawnOffspringFromSpawnEgg(
               param0, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level, this.position(), var3
            );
            var5.ifPresent(l_param1 -> this.onOffspringSpawnedFromEgg(param0, l_param1));
            return var5.isPresent() ? InteractionResult.SUCCESS : InteractionResult.PASS;
         } else {
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void onOffspringSpawnedFromEgg(Player param0, Mob param1) {
   }

   protected InteractionResult mobInteract(Player param0, InteractionHand param1) {
      return InteractionResult.PASS;
   }

   public boolean isWithinRestriction() {
      return this.isWithinRestriction(this.blockPosition());
   }

   public boolean isWithinRestriction(BlockPos param0) {
      return this.restrictRadius == -1.0F ? true : this.restrictCenter.distSqr(param0) < this.restrictRadius * this.restrictRadius;
   }

   public void restrictTo(BlockPos param0, int param1) {
      this.restrictCenter = param0;
      this.restrictRadius = param1;
   }

   public BlockPos getRestrictCenter() {
      return this.restrictCenter;
   }

   public float getRestrictRadius() {
      return this.restrictRadius;
   }

   public void clearRestriction() {
      this.restrictRadius = -1.0F;
   }

   public boolean hasRestriction() {
      return this.restrictRadius != -1.0F;
   }

   @Nullable
   public <T extends Mob> T convertTo(EntityType<T> param0, boolean param1) {
      if (this.isRemoved()) {
         return null;
      }

      T var3 = (T)param0.create(this.level);
      if (var3 == null) {
         return null;
      }

      var3.copyPosition(this);
      var3.setBaby(this.isBaby());
      var3.setNoAi(this.isNoAi());
      if (this.hasCustomName()) {
         var3.setCustomName(this.getCustomName());
         var3.setCustomNameVisible(this.isCustomNameVisible());
      }

      if (this.isPersistenceRequired()) {
         var3.setPersistenceRequired();
      }

      var3.setInvulnerable(this.isInvulnerable());
      if (param1) {
         var3.setCanPickUpLoot(this.canPickUpLoot());

         for (EquipmentSlot var7 : EquipmentSlot.values()) {
            ItemStack var8 = this.getItemBySlot(var7);
            if (!var8.isEmpty()) {
               var3.setItemSlot(var7, var8.copy());
               var3.setDropChance(var7, this.getEquipmentDropChance(var7));
               var8.setCount(0);
            }
         }
      }

      this.level.addFreshEntity(var3);
      if (this.isPassenger()) {
         Entity var4 = this.getVehicle();
         this.stopRiding();
         var3.startRiding(var4, true);
      }

      this.discard();
      return var3;
   }

   protected void tickLeash() {
      if (this.leashInfoTag != null) {
         this.restoreLeashFromSave();
      }

      if (this.leashHolder != null) {
         if (!this.isAlive() || !this.leashHolder.isAlive()) {
            this.dropLeash(true, true);
         }
      }
   }

   public void dropLeash(boolean param0, boolean param1) {
      if (this.leashHolder != null) {
         this.leashHolder = null;
         this.leashInfoTag = null;
         if (!this.level.isClientSide && param1) {
            this.spawnAtLocation(Items.LEAD);
         }

         if (!this.level.isClientSide && param0 && this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, null));
         }
      }
   }

   public boolean canBeLeashed(Player param0) {
      return !this.isLeashed() && !(this instanceof Enemy);
   }

   public boolean isLeashed() {
      return this.leashHolder != null;
   }

   @Nullable
   public Entity getLeashHolder() {
      if (this.leashHolder == null && this.delayedLeashHolderId != 0 && this.level.isClientSide) {
         this.leashHolder = this.level.getEntity(this.delayedLeashHolderId);
      }

      return this.leashHolder;
   }

   public void setLeashedTo(Entity param0, boolean param1) {
      this.leashHolder = param0;
      this.leashInfoTag = null;
      if (!this.level.isClientSide && param1 && this.level instanceof ServerLevel) {
         ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, this.leashHolder));
      }

      if (this.isPassenger()) {
         this.stopRiding();
      }
   }

   public void setDelayedLeashHolderId(int param0) {
      this.delayedLeashHolderId = param0;
      this.dropLeash(false, false);
   }

   @Override
   public boolean startRiding(Entity param0, boolean param1) {
      boolean var3 = super.startRiding(param0, param1);
      if (var3 && this.isLeashed()) {
         this.dropLeash(true, true);
      }

      return var3;
   }

   private void restoreLeashFromSave() {
      if (this.leashInfoTag != null && this.level instanceof ServerLevel) {
         if (this.leashInfoTag.hasUUID("UUID")) {
            UUID var1 = this.leashInfoTag.getUUID("UUID");
            Entity var2 = ((ServerLevel)this.level).getEntity(var1);
            if (var2 != null) {
               this.setLeashedTo(var2, true);
               return;
            }
         } else if (this.leashInfoTag.contains("X", 99) && this.leashInfoTag.contains("Y", 99) && this.leashInfoTag.contains("Z", 99)) {
            BlockPos var1 = NbtUtils.readBlockPos(this.leashInfoTag);
            this.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(this.level, var1), true);
            return;
         }

         if (this.tickCount > 100) {
            this.spawnAtLocation(Items.LEAD);
            this.leashInfoTag = null;
         }
      }
   }

   @Override
   public boolean isEffectiveAi() {
      return super.isEffectiveAi() && !this.isNoAi();
   }

   public void setNoAi(boolean param0) {
      byte var2 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, param0 ? (byte)(var2 | 1) : (byte)(var2 & -2));
   }

   public void setLeftHanded(boolean param0) {
      byte var2 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, param0 ? (byte)(var2 | 2) : (byte)(var2 & -3));
   }

   public void setAggressive(boolean param0) {
      byte var2 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, param0 ? (byte)(var2 | 4) : (byte)(var2 & -5));
   }

   public boolean isNoAi() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
   }

   public boolean isLeftHanded() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
   }

   public boolean isAggressive() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
   }

   public void setBaby(boolean param0) {
   }

   @Override
   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public double getMeleeAttackRangeSqr(LivingEntity param0) {
      return this.getBbWidth() * 2.0F * (this.getBbWidth() * 2.0F) + param0.getBbWidth();
   }

   public double getPerceivedTargetDistanceSquareForMeleeAttack(LivingEntity param0) {
      return Math.max(this.distanceToSqr(param0.getMeleeAttackReferencePosition()), this.distanceToSqr(param0.position()));
   }

   public boolean isWithinMeleeAttackRange(LivingEntity param0) {
      double var2 = this.getPerceivedTargetDistanceSquareForMeleeAttack(param0);
      return var2 <= this.getMeleeAttackRangeSqr(param0);
   }

   @Override
   public boolean doHurtTarget(Entity param0) {
      float var2 = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float var3 = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      if (param0 instanceof LivingEntity) {
         var2 += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)param0).getMobType());
         var3 += EnchantmentHelper.getKnockbackBonus(this);
      }

      int var4 = EnchantmentHelper.getFireAspect(this);
      if (var4 > 0) {
         param0.setSecondsOnFire(var4 * 4);
      }

      boolean var5 = param0.hurt(this.damageSources().mobAttack(this), var2);
      if (var5) {
         if (var3 > 0.0F && param0 instanceof LivingEntity) {
            ((LivingEntity)param0)
               .knockback(var3 * 0.5F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         }

         if (param0 instanceof Player var6) {
            this.maybeDisableShield(var6, this.getMainHandItem(), var6.isUsingItem() ? var6.getUseItem() : ItemStack.EMPTY);
         }

         this.doEnchantDamageEffects(this, param0);
         this.setLastHurtMob(param0);
      }

      return var5;
   }

   private void maybeDisableShield(Player param0, ItemStack param1, ItemStack param2) {
      if (!param1.isEmpty() && !param2.isEmpty() && param1.getItem() instanceof AxeItem && param2.is(Items.SHIELD)) {
         float var4 = 0.25F + EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
         if (this.random.nextFloat() < var4) {
            param0.getCooldowns().addCooldown(Items.SHIELD, 100);
            this.level.broadcastEntityEvent(param0, (byte)30);
         }
      }
   }

   protected boolean isSunBurnTick() {
      if (this.level.isDay() && !this.level.isClientSide) {
         float var1 = this.getLightLevelDependentMagicValue();
         BlockPos var2 = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
         boolean var3 = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;
         if (var1 > 0.5F && this.random.nextFloat() * 30.0F < (var1 - 0.4F) * 2.0F && !var3 && this.level.canSeeSky(var2)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void jumpInLiquid(TagKey<Fluid> param0) {
      if (this.getNavigation().canFloat()) {
         super.jumpInLiquid(param0);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
      }
   }

   public void removeFreeWill() {
      this.removeAllGoals(l_param0 -> true);
      this.getBrain().removeAllBehaviors();
   }

   public void removeAllGoals(Predicate<Goal> param0) {
      this.goalSelector.removeAllGoals(param0);
   }

   @Override
   protected void removeAfterChangingDimensions() {
      super.removeAfterChangingDimensions();
      this.dropLeash(true, false);
      this.getAllSlots().forEach(l_param0 -> l_param0.setCount(0));
   }

   @Nullable
   @Override
   public ItemStack getPickResult() {
      SpawnEggItem var1 = SpawnEggItem.byId(this.getType());
      return var1 == null ? null : new ItemStack(var1);
   }
}
