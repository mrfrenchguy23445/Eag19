package net.minecraft.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.DemoMode;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public abstract class MinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements CommandSource, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String VANILLA_BRAND = "vanilla";
   private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
   private static final int TICK_STATS_SPAN = 100;
   public static final int MS_PER_TICK = 50;
   private static final int OVERLOADED_THRESHOLD = 2000;
   private static final int OVERLOADED_WARNING_INTERVAL = 15000;
   private static final long STATUS_EXPIRE_TIME_NS = 5000000000L;
   private static final int MAX_STATUS_PLAYER_SAMPLE = 12;
   public static final int START_CHUNK_RADIUS = 11;
   private static final int START_TICKING_CHUNK_COUNT = 441;
   private static final int AUTOSAVE_INTERVAL = 6000;
   private static final int MAX_TICK_LATENCY = 3;
   public static final int ABSOLUTE_MAX_WORLD_SIZE = 29999984;
   public static final LevelSettings DEMO_SETTINGS = new LevelSettings(
      "Demo World", GameType.SURVIVAL, false, Difficulty.NORMAL, false, new GameRules(), WorldDataConfiguration.DEFAULT
   );
   private static final long DELAYED_TASKS_TICK_EXTENSION = 50L;
   public static final GameProfile ANONYMOUS_PLAYER_PROFILE = new GameProfile(Util.NIL_UUID, "Anonymous Player");
   protected final LevelStorageSource.LevelStorageAccess storageSource;
   protected final PlayerDataStorage playerDataStorage;
   private final List<Runnable> tickables = Lists.newArrayList();
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private ProfilerFiller profiler = this.metricsRecorder.getProfiler();
   private Consumer<ProfileResults> onMetricsRecordingStopped = l_param0 -> this.stopRecordingMetrics();
   private Consumer<Path> onMetricsRecordingFinished = l_param0 -> {};
   private boolean willStartRecordingMetrics;
   @Nullable
   private MinecraftServer.TimeProfiler debugCommandProfiler;
   private boolean debugCommandProfilerDelayStart;
   private final ServerConnectionListener connection;
   private final ChunkProgressListenerFactory progressListenerFactory;
   @Nullable
   private ServerStatus status;
   @Nullable
   private ServerStatus.Favicon statusIcon;
   private final RandomSource random = RandomSource.create();
   private final DataFixer fixerUpper;
   private String localIp;
   private int port = -1;
   private final LayeredRegistryAccess<RegistryLayer> registries;
   private final Map<ResourceKey<Level>, ServerLevel> levels = Maps.newLinkedHashMap();
   private PlayerList playerList;
   private volatile boolean running = true;
   private boolean stopped;
   private int tickCount;
   protected final Proxy proxy;
   private boolean onlineMode;
   private boolean preventProxyConnections;
   private boolean pvp;
   private boolean allowFlight;
   @Nullable
   private String motd;
   private int playerIdleTimeout;
   public final long[] tickTimes = new long[100];
   @Nullable
   private KeyPair keyPair;
   @Nullable
   private GameProfile singleplayerProfile;
   private boolean isDemo;
   private volatile boolean isReady;
   private long lastOverloadWarning;
   protected final Services services;
   private long lastServerStatus;
   private final Thread serverThread;
   private long nextTickTime = Util.getMillis();
   private long delayedTasksMaxNextTickTime;
   private boolean mayHaveDelayedTasks;
   private final PackRepository packRepository;
   private final ServerScoreboard scoreboard = new ServerScoreboard(this);
   @Nullable
   private CommandStorage commandStorage;
   private final CustomBossEvents customBossEvents = new CustomBossEvents();
   private final ServerFunctionManager functionManager;
   private final FrameTimer frameTimer = new FrameTimer();
   private boolean enforceWhitelist;
   private float averageTickTime;
   private final Executor executor;
   @Nullable
   private String serverId;
   private MinecraftServer.ReloadableResources resources;
   private final StructureTemplateManager structureTemplateManager;
   protected final WorldData worldData;
   private volatile boolean isSaving;

   public static <S extends MinecraftServer> S spin(Function<Thread, S> param0) {
      AtomicReference<S> var1 = new AtomicReference<>();
      Thread var2 = new Thread(() -> var1.get().runServer(), "Server thread");
      var2.setUncaughtExceptionHandler((l_param0, l_param1) -> LOGGER.error("Uncaught exception in server thread", l_param1));
      if (Runtime.getRuntime().availableProcessors() > 4) {
         var2.setPriority(8);
      }

      S var3 = (S)param0.apply(var2);
      var1.set(var3);
      var2.start();
      return var3;
   }

   public MinecraftServer(
      Thread param0,
      LevelStorageSource.LevelStorageAccess param1,
      PackRepository param2,
      WorldStem param3,
      Proxy param4,
      DataFixer param5,
      Services param6,
      ChunkProgressListenerFactory param7
   ) {
      super("Server");
      this.registries = param3.registries();
      this.worldData = param3.worldData();
      if (!this.registries.compositeAccess().registryOrThrow(Registries.LEVEL_STEM).containsKey(LevelStem.OVERWORLD)) {
         throw new IllegalStateException("Missing Overworld dimension data");
      }

      this.proxy = param4;
      this.packRepository = param2;
      this.resources = new MinecraftServer.ReloadableResources(param3.resourceManager(), param3.dataPackResources());
      this.services = param6;
      if (param6.profileCache() != null) {
         param6.profileCache().setExecutor(this);
      }

      this.connection = new ServerConnectionListener(this);
      this.progressListenerFactory = param7;
      this.storageSource = param1;
      this.playerDataStorage = param1.createPlayerStorage();
      this.fixerUpper = param5;
      this.functionManager = new ServerFunctionManager(this, this.resources.managers.getFunctionLibrary());
      HolderGetter<Block> var9 = this.registries
         .compositeAccess()
         .registryOrThrow(Registries.BLOCK)
         .asLookup()
         .filterFeatures(this.worldData.enabledFeatures());
      this.structureTemplateManager = new StructureTemplateManager(param3.resourceManager(), param1, param5, var9);
      this.serverThread = param0;
      this.executor = Util.backgroundExecutor();
   }

   private void readScoreboard(DimensionDataStorage param0) {
      param0.computeIfAbsent(this.getScoreboard()::createData, this.getScoreboard()::createData, "scoreboard");
   }

   protected abstract boolean initServer() throws IOException;

   protected void loadLevel() {
      if (!JvmProfiler.INSTANCE.isRunning()) {
      }

      boolean var1 = false;
      ProfiledDuration var2 = JvmProfiler.INSTANCE.onWorldLoadedStarted();
      this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
      ChunkProgressListener var3 = this.progressListenerFactory.create(11);
      this.createLevels(var3);
      this.forceDifficulty();
      this.prepareLevels(var3);
      if (var2 != null) {
         var2.finish();
      }

      if (var1) {
         try {
            JvmProfiler.INSTANCE.stop();
         } catch (Throwable var4) {
            LOGGER.warn("Failed to stop JFR profiling", var4);
         }
      }
   }

   protected void forceDifficulty() {
   }

   protected void createLevels(ChunkProgressListener param0) {
      ServerLevelData var2 = this.worldData.overworldData();
      boolean var3 = this.worldData.isDebugWorld();
      Registry<LevelStem> var4 = this.registries.compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
      WorldOptions var5 = this.worldData.worldGenOptions();
      long var6 = var5.seed();
      long var8 = BiomeManager.obfuscateSeed(var6);
      List<CustomSpawner> var10 = ImmutableList.of(
         new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(var2)
      );
      LevelStem var11 = var4.get(LevelStem.OVERWORLD);
      ServerLevel var12 = new ServerLevel(this, this.executor, this.storageSource, var2, Level.OVERWORLD, var11, param0, var3, var8, var10, true);
      this.levels.put(Level.OVERWORLD, var12);
      DimensionDataStorage var13 = var12.getDataStorage();
      this.readScoreboard(var13);
      this.commandStorage = new CommandStorage(var13);
      WorldBorder var14 = var12.getWorldBorder();
      if (!var2.isInitialized()) {
         try {
            setInitialSpawn(var12, var2, var5.generateBonusChest(), var3);
            var2.setInitialized(true);
            if (var3) {
               this.setupDebugLevel(this.worldData);
            }
         } catch (Throwable var15) {
            CrashReport var16 = CrashReport.forThrowable(var15, "Exception initializing level");

            try {
               var12.fillReportDetails(var16);
            } catch (Throwable var21) {
            }

            throw new ReportedException(var16);
         }

         var2.setInitialized(true);
      }

      this.getPlayerList().addWorldborderListener(var12);
      if (this.worldData.getCustomBossEvents() != null) {
         this.getCustomBossEvents().load(this.worldData.getCustomBossEvents());
      }

      for (Entry<ResourceKey<LevelStem>, LevelStem> var16 : var4.entrySet()) {
         ResourceKey<LevelStem> var17 = var16.getKey();
         if (var17 != LevelStem.OVERWORLD) {
            ResourceKey<Level> var18 = ResourceKey.create(Registries.DIMENSION, var17.location());
            DerivedLevelData var19 = new DerivedLevelData(this.worldData, var2);
            ServerLevel var20 = new ServerLevel(
               this, this.executor, this.storageSource, var19, var18, var16.getValue(), param0, var3, var8, ImmutableList.of(), false
            );
            var14.addListener(new BorderChangeListener.DelegateBorderChangeListener(var20.getWorldBorder()));
            this.levels.put(var18, var20);
         }
      }

      var14.applySettings(var2.getWorldBorder());
   }

   private static void setInitialSpawn(ServerLevel param0, ServerLevelData param1, boolean param2, boolean param3) {
      if (param3) {
         param1.setSpawn(BlockPos.ZERO.above(80), 0.0F);
      } else {
         ServerChunkCache var4 = param0.getChunkSource();
         ChunkPos var5 = new ChunkPos(var4.randomState().sampler().findSpawnPosition());
         int var6 = var4.getGenerator().getSpawnHeight(param0);
         if (var6 < param0.getMinBuildHeight()) {
            BlockPos var7 = var5.getWorldPosition();
            var6 = param0.getHeight(Heightmap.Types.WORLD_SURFACE, var7.getX() + 8, var7.getZ() + 8);
         }

         param1.setSpawn(var5.getWorldPosition().offset(8, var6, 8), 0.0F);
         int var7 = 0;
         int var8 = 0;
         int var9 = 0;
         int var10 = -1;
         int var11 = 5;

         for (int var12 = 0; var12 < Mth.square(11); var12++) {
            if (var7 >= -5 && var7 <= 5 && var8 >= -5 && var8 <= 5) {
               BlockPos var13 = PlayerRespawnLogic.getSpawnPosInChunk(param0, new ChunkPos(var5.x + var7, var5.z + var8));
               if (var13 != null) {
                  param1.setSpawn(var13, 0.0F);
                  break;
               }
            }

            if (var7 == var8 || var7 < 0 && var7 == -var8 || var7 > 0 && var7 == 1 - var8) {
               int var13 = var9;
               var9 = -var10;
               var10 = var13;
            }

            var7 += var9;
            var8 += var10;
         }

         if (param2) {
            param0.registryAccess()
               .registry(Registries.CONFIGURED_FEATURE)
               .flatMap(l_param0 -> l_param0.getHolder(MiscOverworldFeatures.BONUS_CHEST))
               .ifPresent(
                  l_param3 -> l_param3.value()
                     .place(param0, var4.getGenerator(), param0.random, new BlockPos(param1.getXSpawn(), param1.getYSpawn(), param1.getZSpawn()))
               );
         }
      }
   }

   private void setupDebugLevel(WorldData param0) {
      param0.setDifficulty(Difficulty.PEACEFUL);
      param0.setDifficultyLocked(true);
      ServerLevelData var2 = param0.overworldData();
      var2.setRaining(false);
      var2.setThundering(false);
      var2.setClearWeatherTime(1000000000);
      var2.setDayTime(6000L);
      var2.setGameType(GameType.SPECTATOR);
   }

   private void prepareLevels(ChunkProgressListener param0) {
      ServerLevel var2 = this.overworld();
      LOGGER.info("Preparing start region for dimension {}", var2.dimension().location());
      BlockPos var3 = var2.getSharedSpawnPos();
      param0.updateSpawnPos(new ChunkPos(var3));
      ServerChunkCache var4 = var2.getChunkSource();
      var4.getLightEngine().setTaskPerBatch(500);
      this.nextTickTime = Util.getMillis();
      var4.addRegionTicket(TicketType.START, new ChunkPos(var3), 11, Unit.INSTANCE);

      while (var4.getTickingGenerated() != 441) {
         this.nextTickTime = Util.getMillis() + 10L;
         this.waitUntilNextTick();
      }

      this.nextTickTime = Util.getMillis() + 10L;
      this.waitUntilNextTick();

      for (ServerLevel var6 : this.levels.values()) {
         ForcedChunksSavedData var7 = var6.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
         if (var7 != null) {
            LongIterator var8 = var7.getChunks().iterator();

            while (var8.hasNext()) {
               long var9 = var8.nextLong();
               ChunkPos var11 = new ChunkPos(var9);
               var6.getChunkSource().updateChunkForced(var11, true);
            }
         }
      }

      this.nextTickTime = Util.getMillis() + 10L;
      this.waitUntilNextTick();
      param0.stop();
      var4.getLightEngine().setTaskPerBatch(5);
      this.updateMobSpawningFlags();
   }

   public GameType getDefaultGameType() {
      return this.worldData.getGameType();
   }

   public boolean isHardcore() {
      return this.worldData.isHardcore();
   }

   public abstract int getOperatorUserPermissionLevel();

   public abstract int getFunctionCompilationLevel();

   public abstract boolean shouldRconBroadcast();

   public boolean saveAllChunks(boolean param0, boolean param1, boolean param2) {
      boolean var4 = false;

      for (ServerLevel var6 : this.getAllLevels()) {
         if (!param0) {
            LOGGER.info("Saving chunks for level '{}'/{}", var6, var6.dimension().location());
         }

         var6.save(null, param1, var6.noSave && !param2);
         var4 = true;
      }

      ServerLevel var5 = this.overworld();
      ServerLevelData var6 = this.worldData.overworldData();
      var6.setWorldBorder(var5.getWorldBorder().createSettings());
      this.worldData.setCustomBossEvents(this.getCustomBossEvents().save());
      this.storageSource.saveDataTag(this.registryAccess(), this.worldData, this.getPlayerList().getSingleplayerData());
      if (param1) {
         for (ServerLevel var8 : this.getAllLevels()) {
            LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", var8.getChunkSource().chunkMap.getStorageName());
         }

         LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
      }

      return var4;
   }

   public boolean saveEverything(boolean param0, boolean param1, boolean param2) {
      try {
         this.isSaving = true;
         this.getPlayerList().saveAll();
         return this.saveAllChunks(param0, param1, param2);
      } finally {
         this.isSaving = false;
      }
   }

   @Override
   public void close() {
      this.stopServer();
   }

   public void stopServer() {
      if (this.metricsRecorder.isRecording()) {
         this.cancelRecordingMetrics();
      }

      LOGGER.info("Stopping server");
      if (this.getConnection() != null) {
         this.getConnection().stop();
      }

      this.isSaving = true;
      if (this.playerList != null) {
         LOGGER.info("Saving players");
         this.playerList.saveAll();
         this.playerList.removeAll();
      }

      LOGGER.info("Saving worlds");

      for (ServerLevel var2 : this.getAllLevels()) {
         if (var2 != null) {
            var2.noSave = false;
         }
      }

      while (this.levels.values().stream().anyMatch(l_param0 -> l_param0.getChunkSource().chunkMap.hasWork())) {
         this.nextTickTime = Util.getMillis() + 1L;

         for (ServerLevel var2 : this.getAllLevels()) {
            var2.getChunkSource().removeTicketsOnClosing();
            var2.getChunkSource().tick(() -> true, false);
         }

         this.waitUntilNextTick();
      }

      this.saveAllChunks(false, true, false);

      for (ServerLevel var2 : this.getAllLevels()) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (IOException var3) {
               LOGGER.error("Exception closing the level", var3);
            }
         }
      }

      this.isSaving = false;
      this.resources.close();

      try {
         this.storageSource.close();
      } catch (IOException var1) {
         LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), var1);
      }
   }

   public String getLocalIp() {
      return this.localIp;
   }

   public void setLocalIp(String param0) {
      this.localIp = param0;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void halt(boolean param0) {
      this.running = false;
      if (param0) {
         try {
            this.serverThread.join();
         } catch (InterruptedException var2) {
            LOGGER.error("Error while shutting down", var2);
         }
      }
   }

   protected void runServer() {
      try {
         if (!this.initServer()) {
            throw new IllegalStateException("Failed to initialize server");
         }

         this.nextTickTime = Util.getMillis();
         this.statusIcon = this.loadStatusIcon().orElse(null);
         this.status = this.buildServerStatus();

         while (this.running) {
            long var1 = Util.getMillis() - this.nextTickTime;
            if (var1 > 2000L && this.nextTickTime - this.lastOverloadWarning >= 15000L) {
               long var3 = var1 / 50L;
               LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", var1, var3);
               this.nextTickTime += var3 * 50L;
               this.lastOverloadWarning = this.nextTickTime;
            }

            if (this.debugCommandProfilerDelayStart) {
               this.debugCommandProfilerDelayStart = false;
               this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
            }

            this.nextTickTime += 50L;
            this.startMetricsRecordingTick();
            this.profiler.push("tick");
            this.tickServer(this::haveTime);
            this.profiler.popPush("nextTickWait");
            this.mayHaveDelayedTasks = true;
            this.delayedTasksMaxNextTickTime = Math.max(Util.getMillis() + 50L, this.nextTickTime);
            this.waitUntilNextTick();
            this.profiler.pop();
            this.endMetricsRecordingTick();
            this.isReady = true;
            JvmProfiler.INSTANCE.onServerTick(this.averageTickTime);
         }
      } catch (Throwable var1) {
         LOGGER.error("Encountered an unexpected exception", var1);
         CrashReport var2 = constructOrExtractCrashReport(var1);
         this.fillSystemReport(var2.getSystemReport());
         File var3 = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
         if (var2.saveToFile(var3)) {
            LOGGER.error("This crash report has been saved to: {}", var3.getAbsolutePath());
         } else {
            LOGGER.error("We were unable to save this crash report to disk.");
         }

         this.onServerCrash(var2);
      } finally {
         try {
            this.stopped = true;
            this.stopServer();
         } catch (Throwable var8) {
            LOGGER.error("Exception stopping the server", var8);
         } finally {
            if (this.services.profileCache() != null) {
               this.services.profileCache().clearExecutor();
            }

            this.onServerExit();
         }
      }
   }

   private static CrashReport constructOrExtractCrashReport(Throwable param0) {
      ReportedException var1 = null;

      for (Throwable var2 = param0; var2 != null; var2 = var2.getCause()) {
         if (var2 instanceof ReportedException var3) {
            var1 = var3;
         }
      }

      CrashReport var3;
      if (var1 != null) {
         var3 = var1.getReport();
         if (var1 != param0) {
            var3.addCategory("Wrapped in").setDetailError("Wrapping exception", param0);
         }
      } else {
         var3 = new CrashReport("Exception in server tick loop", param0);
      }

      return var3;
   }

   private boolean haveTime() {
      return this.runningTask() || Util.getMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
   }

   protected void waitUntilNextTick() {
      this.runAllTasks();
      this.managedBlock(() -> !this.haveTime());
   }

   protected TickTask wrapRunnable(Runnable param0) {
      return new TickTask(this.tickCount, param0);
   }

   protected boolean shouldRun(TickTask param0) {
      return param0.getTick() + 3 < this.tickCount || this.haveTime();
   }

   @Override
   public boolean pollTask() {
      boolean var1 = this.pollTaskInternal();
      this.mayHaveDelayedTasks = var1;
      return var1;
   }

   private boolean pollTaskInternal() {
      if (super.pollTask()) {
         return true;
      }

      if (this.haveTime()) {
         for (ServerLevel var2 : this.getAllLevels()) {
            if (var2.getChunkSource().pollTask()) {
               return true;
            }
         }
      }

      return false;
   }

   public void doRunTask(TickTask param0) {
      this.getProfiler().incrementCounter("runTask");
      super.doRunTask(param0);
   }

   private Optional<ServerStatus.Favicon> loadStatusIcon() {
      Optional<Path> var1 = Optional.of(this.getFile("server-icon.png").toPath())
         .filter(l_param0 -> Files.isRegularFile(l_param0))
         .or(() -> this.storageSource.getIconFile().filter(l2_param0 -> Files.isRegularFile(l2_param0)));
      return var1.flatMap(l_param0 -> {
         try {
            BufferedImage l_var1 = ImageIO.read(l_param0.toFile());
            Preconditions.checkState(l_var1.getWidth() == 64, "Must be 64 pixels wide");
            Preconditions.checkState(l_var1.getHeight() == 64, "Must be 64 pixels high");
            ByteArrayOutputStream l_var2 = new ByteArrayOutputStream();
            ImageIO.write(l_var1, "PNG", l_var2);
            return Optional.of(new ServerStatus.Favicon(l_var2.toByteArray()));
         } catch (Exception l_var1) {
            LOGGER.error("Couldn't load server icon", l_var1);
            return Optional.empty();
         }
      });
   }

   public Optional<Path> getWorldScreenshotFile() {
      return this.storageSource.getIconFile();
   }

   public File getServerDirectory() {
      return new File(".");
   }

   public void onServerCrash(CrashReport param0) {
   }

   public void onServerExit() {
   }

   public void tickServer(BooleanSupplier param0) {
      long var2 = Util.getNanos();
      this.tickCount++;
      this.tickChildren(param0);
      if (var2 - this.lastServerStatus >= 5000000000L) {
         this.lastServerStatus = var2;
         this.status = this.buildServerStatus();
      }

      if (this.tickCount % 6000 == 0) {
         LOGGER.debug("Autosave started");
         this.profiler.push("save");
         this.saveEverything(true, false, false);
         this.profiler.pop();
         LOGGER.debug("Autosave finished");
      }

      this.profiler.push("tallying");
      long var4 = this.tickTimes[this.tickCount % 100] = Util.getNanos() - var2;
      this.averageTickTime = this.averageTickTime * 0.8F + (float)var4 / 1000000.0F * 0.19999999F;
      long var6 = Util.getNanos();
      this.frameTimer.logFrameDuration(var6 - var2);
      this.profiler.pop();
   }

   private ServerStatus buildServerStatus() {
      ServerStatus.Players var1 = this.buildPlayerStatus();
      return new ServerStatus(
         Component.nullToEmpty(this.motd),
         Optional.of(var1),
         Optional.of(ServerStatus.Version.current()),
         Optional.ofNullable(this.statusIcon),
         this.enforceSecureProfile()
      );
   }

   private ServerStatus.Players buildPlayerStatus() {
      List<ServerPlayer> var1 = this.playerList.getPlayers();
      int var2 = this.getMaxPlayers();
      if (this.hidesOnlinePlayers()) {
         return new ServerStatus.Players(var2, var1.size(), List.of());
      }

      int var3 = Math.min(var1.size(), 12);
      ObjectArrayList<GameProfile> var4 = new ObjectArrayList<>(var3);
      int var5 = Mth.nextInt(this.random, 0, var1.size() - var3);

      for (int var6 = 0; var6 < var3; var6++) {
         ServerPlayer var7 = var1.get(var5 + var6);
         var4.add(var7.allowsListing() ? var7.getGameProfile() : ANONYMOUS_PLAYER_PROFILE);
      }

      Util.shuffle(var4, this.random);
      return new ServerStatus.Players(var2, var1.size(), var4);
   }

   public void tickChildren(BooleanSupplier param0) {
      this.profiler.push("commandFunctions");
      this.getFunctions().tick();
      this.profiler.popPush("levels");

      for (ServerLevel var3 : this.getAllLevels()) {
         this.profiler.push(() -> var3 + " " + var3.dimension().location());
         if (this.tickCount % 20 == 0) {
            this.profiler.push("timeSync");
            this.synchronizeTime(var3);
            this.profiler.pop();
         }

         this.profiler.push("tick");

         try {
            var3.tick(param0);
         } catch (Throwable var4) {
            CrashReport var5 = CrashReport.forThrowable(var4, "Exception ticking world");
            var3.fillReportDetails(var5);
            throw new ReportedException(var5);
         }

         this.profiler.pop();
         this.profiler.pop();
      }

      this.profiler.popPush("connection");
      this.getConnection().tick();
      this.profiler.popPush("players");
      this.playerList.tick();
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         GameTestTicker.SINGLETON.tick();
      }

      this.profiler.popPush("server gui refresh");

      for (int var2 = 0; var2 < this.tickables.size(); var2++) {
         this.tickables.get(var2).run();
      }

      this.profiler.pop();
   }

   private void synchronizeTime(ServerLevel param0) {
      this.playerList
         .broadcastAll(
            new ClientboundSetTimePacket(param0.getGameTime(), param0.getDayTime(), param0.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)),
            param0.dimension()
         );
   }

   public void forceTimeSynchronization() {
      this.profiler.push("timeSync");

      for (ServerLevel var2 : this.getAllLevels()) {
         this.synchronizeTime(var2);
      }

      this.profiler.pop();
   }

   public boolean isNetherEnabled() {
      return true;
   }

   public void addTickable(Runnable param0) {
      this.tickables.add(param0);
   }

   protected void setId(String param0) {
      this.serverId = param0;
   }

   public boolean isShutdown() {
      return !this.serverThread.isAlive();
   }

   public File getFile(String param0) {
      return new File(this.getServerDirectory(), param0);
   }

   public final ServerLevel overworld() {
      return this.levels.get(Level.OVERWORLD);
   }

   @Nullable
   public ServerLevel getLevel(ResourceKey<Level> param0) {
      return this.levels.get(param0);
   }

   public Set<ResourceKey<Level>> levelKeys() {
      return this.levels.keySet();
   }

   public Iterable<ServerLevel> getAllLevels() {
      return this.levels.values();
   }

   public String getServerVersion() {
      return SharedConstants.getCurrentVersion().getName();
   }

   public int getPlayerCount() {
      return this.playerList.getPlayerCount();
   }

   public int getMaxPlayers() {
      return this.playerList.getMaxPlayers();
   }

   public String[] getPlayerNames() {
      return this.playerList.getPlayerNamesArray();
   }

   @DontObfuscate
   public String getServerModName() {
      return "vanilla";
   }

   public SystemReport fillSystemReport(SystemReport param0) {
      param0.setDetail("Server Running", () -> Boolean.toString(this.running));
      if (this.playerList != null) {
         param0.setDetail(
            "Player Count", () -> this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers()
         );
      }

      param0.setDetail(
         "Data Packs",
         () -> this.packRepository
            .getSelectedPacks()
            .stream()
            .map(l2_param0 -> l2_param0.getId() + (l2_param0.getCompatibility().isCompatible() ? "" : " (incompatible)"))
            .collect(Collectors.joining(", "))
      );
      param0.setDetail(
         "Enabled Feature Flags",
         () -> FeatureFlags.REGISTRY.toNames(this.worldData.enabledFeatures()).stream().map(ResourceLocation::toString).collect(Collectors.joining(", "))
      );
      param0.setDetail("World Generation", () -> this.worldData.worldGenSettingsLifecycle().toString());
      if (this.serverId != null) {
         param0.setDetail("Server Id", () -> this.serverId);
      }

      return this.fillServerSystemReport(param0);
   }

   public abstract SystemReport fillServerSystemReport(SystemReport var1);

   public ModCheck getModdedStatus() {
      return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
   }

   @Override
   public void sendSystemMessage(Component param0) {
      LOGGER.info(param0.getString());
   }

   public KeyPair getKeyPair() {
      return this.keyPair;
   }

   public int getPort() {
      return this.port;
   }

   public void setPort(int param0) {
      this.port = param0;
   }

   @Nullable
   public GameProfile getSingleplayerProfile() {
      return this.singleplayerProfile;
   }

   public void setSingleplayerProfile(@Nullable GameProfile param0) {
      this.singleplayerProfile = param0;
   }

   public boolean isSingleplayer() {
      return this.singleplayerProfile != null;
   }

   protected void initializeKeyPair() {
      // Port note (wasm14): the RSA keypair is deliberately not generated. Upstream this called
      // Crypt.generateKeyPair(), i.e. KeyPairGenerator.getInstance("RSA") with a 1024-bit key.
      //
      // TeaVM's WASM-GC classlib has no java.security.KeyPairGenerator, so the name is answered by
      // the HOST JDK's jrt image instead -- scripts/audit-classlib.py reports it as host-JDK-served,
      // as the one such class this path uses. That is the fragile resolution the audit exists to
      // catch: the host class reaches a JCE provider (SunRsaSign) whose RSA implementation is not
      // translatable, so this is a run-time failure that no link error would have announced.
      //
      // The keypair has exactly one use in this port. ServerLoginPacketListenerImpl reads it only
      // under `usesAuthentication() && !isMemoryConnection()` (handleHello line 158, handleKey lines
      // 175/183) to build the encrypted-login handshake; a browser client runs its integrated server
      // in offline mode, so getKeyPair() below is never read. It now returns the null the field
      // already held, which is what those gated callers would have seen had the field never been
      // assigned. Leaving it unassigned keeps handleHello's gate honest: online-mode login stays
      // unavailable here exactly as it was before, and it fails at that gate rather than inside a
      // provider.
      LOGGER.info("Skipping keypair generation: this port runs its server without encrypted login");
   }

   public void setDifficulty(Difficulty param0, boolean param1) {
      if (param1 || !this.worldData.isDifficultyLocked()) {
         this.worldData.setDifficulty(this.worldData.isHardcore() ? Difficulty.HARD : param0);
         this.updateMobSpawningFlags();
         this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
      }
   }

   public int getScaledTrackingDistance(int param0) {
      return param0;
   }

   private void updateMobSpawningFlags() {
      for (ServerLevel var2 : this.getAllLevels()) {
         var2.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
      }
   }

   public void setDifficultyLocked(boolean param0) {
      this.worldData.setDifficultyLocked(param0);
      this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
   }

   private void sendDifficultyUpdate(ServerPlayer param0) {
      LevelData var2 = param0.getLevel().getLevelData();
      param0.connection.send(new ClientboundChangeDifficultyPacket(var2.getDifficulty(), var2.isDifficultyLocked()));
   }

   public boolean isSpawningMonsters() {
      return this.worldData.getDifficulty() != Difficulty.PEACEFUL;
   }

   public boolean isDemo() {
      return this.isDemo;
   }

   public void setDemo(boolean param0) {
      this.isDemo = param0;
   }

   public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
      return Optional.empty();
   }

   public boolean isResourcePackRequired() {
      return this.getServerResourcePack().filter(MinecraftServer.ServerResourcePackInfo::isRequired).isPresent();
   }

   public abstract boolean isDedicatedServer();

   public abstract int getRateLimitPacketsPerSecond();

   public boolean usesAuthentication() {
      return this.onlineMode;
   }

   public void setUsesAuthentication(boolean param0) {
      this.onlineMode = param0;
   }

   public boolean getPreventProxyConnections() {
      return this.preventProxyConnections;
   }

   public void setPreventProxyConnections(boolean param0) {
      this.preventProxyConnections = param0;
   }

   public boolean isSpawningAnimals() {
      return true;
   }

   public boolean areNpcsEnabled() {
      return true;
   }

   public abstract boolean isEpollEnabled();

   public boolean isPvpAllowed() {
      return this.pvp;
   }

   public void setPvpAllowed(boolean param0) {
      this.pvp = param0;
   }

   public boolean isFlightAllowed() {
      return this.allowFlight;
   }

   public void setFlightAllowed(boolean param0) {
      this.allowFlight = param0;
   }

   public abstract boolean isCommandBlockEnabled();

   public String getMotd() {
      return this.motd;
   }

   public void setMotd(String param0) {
      this.motd = param0;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public void setPlayerList(PlayerList param0) {
      this.playerList = param0;
   }

   public abstract boolean isPublished();

   public void setDefaultGameType(GameType param0) {
      this.worldData.setGameType(param0);
   }

   @Nullable
   public ServerConnectionListener getConnection() {
      return this.connection;
   }

   public boolean isReady() {
      return this.isReady;
   }

   public boolean hasGui() {
      return false;
   }

   public boolean publishServer(@Nullable GameType param0, boolean param1, int param2) {
      return false;
   }

   public int getTickCount() {
      return this.tickCount;
   }

   public int getSpawnProtectionRadius() {
      return 16;
   }

   public boolean isUnderSpawnProtection(ServerLevel param0, BlockPos param1, Player param2) {
      return false;
   }

   public boolean repliesToStatus() {
      return true;
   }

   public boolean hidesOnlinePlayers() {
      return false;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public int getPlayerIdleTimeout() {
      return this.playerIdleTimeout;
   }

   public void setPlayerIdleTimeout(int param0) {
      this.playerIdleTimeout = param0;
   }

   public MinecraftSessionService getSessionService() {
      return this.services.sessionService();
   }

   public SignatureValidator getServiceSignatureValidator() {
      return this.services.serviceSignatureValidator();
   }

   public GameProfileRepository getProfileRepository() {
      return this.services.profileRepository();
   }

   public GameProfileCache getProfileCache() {
      return this.services.profileCache();
   }

   @Nullable
   public ServerStatus getStatus() {
      return this.status;
   }

   public void invalidateStatus() {
      this.lastServerStatus = 0L;
   }

   public int getAbsoluteMaxWorldSize() {
      return 29999984;
   }

   @Override
   public boolean scheduleExecutables() {
      return super.scheduleExecutables() && !this.isStopped();
   }

   @Override
   public void executeIfPossible(Runnable param0) {
      if (this.isStopped()) {
         throw new RejectedExecutionException("Server already shutting down");
      }

      super.executeIfPossible(param0);
   }

   @Override
   public Thread getRunningThread() {
      return this.serverThread;
   }

   public int getCompressionThreshold() {
      return 256;
   }

   public boolean enforceSecureProfile() {
      return false;
   }

   public long getNextTickTime() {
      return this.nextTickTime;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public int getSpawnRadius(@Nullable ServerLevel param0) {
      return param0 != null ? param0.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS) : 10;
   }

   public ServerAdvancementManager getAdvancements() {
      return this.resources.managers.getAdvancements();
   }

   public ServerFunctionManager getFunctions() {
      return this.functionManager;
   }

   public CompletableFuture<Void> reloadResources(Collection<String> param0) {
      RegistryAccess.Frozen var2 = this.registries.getAccessForLoading(RegistryLayer.RELOADABLE);
      CompletableFuture<Void> var3 = CompletableFuture.<ImmutableList>supplyAsync(
            () -> param0.stream().map(this.packRepository::getPack).filter(Objects::nonNull).map(Pack::open).collect(ImmutableList.toImmutableList()), this
         )
         .thenCompose(
            l_param1 -> {
               CloseableResourceManager l_var3 = new MultiPackResourceManager(PackType.SERVER_DATA, l_param1);
               return ReloadableServerResources.loadResources(
                     l_var3,
                     var2,
                     this.worldData.enabledFeatures(),
                     this.isDedicatedServer() ? Commands.CommandSelection.DEDICATED : Commands.CommandSelection.INTEGRATED,
                     this.getFunctionCompilationLevel(),
                     this.executor,
                     this
                  )
                  .whenComplete((l2_param1, l2_param2) -> {
                     if (l2_param2 != null) {
                        l_var3.close();
                     }
                  })
                  .thenApply(l2_param1 -> new MinecraftServer.ReloadableResources(l_var3, l2_param1));
            }
         )
         .thenAcceptAsync(l_param1 -> {
            this.resources.close();
            this.resources = l_param1;
            this.packRepository.setSelected(param0);
            WorldDataConfiguration l_var3 = new WorldDataConfiguration(getSelectedPacks(this.packRepository), this.worldData.enabledFeatures());
            this.worldData.setDataConfiguration(l_var3);
            this.resources.managers.updateRegistryTags(this.registryAccess());
            this.getPlayerList().saveAll();
            this.getPlayerList().reloadResources();
            this.functionManager.replaceLibrary(this.resources.managers.getFunctionLibrary());
            this.structureTemplateManager.onResourceManagerReload(this.resources.resourceManager);
         }, this);
      if (this.isSameThread()) {
         this.managedBlock(var3::isDone);
      }

      return var3;
   }

   public static WorldDataConfiguration configurePackRepository(PackRepository param0, DataPackConfig param1, boolean param2, FeatureFlagSet param3) {
      param0.reload();
      if (param2) {
         param0.setSelected(Collections.singleton("vanilla"));
         return WorldDataConfiguration.DEFAULT;
      }

      Set<String> var4 = Sets.newLinkedHashSet();

      for (String var6 : param1.getEnabled()) {
         if (param0.isAvailable(var6)) {
            var4.add(var6);
         } else {
            LOGGER.warn("Missing data pack {}", var6);
         }
      }

      for (Pack var6 : param0.getAvailablePacks()) {
         String var7 = var6.getId();
         if (!param1.getDisabled().contains(var7)) {
            FeatureFlagSet var8 = var6.getRequestedFeatures();
            boolean var9 = var4.contains(var7);
            if (!var9 && var6.getPackSource().shouldAddAutomatically()) {
               if (var8.isSubsetOf(param3)) {
                  LOGGER.info("Found new data pack {}, loading it automatically", var7);
                  var4.add(var7);
               } else {
                  LOGGER.info("Found new data pack {}, but can't load it due to missing features {}", var7, FeatureFlags.printMissingFlags(param3, var8));
               }
            }

            if (var9 && !var8.isSubsetOf(param3)) {
               LOGGER.warn(
                  "Pack {} requires features {} that are not enabled for this world, disabling pack.", var7, FeatureFlags.printMissingFlags(param3, var8)
               );
               var4.remove(var7);
            }
         }
      }

      if (var4.isEmpty()) {
         LOGGER.info("No datapacks selected, forcing vanilla");
         var4.add("vanilla");
      }

      param0.setSelected(var4);
      DataPackConfig var5 = getSelectedPacks(param0);
      FeatureFlagSet var6 = param0.getRequestedFeatureFlags();
      return new WorldDataConfiguration(var5, var6);
   }

   private static DataPackConfig getSelectedPacks(PackRepository param0) {
      Collection<String> var1 = param0.getSelectedIds();
      List<String> var2 = ImmutableList.copyOf(var1);
      List<String> var3 = param0.getAvailableIds().stream().filter(l_param1 -> !var1.contains(l_param1)).collect(ImmutableList.toImmutableList());
      return new DataPackConfig(var2, var3);
   }

   public void kickUnlistedPlayers(CommandSourceStack param0) {
      if (this.isEnforceWhitelist()) {
         PlayerList var2 = param0.getServer().getPlayerList();
         UserWhiteList var3 = var2.getWhiteList();

         for (ServerPlayer var6 : Lists.newArrayList(var2.getPlayers())) {
            if (!var3.isWhiteListed(var6.getGameProfile())) {
               var6.connection.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted"));
            }
         }
      }
   }

   public PackRepository getPackRepository() {
      return this.packRepository;
   }

   public Commands getCommands() {
      return this.resources.managers.getCommands();
   }

   public CommandSourceStack createCommandSourceStack() {
      ServerLevel var1 = this.overworld();
      return new CommandSourceStack(
         this, var1 == null ? Vec3.ZERO : Vec3.atLowerCornerOf(var1.getSharedSpawnPos()), Vec2.ZERO, var1, 4, "Server", Component.literal("Server"), this, null
      );
   }

   @Override
   public boolean acceptsSuccess() {
      return true;
   }

   @Override
   public boolean acceptsFailure() {
      return true;
   }

   @Override
   public abstract boolean shouldInformAdmins();

   public RecipeManager getRecipeManager() {
      return this.resources.managers.getRecipeManager();
   }

   public ServerScoreboard getScoreboard() {
      return this.scoreboard;
   }

   public CommandStorage getCommandStorage() {
      if (this.commandStorage == null) {
         throw new NullPointerException("Called before server init");
      } else {
         return this.commandStorage;
      }
   }

   public LootTables getLootTables() {
      return this.resources.managers.getLootTables();
   }

   public PredicateManager getPredicateManager() {
      return this.resources.managers.getPredicateManager();
   }

   public ItemModifierManager getItemModifierManager() {
      return this.resources.managers.getItemModifierManager();
   }

   public GameRules getGameRules() {
      return this.overworld().getGameRules();
   }

   public CustomBossEvents getCustomBossEvents() {
      return this.customBossEvents;
   }

   public boolean isEnforceWhitelist() {
      return this.enforceWhitelist;
   }

   public void setEnforceWhitelist(boolean param0) {
      this.enforceWhitelist = param0;
   }

   public float getAverageTickTime() {
      return this.averageTickTime;
   }

   public int getProfilePermissions(GameProfile param0) {
      if (this.getPlayerList().isOp(param0)) {
         ServerOpListEntry var2 = this.getPlayerList().getOps().get(param0);
         if (var2 != null) {
            return var2.getLevel();
         } else if (this.isSingleplayerOwner(param0)) {
            return 4;
         } else if (this.isSingleplayer()) {
            return this.getPlayerList().isAllowCheatsForAllPlayers() ? 4 : 0;
         } else {
            return this.getOperatorUserPermissionLevel();
         }
      } else {
         return 0;
      }
   }

   public FrameTimer getFrameTimer() {
      return this.frameTimer;
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   public abstract boolean isSingleplayerOwner(GameProfile var1);

   public void dumpServerProperties(Path param0) throws IOException {
   }

   private void saveDebugReport(Path param0) {
      // Port note (wasm14): debug-report dumping is disabled. It wrote a thread dump via
      // java.lang.management.ManagementFactory/ThreadInfo and, through ServerLevel, a section
      // dump that builds a LongStream from StreamSupport.longStream -- three classlib members
      // this target does not provide -- and it wrote all of it through java.nio.file.Files to a
      // path a browser has no way to name. Nothing else called it: its only caller is the
      // metrics-recording stop hook below.
      LOGGER.debug("Debug report dumping is not available in this build; skipping.");
   }

   private void dumpMiscStats(Path param0) throws IOException {
      try (Writer var2 = Files.newBufferedWriter(param0)) {
         var2.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getPendingTasksCount()));
         var2.write(String.format(Locale.ROOT, "average_tick_time: %f\n", this.getAverageTickTime()));
         var2.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(this.tickTimes)));
         var2.write(String.format(Locale.ROOT, "queue: %s\n", Util.backgroundExecutor()));
      }
   }

   private void dumpGameRules(Path param0) throws IOException {
      try (Writer var2 = Files.newBufferedWriter(param0)) {
         final List<String> var3 = Lists.newArrayList();
         final GameRules var4 = this.getGameRules();
         GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> l_param0, GameRules.Type<T> l_param1) {
               var3.add(String.format(Locale.ROOT, "%s=%s\n", l_param0.getId(), var4.getRule(l_param0)));
            }
         });

         for (String var6 : var3) {
            var2.write(var6);
         }
      }
   }

   private void dumpClasspath(Path param0) throws IOException {
      try (Writer var2 = Files.newBufferedWriter(param0)) {
         String var3 = System.getProperty("java.class.path");
         String var4 = System.getProperty("path.separator");

         for (String var6 : Splitter.on(var4).split(var3)) {
            var2.write(var6);
            var2.write("\n");
         }
      }
   }

   private void dumpThreads(Path param0) throws IOException {
      ThreadMXBean var2 = ManagementFactory.getThreadMXBean();
      ThreadInfo[] var3 = var2.dumpAllThreads(true, true);
      Arrays.sort(var3, Comparator.comparing(ThreadInfo::getThreadName));

      try (Writer var4 = Files.newBufferedWriter(param0)) {
         for (ThreadInfo var8 : var3) {
            var4.write(var8.toString());
            var4.write(10);
         }
      }
   }

   private void dumpNativeModules(Path param0) throws IOException {
      try (Writer var2 = Files.newBufferedWriter(param0)) {
         List<NativeModuleLister.NativeModuleInfo> var3;
         try {
            var3 = Lists.newArrayList(NativeModuleLister.listModules());
         } catch (Throwable var4) {
            LOGGER.warn("Failed to list native modules", var4);
            return;
         }

         var3.sort(Comparator.comparing(l_param0 -> l_param0.name));

         for (NativeModuleLister.NativeModuleInfo var5 : var3) {
            var2.write(var5.toString());
            var2.write(10);
         }
      }
   }

   private void startMetricsRecordingTick() {
      if (this.willStartRecordingMetrics) {
         this.metricsRecorder = ActiveMetricsRecorder.createStarted(
            new ServerMetricsSamplersProvider(Util.timeSource, this.isDedicatedServer()),
            Util.timeSource,
            Util.ioPool(),
            new MetricsPersister("server"),
            this.onMetricsRecordingStopped,
            l_param0 -> {
               this.executeBlocking(() -> this.saveDebugReport(l_param0.resolve("server")));
               this.onMetricsRecordingFinished.accept(l_param0);
            }
         );
         this.willStartRecordingMetrics = false;
      }

      this.profiler = SingleTickProfiler.decorateFiller(this.metricsRecorder.getProfiler(), SingleTickProfiler.createTickProfiler("Server"));
      this.metricsRecorder.startTick();
      this.profiler.startTick();
   }

   private void endMetricsRecordingTick() {
      this.profiler.endTick();
      this.metricsRecorder.endTick();
   }

   public boolean isRecordingMetrics() {
      return this.metricsRecorder.isRecording();
   }

   public void startRecordingMetrics(Consumer<ProfileResults> param0, Consumer<Path> param1) {
      this.onMetricsRecordingStopped = l_param1 -> {
         this.stopRecordingMetrics();
         param0.accept(l_param1);
      };
      this.onMetricsRecordingFinished = param1;
      this.willStartRecordingMetrics = true;
   }

   public void stopRecordingMetrics() {
      this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   }

   public void finishRecordingMetrics() {
      this.metricsRecorder.end();
   }

   public void cancelRecordingMetrics() {
      this.metricsRecorder.cancel();
      this.profiler = this.metricsRecorder.getProfiler();
   }

   public Path getWorldPath(LevelResource param0) {
      return this.storageSource.getLevelPath(param0);
   }

   public boolean forceSynchronousWrites() {
      return true;
   }

   public StructureTemplateManager getStructureManager() {
      return this.structureTemplateManager;
   }

   public WorldData getWorldData() {
      return this.worldData;
   }

   public RegistryAccess.Frozen registryAccess() {
      return this.registries.compositeAccess();
   }

   public LayeredRegistryAccess<RegistryLayer> registries() {
      return this.registries;
   }

   public TextFilter createTextFilterForPlayer(ServerPlayer param0) {
      return TextFilter.DUMMY;
   }

   public ServerPlayerGameMode createGameModeForPlayer(ServerPlayer param0) {
      return this.isDemo() ? new DemoMode(param0) : new ServerPlayerGameMode(param0);
   }

   @Nullable
   public GameType getForcedGameType() {
      return null;
   }

   public ResourceManager getResourceManager() {
      return this.resources.resourceManager;
   }

   public boolean isCurrentlySaving() {
      return this.isSaving;
   }

   public boolean isTimeProfilerRunning() {
      return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
   }

   public void startTimeProfiler() {
      this.debugCommandProfilerDelayStart = true;
   }

   public ProfileResults stopTimeProfiler() {
      if (this.debugCommandProfiler == null) {
         return EmptyProfileResults.EMPTY;
      }

      ProfileResults var1 = this.debugCommandProfiler.stop(Util.getNanos(), this.tickCount);
      this.debugCommandProfiler = null;
      return var1;
   }

   public int getMaxChainedNeighborUpdates() {
      return 1000000;
   }

   public void logChatMessage(Component param0, ChatType.Bound param1, @Nullable String param2) {
      String var4 = param1.decorate(param0).getString();
      if (param2 != null) {
         LOGGER.info("[{}] {}", param2, var4);
      } else {
         LOGGER.info("{}", var4);
      }
   }

   public ChatDecorator getChatDecorator() {
      return ChatDecorator.PLAIN;
   }

   record ReloadableResources(CloseableResourceManager resourceManager, ReloadableServerResources managers) implements AutoCloseable {

      @Override
      public void close() {
         this.resourceManager.close();
      }
   }

   public record ServerResourcePackInfo(String url, String hash, boolean isRequired, @Nullable Component prompt) {
   }

   static class TimeProfiler {
      final long startNanos;
      final int startTick;

      TimeProfiler(long param0, int param2) {
         this.startNanos = param0;
         this.startTick = param2;
      }

      ProfileResults stop(final long param0, final int param2) {
         return new ProfileResults() {
            @Override
            public List<ResultField> getTimes(String l_param0) {
               return Collections.emptyList();
            }

            @Override
            public boolean saveResults(Path l_param0) {
               return false;
            }

            @Override
            public long getStartTimeNano() {
               return TimeProfiler.this.startNanos;
            }

            @Override
            public int getStartTimeTicks() {
               return TimeProfiler.this.startTick;
            }

            @Override
            public long getEndTimeNano() {
               return param0;
            }

            @Override
            public int getEndTimeTicks() {
               return param2;
            }

            @Override
            public String getProfilerResults() {
               return "";
            }
         };
      }
   }
}
