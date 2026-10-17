package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public abstract class Raider extends PatrollingMonster {
   protected static final EntityDataAccessor<Boolean> IS_CELEBRATING = SynchedEntityData.defineId(Raider.class, EntityDataSerializers.BOOLEAN);
   static final Predicate<ItemEntity> ALLOWED_ITEMS = param0 -> !param0.hasPickUpDelay()
      && param0.isAlive()
      && ItemStack.matches(param0.getItem(), Raid.getLeaderBannerInstance());
   @Nullable
   protected Raid raid;
   private int wave;
   private boolean canJoinRaid;
   private int ticksOutsideRaid;

   protected Raider(EntityType<? extends Raider> param0, Level param1) {
      super(param0, param1);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(1, new Raider.ObtainRaidLeaderBannerGoal<>(this));
      this.goalSelector.addGoal(3, new PathfindToRaidGoal<>(this));
      this.goalSelector.addGoal(4, new Raider.RaiderMoveThroughVillageGoal(this, 1.05F, 1));
      this.goalSelector.addGoal(5, new Raider.RaiderCelebration(this));
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(IS_CELEBRATING, false);
   }

   public abstract void applyRaidBuffs(int var1, boolean var2);

   public boolean canJoinRaid() {
      return this.canJoinRaid;
   }

   public void setCanJoinRaid(boolean param0) {
      this.canJoinRaid = param0;
   }

   @Override
   public void aiStep() {
      if (this.level instanceof ServerLevel && this.isAlive()) {
         Raid var1 = this.getCurrentRaid();
         if (this.canJoinRaid()) {
            if (var1 == null) {
               if (this.level.getGameTime() % 20L == 0L) {
                  Raid var2 = ((ServerLevel)this.level).getRaidAt(this.blockPosition());
                  if (var2 != null && Raids.canJoinRaid(this, var2)) {
                     var2.joinRaid(var2.getGroupsSpawned(), this, null, true);
                  }
               }
            } else {
               LivingEntity var2 = this.getTarget();
               if (var2 != null && (var2.getType() == EntityType.PLAYER || var2.getType() == EntityType.IRON_GOLEM)) {
                  this.noActionTime = 0;
               }
            }
         }
      }

      super.aiStep();
   }

   @Override
   protected void updateNoActionTime() {
      this.noActionTime += 2;
   }

   @Override
   public void die(DamageSource param0) {
      if (this.level instanceof ServerLevel) {
         Entity var2 = param0.getEntity();
         Raid var3 = this.getCurrentRaid();
         if (var3 != null) {
            if (this.isPatrolLeader()) {
               var3.removeLeader(this.getWave());
            }

            if (var2 != null && var2.getType() == EntityType.PLAYER) {
               var3.addHeroOfTheVillage(var2);
            }

            var3.removeFromRaid(this, false);
         }

         if (this.isPatrolLeader() && var3 == null && ((ServerLevel)this.level).getRaidAt(this.blockPosition()) == null) {
            ItemStack var4 = this.getItemBySlot(EquipmentSlot.HEAD);
            Player var5 = null;
            Entity var6 = var2;
            if (var6 instanceof Player) {
               var5 = (Player)var6;
            } else if (var6 instanceof Wolf var7) {
               LivingEntity var8 = var7.getOwner();
               if (var7.isTame() && var8 instanceof Player) {
                  var5 = (Player)var8;
               }
            }

            if (!var4.isEmpty() && ItemStack.matches(var4, Raid.getLeaderBannerInstance()) && var5 != null) {
               MobEffectInstance var7 = var5.getEffect(MobEffects.BAD_OMEN);
               int var8 = 1;
               if (var7 != null) {
                  var8 += var7.getAmplifier();
                  var5.removeEffectNoUpdate(MobEffects.BAD_OMEN);
               } else {
                  var8--;
               }

               var8 = Mth.clamp(var8, 0, 4);
               MobEffectInstance var9 = new MobEffectInstance(MobEffects.BAD_OMEN, 120000, var8, false, false, true);
               if (!this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                  var5.addEffect(var9);
               }
            }
         }
      }

      super.die(param0);
   }

   @Override
   public boolean canJoinPatrol() {
      return !this.hasActiveRaid();
   }

   public void setCurrentRaid(@Nullable Raid param0) {
      this.raid = param0;
   }

   @Nullable
   public Raid getCurrentRaid() {
      return this.raid;
   }

   public boolean hasActiveRaid() {
      return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
   }

   public void setWave(int param0) {
      this.wave = param0;
   }

   public int getWave() {
      return this.wave;
   }

   public boolean isCelebrating() {
      return this.entityData.get(IS_CELEBRATING);
   }

   public void setCelebrating(boolean param0) {
      this.entityData.set(IS_CELEBRATING, param0);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag param0) {
      super.addAdditionalSaveData(param0);
      param0.putInt("Wave", this.wave);
      param0.putBoolean("CanJoinRaid", this.canJoinRaid);
      if (this.raid != null) {
         param0.putInt("RaidId", this.raid.getId());
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag param0) {
      super.readAdditionalSaveData(param0);
      this.wave = param0.getInt("Wave");
      this.canJoinRaid = param0.getBoolean("CanJoinRaid");
      if (param0.contains("RaidId", 3)) {
         if (this.level instanceof ServerLevel) {
            this.raid = ((ServerLevel)this.level).getRaids().get(param0.getInt("RaidId"));
         }

         if (this.raid != null) {
            this.raid.addWaveMob(this.wave, this, false);
            if (this.isPatrolLeader()) {
               this.raid.setLeader(this.wave, this);
            }
         }
      }
   }

   @Override
   protected void pickUpItem(ItemEntity param0) {
      ItemStack var2 = param0.getItem();
      boolean var3 = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
      if (this.hasActiveRaid() && !var3 && ItemStack.matches(var2, Raid.getLeaderBannerInstance())) {
         EquipmentSlot var4 = EquipmentSlot.HEAD;
         ItemStack var5 = this.getItemBySlot(var4);
         double var6 = this.getEquipmentDropChance(var4);
         if (!var5.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < var6) {
            this.spawnAtLocation(var5);
         }

         this.onItemPickup(param0);
         this.setItemSlot(var4, var2);
         this.take(param0, var2.getCount());
         param0.discard();
         this.getCurrentRaid().setLeader(this.getWave(), this);
         this.setPatrolLeader(true);
      } else {
         super.pickUpItem(param0);
      }
   }

   @Override
   public boolean removeWhenFarAway(double param0) {
      return this.getCurrentRaid() == null ? super.removeWhenFarAway(param0) : false;
   }

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
   }

   public int getTicksOutsideRaid() {
      return this.ticksOutsideRaid;
   }

   public void setTicksOutsideRaid(int param0) {
      this.ticksOutsideRaid = param0;
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      if (this.hasActiveRaid()) {
         this.getCurrentRaid().updateBossbar();
      }

      return super.hurt(param0, param1);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor param0, DifficultyInstance param1, MobSpawnType param2, @Nullable SpawnGroupData param3, @Nullable CompoundTag param4
   ) {
      this.setCanJoinRaid(this.getType() != EntityType.WITCH || param2 != MobSpawnType.NATURAL);
      return super.finalizeSpawn(param0, param1, param2, param3, param4);
   }

   public abstract SoundEvent getCelebrateSound();

   protected class HoldGroundAttackGoal extends Goal {
      private final Raider mob;
      private final float hostileRadiusSqr;
      public final TargetingConditions shoutTargeting = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight().ignoreInvisibilityTesting();

      public HoldGroundAttackGoal(AbstractIllager param1, float param2) {
         this.mob = param1;
         this.hostileRadiusSqr = param2 * param2;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         LivingEntity var1 = this.mob.getLastHurtByMob();
         return this.mob.getCurrentRaid() == null
            && this.mob.isPatrolling()
            && this.mob.getTarget() != null
            && !this.mob.isAggressive()
            && (var1 == null || var1.getType() != EntityType.PLAYER);
      }

      @Override
      public void start() {
         super.start();
         this.mob.getNavigation().stop();

         for (Raider var3 : this.mob.level.getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
            var3.setTarget(this.mob.getTarget());
         }
      }

      @Override
      public void stop() {
         super.stop();
         LivingEntity var1 = this.mob.getTarget();
         if (var1 != null) {
            for (Raider var4 : this.mob.level.getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
               var4.setTarget(var1);
               var4.setAggressive(true);
            }

            this.mob.setAggressive(true);
         }
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity var1 = this.mob.getTarget();
         if (var1 != null) {
            if (this.mob.distanceToSqr(var1) > this.hostileRadiusSqr) {
               this.mob.getLookControl().setLookAt(var1, 30.0F, 30.0F);
               if (this.mob.random.nextInt(50) == 0) {
                  this.mob.playAmbientSound();
               }
            } else {
               this.mob.setAggressive(true);
            }

            super.tick();
         }
      }
   }

   public class ObtainRaidLeaderBannerGoal<T extends Raider> extends Goal {
      private final T mob;

      public ObtainRaidLeaderBannerGoal(T param1) {
         this.mob = param1;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         Raid var1 = this.mob.getCurrentRaid();
         if (this.mob.hasActiveRaid()
            && !this.mob.getCurrentRaid().isOver()
            && this.mob.canBeLeader()
            && !ItemStack.matches(this.mob.getItemBySlot(EquipmentSlot.HEAD), Raid.getLeaderBannerInstance())) {
            Raider var2 = var1.getLeader(this.mob.getWave());
            if (var2 == null || !var2.isAlive()) {
               List<ItemEntity> var3 = this.mob
                  .level
                  .getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(16.0, 8.0, 16.0), Raider.ALLOWED_ITEMS);
               if (!var3.isEmpty()) {
                  return this.mob.getNavigation().moveTo(var3.get(0), 1.15F);
               }
            }

            return false;
         } else {
            return false;
         }
      }

      @Override
      public void tick() {
         if (this.mob.getNavigation().getTargetPos().closerToCenterThan(this.mob.position(), 1.414)) {
            List<ItemEntity> var1 = this.mob.level.getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(4.0, 4.0, 4.0), Raider.ALLOWED_ITEMS);
            if (!var1.isEmpty()) {
               this.mob.pickUpItem(var1.get(0));
            }
         }
      }
   }

   public class RaiderCelebration extends Goal {
      private final Raider mob;

      RaiderCelebration(Raider param1) {
         this.mob = param1;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         Raid var1 = this.mob.getCurrentRaid();
         return this.mob.isAlive() && this.mob.getTarget() == null && var1 != null && var1.isLoss();
      }

      @Override
      public void start() {
         this.mob.setCelebrating(true);
         super.start();
      }

      @Override
      public void stop() {
         this.mob.setCelebrating(false);
         super.stop();
      }

      @Override
      public void tick() {
         if (!this.mob.isSilent() && this.mob.random.nextInt(this.adjustedTickDelay(100)) == 0) {
            Raider.this.playSound(Raider.this.getCelebrateSound(), Raider.this.getSoundVolume(), Raider.this.getVoicePitch());
         }

         if (!this.mob.isPassenger() && this.mob.random.nextInt(this.adjustedTickDelay(50)) == 0) {
            this.mob.getJumpControl().jump();
         }

         super.tick();
      }
   }

   static class RaiderMoveThroughVillageGoal extends Goal {
      private final Raider raider;
      private final double speedModifier;
      private BlockPos poiPos;
      private final List<BlockPos> visited = Lists.newArrayList();
      private final int distanceToPoi;
      private boolean stuck;

      public RaiderMoveThroughVillageGoal(Raider param0, double param1, int param3) {
         this.raider = param0;
         this.speedModifier = param1;
         this.distanceToPoi = param3;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         this.updateVisited();
         return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getTarget() == null;
      }

      private boolean isValidRaid() {
         return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
      }

      private boolean hasSuitablePoi() {
         ServerLevel var1 = (ServerLevel)this.raider.level;
         BlockPos var2 = this.raider.blockPosition();
         Optional<BlockPos> var3 = var1.getPoiManager()
            .getRandom(l_param0 -> l_param0.is(PoiTypes.HOME), this::hasNotVisited, PoiManager.Occupancy.ANY, var2, 48, this.raider.random);
         if (!var3.isPresent()) {
            return false;
         }

         this.poiPos = var3.get().immutable();
         return true;
      }

      @Override
      public boolean canContinueToUse() {
         return this.raider.getNavigation().isDone()
            ? false
            : this.raider.getTarget() == null
               && !this.poiPos.closerToCenterThan(this.raider.position(), this.raider.getBbWidth() + this.distanceToPoi)
               && !this.stuck;
      }

      @Override
      public void stop() {
         if (this.poiPos.closerToCenterThan(this.raider.position(), this.distanceToPoi)) {
            this.visited.add(this.poiPos);
         }
      }

      @Override
      public void start() {
         super.start();
         this.raider.setNoActionTime(0);
         this.raider.getNavigation().moveTo(this.poiPos.getX(), this.poiPos.getY(), this.poiPos.getZ(), this.speedModifier);
         this.stuck = false;
      }

      @Override
      public void tick() {
         if (this.raider.getNavigation().isDone()) {
            Vec3 var1 = Vec3.atBottomCenterOf(this.poiPos);
            Vec3 var2 = DefaultRandomPos.getPosTowards(this.raider, 16, 7, var1, (float) (Math.PI / 10));
            if (var2 == null) {
               var2 = DefaultRandomPos.getPosTowards(this.raider, 8, 7, var1, (float) (Math.PI / 2));
            }

            if (var2 == null) {
               this.stuck = true;
               return;
            }

            this.raider.getNavigation().moveTo(var2.x, var2.y, var2.z, this.speedModifier);
         }
      }

      private boolean hasNotVisited(BlockPos param0) {
         for (BlockPos var3 : this.visited) {
            if (Objects.equals(param0, var3)) {
               return false;
            }
         }

         return true;
      }

      private void updateVisited() {
         if (this.visited.size() > 2) {
            this.visited.remove(0);
         }
      }
   }
}
