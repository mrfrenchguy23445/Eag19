package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class Raid {
   private static final int SECTION_RADIUS_FOR_FINDING_NEW_VILLAGE_CENTER = 2;
   private static final int ATTEMPT_RAID_FARTHEST = 0;
   private static final int ATTEMPT_RAID_CLOSE = 1;
   private static final int ATTEMPT_RAID_INSIDE = 2;
   private static final int VILLAGE_SEARCH_RADIUS = 32;
   private static final int RAID_TIMEOUT_TICKS = 48000;
   private static final int NUM_SPAWN_ATTEMPTS = 3;
   private static final String OMINOUS_BANNER_PATTERN_NAME = "block.minecraft.ominous_banner";
   private static final String RAIDERS_REMAINING = "event.minecraft.raid.raiders_remaining";
   public static final int VILLAGE_RADIUS_BUFFER = 16;
   private static final int POST_RAID_TICK_LIMIT = 40;
   private static final int DEFAULT_PRE_RAID_TICKS = 300;
   public static final int MAX_NO_ACTION_TIME = 2400;
   public static final int MAX_CELEBRATION_TICKS = 600;
   private static final int OUTSIDE_RAID_BOUNDS_TIMEOUT = 30;
   public static final int TICKS_PER_DAY = 24000;
   public static final int DEFAULT_MAX_BAD_OMEN_LEVEL = 5;
   private static final int LOW_MOB_THRESHOLD = 2;
   private static final Component RAID_NAME_COMPONENT = Component.translatable("event.minecraft.raid");
   private static final Component VICTORY = Component.translatable("event.minecraft.raid.victory");
   private static final Component DEFEAT = Component.translatable("event.minecraft.raid.defeat");
   private static final Component RAID_BAR_VICTORY_COMPONENT = RAID_NAME_COMPONENT.copy().append(" - ").append(VICTORY);
   private static final Component RAID_BAR_DEFEAT_COMPONENT = RAID_NAME_COMPONENT.copy().append(" - ").append(DEFEAT);
   private static final int HERO_OF_THE_VILLAGE_DURATION = 48000;
   public static final int VALID_RAID_RADIUS_SQR = 9216;
   public static final int RAID_REMOVAL_THRESHOLD_SQR = 12544;
   private final Map<Integer, Raider> groupToLeaderMap = Maps.newHashMap();
   private final Map<Integer, Set<Raider>> groupRaiderMap = Maps.newHashMap();
   private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
   private long ticksActive;
   private BlockPos center;
   private final ServerLevel level;
   private boolean started;
   private final int id;
   private float totalHealth;
   private int badOmenLevel;
   private boolean active;
   private int groupsSpawned;
   private final ServerBossEvent raidEvent = new ServerBossEvent(RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
   private int postRaidTicks;
   private int raidCooldownTicks;
   private final RandomSource random = RandomSource.create();
   private final int numGroups;
   private Raid.RaidStatus status;
   private int celebrationTicks;
   private Optional<BlockPos> waveSpawnPos = Optional.empty();

   public Raid(int param0, ServerLevel param1, BlockPos param2) {
      this.id = param0;
      this.level = param1;
      this.active = true;
      this.raidCooldownTicks = 300;
      this.raidEvent.setProgress(0.0F);
      this.center = param2;
      this.numGroups = this.getNumGroups(param1.getDifficulty());
      this.status = Raid.RaidStatus.ONGOING;
   }

   public Raid(ServerLevel param0, CompoundTag param1) {
      this.level = param0;
      this.id = param1.getInt("Id");
      this.started = param1.getBoolean("Started");
      this.active = param1.getBoolean("Active");
      this.ticksActive = param1.getLong("TicksActive");
      this.badOmenLevel = param1.getInt("BadOmenLevel");
      this.groupsSpawned = param1.getInt("GroupsSpawned");
      this.raidCooldownTicks = param1.getInt("PreRaidTicks");
      this.postRaidTicks = param1.getInt("PostRaidTicks");
      this.totalHealth = param1.getFloat("TotalHealth");
      this.center = new BlockPos(param1.getInt("CX"), param1.getInt("CY"), param1.getInt("CZ"));
      this.numGroups = param1.getInt("NumGroups");
      this.status = Raid.RaidStatus.getByName(param1.getString("Status"));
      this.heroesOfTheVillage.clear();
      if (param1.contains("HeroesOfTheVillage", 9)) {
         ListTag var3 = param1.getList("HeroesOfTheVillage", 11);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            this.heroesOfTheVillage.add(NbtUtils.loadUUID(var3.get(var4)));
         }
      }
   }

   public boolean isOver() {
      return this.isVictory() || this.isLoss();
   }

   public boolean isBetweenWaves() {
      return this.hasFirstWaveSpawned() && this.getTotalRaidersAlive() == 0 && this.raidCooldownTicks > 0;
   }

   public boolean hasFirstWaveSpawned() {
      return this.groupsSpawned > 0;
   }

   public boolean isStopped() {
      return this.status == Raid.RaidStatus.STOPPED;
   }

   public boolean isVictory() {
      return this.status == Raid.RaidStatus.VICTORY;
   }

   public boolean isLoss() {
      return this.status == Raid.RaidStatus.LOSS;
   }

   public float getTotalHealth() {
      return this.totalHealth;
   }

   public Set<Raider> getAllRaiders() {
      Set<Raider> var1 = Sets.newHashSet();

      for (Set<Raider> var3 : this.groupRaiderMap.values()) {
         var1.addAll(var3);
      }

      return var1;
   }

   public Level getLevel() {
      return this.level;
   }

   public boolean isStarted() {
      return this.started;
   }

   public int getGroupsSpawned() {
      return this.groupsSpawned;
   }

   private Predicate<ServerPlayer> validPlayer() {
      return l_param0 -> {
         BlockPos l_var2 = l_param0.blockPosition();
         return l_param0.isAlive() && this.level.getRaidAt(l_var2) == this;
      };
   }

   private void updatePlayers() {
      Set<ServerPlayer> var1 = Sets.newHashSet(this.raidEvent.getPlayers());
      List<ServerPlayer> var2 = this.level.getPlayers(this.validPlayer());

      for (ServerPlayer var4 : var2) {
         if (!var1.contains(var4)) {
            this.raidEvent.addPlayer(var4);
         }
      }

      for (ServerPlayer var4 : var1) {
         if (!var2.contains(var4)) {
            this.raidEvent.removePlayer(var4);
         }
      }
   }

   public int getMaxBadOmenLevel() {
      return 5;
   }

   public int getBadOmenLevel() {
      return this.badOmenLevel;
   }

   public void setBadOmenLevel(int param0) {
      this.badOmenLevel = param0;
   }

   public void absorbBadOmen(Player param0) {
      if (param0.hasEffect(MobEffects.BAD_OMEN)) {
         this.badOmenLevel = this.badOmenLevel + param0.getEffect(MobEffects.BAD_OMEN).getAmplifier() + 1;
         this.badOmenLevel = Mth.clamp(this.badOmenLevel, 0, this.getMaxBadOmenLevel());
      }

      param0.removeEffect(MobEffects.BAD_OMEN);
   }

   public void stop() {
      this.active = false;
      this.raidEvent.removeAllPlayers();
      this.status = Raid.RaidStatus.STOPPED;
   }

   public void tick() {
      if (!this.isStopped()) {
         if (this.status == Raid.RaidStatus.ONGOING) {
            boolean var1 = this.active;
            this.active = this.level.hasChunkAt(this.center);
            if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
               this.stop();
               return;
            }

            if (var1 != this.active) {
               this.raidEvent.setVisible(this.active);
            }

            if (!this.active) {
               return;
            }

            if (!this.level.isVillage(this.center)) {
               this.moveRaidCenterToNearbyVillageSection();
            }

            if (!this.level.isVillage(this.center)) {
               if (this.groupsSpawned > 0) {
                  this.status = Raid.RaidStatus.LOSS;
               } else {
                  this.stop();
               }
            }

            this.ticksActive++;
            if (this.ticksActive >= 48000L) {
               this.stop();
               return;
            }

            int var2 = this.getTotalRaidersAlive();
            if (var2 == 0 && this.hasMoreWaves()) {
               if (this.raidCooldownTicks <= 0) {
                  if (this.raidCooldownTicks == 0 && this.groupsSpawned > 0) {
                     this.raidCooldownTicks = 300;
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                     return;
                  }
               } else {
                  boolean var3 = this.waveSpawnPos.isPresent();
                  boolean var4 = !var3 && this.raidCooldownTicks % 5 == 0;
                  if (var3 && !this.level.isPositionEntityTicking(this.waveSpawnPos.get())) {
                     var4 = true;
                  }

                  if (var4) {
                     int var5 = 0;
                     if (this.raidCooldownTicks < 100) {
                        var5 = 1;
                     } else if (this.raidCooldownTicks < 40) {
                        var5 = 2;
                     }

                     this.waveSpawnPos = this.getValidSpawnPos(var5);
                  }

                  if (this.raidCooldownTicks == 300 || this.raidCooldownTicks % 20 == 0) {
                     this.updatePlayers();
                  }

                  this.raidCooldownTicks--;
                  this.raidEvent.setProgress(Mth.clamp((300 - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
               }
            }

            if (this.ticksActive % 20L == 0L) {
               this.updatePlayers();
               this.updateRaiders();
               if (var2 > 0) {
                  if (var2 <= 2) {
                     this.raidEvent
                        .setName(RAID_NAME_COMPONENT.copy().append(" - ").append(Component.translatable("event.minecraft.raid.raiders_remaining", var2)));
                  } else {
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                  }
               } else {
                  this.raidEvent.setName(RAID_NAME_COMPONENT);
               }
            }

            boolean var3 = false;
            int var4 = 0;

            while (this.shouldSpawnGroup()) {
               BlockPos var5 = this.waveSpawnPos.isPresent() ? this.waveSpawnPos.get() : this.findRandomSpawnPos(var4, 20);
               if (var5 != null) {
                  this.started = true;
                  this.spawnGroup(var5);
                  if (!var3) {
                     this.playSound(var5);
                     var3 = true;
                  }
               } else {
                  var4++;
               }

               if (var4 > 3) {
                  this.stop();
                  break;
               }
            }

            if (this.isStarted() && !this.hasMoreWaves() && var2 == 0) {
               if (this.postRaidTicks < 40) {
                  this.postRaidTicks++;
               } else {
                  this.status = Raid.RaidStatus.VICTORY;

                  for (UUID var6 : this.heroesOfTheVillage) {
                     Entity var7 = this.level.getEntity(var6);
                     if (var7 instanceof LivingEntity && !var7.isSpectator()) {
                        LivingEntity var8 = (LivingEntity)var7;
                        var8.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 48000, this.badOmenLevel - 1, false, false, true));
                        if (var8 instanceof ServerPlayer var9) {
                           var9.awardStat(Stats.RAID_WIN);
                           CriteriaTriggers.RAID_WIN.trigger(var9);
                        }
                     }
                  }
               }
            }

            this.setDirty();
         } else if (this.isOver()) {
            this.celebrationTicks++;
            if (this.celebrationTicks >= 600) {
               this.stop();
               return;
            }

            if (this.celebrationTicks % 20 == 0) {
               this.updatePlayers();
               this.raidEvent.setVisible(true);
               if (this.isVictory()) {
                  this.raidEvent.setProgress(0.0F);
                  this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
               } else {
                  this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
               }
            }
         }
      }
   }

   private void moveRaidCenterToNearbyVillageSection() {
      Stream<SectionPos> var1 = SectionPos.cube(SectionPos.of(this.center), 2);
      var1.filter(this.level::isVillage)
         .map(SectionPos::center)
         .min(Comparator.comparingDouble(l_param0 -> l_param0.distSqr(this.center)))
         .ifPresent(this::setCenter);
   }

   private Optional<BlockPos> getValidSpawnPos(int param0) {
      for (int var2 = 0; var2 < 3; var2++) {
         BlockPos var3 = this.findRandomSpawnPos(param0, 1);
         if (var3 != null) {
            return Optional.of(var3);
         }
      }

      return Optional.empty();
   }

   private boolean hasMoreWaves() {
      return this.hasBonusWave() ? !this.hasSpawnedBonusWave() : !this.isFinalWave();
   }

   private boolean isFinalWave() {
      return this.getGroupsSpawned() == this.numGroups;
   }

   private boolean hasBonusWave() {
      return this.badOmenLevel > 1;
   }

   private boolean hasSpawnedBonusWave() {
      return this.getGroupsSpawned() > this.numGroups;
   }

   private boolean shouldSpawnBonusGroup() {
      return this.isFinalWave() && this.getTotalRaidersAlive() == 0 && this.hasBonusWave();
   }

   private void updateRaiders() {
      Iterator<Set<Raider>> var1 = this.groupRaiderMap.values().iterator();
      Set<Raider> var2 = Sets.newHashSet();

      while (var1.hasNext()) {
         Set<Raider> var3 = var1.next();

         for (Raider var5 : var3) {
            BlockPos var6 = var5.blockPosition();
            if (var5.isRemoved() || var5.level.dimension() != this.level.dimension() || this.center.distSqr(var6) >= 12544.0) {
               var2.add(var5);
            } else if (var5.tickCount > 600) {
               if (this.level.getEntity(var5.getUUID()) == null) {
                  var2.add(var5);
               }

               if (!this.level.isVillage(var6) && var5.getNoActionTime() > 2400) {
                  var5.setTicksOutsideRaid(var5.getTicksOutsideRaid() + 1);
               }

               if (var5.getTicksOutsideRaid() >= 30) {
                  var2.add(var5);
               }
            }
         }
      }

      for (Raider var4 : var2) {
         this.removeFromRaid(var4, true);
      }
   }

   private void playSound(BlockPos param0) {
      float var2 = 13.0F;
      int var3 = 64;
      Collection<ServerPlayer> var4 = this.raidEvent.getPlayers();
      long var5 = this.random.nextLong();

      for (ServerPlayer var8 : this.level.players()) {
         Vec3 var9 = var8.position();
         Vec3 var10 = Vec3.atCenterOf(param0);
         double var11 = Math.sqrt((var10.x - var9.x) * (var10.x - var9.x) + (var10.z - var9.z) * (var10.z - var9.z));
         double var13 = var9.x + 13.0 / var11 * (var10.x - var9.x);
         double var15 = var9.z + 13.0 / var11 * (var10.z - var9.z);
         if (var11 <= 64.0 || var4.contains(var8)) {
            var8.connection.send(new ClientboundSoundPacket(SoundEvents.RAID_HORN, SoundSource.NEUTRAL, var13, var8.getY(), var15, 64.0F, 1.0F, var5));
         }
      }
   }

   private void spawnGroup(BlockPos param0) {
      boolean var2 = false;
      int var3 = this.groupsSpawned + 1;
      this.totalHealth = 0.0F;
      DifficultyInstance var4 = this.level.getCurrentDifficultyAt(param0);
      boolean var5 = this.shouldSpawnBonusGroup();

      for (Raid.RaiderType var9 : Raid.RaiderType.VALUES) {
         int var10 = this.getDefaultNumSpawns(var9, var3, var5) + this.getPotentialBonusSpawns(var9, this.random, var3, var4, var5);
         int var11 = 0;

         for (int var12 = 0; var12 < var10; var12++) {
            Raider var13 = var9.entityType.create(this.level);
            if (var13 == null) {
               break;
            }

            if (!var2 && var13.canBeLeader()) {
               var13.setPatrolLeader(true);
               this.setLeader(var3, var13);
               var2 = true;
            }

            this.joinRaid(var3, var13, param0, false);
            if (var9.entityType == EntityType.RAVAGER) {
               Raider var14 = null;
               if (var3 == this.getNumGroups(Difficulty.NORMAL)) {
                  var14 = EntityType.PILLAGER.create(this.level);
               } else if (var3 >= this.getNumGroups(Difficulty.HARD)) {
                  if (var11 == 0) {
                     var14 = EntityType.EVOKER.create(this.level);
                  } else {
                     var14 = EntityType.VINDICATOR.create(this.level);
                  }
               }

               var11++;
               if (var14 != null) {
                  this.joinRaid(var3, var14, param0, false);
                  var14.moveTo(param0, 0.0F, 0.0F);
                  var14.startRiding(var13);
               }
            }
         }
      }

      this.waveSpawnPos = Optional.empty();
      this.groupsSpawned++;
      this.updateBossbar();
      this.setDirty();
   }

   public void joinRaid(int param0, Raider param1, @Nullable BlockPos param2, boolean param3) {
      boolean var5 = this.addWaveMob(param0, param1);
      if (var5) {
         param1.setCurrentRaid(this);
         param1.setWave(param0);
         param1.setCanJoinRaid(true);
         param1.setTicksOutsideRaid(0);
         if (!param3 && param2 != null) {
            param1.setPos(param2.getX() + 0.5, param2.getY() + 1.0, param2.getZ() + 0.5);
            param1.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(param2), MobSpawnType.EVENT, null, null);
            param1.applyRaidBuffs(param0, false);
            param1.setOnGround(true);
            this.level.addFreshEntityWithPassengers(param1);
         }
      }
   }

   public void updateBossbar() {
      this.raidEvent.setProgress(Mth.clamp(this.getHealthOfLivingRaiders() / this.totalHealth, 0.0F, 1.0F));
   }

   public float getHealthOfLivingRaiders() {
      float var1 = 0.0F;

      for (Set<Raider> var3 : this.groupRaiderMap.values()) {
         for (Raider var5 : var3) {
            var1 += var5.getHealth();
         }
      }

      return var1;
   }

   private boolean shouldSpawnGroup() {
      return this.raidCooldownTicks == 0 && (this.groupsSpawned < this.numGroups || this.shouldSpawnBonusGroup()) && this.getTotalRaidersAlive() == 0;
   }

   public int getTotalRaidersAlive() {
      return this.groupRaiderMap.values().stream().mapToInt(Set::size).sum();
   }

   public void removeFromRaid(Raider param0, boolean param1) {
      Set<Raider> var3 = this.groupRaiderMap.get(param0.getWave());
      if (var3 != null) {
         boolean var4 = var3.remove(param0);
         if (var4) {
            if (param1) {
               this.totalHealth = this.totalHealth - param0.getHealth();
            }

            param0.setCurrentRaid(null);
            this.updateBossbar();
            this.setDirty();
         }
      }
   }

   private void setDirty() {
      this.level.getRaids().setDirty();
   }

   public static ItemStack getLeaderBannerInstance() {
      ItemStack var0 = new ItemStack(Items.WHITE_BANNER);
      CompoundTag var1 = new CompoundTag();
      ListTag var2 = new BannerPattern.Builder()
         .addPattern(BannerPatterns.RHOMBUS_MIDDLE, DyeColor.CYAN)
         .addPattern(BannerPatterns.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY)
         .addPattern(BannerPatterns.STRIPE_CENTER, DyeColor.GRAY)
         .addPattern(BannerPatterns.BORDER, DyeColor.LIGHT_GRAY)
         .addPattern(BannerPatterns.STRIPE_MIDDLE, DyeColor.BLACK)
         .addPattern(BannerPatterns.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY)
         .addPattern(BannerPatterns.CIRCLE_MIDDLE, DyeColor.LIGHT_GRAY)
         .addPattern(BannerPatterns.BORDER, DyeColor.BLACK)
         .toListTag();
      var1.put("Patterns", var2);
      BlockItem.setBlockEntityData(var0, BlockEntityType.BANNER, var1);
      var0.hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL);
      var0.setHoverName(Component.translatable("block.minecraft.ominous_banner").withStyle(ChatFormatting.GOLD));
      return var0;
   }

   @Nullable
   public Raider getLeader(int param0) {
      return this.groupToLeaderMap.get(param0);
   }

   @Nullable
   private BlockPos findRandomSpawnPos(int param0, int param1) {
      int var3 = param0 == 0 ? 2 : 2 - param0;
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for (int var8 = 0; var8 < param1; var8++) {
         float var9 = this.level.random.nextFloat() * (float) (Math.PI * 2);
         int var4 = this.center.getX() + Mth.floor(Mth.cos(var9) * 32.0F * var3) + this.level.random.nextInt(5);
         int var6 = this.center.getZ() + Mth.floor(Mth.sin(var9) * 32.0F * var3) + this.level.random.nextInt(5);
         int var5 = this.level.getHeight(Heightmap.Types.WORLD_SURFACE, var4, var6);
         var7.set(var4, var5, var6);
         if (!this.level.isVillage(var7) || param0 >= 2) {
            int var10 = 10;
            if (this.level.hasChunksAt(var7.getX() - 10, var7.getZ() - 10, var7.getX() + 10, var7.getZ() + 10)
               && this.level.isPositionEntityTicking(var7)
               && (
                  NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, this.level, var7, EntityType.RAVAGER)
                     || this.level.getBlockState(var7.below()).is(Blocks.SNOW) && this.level.getBlockState(var7).isAir()
               )) {
               return var7;
            }
         }
      }

      return null;
   }

   private boolean addWaveMob(int param0, Raider param1) {
      return this.addWaveMob(param0, param1, true);
   }

   public boolean addWaveMob(int param0, Raider param1, boolean param2) {
      this.groupRaiderMap.computeIfAbsent(param0, l_param0 -> Sets.newHashSet());
      Set<Raider> var4 = this.groupRaiderMap.get(param0);
      Raider var5 = null;

      for (Raider var7 : var4) {
         if (var7.getUUID().equals(param1.getUUID())) {
            var5 = var7;
            break;
         }
      }

      if (var5 != null) {
         var4.remove(var5);
         var4.add(param1);
      }

      var4.add(param1);
      if (param2) {
         this.totalHealth = this.totalHealth + param1.getHealth();
      }

      this.updateBossbar();
      this.setDirty();
      return true;
   }

   public void setLeader(int param0, Raider param1) {
      this.groupToLeaderMap.put(param0, param1);
      param1.setItemSlot(EquipmentSlot.HEAD, getLeaderBannerInstance());
      param1.setDropChance(EquipmentSlot.HEAD, 2.0F);
   }

   public void removeLeader(int param0) {
      this.groupToLeaderMap.remove(param0);
   }

   public BlockPos getCenter() {
      return this.center;
   }

   private void setCenter(BlockPos param0) {
      this.center = param0;
   }

   public int getId() {
      return this.id;
   }

   private int getDefaultNumSpawns(Raid.RaiderType param0, int param1, boolean param2) {
      return param2 ? param0.spawnsPerWaveBeforeBonus[this.numGroups] : param0.spawnsPerWaveBeforeBonus[param1];
   }

   private int getPotentialBonusSpawns(Raid.RaiderType param0, RandomSource param1, int param2, DifficultyInstance param3, boolean param4) {
      Difficulty var6 = param3.getDifficulty();
      boolean var7 = var6 == Difficulty.EASY;
      boolean var8 = var6 == Difficulty.NORMAL;
      int var9;
      switch (param0) {
         case WITCH:
            if (var7 || param2 <= 2 || param2 == 4) {
               return 0;
            }

            var9 = 1;
            break;
         case PILLAGER:
         case VINDICATOR:
            if (var7) {
               var9 = param1.nextInt(2);
            } else if (var8) {
               var9 = 1;
            } else {
               var9 = 2;
            }
            break;
         case RAVAGER:
            var9 = !var7 && param4 ? 1 : 0;
            break;
         default:
            return 0;
      }

      return var9 > 0 ? param1.nextInt(var9 + 1) : 0;
   }

   public boolean isActive() {
      return this.active;
   }

   public CompoundTag save(CompoundTag param0) {
      param0.putInt("Id", this.id);
      param0.putBoolean("Started", this.started);
      param0.putBoolean("Active", this.active);
      param0.putLong("TicksActive", this.ticksActive);
      param0.putInt("BadOmenLevel", this.badOmenLevel);
      param0.putInt("GroupsSpawned", this.groupsSpawned);
      param0.putInt("PreRaidTicks", this.raidCooldownTicks);
      param0.putInt("PostRaidTicks", this.postRaidTicks);
      param0.putFloat("TotalHealth", this.totalHealth);
      param0.putInt("NumGroups", this.numGroups);
      param0.putString("Status", this.status.getName());
      param0.putInt("CX", this.center.getX());
      param0.putInt("CY", this.center.getY());
      param0.putInt("CZ", this.center.getZ());
      ListTag var2 = new ListTag();

      for (UUID var4 : this.heroesOfTheVillage) {
         var2.add(NbtUtils.createUUID(var4));
      }

      param0.put("HeroesOfTheVillage", var2);
      return param0;
   }

   public int getNumGroups(Difficulty param0) {
      switch (param0) {
         case EASY:
            return 3;
         case NORMAL:
            return 5;
         case HARD:
            return 7;
         default:
            return 0;
      }
   }

   public float getEnchantOdds() {
      int var1 = this.getBadOmenLevel();
      if (var1 == 2) {
         return 0.1F;
      } else if (var1 == 3) {
         return 0.25F;
      } else if (var1 == 4) {
         return 0.5F;
      } else {
         return var1 == 5 ? 0.75F : 0.0F;
      }
   }

   public void addHeroOfTheVillage(Entity param0) {
      this.heroesOfTheVillage.add(param0.getUUID());
   }

   enum RaidStatus {
      ONGOING,
      VICTORY,
      LOSS,
      STOPPED;

      private static final Raid.RaidStatus[] VALUES = values();

      static Raid.RaidStatus getByName(String param0) {
         for (Raid.RaidStatus var4 : VALUES) {
            if (param0.equalsIgnoreCase(var4.name())) {
               return var4;
            }
         }

         return ONGOING;
      }

      public String getName() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   enum RaiderType {
      VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
      EVOKER(EntityType.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
      PILLAGER(EntityType.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
      WITCH(EntityType.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
      RAVAGER(EntityType.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

      static final Raid.RaiderType[] VALUES = values();
      final EntityType<? extends Raider> entityType;
      final int[] spawnsPerWaveBeforeBonus;

      RaiderType(EntityType<? extends Raider> param2, int[] param3) {
         this.entityType = param2;
         this.spawnsPerWaveBeforeBonus = param3;
      }
   }
}
