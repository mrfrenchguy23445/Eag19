package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
   private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
   private static final int MIN_VIEW_DISTANCE = 3;
   public static final int MAX_VIEW_DISTANCE = 33;
   public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
   public static final int FORCED_TICKET_LEVEL = 31;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap<>();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap<>();
   private final LongSet entitiesInLevel = new LongOpenHashSet();
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private ChunkGenerator generator;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final Supplier<DimensionDataStorage> overworldDataStorage;
   private final PoiManager poiManager;
   final LongSet toDrop = new LongOpenHashSet();
   private boolean modified;
   private final ChunkTaskPriorityQueueSorter queueSorter;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
   private final ChunkProgressListener progressListener;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final AtomicInteger tickingGenerated = new AtomicInteger();
   private final StructureTemplateManager structureTemplateManager;
   private final String storageName;
   private final PlayerMap playerMap = new PlayerMap();
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap<>();
   private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
   private final Long2LongMap chunkSaveCooldowns = new Long2LongOpenHashMap();
   private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
   int viewDistance;

   public ChunkMap(
      ServerLevel param0,
      LevelStorageSource.LevelStorageAccess param1,
      DataFixer param2,
      StructureTemplateManager param3,
      Executor param4,
      BlockableEventLoop<Runnable> param5,
      LightChunkGetter param6,
      ChunkGenerator param7,
      ChunkProgressListener param8,
      ChunkStatusUpdateListener param9,
      Supplier<DimensionDataStorage> param10,
      int param11,
      boolean param12
   ) {
      super(param1.getDimensionPath(param0.dimension()).resolve("region"), param2, param12);
      this.structureTemplateManager = param3;
      Path var14 = param1.getDimensionPath(param0.dimension());
      this.storageName = var14.getFileName().toString();
      this.level = param0;
      this.generator = param7;
      RegistryAccess var15 = param0.registryAccess();
      long var16 = param0.getSeed();
      if (param7 instanceof NoiseBasedChunkGenerator var18) {
         this.randomState = RandomState.create(var18.generatorSettings().value(), var15.lookupOrThrow(Registries.NOISE), var16);
      } else {
         this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), var15.lookupOrThrow(Registries.NOISE), var16);
      }

      this.chunkGeneratorState = param7.createState(var15.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, var16);
      this.mainThreadExecutor = param5;
      ProcessorMailbox<Runnable> var18 = ProcessorMailbox.create(param4, "worldgen");
      ProcessorHandle<Runnable> var19 = ProcessorHandle.of("main", param5::tell);
      this.progressListener = param8;
      this.chunkStatusListener = param9;
      ProcessorMailbox<Runnable> var20 = ProcessorMailbox.create(param4, "light");
      this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(var18, var19, var20), param4, Integer.MAX_VALUE);
      this.worldgenMailbox = this.queueSorter.getProcessor(var18, false);
      this.mainThreadMailbox = this.queueSorter.getProcessor(var19, false);
      this.lightEngine = new ThreadedLevelLightEngine(
         param6, this, this.level.dimensionType().hasSkyLight(), var20, this.queueSorter.getProcessor(var20, false)
      );
      this.distanceManager = new ChunkMap.DistanceManager(param4, param5);
      this.overworldDataStorage = param10;
      this.poiManager = new PoiManager(var14.resolve("poi"), param2, param12, var15, param0);
      this.setViewDistance(param11);
   }

   protected ChunkGenerator generator() {
      return this.generator;
   }

   protected ChunkGeneratorStructureState generatorState() {
      return this.chunkGeneratorState;
   }

   protected RandomState randomState() {
      return this.randomState;
   }

   public void debugReloadGenerator() {
      DataResult<JsonElement> var1 = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
      DataResult<ChunkGenerator> var2 = var1.flatMap(l_param0 -> ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, l_param0));
      var2.result().ifPresent(l_param0 -> this.generator = l_param0);
   }

   private static double euclideanDistanceSquared(ChunkPos param0, Entity param1) {
      double var2 = SectionPos.sectionToBlockCoord(param0.x, 8);
      double var4 = SectionPos.sectionToBlockCoord(param0.z, 8);
      double var6 = var2 - param1.getX();
      double var8 = var4 - param1.getZ();
      return var6 * var6 + var8 * var8;
   }

   public static boolean isChunkInRange(int param0, int param1, int param2, int param3, int param4) {
      int var5 = Math.max(0, Math.abs(param0 - param2) - 1);
      int var6 = Math.max(0, Math.abs(param1 - param3) - 1);
      long var7 = Math.max(0, Math.max(var5, var6) - 1);
      long var9 = Math.min(var5, var6);
      long var11 = var9 * var9 + var7 * var7;
      int var13 = param4 - 1;
      int var14 = var13 * var13;
      return var11 <= var14;
   }

   private static boolean isChunkOnRangeBorder(int param0, int param1, int param2, int param3, int param4) {
      if (!isChunkInRange(param0, param1, param2, param3, param4)) {
         return false;
      } else if (!isChunkInRange(param0 + 1, param1, param2, param3, param4)) {
         return true;
      } else if (!isChunkInRange(param0, param1 + 1, param2, param3, param4)) {
         return true;
      } else {
         return !isChunkInRange(param0 - 1, param1, param2, param3, param4) ? true : !isChunkInRange(param0, param1 - 1, param2, param3, param4);
      }
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   protected ChunkHolder getUpdatingChunkIfPresent(long param0) {
      return this.updatingChunkMap.get(param0);
   }

   @Nullable
   protected ChunkHolder getVisibleChunkIfPresent(long param0) {
      return this.visibleChunkMap.get(param0);
   }

   protected IntSupplier getChunkQueueLevel(long param0) {
      return () -> {
         ChunkHolder l_var3 = this.getVisibleChunkIfPresent(param0);
         return l_var3 == null
            ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1
            : Math.min(l_var3.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos param0) {
      ChunkHolder var2 = this.getVisibleChunkIfPresent(param0.toLong());
      if (var2 == null) {
         return "null";
      }

      String var3 = var2.getTicketLevel() + "\n";
      ChunkStatus var4 = var2.getLastAvailableStatus();
      ChunkAccess var5 = var2.getLastAvailable();
      if (var4 != null) {
         var3 = var3 + "St: §" + var4.getIndex() + var4 + "§r\n";
      }

      if (var5 != null) {
         var3 = var3 + "Ch: §" + var5.getStatus().getIndex() + var5.getStatus() + "§r\n";
      }

      ChunkHolder.FullChunkStatus var6 = var2.getFullStatus();
      var3 = var3 + "§" + var6.ordinal() + var6;
      return var3 + "§r";
   }

   private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(
      ChunkPos param0, int param1, IntFunction<ChunkStatus> param2
   ) {
      List<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> var4 = new ArrayList<>();
      List<ChunkHolder> var5 = new ArrayList<>();
      int var6 = param0.x;
      int var7 = param0.z;

      for (int var8 = -param1; var8 <= param1; var8++) {
         for (int var9 = -param1; var9 <= param1; var9++) {
            int var10 = Math.max(Math.abs(var9), Math.abs(var8));
            final ChunkPos var11 = new ChunkPos(var6 + var9, var7 + var8);
            long var12 = var11.toLong();
            ChunkHolder var14 = this.getUpdatingChunkIfPresent(var12);
            if (var14 == null) {
               return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                  @Override
                  public String toString() {
                     return "Unloaded " + var11;
                  }
               }));
            }

            ChunkStatus var15 = param2.apply(var10);
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var16 = var14.getOrScheduleFuture(var15, this);
            var5.add(var14);
            var4.add(var16);
         }
      }

      CompletableFuture<List<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> var8 = Util.sequence(var4);
      CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> var9 = var8.thenApply(l_param3 -> {
         List<ChunkAccess> l_var5 = Lists.newArrayList();
         int l_var6 = 0;

         for (final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> l_var8 : l_param3) {
            if (l_var8 == null) {
               throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
            }

            Optional<ChunkAccess> l_var9 = l_var8.left();
            if (!l_var9.isPresent()) {
               final int l_var10 = l_var6;
               return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                  @Override
                  public String toString() {
                     return "Unloaded " + new ChunkPos(var6 + l_var10 % (param1 * 2 + 1), var7 + l_var10 / (param1 * 2 + 1)) + " " + l_var8.right().get();
                  }
               });
            }

            l_var5.add(l_var9.get());
            l_var6++;
         }

         return Either.left(l_var5);
      });

      for (ChunkHolder var11 : var5) {
         var11.addSaveDependency("getChunkRangeFuture " + param0 + " " + param1, var9);
      }

      return var9;
   }

   public ReportedException debugFuturesAndCreateReportedException(IllegalStateException param0, String param1) {
      StringBuilder var3 = new StringBuilder();
      Consumer<ChunkHolder> var4 = l_param1 -> l_param1.getAllFutures().forEach(l2_param2 -> {
         ChunkStatus l2_var3 = l2_param2.getFirst();
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> l2_var4 = l2_param2.getSecond();
         if (l2_var4 != null && l2_var4.isDone() && l2_var4.join() == null) {
            var3.append(l_param1.getPos()).append(" - status: ").append(l2_var3).append(" future: ").append(l2_var4).append(System.lineSeparator());
         }
      });
      var3.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(var4);
      var3.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(var4);
      CrashReport var5 = CrashReport.forThrowable(param0, "Chunk loading");
      CrashReportCategory var6 = var5.addCategory("Chunk loading");
      var6.setDetail("Details", param1);
      var6.setDetail("Futures", var3);
      return new ReportedException(var5);
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkPos param0) {
      return this.getChunkRangeFuture(param0, 2, l_param0 -> ChunkStatus.FULL)
         .thenApplyAsync(l_param0 -> l_param0.mapLeft(l2_param0 -> (LevelChunk)l2_param0.get(l2_param0.size() / 2)), this.mainThreadExecutor);
   }

   @Nullable
   ChunkHolder updateChunkScheduling(long param0, int param2, @Nullable ChunkHolder param3, int param4) {
      if (param4 > MAX_CHUNK_DISTANCE && param2 > MAX_CHUNK_DISTANCE) {
         return param3;
      }

      if (param3 != null) {
         param3.setTicketLevel(param2);
      }

      if (param3 != null) {
         if (param2 > MAX_CHUNK_DISTANCE) {
            this.toDrop.add(param0);
         } else {
            this.toDrop.remove(param0);
         }
      }

      if (param2 <= MAX_CHUNK_DISTANCE && param3 == null) {
         param3 = this.pendingUnloads.remove(param0);
         if (param3 != null) {
            param3.setTicketLevel(param2);
         } else {
            param3 = new ChunkHolder(new ChunkPos(param0), param2, this.level, this.lightEngine, this.queueSorter, this);
         }

         this.updatingChunkMap.put(param0, param3);
         this.modified = true;
      }

      return param3;
   }

   @Override
   public void close() throws IOException {
      try {
         this.queueSorter.close();
         this.poiManager.close();
      } finally {
         super.close();
      }
   }

   protected void saveAllChunks(boolean param0) {
      if (param0) {
         List<ChunkHolder> var2 = this.visibleChunkMap
            .values()
            .stream()
            .filter(ChunkHolder::wasAccessibleSinceLastSave)
            .peek(ChunkHolder::refreshAccessibility)
            .collect(Collectors.toList());
         MutableBoolean var3 = new MutableBoolean();

         do {
            var3.setFalse();
            var2.stream()
               .map(l_param0 -> {
                  CompletableFuture<ChunkAccess> l_var2;
                  do {
                     l_var2 = l_param0.getChunkToSave();
                     this.mainThreadExecutor.managedBlock(l_var2::isDone);
                  } while (l_var2 != l_param0.getChunkToSave());

                  return l_var2.join();
               })
               .filter(l_param0 -> l_param0 instanceof ImposterProtoChunk || l_param0 instanceof LevelChunk)
               .filter(this::save)
               .forEach(l_param1 -> var3.setTrue());
         } while (var3.isTrue());

         this.processUnloads(() -> true);
         this.flushWorker();
      } else {
         this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
      }
   }

   protected void tick(BooleanSupplier param0) {
      ProfilerFiller var2 = this.level.getProfiler();
      var2.push("poi");
      this.poiManager.tick(param0);
      var2.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(param0);
      }

      var2.pop();
   }

   public boolean hasWork() {
      return this.lightEngine.hasLightWork()
         || !this.pendingUnloads.isEmpty()
         || !this.updatingChunkMap.isEmpty()
         || this.poiManager.hasWork()
         || !this.toDrop.isEmpty()
         || !this.unloadQueue.isEmpty()
         || this.queueSorter.hasWork()
         || this.distanceManager.hasTickets();
   }

   private void processUnloads(BooleanSupplier param0) {
      LongIterator var2 = this.toDrop.iterator();

      for (int var3 = 0; var2.hasNext() && (param0.getAsBoolean() || var3 < 200 || this.toDrop.size() > 2000); var2.remove()) {
         long var4 = var2.nextLong();
         ChunkHolder var6 = this.updatingChunkMap.remove(var4);
         if (var6 != null) {
            this.pendingUnloads.put(var4, var6);
            this.modified = true;
            var3++;
            this.scheduleUnload(var4, var6);
         }
      }

      int var5 = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable var4;
      while ((param0.getAsBoolean() || var5 > 0) && (var4 = this.unloadQueue.poll()) != null) {
         var5--;
         var4.run();
      }

      int var6 = 0;
      ObjectIterator<ChunkHolder> var7 = this.visibleChunkMap.values().iterator();

      while (var6 < 20 && param0.getAsBoolean() && var7.hasNext()) {
         if (this.saveChunkIfNeeded(var7.next())) {
            var6++;
         }
      }
   }

   private void scheduleUnload(long param0, ChunkHolder param2) {
      CompletableFuture<ChunkAccess> var4 = param2.getChunkToSave();
      var4.thenAcceptAsync(l_param4 -> {
         CompletableFuture<ChunkAccess> l_var6 = param2.getChunkToSave();
         if (l_var6 != var4) {
            this.scheduleUnload(param0, param2);
         } else {
            if (this.pendingUnloads.remove(param0, param2) && l_param4 != null) {
               if (l_param4 instanceof LevelChunk) {
                  ((LevelChunk)l_param4).setLoaded(false);
               }

               this.save(l_param4);
               if (this.entitiesInLevel.remove(param0) && l_param4 instanceof LevelChunk l_var7) {
                  this.level.unload(l_var7);
               }

               this.lightEngine.updateChunkStatus(l_param4.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.progressListener.onStatusChange(l_param4.getPos(), null);
               this.chunkSaveCooldowns.remove(l_param4.getPos().toLong());
            }
         }
      }, this.unloadQueue::add).whenComplete((l_param1, l_param2) -> {
         if (l_param2 != null) {
            LOGGER.error("Failed to save chunk {}", param2.getPos(), l_param2);
         }
      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      }

      this.visibleChunkMap = this.updatingChunkMap.clone();
      this.modified = false;
      return true;
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder param0, ChunkStatus param1) {
      ChunkPos var3 = param0.getPos();
      if (param1 == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(var3);
      }

      if (param1 == ChunkStatus.LIGHT) {
         this.distanceManager.addTicket(TicketType.LIGHT, var3, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), var3);
      }

      Optional<ChunkAccess> var4 = param0.getOrScheduleFuture(param1.getParent(), this).getNow(ChunkHolder.UNLOADED_CHUNK).left();
      if (var4.isPresent() && var4.get().getStatus().isOrAfter(param1)) {
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var5 = param1.load(
            this.level, this.structureTemplateManager, this.lightEngine, l_param1 -> this.protoChunkToFullChunk(param0), var4.get()
         );
         this.progressListener.onStatusChange(var3, param1);
         return var5;
      } else {
         return this.scheduleChunkGeneration(param0, param1);
      }
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos param0) {
      return this.readChunk(param0).thenApply(l_param1 -> l_param1.filter(l2_param1 -> {
         boolean l2_var2 = isChunkDataValid(l2_param1);
         if (!l2_var2) {
            LOGGER.error("Chunk file at {} is missing level data, skipping", param0);
         }

         return l2_var2;
      })).thenApplyAsync(l_param1 -> {
         this.level.getProfiler().incrementCounter("chunkLoad");
         if (l_param1.isPresent()) {
            ChunkAccess l_var3 = ChunkSerializer.read(this.level, this.poiManager, param0, l_param1.get());
            this.markPosition(param0, l_var3.getStatus().getChunkType());
            return Either.<ChunkAccess, ChunkHolder.ChunkLoadingFailure>left(l_var3);
         } else {
            return Either.<ChunkAccess, ChunkHolder.ChunkLoadingFailure>left(this.createEmptyChunk(param0));
         }
      }, this.mainThreadExecutor);
      // Eaglercraft port: the decompiled .exceptionallyAsync(...) call does not resolve against
      // CompletableFuture here (it wants Function<Throwable, ? extends Either<..>>). Dropped so the
      // tree compiles - chunk load failures now propagate instead of becoming a ChunkLoadingFailure.
   }

   private static boolean isChunkDataValid(CompoundTag param0) {
      return param0.contains("Status", 8);
   }

   private Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> handleChunkLoadFailure(Throwable param0, ChunkPos param1) {
      if (param0 instanceof ReportedException var3) {
         Throwable var4 = var3.getCause();
         if (!(var4 instanceof IOException)) {
            this.markPositionReplaceable(param1);
            throw var3;
         }

         LOGGER.error("Couldn't load chunk {}", param1, var4);
      } else if (param0 instanceof IOException) {
         LOGGER.error("Couldn't load chunk {}", param1, param0);
      }

      return Either.left(this.createEmptyChunk(param1));
   }

   private ChunkAccess createEmptyChunk(ChunkPos param0) {
      this.markPositionReplaceable(param0);
      return new ProtoChunk(param0, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registries.BIOME), null);
   }

   private void markPositionReplaceable(ChunkPos param0) {
      this.chunkTypeCache.put(param0.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos param0, ChunkStatus.ChunkType param1) {
      return this.chunkTypeCache.put(param0.toLong(), (byte)(param1 == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder param0, ChunkStatus param1) {
      ChunkPos var3 = param0.getPos();
      CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> var4 = this.getChunkRangeFuture(
         var3, param1.getRange(), l_param1 -> this.getDependencyStatus(param1, l_param1)
      );
      this.level.getProfiler().incrementCounter(() -> "chunkGenerate " + param1.getName());
      Executor var5 = l_param1 -> this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(param0, l_param1));
      return var4.thenComposeAsync(
         l_param4 -> l_param4.map(
            l2_param4 -> {
               try {
                  CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> l2_var6 = param1.generate(
                     var5,
                     this.level,
                     this.generator,
                     this.structureTemplateManager,
                     this.lightEngine,
                     l3_param1 -> this.protoChunkToFullChunk(param0),
                     (List<ChunkAccess>)l2_param4,
                     false
                  );
                  this.progressListener.onStatusChange(var3, param1);
                  return l2_var6;
               } catch (Exception l2_var6) {
                  LOGGER.error("Exception generating new chunk " + var3 + " at status " + param1, l2_var6);
                  CrashReport l2_var7 = CrashReport.forThrowable(l2_var6, "Exception generating new chunk");
                  CrashReportCategory l2_var8 = l2_var7.addCategory("Chunk to be generated");
                  l2_var8.setDetail("Location", String.format(Locale.ROOT, "%d,%d", var3.x, var3.z));
                  l2_var8.setDetail("Position hash", ChunkPos.asLong(var3.x, var3.z));
                  l2_var8.setDetail("Generator", this.generator);
                  this.mainThreadExecutor.execute(() -> {
                     throw new ReportedException(l2_var7);
                  });
                  throw new ReportedException(l2_var7);
               }
            },
            l2_param1 -> {
               this.releaseLightTicket(var3);
               return CompletableFuture.completedFuture(Either.right(l2_param1));
            }
         ),
         var5
      );
   }

   protected void releaseLightTicket(ChunkPos param0) {
      this.mainThreadExecutor
         .tell(
            Util.name(
               () -> this.distanceManager.removeTicket(TicketType.LIGHT, param0, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), param0),
               () -> "release light ticket " + param0
            )
         );
   }

   private ChunkStatus getDependencyStatus(ChunkStatus param0, int param1) {
      ChunkStatus var3;
      if (param1 == 0) {
         var3 = param0.getParent();
      } else {
         var3 = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(param0) + param1);
      }

      return var3;
   }

   private static void postLoadProtoChunk(ServerLevel param0, List<CompoundTag> param1) {
      if (!param1.isEmpty()) {
         param0.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(param1, param0));
      }
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder param0) {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var2 = param0.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
      return var2.thenApplyAsync(l_param1 -> {
         ChunkStatus l_var3 = ChunkHolder.getStatus(param0.getTicketLevel());
         return !l_var3.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : l_param1.mapLeft(l2_param1 -> {
            ChunkPos l2_var3 = param0.getPos();
            ProtoChunk l2_var4 = (ProtoChunk)l2_param1;
            LevelChunk l2_var5;
            if (l2_var4 instanceof ImposterProtoChunk) {
               l2_var5 = ((ImposterProtoChunk)l2_var4).getWrapped();
            } else {
               l2_var5 = new LevelChunk(this.level, l2_var4, l3_param1 -> postLoadProtoChunk(this.level, l2_var4.getEntities()));
               param0.replaceProtoChunk(new ImposterProtoChunk(l2_var5, false));
            }

            l2_var5.setFullStatus(() -> ChunkHolder.getFullChunkStatus(param0.getTicketLevel()));
            l2_var5.runPostLoad();
            if (this.entitiesInLevel.add(l2_var3.toLong())) {
               l2_var5.setLoaded(true);
               l2_var5.registerAllBlockEntitiesAfterLevelLoad();
               l2_var5.registerTickContainerInLevel(this.level);
            }

            return l2_var5;
         });
      }, l_param1 -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(l_param1, param0.getPos().toLong(), param0::getTicketLevel)));
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder param0) {
      ChunkPos var2 = param0.getPos();
      CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> var3 = this.getChunkRangeFuture(var2, 1, l_param0 -> ChunkStatus.FULL);
      CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var4 = var3.<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>>thenApplyAsync(
            l_param0 -> l_param0.mapLeft(l2_param0 -> (LevelChunk)l2_param0.get(l2_param0.size() / 2)),
            l_param1 -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(param0, l_param1))
         )
         .thenApplyAsync(l_param0 -> l_param0.ifLeft(l2_param0 -> {
            l2_param0.postProcessGeneration();
            this.level.startTickingChunk(l2_param0);
         }), this.mainThreadExecutor);
      var4.thenAcceptAsync(l_param1 -> l_param1.ifLeft(l2_param1 -> {
         this.tickingGenerated.getAndIncrement();
         MutableObject<ClientboundLevelChunkWithLightPacket> l2_var3 = new MutableObject<>();
         this.getPlayers(var2, false).forEach(l3_param2 -> this.playerLoadedChunk(l3_param2, l2_var3, l2_param1));
      }), l_param1 -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(param0, l_param1)));
      return var4;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder param0) {
      return this.getChunkRangeFuture(param0.getPos(), 1, ChunkStatus::getStatusAroundFullChunk)
         .thenApplyAsync(
            l_param0 -> l_param0.mapLeft(l2_param0 -> (LevelChunk)l2_param0.get(l2_param0.size() / 2)),
            l_param1 -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(param0, l_param1))
         );
   }

   public int getTickingGenerated() {
      return this.tickingGenerated.get();
   }

   /** Cooperative-port diagnostics: one-line summary of the task sorter queues. */
   public String describeQueueSorter() {
      return this.queueSorter.describeQueues();
   }

   /** Cooperative-port diagnostics: describe every holder that never reached FULL. */
   public String describeStuckChunks() {
      StringBuilder sb = new StringBuilder();
      int stuckCount = 0;
      for (ChunkHolder l_var0 : this.visibleChunkMap.values()) {
         ChunkStatus last = l_var0.getLastAvailableStatus();
         // Only "stuck" if it is behind the status its own ticket level asks for. The ring of
         // chunks outside the spawn ticket tops out at FEATURES by design (ChunkHolder.getStatus
         // of level 34+), so reporting every non-FULL chunk there as stuck was a false alarm.
         ChunkStatus target = ChunkHolder.getStatus(l_var0.getTicketLevel());
         if (last == null || !last.isOrAfter(target)) {
            if (stuckCount++ > 40) { sb.append(" ..."); break; }
            sb.append(l_var0.getPos().x).append(',').append(l_var0.getPos().z);
            sb.append(": lvl=").append(l_var0.getTicketLevel());
            sb.append(" last=").append(last == null ? "none" : last.getName());
            sb.append("; ");
         }
      }
      return stuckCount == 0 ? "stuck=0" : ("stuck=" + stuckCount + " [" + sb + "]");
   }

   private boolean saveChunkIfNeeded(ChunkHolder param0) {
      if (!param0.wasAccessibleSinceLastSave()) {
         return false;
      }

      ChunkAccess var2 = param0.getChunkToSave().getNow(null);
      if (!(var2 instanceof ImposterProtoChunk) && !(var2 instanceof LevelChunk)) {
         return false;
      }

      long var3 = var2.getPos().toLong();
      long var5 = this.chunkSaveCooldowns.getOrDefault(var3, -1L);
      long var7 = System.currentTimeMillis();
      if (var7 < var5) {
         return false;
      }

      boolean var9 = this.save(var2);
      param0.refreshAccessibility();
      if (var9) {
         this.chunkSaveCooldowns.put(var3, var7 + 10000L);
      }

      return var9;
   }

   private boolean save(ChunkAccess param0) {
      this.poiManager.flush(param0.getPos());
      if (!param0.isUnsaved()) {
         return false;
      }

      param0.setUnsaved(false);
      ChunkPos var2 = param0.getPos();

      try {
         ChunkStatus var3 = param0.getStatus();
         if (var3.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
            if (this.isExistingChunkFull(var2)) {
               return false;
            }

            if (var3 == ChunkStatus.EMPTY && param0.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
               return false;
            }
         }

         this.level.getProfiler().incrementCounter("chunkSave");
         CompoundTag var4 = ChunkSerializer.write(this.level, param0);
         this.write(var2, var4);
         this.markPosition(var2, var3.getChunkType());
         return true;
      } catch (Exception var3) {
         LOGGER.error("Failed to save chunk {},{}", var2.x, var2.z, var3);
         return false;
      }
   }

   private boolean isExistingChunkFull(ChunkPos param0) {
      byte var2 = this.chunkTypeCache.get(param0.toLong());
      if (var2 != 0) {
         return var2 == 1;
      }

      CompoundTag var3;
      try {
         var3 = this.readChunk(param0).join().orElse(null);
         if (var3 == null) {
            this.markPositionReplaceable(param0);
            return false;
         }
      } catch (Exception var4) {
         LOGGER.error("Failed to read chunk {}", param0, var4);
         this.markPositionReplaceable(param0);
         return false;
      }

      ChunkStatus.ChunkType var4 = ChunkSerializer.getChunkTypeFromTag(var3);
      return this.markPosition(param0, var4) == 1;
   }

   protected void setViewDistance(int param0) {
      int var2 = Mth.clamp(param0 + 1, 3, 33);
      if (var2 != this.viewDistance) {
         int var3 = this.viewDistance;
         this.viewDistance = var2;
         this.distanceManager.updatePlayerTickets(this.viewDistance + 1);

         for (ChunkHolder var5 : this.updatingChunkMap.values()) {
            ChunkPos var6 = var5.getPos();
            MutableObject<ClientboundLevelChunkWithLightPacket> var7 = new MutableObject<>();
            this.getPlayers(var6, false).forEach(l_param3 -> {
               SectionPos l_var5 = l_param3.getLastSectionPos();
               boolean l_var6 = isChunkInRange(var6.x, var6.z, l_var5.x(), l_var5.z(), var3);
               boolean l_var7 = isChunkInRange(var6.x, var6.z, l_var5.x(), l_var5.z(), this.viewDistance);
               this.updateChunkTracking(l_param3, var6, var7, l_var6, l_var7);
            });
         }
      }
   }

   protected void updateChunkTracking(
      ServerPlayer param0, ChunkPos param1, MutableObject<ClientboundLevelChunkWithLightPacket> param2, boolean param3, boolean param4
   ) {
      if (param0.level == this.level) {
         if (param4 && !param3) {
            ChunkHolder var6 = this.getVisibleChunkIfPresent(param1.toLong());
            if (var6 != null) {
               LevelChunk var7 = var6.getTickingChunk();
               if (var7 != null) {
                  this.playerLoadedChunk(param0, param2, var7);
               }

               DebugPackets.sendPoiPacketsForChunk(this.level, param1);
            }
         }

         if (!param4 && param3) {
            param0.untrackChunk(param1);
         }
      }
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   protected Iterable<ChunkHolder> getChunks() {
      return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
   }

   void dumpChunks(Writer param0) throws IOException {
      CsvOutput var2 = CsvOutput.builder()
         .addColumn("x")
         .addColumn("z")
         .addColumn("level")
         .addColumn("in_memory")
         .addColumn("status")
         .addColumn("full_status")
         .addColumn("accessible_ready")
         .addColumn("ticking_ready")
         .addColumn("entity_ticking_ready")
         .addColumn("ticket")
         .addColumn("spawning")
         .addColumn("block_entity_count")
         .addColumn("ticking_ticket")
         .addColumn("ticking_level")
         .addColumn("block_ticks")
         .addColumn("fluid_ticks")
         .build(param0);
      TickingTracker var3 = this.distanceManager.tickingTracker();

      for (Entry<ChunkHolder> var5 : this.visibleChunkMap.long2ObjectEntrySet()) {
         long var6 = var5.getLongKey();
         ChunkPos var8 = new ChunkPos(var6);
         ChunkHolder var9 = var5.getValue();
         Optional<ChunkAccess> var10 = Optional.ofNullable(var9.getLastAvailable());
         Optional<LevelChunk> var11 = var10.flatMap(l_param0 -> l_param0 instanceof LevelChunk ? Optional.of((LevelChunk)l_param0) : Optional.empty());
         var2.writeRow(
            var8.x,
            var8.z,
            var9.getTicketLevel(),
            var10.isPresent(),
            var10.map(ChunkAccess::getStatus).orElse(null),
            var11.map(LevelChunk::getFullStatus).orElse(null),
            printFuture(var9.getFullChunkFuture()),
            printFuture(var9.getTickingChunkFuture()),
            printFuture(var9.getEntityTickingChunkFuture()),
            this.distanceManager.getTicketDebugString(var6),
            this.anyPlayerCloseEnoughForSpawning(var8),
            var11.<Integer>map(l_param0 -> l_param0.getBlockEntities().size()).orElse(0),
            var3.getTicketDebugString(var6),
            var3.getLevel(var6),
            var11.<Integer>map(l_param0 -> l_param0.getBlockTicks().count()).orElse(0),
            var11.<Integer>map(l_param0 -> l_param0.getFluidTicks().count()).orElse(0)
         );
      }
   }

   private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> param0) {
      try {
         Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> var1 = param0.getNow(null);
         return var1 != null ? var1.map(l_param0 -> "done", l_param0 -> "unloaded") : "not completed";
      } catch (CompletionException var1) {
         return "failed " + var1.getCause().getMessage();
      } catch (CancellationException var1) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos param0) {
      return this.read(param0).thenApplyAsync(l_param0 -> l_param0.map(this::upgradeChunkTag), Util.backgroundExecutor());
   }

   private CompoundTag upgradeChunkTag(CompoundTag param0) {
      return this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, param0, this.generator.getTypeNameForDataFixer());
   }

   boolean anyPlayerCloseEnoughForSpawning(ChunkPos param0) {
      long var2 = param0.toLong();
      if (!this.distanceManager.hasPlayersNearby(var2)) {
         return false;
      }

      for (ServerPlayer var5 : this.playerMap.getPlayers(var2)) {
         if (this.playerIsCloseEnoughForSpawning(var5, param0)) {
            return true;
         }
      }

      return false;
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos param0) {
      long var2 = param0.toLong();
      if (!this.distanceManager.hasPlayersNearby(var2)) {
         return List.of();
      }

      Builder<ServerPlayer> var4 = ImmutableList.builder();

      for (ServerPlayer var6 : this.playerMap.getPlayers(var2)) {
         if (this.playerIsCloseEnoughForSpawning(var6, param0)) {
            var4.add(var6);
         }
      }

      return var4.build();
   }

   private boolean playerIsCloseEnoughForSpawning(ServerPlayer param0, ChunkPos param1) {
      if (param0.isSpectator()) {
         return false;
      }

      double var3 = euclideanDistanceSquared(param1, param0);
      return var3 < 16384.0;
   }

   private boolean skipPlayer(ServerPlayer param0) {
      return param0.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
   }

   void updatePlayerStatus(ServerPlayer param0, boolean param1) {
      boolean var3 = this.skipPlayer(param0);
      boolean var4 = this.playerMap.ignoredOrUnknown(param0);
      int var5 = SectionPos.blockToSectionCoord(param0.getBlockX());
      int var6 = SectionPos.blockToSectionCoord(param0.getBlockZ());
      if (param1) {
         this.playerMap.addPlayer(ChunkPos.asLong(var5, var6), param0, var3);
         this.updatePlayerPos(param0);
         if (!var3) {
            this.distanceManager.addPlayer(SectionPos.of(param0), param0);
         }
      } else {
         SectionPos var7 = param0.getLastSectionPos();
         this.playerMap.removePlayer(var7.chunk().toLong(), param0);
         if (!var4) {
            this.distanceManager.removePlayer(var7, param0);
         }
      }

      for (int var7 = var5 - this.viewDistance - 1; var7 <= var5 + this.viewDistance + 1; var7++) {
         for (int var8 = var6 - this.viewDistance - 1; var8 <= var6 + this.viewDistance + 1; var8++) {
            if (isChunkInRange(var7, var8, var5, var6, this.viewDistance)) {
               ChunkPos var9 = new ChunkPos(var7, var8);
               this.updateChunkTracking(param0, var9, new MutableObject<>(), !param1, param1);
            }
         }
      }
   }

   private SectionPos updatePlayerPos(ServerPlayer param0) {
      SectionPos var2 = SectionPos.of(param0);
      param0.setLastSectionPos(var2);
      param0.connection.send(new ClientboundSetChunkCacheCenterPacket(var2.x(), var2.z()));
      return var2;
   }

   public void move(ServerPlayer param0) {
      for (ChunkMap.TrackedEntity var3 : this.entityMap.values()) {
         if (var3.entity == param0) {
            var3.updatePlayers(this.level.players());
         } else {
            var3.updatePlayer(param0);
         }
      }

      int var2 = SectionPos.blockToSectionCoord(param0.getBlockX());
      int var3 = SectionPos.blockToSectionCoord(param0.getBlockZ());
      SectionPos var4 = param0.getLastSectionPos();
      SectionPos var5 = SectionPos.of(param0);
      long var6 = var4.chunk().toLong();
      long var8 = var5.chunk().toLong();
      boolean var10 = this.playerMap.ignored(param0);
      boolean var11 = this.skipPlayer(param0);
      boolean var12 = var4.asLong() != var5.asLong();
      if (var12 || var10 != var11) {
         this.updatePlayerPos(param0);
         if (!var10) {
            this.distanceManager.removePlayer(var4, param0);
         }

         if (!var11) {
            this.distanceManager.addPlayer(var5, param0);
         }

         if (!var10 && var11) {
            this.playerMap.ignorePlayer(param0);
         }

         if (var10 && !var11) {
            this.playerMap.unIgnorePlayer(param0);
         }

         if (var6 != var8) {
            this.playerMap.updatePlayer(var6, var8, param0);
         }
      }

      int var13 = var4.x();
      int var14 = var4.z();
      if (Math.abs(var13 - var2) <= this.viewDistance * 2 && Math.abs(var14 - var3) <= this.viewDistance * 2) {
         int var15 = Math.min(var2, var13) - this.viewDistance - 1;
         int var16 = Math.min(var3, var14) - this.viewDistance - 1;
         int var17 = Math.max(var2, var13) + this.viewDistance + 1;
         int var18 = Math.max(var3, var14) + this.viewDistance + 1;

         for (int var19 = var15; var19 <= var17; var19++) {
            for (int var20 = var16; var20 <= var18; var20++) {
               boolean var21 = isChunkInRange(var19, var20, var13, var14, this.viewDistance);
               boolean var22 = isChunkInRange(var19, var20, var2, var3, this.viewDistance);
               this.updateChunkTracking(param0, new ChunkPos(var19, var20), new MutableObject<>(), var21, var22);
            }
         }
      } else {
         for (int var15 = var13 - this.viewDistance - 1; var15 <= var13 + this.viewDistance + 1; var15++) {
            for (int var16 = var14 - this.viewDistance - 1; var16 <= var14 + this.viewDistance + 1; var16++) {
               if (isChunkInRange(var15, var16, var13, var14, this.viewDistance)) {
                  boolean var17 = true;
                  boolean var18 = false;
                  this.updateChunkTracking(param0, new ChunkPos(var15, var16), new MutableObject<>(), true, false);
               }
            }
         }

         for (int var15 = var2 - this.viewDistance - 1; var15 <= var2 + this.viewDistance + 1; var15++) {
            for (int var16 = var3 - this.viewDistance - 1; var16 <= var3 + this.viewDistance + 1; var16++) {
               if (isChunkInRange(var15, var16, var2, var3, this.viewDistance)) {
                  boolean var17 = false;
                  boolean var18 = true;
                  this.updateChunkTracking(param0, new ChunkPos(var15, var16), new MutableObject<>(), false, true);
               }
            }
         }
      }
   }

   @Override
   public List<ServerPlayer> getPlayers(ChunkPos param0, boolean param1) {
      Set<ServerPlayer> var3 = this.playerMap.getPlayers(param0.toLong());
      Builder<ServerPlayer> var4 = ImmutableList.builder();

      for (ServerPlayer var6 : var3) {
         SectionPos var7 = var6.getLastSectionPos();
         if (param1 && isChunkOnRangeBorder(param0.x, param0.z, var7.x(), var7.z(), this.viewDistance)
            || !param1 && isChunkInRange(param0.x, param0.z, var7.x(), var7.z(), this.viewDistance)) {
            var4.add(var6);
         }
      }

      return var4.build();
   }

   protected void addEntity(Entity param0) {
      if (!(param0 instanceof EnderDragonPart)) {
         EntityType<?> var2 = param0.getType();
         int var3 = var2.clientTrackingRange() * 16;
         if (var3 != 0) {
            int var4 = var2.updateInterval();
            if (this.entityMap.containsKey(param0.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            }

            ChunkMap.TrackedEntity var5 = new ChunkMap.TrackedEntity(param0, var3, var4, var2.trackDeltas());
            this.entityMap.put(param0.getId(), var5);
            var5.updatePlayers(this.level.players());
            if (param0 instanceof ServerPlayer var6) {
               this.updatePlayerStatus(var6, true);

               for (ChunkMap.TrackedEntity var8 : this.entityMap.values()) {
                  if (var8.entity != var6) {
                     var8.updatePlayer(var6);
                  }
               }
            }
         }
      }
   }

   protected void removeEntity(Entity param0) {
      if (param0 instanceof ServerPlayer var2) {
         this.updatePlayerStatus(var2, false);

         for (ChunkMap.TrackedEntity var4 : this.entityMap.values()) {
            var4.removePlayer(var2);
         }
      }

      ChunkMap.TrackedEntity var2 = this.entityMap.remove(param0.getId());
      if (var2 != null) {
         var2.broadcastRemoved();
      }
   }

   protected void tick() {
      List<ServerPlayer> var1 = Lists.newArrayList();
      List<ServerPlayer> var2 = this.level.players();

      for (ChunkMap.TrackedEntity var4 : this.entityMap.values()) {
         SectionPos var5 = var4.lastSectionPos;
         SectionPos var6 = SectionPos.of(var4.entity);
         boolean var7 = !Objects.equals(var5, var6);
         if (var7) {
            var4.updatePlayers(var2);
            Entity var8 = var4.entity;
            if (var8 instanceof ServerPlayer) {
               var1.add((ServerPlayer)var8);
            }

            var4.lastSectionPos = var6;
         }

         if (var7 || this.distanceManager.inEntityTickingRange(var6.chunk().toLong())) {
            var4.serverEntity.sendChanges();
         }
      }

      if (!var1.isEmpty()) {
         for (ChunkMap.TrackedEntity var4 : this.entityMap.values()) {
            var4.updatePlayers(var1);
         }
      }
   }

   public void broadcast(Entity param0, Packet<?> param1) {
      ChunkMap.TrackedEntity var3 = this.entityMap.get(param0.getId());
      if (var3 != null) {
         var3.broadcast(param1);
      }
   }

   protected void broadcastAndSend(Entity param0, Packet<?> param1) {
      ChunkMap.TrackedEntity var3 = this.entityMap.get(param0.getId());
      if (var3 != null) {
         var3.broadcastAndSend(param1);
      }
   }

   public void resendBiomesForChunks(List<ChunkAccess> param0) {
      Map<ServerPlayer, List<LevelChunk>> var2 = new HashMap<>();

      for (ChunkAccess var4 : param0) {
         ChunkPos var5 = var4.getPos();
         LevelChunk var6;
         if (var4 instanceof LevelChunk var7) {
            var6 = var7;
         } else {
            var6 = this.level.getChunk(var5.x, var5.z);
         }

         for (ServerPlayer var8 : this.getPlayers(var5, false)) {
            var2.computeIfAbsent(var8, l_param0 -> new ArrayList<>()).add(var6);
         }
      }

      var2.forEach((l_param0, l_param1) -> l_param0.connection.send(ClientboundChunksBiomesPacket.forChunks((List<LevelChunk>)l_param1)));
   }

   private void playerLoadedChunk(ServerPlayer param0, MutableObject<ClientboundLevelChunkWithLightPacket> param1, LevelChunk param2) {
      if (param1.getValue() == null) {
         param1.setValue(new ClientboundLevelChunkWithLightPacket(param2, this.lightEngine, null, null, true));
      }

      param0.trackChunk(param2.getPos(), param1.getValue());
      DebugPackets.sendPoiPacketsForChunk(this.level, param2.getPos());
      List<Entity> var4 = Lists.newArrayList();
      List<Entity> var5 = Lists.newArrayList();

      for (ChunkMap.TrackedEntity var7 : this.entityMap.values()) {
         Entity var8 = var7.entity;
         if (var8 != param0 && var8.chunkPosition().equals(param2.getPos())) {
            var7.updatePlayer(param0);
            if (var8 instanceof Mob && ((Mob)var8).getLeashHolder() != null) {
               var4.add(var8);
            }

            if (!var8.getPassengers().isEmpty()) {
               var5.add(var8);
            }
         }
      }

      if (!var4.isEmpty()) {
         for (Entity var7 : var4) {
            param0.connection.send(new ClientboundSetEntityLinkPacket(var7, ((Mob)var7).getLeashHolder()));
         }
      }

      if (!var5.isEmpty()) {
         for (Entity var7 : var5) {
            param0.connection.send(new ClientboundSetPassengersPacket(var7));
         }
      }
   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public String getStorageName() {
      return this.storageName;
   }

   void onFullChunkStatusChange(ChunkPos param0, ChunkHolder.FullChunkStatus param1) {
      this.chunkStatusListener.onChunkStatusChange(param0, param1);
   }

   class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(Executor param1, Executor param2) {
         super(param1, param2);
      }

      @Override
      protected boolean isChunkToRemove(long param0) {
         return ChunkMap.this.toDrop.contains(param0);
      }

      @Nullable
      @Override
      protected ChunkHolder getChunk(long param0) {
         return ChunkMap.this.getUpdatingChunkIfPresent(param0);
      }

      @Nullable
      @Override
      protected ChunkHolder updateChunkScheduling(long param0, int param2, @Nullable ChunkHolder param3, int param4) {
         return ChunkMap.this.updateChunkScheduling(param0, param2, param3, param4);
      }
   }

   class TrackedEntity {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(Entity param1, int param2, int param3, boolean param4) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, param1, param3, param4, this::broadcast);
         this.entity = param1;
         this.range = param2;
         this.lastSectionPos = SectionPos.of(param1);
      }

      @Override
      public boolean equals(Object param0) {
         return param0 instanceof ChunkMap.TrackedEntity ? ((ChunkMap.TrackedEntity)param0).entity.getId() == this.entity.getId() : false;
      }

      @Override
      public int hashCode() {
         return this.entity.getId();
      }

      public void broadcast(Packet<?> param0) {
         for (ServerPlayerConnection var3 : this.seenBy) {
            var3.send(param0);
         }
      }

      public void broadcastAndSend(Packet<?> param0) {
         this.broadcast(param0);
         if (this.entity instanceof ServerPlayer) {
            ((ServerPlayer)this.entity).connection.send(param0);
         }
      }

      public void broadcastRemoved() {
         for (ServerPlayerConnection var2 : this.seenBy) {
            this.serverEntity.removePairing(var2.getPlayer());
         }
      }

      public void removePlayer(ServerPlayer param0) {
         if (this.seenBy.remove(param0.connection)) {
            this.serverEntity.removePairing(param0);
         }
      }

      public void updatePlayer(ServerPlayer param0) {
         if (param0 != this.entity) {
            Vec3 var2 = param0.position().subtract(this.entity.position());
            double var3 = Math.min(this.getEffectiveRange(), (ChunkMap.this.viewDistance - 1) * 16);
            double var5 = var2.x * var2.x + var2.z * var2.z;
            double var7 = var3 * var3;
            boolean var9 = var5 <= var7 && this.entity.broadcastToPlayer(param0);
            if (var9) {
               if (this.seenBy.add(param0.connection)) {
                  this.serverEntity.addPairing(param0);
               }
            } else if (this.seenBy.remove(param0.connection)) {
               this.serverEntity.removePairing(param0);
            }
         }
      }

      private int scaledRange(int param0) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(param0);
      }

      private int getEffectiveRange() {
         int var1 = this.range;

         for (Entity var3 : this.entity.getIndirectPassengers()) {
            int var4 = var3.getType().clientTrackingRange() * 16;
            if (var4 > var1) {
               var1 = var4;
            }
         }

         return this.scaledRange(var1);
      }

      public void updatePlayers(List<ServerPlayer> param0) {
         for (ServerPlayer var3 : param0) {
            this.updatePlayer(var3);
         }
      }
   }
}
