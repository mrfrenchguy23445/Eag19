package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import org.slf4j.Logger;

public class ServerLevel extends Level implements WorldGenLevel {
   public static final BlockPos END_SPAWN_POINT = new BlockPos(100, 50, 0);
   public static final IntProvider RAIN_DELAY = UniformInt.of(12000, 180000);
   public static final IntProvider RAIN_DURATION = UniformInt.of(12000, 24000);
   private static final IntProvider THUNDER_DELAY = UniformInt.of(12000, 180000);
   public static final IntProvider THUNDER_DURATION = UniformInt.of(3600, 15600);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int EMPTY_TIME_NO_TICK = 300;
   private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
   final List<ServerPlayer> players = Lists.newArrayList();
   private final ServerChunkCache chunkSource;
   private final MinecraftServer server;
   private final ServerLevelData serverLevelData;
   final EntityTickList entityTickList = new EntityTickList();
   private final PersistentEntitySectionManager<Entity> entityManager;
   private final GameEventDispatcher gameEventDispatcher;
   public boolean noSave;
   private final SleepStatus sleepStatus;
   private int emptyTime;
   private final PortalForcer portalForcer;
   private final LevelTicks<Block> blockTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
   private final LevelTicks<Fluid> fluidTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
   final Set<Mob> navigatingMobs = new ObjectOpenHashSet<>();
   volatile boolean isUpdatingNavigations;
   protected final Raids raids;
   private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
   private final List<BlockEventData> blockEventsToReschedule = new ArrayList<>(64);
   private boolean handlingTick;
   private final List<CustomSpawner> customSpawners;
   @Nullable
   private final EndDragonFight dragonFight;
   final Int2ObjectMap<EnderDragonPart> dragonParts = new Int2ObjectOpenHashMap<>();
   private final StructureManager structureManager;
   private final StructureCheck structureCheck;
   private final boolean tickTime;

   public ServerLevel(
      MinecraftServer param0,
      Executor param1,
      LevelStorageSource.LevelStorageAccess param2,
      ServerLevelData param3,
      ResourceKey<Level> param4,
      LevelStem param5,
      ChunkProgressListener param6,
      boolean param7,
      long param8,
      List<CustomSpawner> param10,
      boolean param11
   ) {
      super(param3, param4, param0.registryAccess(), param5.type(), param0::getProfiler, false, param7, param8, param0.getMaxChainedNeighborUpdates());
      this.tickTime = param11;
      this.server = param0;
      this.customSpawners = param10;
      this.serverLevelData = param3;
      ChunkGenerator var13 = param5.generator();
      boolean var14 = param0.forceSynchronousWrites();
      DataFixer var15 = param0.getFixerUpper();
      EntityPersistentStorage<Entity> var16 = new EntityStorage(this, param2.getDimensionPath(param4).resolve("entities"), var15, var14, param0);
      this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new ServerLevel.EntityCallbacks(), var16);
      this.chunkSource = new ServerChunkCache(
         this,
         param2,
         var15,
         param0.getStructureManager(),
         param1,
         var13,
         param0.getPlayerList().getViewDistance(),
         param0.getPlayerList().getSimulationDistance(),
         var14,
         param6,
         this.entityManager::updateChunkStatus,
         () -> param0.overworld().getDataStorage()
      );
      this.chunkSource.getGeneratorState().ensureStructuresGenerated();
      this.portalForcer = new PortalForcer(this);
      this.updateSkyBrightness();
      this.prepareWeather();
      this.getWorldBorder().setAbsoluteMaxSize(param0.getAbsoluteMaxWorldSize());
      this.raids = this.getDataStorage()
         .computeIfAbsent(l_param0 -> Raids.load(this, l_param0), () -> new Raids(this), Raids.getFileId(this.dimensionTypeRegistration()));
      if (!param0.isSingleplayer()) {
         param3.setGameType(param0.getDefaultGameType());
      }

      long var17 = param0.getWorldData().worldGenOptions().seed();
      this.structureCheck = new StructureCheck(
         this.chunkSource.chunkScanner(),
         this.registryAccess(),
         param0.getStructureManager(),
         param4,
         var13,
         this.chunkSource.randomState(),
         this,
         var13.getBiomeSource(),
         var17,
         var15
      );
      this.structureManager = new StructureManager(this, param0.getWorldData().worldGenOptions(), this.structureCheck);
      if (this.dimension() == Level.END && this.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
         this.dragonFight = new EndDragonFight(this, var17, param0.getWorldData().endDragonFightData());
      } else {
         this.dragonFight = null;
      }

      this.sleepStatus = new SleepStatus();
      this.gameEventDispatcher = new GameEventDispatcher(this);
   }

   public void setWeatherParameters(int param0, int param1, boolean param2, boolean param3) {
      this.serverLevelData.setClearWeatherTime(param0);
      this.serverLevelData.setRainTime(param1);
      this.serverLevelData.setThunderTime(param1);
      this.serverLevelData.setRaining(param2);
      this.serverLevelData.setThundering(param3);
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int param0, int param1, int param2) {
      return this.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(param0, param1, param2, this.getChunkSource().randomState().sampler());
   }

   public StructureManager structureManager() {
      return this.structureManager;
   }

   public void tick(BooleanSupplier param0) {
      ProfilerFiller var2 = this.getProfiler();
      this.handlingTick = true;
      var2.push("world border");
      this.getWorldBorder().tick();
      var2.popPush("weather");
      this.advanceWeatherCycle();
      int var3 = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
      if (this.sleepStatus.areEnoughSleeping(var3) && this.sleepStatus.areEnoughDeepSleeping(var3, this.players)) {
         if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            long var4 = this.levelData.getDayTime() + 24000L;
            this.setDayTime(var4 - var4 % 24000L);
         }

         this.wakeUpAllPlayers();
         if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && this.isRaining()) {
            this.resetWeatherCycle();
         }
      }

      this.updateSkyBrightness();
      this.tickTime();
      var2.popPush("tickPending");
      if (!this.isDebug()) {
         long var4 = this.getGameTime();
         var2.push("blockTicks");
         this.blockTicks.tick(var4, 65536, this::tickBlock);
         var2.popPush("fluidTicks");
         this.fluidTicks.tick(var4, 65536, this::tickFluid);
         var2.pop();
      }

      var2.popPush("raid");
      this.raids.tick();
      var2.popPush("chunkSource");
      this.getChunkSource().tick(param0, true);
      var2.popPush("blockEvents");
      this.runBlockEvents();
      this.handlingTick = false;
      var2.pop();
      boolean var4 = !this.players.isEmpty() || !this.getForcedChunks().isEmpty();
      if (var4) {
         this.resetEmptyTime();
      }

      if (var4 || this.emptyTime++ < 300) {
         var2.push("entities");
         if (this.dragonFight != null) {
            var2.push("dragonFight");
            this.dragonFight.tick();
            var2.pop();
         }

         this.entityTickList.forEach(l_param1 -> {
            if (!l_param1.isRemoved()) {
               if (this.shouldDiscardEntity(l_param1)) {
                  l_param1.discard();
               } else {
                  var2.push("checkDespawn");
                  l_param1.checkDespawn();
                  var2.pop();
                  if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(l_param1.chunkPosition().toLong())) {
                     Entity l_var3 = l_param1.getVehicle();
                     if (l_var3 != null) {
                        if (!l_var3.isRemoved() && l_var3.hasPassenger(l_param1)) {
                           return;
                        }

                        l_param1.stopRiding();
                     }

                     var2.push("tick");
                     this.guardEntityTick(this::tickNonPassenger, l_param1);
                     var2.pop();
                  }
               }
            }
         });
         var2.pop();
         this.tickBlockEntities();
      }

      var2.push("entityManagement");
      this.entityManager.tick();
      var2.pop();
   }

   @Override
   public boolean shouldTickBlocksAt(long param0) {
      return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(param0);
   }

   protected void tickTime() {
      if (this.tickTime) {
         long var1 = this.levelData.getGameTime() + 1L;
         this.serverLevelData.setGameTime(var1);
         this.serverLevelData.getScheduledEvents().tick(this.server, var1);
         if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            this.setDayTime(this.levelData.getDayTime() + 1L);
         }
      }
   }

   public void setDayTime(long param0) {
      this.serverLevelData.setDayTime(param0);
   }

   public void tickCustomSpawners(boolean param0, boolean param1) {
      for (CustomSpawner var4 : this.customSpawners) {
         var4.tick(this, param0, param1);
      }
   }

   private boolean shouldDiscardEntity(Entity param0) {
      return this.server.isSpawningAnimals() || !(param0 instanceof Animal) && !(param0 instanceof WaterAnimal)
         ? !this.server.areNpcsEnabled() && param0 instanceof Npc
         : true;
   }

   private void wakeUpAllPlayers() {
      this.sleepStatus.removeAllSleepers();
      this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()).forEach(l_param0 -> l_param0.stopSleepInBed(false, false));
   }

   public void tickChunk(LevelChunk param0, int param1) {
      ChunkPos var3 = param0.getPos();
      boolean var4 = this.isRaining();
      int var5 = var3.getMinBlockX();
      int var6 = var3.getMinBlockZ();
      ProfilerFiller var7 = this.getProfiler();
      var7.push("thunder");
      if (var4 && this.isThundering() && this.random.nextInt(100000) == 0) {
         BlockPos var8 = this.findLightningTargetAround(this.getBlockRandomPos(var5, 0, var6, 15));
         if (this.isRainingAt(var8)) {
            DifficultyInstance var9 = this.getCurrentDifficultyAt(var8);
            boolean var10 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
               && this.random.nextDouble() < var9.getEffectiveDifficulty() * 0.01
               && !this.getBlockState(var8.below()).is(Blocks.LIGHTNING_ROD);
            if (var10) {
               SkeletonHorse var11 = EntityType.SKELETON_HORSE.create(this);
               if (var11 != null) {
                  var11.setTrap(true);
                  var11.setAge(0);
                  var11.setPos(var8.getX(), var8.getY(), var8.getZ());
                  this.addFreshEntity(var11);
               }
            }

            LightningBolt var11 = EntityType.LIGHTNING_BOLT.create(this);
            if (var11 != null) {
               var11.moveTo(Vec3.atBottomCenterOf(var8));
               var11.setVisualOnly(var10);
               this.addFreshEntity(var11);
            }
         }
      }

      var7.popPush("iceandsnow");
      if (this.random.nextInt(16) == 0) {
         BlockPos var8 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.getBlockRandomPos(var5, 0, var6, 15));
         BlockPos var9 = var8.below();
         Biome var10 = this.getBiome(var8).value();
         if (var10.shouldFreeze(this, var9)) {
            this.setBlockAndUpdate(var9, Blocks.ICE.defaultBlockState());
         }

         if (var4) {
            int var11 = this.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
            if (var11 > 0 && var10.shouldSnow(this, var8)) {
               BlockState var12 = this.getBlockState(var8);
               if (var12.is(Blocks.SNOW)) {
                  int var13 = var12.getValue(SnowLayerBlock.LAYERS);
                  if (var13 < Math.min(var11, 8)) {
                     BlockState var14 = var12.setValue(SnowLayerBlock.LAYERS, var13 + 1);
                     Block.pushEntitiesUp(var12, var14, this, var8);
                     this.setBlockAndUpdate(var8, var14);
                  }
               } else {
                  this.setBlockAndUpdate(var8, Blocks.SNOW.defaultBlockState());
               }
            }

            Biome.Precipitation var12 = var10.getPrecipitationAt(var9);
            if (var12 != Biome.Precipitation.NONE) {
               BlockState var13 = this.getBlockState(var9);
               var13.getBlock().handlePrecipitation(var13, this, var9, var12);
            }
         }
      }

      var7.popPush("tickBlocks");
      if (param1 > 0) {
         for (LevelChunkSection var11 : param0.getSections()) {
            if (var11.isRandomlyTicking()) {
               int var12 = var11.bottomBlockY();

               for (int var13 = 0; var13 < param1; var13++) {
                  BlockPos var14 = this.getBlockRandomPos(var5, var12, var6, 15);
                  var7.push("randomTick");
                  BlockState var15 = var11.getBlockState(var14.getX() - var5, var14.getY() - var12, var14.getZ() - var6);
                  if (var15.isRandomlyTicking()) {
                     var15.randomTick(this, var14, this.random);
                  }

                  FluidState var16 = var15.getFluidState();
                  if (var16.isRandomlyTicking()) {
                     var16.randomTick(this, var14, this.random);
                  }

                  var7.pop();
               }
            }
         }
      }

      var7.pop();
   }

   private Optional<BlockPos> findLightningRod(BlockPos param0) {
      Optional<BlockPos> var2 = this.getPoiManager()
         .findClosest(
            l_param0 -> l_param0.is(PoiTypes.LIGHTNING_ROD),
            l_param0 -> l_param0.getY() == this.getHeight(Heightmap.Types.WORLD_SURFACE, l_param0.getX(), l_param0.getZ()) - 1,
            param0,
            128,
            PoiManager.Occupancy.ANY
         );
      return var2.map(l_param0 -> l_param0.above(1));
   }

   protected BlockPos findLightningTargetAround(BlockPos param0) {
      BlockPos var2 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, param0);
      Optional<BlockPos> var3 = this.findLightningRod(var2);
      if (var3.isPresent()) {
         return var3.get();
      }

      AABB var4 = new AABB(var2, new BlockPos(var2.getX(), this.getMaxBuildHeight(), var2.getZ())).inflate(3.0);
      List<LivingEntity> var5 = this.getEntitiesOfClass(
         LivingEntity.class, var4, l_param0 -> l_param0 != null && l_param0.isAlive() && this.canSeeSky(l_param0.blockPosition())
      );
      if (!var5.isEmpty()) {
         return var5.get(this.random.nextInt(var5.size())).blockPosition();
      }

      if (var2.getY() == this.getMinBuildHeight() - 1) {
         var2 = var2.above(2);
      }

      return var2;
   }

   public boolean isHandlingTick() {
      return this.handlingTick;
   }

   public boolean canSleepThroughNights() {
      return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
   }

   private void announceSleepStatus() {
      if (this.canSleepThroughNights()) {
         if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
            int var1 = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
            Component var2;
            if (this.sleepStatus.areEnoughSleeping(var1)) {
               var2 = Component.translatable("sleep.skipping_night");
            } else {
               var2 = Component.translatable("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(var1));
            }

            for (ServerPlayer var4 : this.players) {
               var4.displayClientMessage(var2, true);
            }
         }
      }
   }

   public void updateSleepingPlayerList() {
      if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
         this.announceSleepStatus();
      }
   }

   public ServerScoreboard getScoreboard() {
      return this.server.getScoreboard();
   }

   private void advanceWeatherCycle() {
      boolean var1 = this.isRaining();
      if (this.dimensionType().hasSkyLight()) {
         if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            int var2 = this.serverLevelData.getClearWeatherTime();
            int var3 = this.serverLevelData.getThunderTime();
            int var4 = this.serverLevelData.getRainTime();
            boolean var5 = this.levelData.isThundering();
            boolean var6 = this.levelData.isRaining();
            if (var2 > 0) {
               var2--;
               var3 = var5 ? 0 : 1;
               var4 = var6 ? 0 : 1;
               var5 = false;
               var6 = false;
            } else {
               if (var3 > 0) {
                  if (--var3 == 0) {
                     var5 = !var5;
                  }
               } else if (var5) {
                  var3 = THUNDER_DURATION.sample(this.random);
               } else {
                  var3 = THUNDER_DELAY.sample(this.random);
               }

               if (var4 > 0) {
                  if (--var4 == 0) {
                     var6 = !var6;
                  }
               } else if (var6) {
                  var4 = RAIN_DURATION.sample(this.random);
               } else {
                  var4 = RAIN_DELAY.sample(this.random);
               }
            }

            this.serverLevelData.setThunderTime(var3);
            this.serverLevelData.setRainTime(var4);
            this.serverLevelData.setClearWeatherTime(var2);
            this.serverLevelData.setThundering(var5);
            this.serverLevelData.setRaining(var6);
         }

         this.oThunderLevel = this.thunderLevel;
         if (this.levelData.isThundering()) {
            this.thunderLevel += 0.01F;
         } else {
            this.thunderLevel -= 0.01F;
         }

         this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
         this.oRainLevel = this.rainLevel;
         if (this.levelData.isRaining()) {
            this.rainLevel += 0.01F;
         } else {
            this.rainLevel -= 0.01F;
         }

         this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
      }

      if (this.oRainLevel != this.rainLevel) {
         this.server
            .getPlayerList()
            .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
      }

      if (this.oThunderLevel != this.thunderLevel) {
         this.server
            .getPlayerList()
            .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
      }

      if (var1 != this.isRaining()) {
         if (var1) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
         } else {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
         }

         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel));
         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel));
      }
   }

   private void resetWeatherCycle() {
      this.serverLevelData.setRainTime(0);
      this.serverLevelData.setRaining(false);
      this.serverLevelData.setThunderTime(0);
      this.serverLevelData.setThundering(false);
   }

   public void resetEmptyTime() {
      this.emptyTime = 0;
   }

   private void tickFluid(BlockPos param0, Fluid param1) {
      FluidState var3 = this.getFluidState(param0);
      if (var3.is(param1)) {
         var3.tick(this, param0);
      }
   }

   private void tickBlock(BlockPos param0, Block param1) {
      BlockState var3 = this.getBlockState(param0);
      if (var3.is(param1)) {
         var3.tick(this, param0, this.random);
      }
   }

   public void tickNonPassenger(Entity param0) {
      param0.setOldPosAndRot();
      ProfilerFiller var2 = this.getProfiler();
      param0.tickCount++;
      this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(param0.getType()).toString());
      var2.incrementCounter("tickNonPassenger");
      param0.tick();
      this.getProfiler().pop();

      for (Entity var4 : param0.getPassengers()) {
         this.tickPassenger(param0, var4);
      }
   }

   private void tickPassenger(Entity param0, Entity param1) {
      if (param1.isRemoved() || param1.getVehicle() != param0) {
         param1.stopRiding();
      } else if (param1 instanceof Player || this.entityTickList.contains(param1)) {
         param1.setOldPosAndRot();
         param1.tickCount++;
         ProfilerFiller var3 = this.getProfiler();
         var3.push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(param1.getType()).toString());
         var3.incrementCounter("tickPassenger");
         param1.rideTick();
         var3.pop();

         for (Entity var5 : param1.getPassengers()) {
            this.tickPassenger(param1, var5);
         }
      }
   }

   @Override
   public boolean mayInteract(Player param0, BlockPos param1) {
      return !this.server.isUnderSpawnProtection(this, param1, param0) && this.getWorldBorder().isWithinBounds(param1);
   }

   public void save(@Nullable ProgressListener param0, boolean param1, boolean param2) {
      ServerChunkCache var4 = this.getChunkSource();
      if (!param2) {
         if (param0 != null) {
            param0.progressStartNoAbort(Component.translatable("menu.savingLevel"));
         }

         this.saveLevelData();
         if (param0 != null) {
            param0.progressStage(Component.translatable("menu.savingChunks"));
         }

         var4.save(param1);
         if (param1) {
            this.entityManager.saveAll();
         } else {
            this.entityManager.autoSave();
         }
      }
   }

   private void saveLevelData() {
      if (this.dragonFight != null) {
         this.server.getWorldData().setEndDragonFightData(this.dragonFight.saveData());
      }

      this.getChunkSource().getDataStorage().save();
   }

   public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> param0, Predicate<? super T> param1) {
      List<T> var3 = Lists.newArrayList();
      this.getEntities(param0, param1, var3);
      return var3;
   }

   public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> param0, Predicate<? super T> param1, List<? super T> param2) {
      this.getEntities(param0, param1, param2, Integer.MAX_VALUE);
   }

   public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> param0, Predicate<? super T> param1, List<? super T> param2, int param3) {
      this.getEntities().get(param0, l_param3 -> {
         if (param1.test(l_param3)) {
            param2.add(l_param3);
            if (param2.size() >= param3) {
               return AbortableIterationConsumer.Continuation.ABORT;
            }
         }

         return AbortableIterationConsumer.Continuation.CONTINUE;
      });
   }

   public List<? extends EnderDragon> getDragons() {
      return this.getEntities(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
   }

   public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> param0) {
      return this.getPlayers(param0, Integer.MAX_VALUE);
   }

   public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> param0, int param1) {
      List<ServerPlayer> var3 = Lists.newArrayList();

      for (ServerPlayer var5 : this.players) {
         if (param0.test(var5)) {
            var3.add(var5);
            if (var3.size() >= param1) {
               return var3;
            }
         }
      }

      return var3;
   }

   @Nullable
   public ServerPlayer getRandomPlayer() {
      List<ServerPlayer> var1 = this.getPlayers(LivingEntity::isAlive);
      return var1.isEmpty() ? null : var1.get(this.random.nextInt(var1.size()));
   }

   @Override
   public boolean addFreshEntity(Entity param0) {
      return this.addEntity(param0);
   }

   public boolean addWithUUID(Entity param0) {
      return this.addEntity(param0);
   }

   public void addDuringTeleport(Entity param0) {
      this.addEntity(param0);
   }

   public void addDuringCommandTeleport(ServerPlayer param0) {
      this.addPlayer(param0);
   }

   public void addDuringPortalTeleport(ServerPlayer param0) {
      this.addPlayer(param0);
   }

   public void addNewPlayer(ServerPlayer param0) {
      this.addPlayer(param0);
   }

   public void addRespawnedPlayer(ServerPlayer param0) {
      this.addPlayer(param0);
   }

   private void addPlayer(ServerPlayer param0) {
      Entity var2 = this.getEntities().get(param0.getUUID());
      if (var2 != null) {
         LOGGER.warn("Force-added player with duplicate UUID {}", param0.getUUID().toString());
         var2.unRide();
         this.removePlayerImmediately((ServerPlayer)var2, Entity.RemovalReason.DISCARDED);
      }

      this.entityManager.addNewEntity(param0);
   }

   private boolean addEntity(Entity param0) {
      if (param0.isRemoved()) {
         LOGGER.warn("Tried to add entity {} but it was marked as removed already", EntityType.getKey(param0.getType()));
         return false;
      } else {
         return this.entityManager.addNewEntity(param0);
      }
   }

   public boolean tryAddFreshEntityWithPassengers(Entity param0) {
      if (param0.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
         return false;
      }

      this.addFreshEntityWithPassengers(param0);
      return true;
   }

   public void unload(LevelChunk param0) {
      param0.clearAllBlockEntities();
      param0.unregisterTickContainerFromLevel(this);
   }

   public void removePlayerImmediately(ServerPlayer param0, Entity.RemovalReason param1) {
      param0.remove(param1);
   }

   @Override
   public void destroyBlockProgress(int param0, BlockPos param1, int param2) {
      for (ServerPlayer var5 : this.server.getPlayerList().getPlayers()) {
         if (var5 != null && var5.level == this && var5.getId() != param0) {
            double var6 = param1.getX() - var5.getX();
            double var8 = param1.getY() - var5.getY();
            double var10 = param1.getZ() - var5.getZ();
            if (var6 * var6 + var8 * var8 + var10 * var10 < 1024.0) {
               var5.connection.send(new ClientboundBlockDestructionPacket(param0, param1, param2));
            }
         }
      }
   }

   @Override
   public void playSeededSound(
      @Nullable Player param0,
      double param1,
      double param3,
      double param5,
      Holder<SoundEvent> param7,
      SoundSource param8,
      float param9,
      float param10,
      long param11
   ) {
      this.server
         .getPlayerList()
         .broadcast(
            param0,
            param1,
            param3,
            param5,
            param7.value().getRange(param9),
            this.dimension(),
            new ClientboundSoundPacket(param7, param8, param1, param3, param5, param9, param10, param11)
         );
   }

   @Override
   public void playSeededSound(@Nullable Player param0, Entity param1, Holder<SoundEvent> param2, SoundSource param3, float param4, float param5, long param6) {
      this.server
         .getPlayerList()
         .broadcast(
            param0,
            param1.getX(),
            param1.getY(),
            param1.getZ(),
            param2.value().getRange(param4),
            this.dimension(),
            new ClientboundSoundEntityPacket(param2, param3, param1, param4, param5, param6)
         );
   }

   @Override
   public void globalLevelEvent(int param0, BlockPos param1, int param2) {
      if (this.getGameRules().getBoolean(GameRules.RULE_GLOBAL_SOUND_EVENTS)) {
         this.server.getPlayerList().broadcastAll(new ClientboundLevelEventPacket(param0, param1, param2, true));
      } else {
         this.levelEvent(null, param0, param1, param2);
      }
   }

   @Override
   public void levelEvent(@Nullable Player param0, int param1, BlockPos param2, int param3) {
      this.server
         .getPlayerList()
         .broadcast(param0, param2.getX(), param2.getY(), param2.getZ(), 64.0, this.dimension(), new ClientboundLevelEventPacket(param1, param2, param3, false));
   }

   public int getLogicalHeight() {
      return this.dimensionType().logicalHeight();
   }

   @Override
   public void gameEvent(GameEvent param0, Vec3 param1, GameEvent.Context param2) {
      this.gameEventDispatcher.post(param0, param1, param2);
   }

   @Override
   public void sendBlockUpdated(BlockPos param0, BlockState param1, BlockState param2, int param3) {
      if (this.isUpdatingNavigations) {
         String var5 = "recursive call to sendBlockUpdated";
         Util.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
      }

      this.getChunkSource().blockChanged(param0);
      VoxelShape var5 = param1.getCollisionShape(this, param0);
      VoxelShape var6 = param2.getCollisionShape(this, param0);
      if (Shapes.joinIsNotEmpty(var5, var6, BooleanOp.NOT_SAME)) {
         List<PathNavigation> var7 = new ObjectArrayList<>();

         for (Mob var9 : this.navigatingMobs) {
            PathNavigation var10 = var9.getNavigation();
            if (var10.shouldRecomputePath(param0)) {
               var7.add(var10);
            }
         }

         try {
            this.isUpdatingNavigations = true;

            for (PathNavigation var9 : var7) {
               var9.recomputePath();
            }
         } finally {
            this.isUpdatingNavigations = false;
         }
      }
   }

   @Override
   public void updateNeighborsAt(BlockPos param0, Block param1) {
      this.neighborUpdater.updateNeighborsAtExceptFromFacing(param0, param1, null);
   }

   @Override
   public void updateNeighborsAtExceptFromFacing(BlockPos param0, Block param1, Direction param2) {
      this.neighborUpdater.updateNeighborsAtExceptFromFacing(param0, param1, param2);
   }

   @Override
   public void neighborChanged(BlockPos param0, Block param1, BlockPos param2) {
      this.neighborUpdater.neighborChanged(param0, param1, param2);
   }

   @Override
   public void neighborChanged(BlockState param0, BlockPos param1, Block param2, BlockPos param3, boolean param4) {
      this.neighborUpdater.neighborChanged(param0, param1, param2, param3, param4);
   }

   @Override
   public void broadcastEntityEvent(Entity param0, byte param1) {
      this.getChunkSource().broadcastAndSend(param0, new ClientboundEntityEventPacket(param0, param1));
   }

   @Override
   public void broadcastDamageEvent(Entity param0, DamageSource param1) {
      this.getChunkSource().broadcastAndSend(param0, new ClientboundDamageEventPacket(param0, param1));
   }

   public ServerChunkCache getChunkSource() {
      return this.chunkSource;
   }

   @Override
   public Explosion explode(
      @Nullable Entity param0,
      @Nullable DamageSource param1,
      @Nullable ExplosionDamageCalculator param2,
      double param3,
      double param5,
      double param7,
      float param9,
      boolean param10,
      Level.ExplosionInteraction param11
   ) {
      Explosion var13 = this.explode(param0, param1, param2, param3, param5, param7, param9, param10, param11, false);
      if (!var13.interactsWithBlocks()) {
         var13.clearToBlow();
      }

      for (ServerPlayer var15 : this.players) {
         if (var15.distanceToSqr(param3, param5, param7) < 4096.0) {
            var15.connection.send(new ClientboundExplodePacket(param3, param5, param7, param9, var13.getToBlow(), var13.getHitPlayers().get(var15)));
         }
      }

      return var13;
   }

   @Override
   public void blockEvent(BlockPos param0, Block param1, int param2, int param3) {
      this.blockEvents.add(new BlockEventData(param0, param1, param2, param3));
   }

   private void runBlockEvents() {
      this.blockEventsToReschedule.clear();

      while (!this.blockEvents.isEmpty()) {
         BlockEventData var1 = this.blockEvents.removeFirst();
         if (this.shouldTickBlocksAt(var1.pos())) {
            if (this.doBlockEvent(var1)) {
               this.server
                  .getPlayerList()
                  .broadcast(
                     null,
                     var1.pos().getX(),
                     var1.pos().getY(),
                     var1.pos().getZ(),
                     64.0,
                     this.dimension(),
                     new ClientboundBlockEventPacket(var1.pos(), var1.block(), var1.paramA(), var1.paramB())
                  );
            }
         } else {
            this.blockEventsToReschedule.add(var1);
         }
      }

      this.blockEvents.addAll(this.blockEventsToReschedule);
   }

   private boolean doBlockEvent(BlockEventData param0) {
      BlockState var2 = this.getBlockState(param0.pos());
      return var2.is(param0.block()) ? var2.triggerEvent(this, param0.pos(), param0.paramA(), param0.paramB()) : false;
   }

   public LevelTicks<Block> getBlockTicks() {
      return this.blockTicks;
   }

   public LevelTicks<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Nonnull
   @Override
   public MinecraftServer getServer() {
      return this.server;
   }

   public PortalForcer getPortalForcer() {
      return this.portalForcer;
   }

   public StructureTemplateManager getStructureManager() {
      return this.server.getStructureManager();
   }

   public <T extends ParticleOptions> int sendParticles(
      T param0, double param1, double param3, double param5, int param7, double param8, double param10, double param12, double param14
   ) {
      ClientboundLevelParticlesPacket var17 = new ClientboundLevelParticlesPacket(
         param0, false, param1, param3, param5, (float)param8, (float)param10, (float)param12, (float)param14, param7
      );
      int var18 = 0;

      for (int var19 = 0; var19 < this.players.size(); var19++) {
         ServerPlayer var20 = this.players.get(var19);
         if (this.sendParticles(var20, false, param1, param3, param5, var17)) {
            var18++;
         }
      }

      return var18;
   }

   public <T extends ParticleOptions> boolean sendParticles(
      ServerPlayer param0,
      T param1,
      boolean param2,
      double param3,
      double param5,
      double param7,
      int param9,
      double param10,
      double param12,
      double param14,
      double param16
   ) {
      Packet<?> var19 = new ClientboundLevelParticlesPacket(
         param1, param2, param3, param5, param7, (float)param10, (float)param12, (float)param14, (float)param16, param9
      );
      return this.sendParticles(param0, param2, param3, param5, param7, var19);
   }

   private boolean sendParticles(ServerPlayer param0, boolean param1, double param2, double param4, double param6, Packet<?> param8) {
      if (param0.getLevel() != this) {
         return false;
      } else {
         BlockPos var10 = param0.blockPosition();
         if (var10.closerToCenterThan(new Vec3(param2, param4, param6), param1 ? 512.0 : 32.0)) {
            param0.connection.send(param8);
            return true;
         } else {
            return false;
         }
      }
   }

   @Nullable
   @Override
   public Entity getEntity(int param0) {
      return this.getEntities().get(param0);
   }

   @Deprecated
   @Nullable
   public Entity getEntityOrPart(int param0) {
      Entity var2 = this.getEntities().get(param0);
      return var2 != null ? var2 : this.dragonParts.get(param0);
   }

   @Nullable
   public Entity getEntity(UUID param0) {
      return this.getEntities().get(param0);
   }

   @Nullable
   public BlockPos findNearestMapStructure(TagKey<Structure> param0, BlockPos param1, int param2, boolean param3) {
      if (!this.server.getWorldData().worldGenOptions().generateStructures()) {
         return null;
      }

      Optional<HolderSet.Named<Structure>> var5 = this.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(param0);
      if (var5.isEmpty()) {
         return null;
      }

      Pair<BlockPos, Holder<Structure>> var6 = this.getChunkSource().getGenerator().findNearestMapStructure(this, var5.get(), param1, param2, param3);
      return var6 != null ? var6.getFirst() : null;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(Predicate<Holder<Biome>> param0, BlockPos param1, int param2, int param3, int param4) {
      return this.getChunkSource()
         .getGenerator()
         .getBiomeSource()
         .findClosestBiome3d(param1, param2, param3, param4, param0, this.getChunkSource().randomState().sampler(), this);
   }

   @Override
   public RecipeManager getRecipeManager() {
      return this.server.getRecipeManager();
   }

   @Override
   public boolean noSave() {
      return this.noSave;
   }

   public DimensionDataStorage getDataStorage() {
      return this.getChunkSource().getDataStorage();
   }

   @Nullable
   @Override
   public MapItemSavedData getMapData(String param0) {
      return this.getServer().overworld().getDataStorage().get(MapItemSavedData::load, param0);
   }

   @Override
   public void setMapData(String param0, MapItemSavedData param1) {
      this.getServer().overworld().getDataStorage().set(param0, param1);
   }

   @Override
   public int getFreeMapId() {
      return this.getServer().overworld().getDataStorage().computeIfAbsent(MapIndex::load, MapIndex::new, "idcounts").getFreeAuxValueForMap();
   }

   public void setDefaultSpawnPos(BlockPos param0, float param1) {
      ChunkPos var3 = new ChunkPos(new BlockPos(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));
      this.levelData.setSpawn(param0, param1);
      this.getChunkSource().removeRegionTicket(TicketType.START, var3, 11, Unit.INSTANCE);
      this.getChunkSource().addRegionTicket(TicketType.START, new ChunkPos(param0), 11, Unit.INSTANCE);
      this.getServer().getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(param0, param1));
   }

   public LongSet getForcedChunks() {
      ForcedChunksSavedData var1 = this.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
      return var1 != null ? LongSets.unmodifiable(var1.getChunks()) : LongSets.EMPTY_SET;
   }

   public boolean setChunkForced(int param0, int param1, boolean param2) {
      ForcedChunksSavedData var4 = this.getDataStorage().computeIfAbsent(ForcedChunksSavedData::load, ForcedChunksSavedData::new, "chunks");
      ChunkPos var5 = new ChunkPos(param0, param1);
      long var6 = var5.toLong();
      boolean var8;
      if (param2) {
         var8 = var4.getChunks().add(var6);
         if (var8) {
            this.getChunk(param0, param1);
         }
      } else {
         var8 = var4.getChunks().remove(var6);
      }

      var4.setDirty(var8);
      if (var8) {
         this.getChunkSource().updateChunkForced(var5, param2);
      }

      return var8;
   }

   @Override
   public List<ServerPlayer> players() {
      return this.players;
   }

   @Override
   public void onBlockStateChange(BlockPos param0, BlockState param1, BlockState param2) {
      Optional<Holder<PoiType>> var4 = PoiTypes.forState(param1);
      Optional<Holder<PoiType>> var5 = PoiTypes.forState(param2);
      if (!Objects.equals(var4, var5)) {
         BlockPos var6 = param0.immutable();
         var4.ifPresent(l_param1 -> this.getServer().execute(() -> {
            this.getPoiManager().remove(var6);
            DebugPackets.sendPoiRemovedPacket(this, var6);
         }));
         var5.ifPresent(l_param1 -> this.getServer().execute(() -> {
            this.getPoiManager().add(var6, (Holder<PoiType>)l_param1);
            DebugPackets.sendPoiAddedPacket(this, var6);
         }));
      }
   }

   public PoiManager getPoiManager() {
      return this.getChunkSource().getPoiManager();
   }

   public boolean isVillage(BlockPos param0) {
      return this.isCloseToVillage(param0, 1);
   }

   public boolean isVillage(SectionPos param0) {
      return this.isVillage(param0.center());
   }

   public boolean isCloseToVillage(BlockPos param0, int param1) {
      return param1 > 6 ? false : this.sectionsToVillage(SectionPos.of(param0)) <= param1;
   }

   public int sectionsToVillage(SectionPos param0) {
      return this.getPoiManager().sectionsToVillage(param0);
   }

   public Raids getRaids() {
      return this.raids;
   }

   @Nullable
   public Raid getRaidAt(BlockPos param0) {
      return this.raids.getNearbyRaid(param0, 9216);
   }

   public boolean isRaided(BlockPos param0) {
      return this.getRaidAt(param0) != null;
   }

   public void onReputationEvent(ReputationEventType param0, Entity param1, ReputationEventHandler param2) {
      param2.onReputationEventFrom(param0, param1);
   }

   public void saveDebugReport(Path param0) throws IOException {
      ChunkMap var2 = this.getChunkSource().chunkMap;

      try (Writer var3 = Files.newBufferedWriter(param0.resolve("stats.txt"))) {
         var3.write(String.format(Locale.ROOT, "spawning_chunks: %d\n", var2.getDistanceManager().getNaturalSpawnChunkCount()));
         NaturalSpawner.SpawnState var4 = this.getChunkSource().getLastSpawnState();
         if (var4 != null) {
            for (Entry<MobCategory> var6 : var4.getMobCategoryCounts().object2IntEntrySet()) {
               var3.write(String.format(Locale.ROOT, "spawn_count.%s: %d\n", var6.getKey().getName(), var6.getIntValue()));
            }
         }

         var3.write(String.format(Locale.ROOT, "entities: %s\n", this.entityManager.gatherStats()));
         var3.write(String.format(Locale.ROOT, "block_entity_tickers: %d\n", this.blockEntityTickers.size()));
         var3.write(String.format(Locale.ROOT, "block_ticks: %d\n", this.getBlockTicks().count()));
         var3.write(String.format(Locale.ROOT, "fluid_ticks: %d\n", this.getFluidTicks().count()));
         var3.write("distance_manager: " + var2.getDistanceManager().getDebugStatus() + "\n");
         var3.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
      }

      CrashReport var3 = new CrashReport("Level dump", new Exception("dummy"));
      this.fillReportDetails(var3);

      try (Writer var4 = Files.newBufferedWriter(param0.resolve("example_crash.txt"))) {
         var4.write(var3.getFriendlyReport());
      }

      Path var4 = param0.resolve("chunks.csv");

      try (Writer var5 = Files.newBufferedWriter(var4)) {
         var2.dumpChunks(var5);
      }

      Path var5 = param0.resolve("entity_chunks.csv");

      try (Writer var6 = Files.newBufferedWriter(var5)) {
         this.entityManager.dumpSections(var6);
      }

      Path var6 = param0.resolve("entities.csv");

      try (Writer var7 = Files.newBufferedWriter(var6)) {
         dumpEntities(var7, this.getEntities().getAll());
      }

      Path var7 = param0.resolve("block_entities.csv");

      try (Writer var8 = Files.newBufferedWriter(var7)) {
         this.dumpBlockEntityTickers(var8);
      }
   }

   private static void dumpEntities(Writer param0, Iterable<Entity> param1) throws IOException {
      CsvOutput var2 = CsvOutput.builder()
         .addColumn("x")
         .addColumn("y")
         .addColumn("z")
         .addColumn("uuid")
         .addColumn("type")
         .addColumn("alive")
         .addColumn("display_name")
         .addColumn("custom_name")
         .build(param0);

      for (Entity var4 : param1) {
         Component var5 = var4.getCustomName();
         Component var6 = var4.getDisplayName();
         var2.writeRow(
            var4.getX(),
            var4.getY(),
            var4.getZ(),
            var4.getUUID(),
            BuiltInRegistries.ENTITY_TYPE.getKey(var4.getType()),
            var4.isAlive(),
            var6.getString(),
            var5 != null ? var5.getString() : null
         );
      }
   }

   private void dumpBlockEntityTickers(Writer param0) throws IOException {
      CsvOutput var2 = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(param0);

      for (TickingBlockEntity var4 : this.blockEntityTickers) {
         BlockPos var5 = var4.getPos();
         var2.writeRow(var5.getX(), var5.getY(), var5.getZ(), var4.getType());
      }
   }

   @VisibleForTesting
   public void clearBlockEvents(BoundingBox param0) {
      this.blockEvents.removeIf(l_param1 -> param0.isInside(l_param1.pos()));
   }

   @Override
   public void blockUpdated(BlockPos param0, Block param1) {
      if (!this.isDebug()) {
         this.updateNeighborsAt(param0, param1);
      }
   }

   @Override
   public float getShade(Direction param0, boolean param1) {
      return 1.0F;
   }

   public Iterable<Entity> getAllEntities() {
      return this.getEntities().getAll();
   }

   @Override
   public String toString() {
      return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
   }

   public boolean isFlat() {
      return this.server.getWorldData().isFlatWorld();
   }

   @Override
   public long getSeed() {
      return this.server.getWorldData().worldGenOptions().seed();
   }

   @Nullable
   public EndDragonFight dragonFight() {
      return this.dragonFight;
   }

   @Override
   public ServerLevel getLevel() {
      return this;
   }

   @VisibleForTesting
   public String getWatchdogStats() {
      return String.format(
         Locale.ROOT,
         "players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s",
         this.players.size(),
         this.entityManager.gatherStats(),
         getTypeCount(this.entityManager.getEntityGetter().getAll(), l_param0 -> BuiltInRegistries.ENTITY_TYPE.getKey(l_param0.getType()).toString()),
         this.blockEntityTickers.size(),
         getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType),
         this.getBlockTicks().count(),
         this.getFluidTicks().count(),
         this.gatherChunkSourceStats()
      );
   }

   private static <T> String getTypeCount(Iterable<T> param0, Function<T, String> param1) {
      try {
         Object2IntOpenHashMap<String> var2 = new Object2IntOpenHashMap<>();

         for (T var4 : param0) {
            String var5 = param1.apply(var4);
            var2.addTo(var5, 1);
         }

         return var2.object2IntEntrySet()
            .stream()
            .sorted(Comparator.comparingInt(l_param1 -> ((it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<?>)l_param1).getIntValue()).reversed())
            .limit(5L)
            .map(l_param0 -> l_param0.getKey() + ":" + l_param0.getIntValue())
            .collect(Collectors.joining(","));
      } catch (Exception var2) {
         return "";
      }
   }

   public static void makeObsidianPlatform(ServerLevel param0) {
      BlockPos var1 = END_SPAWN_POINT;
      int var2 = var1.getX();
      int var3 = var1.getY() - 2;
      int var4 = var1.getZ();
      BlockPos.betweenClosed(var2 - 2, var3 + 1, var4 - 2, var2 + 2, var3 + 3, var4 + 2)
         .forEach(l_param1 -> param0.setBlockAndUpdate(l_param1, Blocks.AIR.defaultBlockState()));
      BlockPos.betweenClosed(var2 - 2, var3, var4 - 2, var2 + 2, var3, var4 + 2)
         .forEach(l_param1 -> param0.setBlockAndUpdate(l_param1, Blocks.OBSIDIAN.defaultBlockState()));
   }

   @Override
   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityManager.getEntityGetter();
   }

   public void addLegacyChunkEntities(Stream<Entity> param0) {
      this.entityManager.addLegacyChunkEntities(param0);
   }

   public void addWorldGenChunkEntities(Stream<Entity> param0) {
      this.entityManager.addWorldGenChunkEntities(param0);
   }

   public void startTickingChunk(LevelChunk param0) {
      param0.unpackTicks(this.getLevelData().getGameTime());
   }

   public void onStructureStartsAvailable(ChunkAccess param0) {
      this.server.execute(() -> this.structureCheck.onStructureLoad(param0.getPos(), param0.getAllStarts()));
   }

   @Override
   public void close() throws IOException {
      super.close();
      this.entityManager.close();
   }

   @Override
   public String gatherChunkSourceStats() {
      return "Chunks[S] W: " + this.chunkSource.gatherStats() + " E: " + this.entityManager.gatherStats();
   }

   public boolean areEntitiesLoaded(long param0) {
      return this.entityManager.areEntitiesLoaded(param0);
   }

   private boolean isPositionTickingWithEntitiesLoaded(long param0) {
      return this.areEntitiesLoaded(param0) && this.chunkSource.isPositionTicking(param0);
   }

   public boolean isPositionEntityTicking(BlockPos param0) {
      return this.entityManager.canPositionTick(param0) && this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.asLong(param0));
   }

   public boolean isNaturalSpawningAllowed(BlockPos param0) {
      return this.entityManager.canPositionTick(param0);
   }

   public boolean isNaturalSpawningAllowed(ChunkPos param0) {
      return this.entityManager.canPositionTick(param0);
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.server.getWorldData().enabledFeatures();
   }

   final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(Entity param0) {
      }

      public void onDestroyed(Entity param0) {
         ServerLevel.this.getScoreboard().entityRemoved(param0);
      }

      public void onTickingStart(Entity param0) {
         ServerLevel.this.entityTickList.add(param0);
      }

      public void onTickingEnd(Entity param0) {
         ServerLevel.this.entityTickList.remove(param0);
      }

      public void onTrackingStart(Entity param0) {
         ServerLevel.this.getChunkSource().addEntity(param0);
         if (param0 instanceof ServerPlayer var2) {
            ServerLevel.this.players.add(var2);
            ServerLevel.this.updateSleepingPlayerList();
         }

         if (param0 instanceof Mob var2) {
            if (ServerLevel.this.isUpdatingNavigations) {
               String var3 = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde(
                  "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
               );
            }

            ServerLevel.this.navigatingMobs.add(var2);
         }

         if (param0 instanceof EnderDragon var2) {
            for (EnderDragonPart var6 : var2.getSubEntities()) {
               ServerLevel.this.dragonParts.put(var6.getId(), var6);
            }
         }

         param0.updateDynamicGameEventListener(DynamicGameEventListener::add);
      }

      public void onTrackingEnd(Entity param0) {
         ServerLevel.this.getChunkSource().removeEntity(param0);
         if (param0 instanceof ServerPlayer var2) {
            ServerLevel.this.players.remove(var2);
            ServerLevel.this.updateSleepingPlayerList();
         }

         if (param0 instanceof Mob var2) {
            if (ServerLevel.this.isUpdatingNavigations) {
               String var3 = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde(
                  "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
               );
            }

            ServerLevel.this.navigatingMobs.remove(var2);
         }

         if (param0 instanceof EnderDragon var2) {
            for (EnderDragonPart var6 : var2.getSubEntities()) {
               ServerLevel.this.dragonParts.remove(var6.getId());
            }
         }

         param0.updateDynamicGameEventListener(DynamicGameEventListener::remove);
      }

      public void onSectionChange(Entity param0) {
         param0.updateDynamicGameEventListener(DynamicGameEventListener::move);
      }
   }
}
