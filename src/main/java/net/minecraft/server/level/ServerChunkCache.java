package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;

public class ServerChunkCache extends ChunkSource {
   private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
   private final DistanceManager distanceManager;
   final ServerLevel level;
   final Thread mainThread;
   final ThreadedLevelLightEngine lightEngine;
   private final ServerChunkCache.MainThreadExecutor mainThreadProcessor;
   public final ChunkMap chunkMap;
   private final DimensionDataStorage dataStorage;
   private long lastInhabitedUpdate;
   private boolean spawnEnemies = true;
   private boolean spawnFriendlies = true;
   private static final int CACHE_SIZE = 4;
   private final long[] lastChunkPos = new long[4];
   private final ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
   private final ChunkAccess[] lastChunk = new ChunkAccess[4];
   @Nullable
   @VisibleForDebug
   private NaturalSpawner.SpawnState lastSpawnState;

   public ServerChunkCache(
      ServerLevel param0,
      LevelStorageSource.LevelStorageAccess param1,
      DataFixer param2,
      StructureTemplateManager param3,
      Executor param4,
      ChunkGenerator param5,
      int param6,
      int param7,
      boolean param8,
      ChunkProgressListener param9,
      ChunkStatusUpdateListener param10,
      Supplier<DimensionDataStorage> param11
   ) {
      this.level = param0;
      this.mainThreadProcessor = new ServerChunkCache.MainThreadExecutor(param0);
      this.mainThread = Thread.currentThread();
      File var13 = param1.getDimensionPath(param0.dimension()).resolve("data").toFile();
      var13.mkdirs();
      this.dataStorage = new DimensionDataStorage(var13, param2);
      this.chunkMap = new ChunkMap(param0, param1, param2, param3, param4, this.mainThreadProcessor, this, param5, param9, param10, param11, param6, param8);
      this.lightEngine = this.chunkMap.getLightEngine();
      this.distanceManager = this.chunkMap.getDistanceManager();
      this.distanceManager.updateSimulationDistance(param7);
      this.clearCache();
   }

   public ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   private ChunkHolder getVisibleChunkIfPresent(long param0) {
      return this.chunkMap.getVisibleChunkIfPresent(param0);
   }

   public int getTickingGenerated() {
      return this.chunkMap.getTickingGenerated();
   }

   private void storeInCache(long param0, ChunkAccess param2, ChunkStatus param3) {
      for (int var5 = 3; var5 > 0; var5--) {
         this.lastChunkPos[var5] = this.lastChunkPos[var5 - 1];
         this.lastChunkStatus[var5] = this.lastChunkStatus[var5 - 1];
         this.lastChunk[var5] = this.lastChunk[var5 - 1];
      }

      this.lastChunkPos[0] = param0;
      this.lastChunkStatus[0] = param3;
      this.lastChunk[0] = param2;
   }

   @Nullable
   @Override
   public ChunkAccess getChunk(int param0, int param1, ChunkStatus param2, boolean param3) {
      if (Thread.currentThread() != this.mainThread) {
         return CompletableFuture.<ChunkAccess>supplyAsync(() -> this.getChunk(param0, param1, param2, param3), this.mainThreadProcessor).join();
      }

      ProfilerFiller var5 = this.level.getProfiler();
      var5.incrementCounter("getChunk");
      long var6 = ChunkPos.asLong(param0, param1);

      for (int var8 = 0; var8 < 4; var8++) {
         if (var6 == this.lastChunkPos[var8] && param2 == this.lastChunkStatus[var8]) {
            ChunkAccess var9 = this.lastChunk[var8];
            if (var9 != null || !param3) {
               return var9;
            }
         }
      }

      var5.incrementCounter("getChunkCacheMiss");
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var8 = this.getChunkFutureMainThread(param0, param1, param2, param3);
      this.mainThreadProcessor.managedBlock(var8::isDone);
      ChunkAccess var9 = var8.join().map(l_param0 -> (ChunkAccess)l_param0, l_param1 -> {
         if (param3) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + l_param1));
         } else {
            return null;
         }
      });
      this.storeInCache(var6, var9, param2);
      return var9;
   }

   @Nullable
   @Override
   public LevelChunk getChunkNow(int param0, int param1) {
      if (Thread.currentThread() != this.mainThread) {
         return null;
      }

      this.level.getProfiler().incrementCounter("getChunkNow");
      long var3 = ChunkPos.asLong(param0, param1);

      for (int var5 = 0; var5 < 4; var5++) {
         if (var3 == this.lastChunkPos[var5] && this.lastChunkStatus[var5] == ChunkStatus.FULL) {
            ChunkAccess var6 = this.lastChunk[var5];
            return var6 instanceof LevelChunk ? (LevelChunk)var6 : null;
         }
      }

      ChunkHolder var5 = this.getVisibleChunkIfPresent(var3);
      if (var5 == null) {
         return null;
      }

      Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> var6 = var5.getFutureIfPresent(ChunkStatus.FULL).getNow(null);
      if (var6 == null) {
         return null;
      }

      ChunkAccess var7 = var6.left().orElse(null);
      if (var7 != null) {
         this.storeInCache(var3, var7, ChunkStatus.FULL);
         if (var7 instanceof LevelChunk) {
            return (LevelChunk)var7;
         }
      }

      return null;
   }

   private void clearCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunkStatus, null);
      Arrays.fill(this.lastChunk, null);
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFuture(int param0, int param1, ChunkStatus param2, boolean param3) {
      boolean var5 = Thread.currentThread() == this.mainThread;
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var6;
      if (var5) {
         var6 = this.getChunkFutureMainThread(param0, param1, param2, param3);
         this.mainThreadProcessor.managedBlock(var6::isDone);
      } else {
         var6 = CompletableFuture.<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>supplyAsync(
               () -> this.getChunkFutureMainThread(param0, param1, param2, param3), this.mainThreadProcessor
            )
            .thenCompose(l_param0 -> (CompletionStage<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>)l_param0);
      }

      return var6;
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFutureMainThread(
      int param0, int param1, ChunkStatus param2, boolean param3
   ) {
      ChunkPos var5 = new ChunkPos(param0, param1);
      long var6 = var5.toLong();
      int var8 = 33 + ChunkStatus.getDistance(param2);
      ChunkHolder var9 = this.getVisibleChunkIfPresent(var6);
      if (param3) {
         this.distanceManager.addTicket(TicketType.UNKNOWN, var5, var8, var5);
         if (this.chunkAbsent(var9, var8)) {
            ProfilerFiller var10 = this.level.getProfiler();
            var10.push("chunkLoad");
            this.runDistanceManagerUpdates();
            var9 = this.getVisibleChunkIfPresent(var6);
            var10.pop();
            if (this.chunkAbsent(var9, var8)) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
            }
         }
      }

      return this.chunkAbsent(var9, var8) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : var9.getOrScheduleFuture(param2, this.chunkMap);
   }

   private boolean chunkAbsent(@Nullable ChunkHolder param0, int param1) {
      return param0 == null || param0.getTicketLevel() > param1;
   }

   @Override
   public boolean hasChunk(int param0, int param1) {
      ChunkHolder var3 = this.getVisibleChunkIfPresent(new ChunkPos(param0, param1).toLong());
      int var4 = 33 + ChunkStatus.getDistance(ChunkStatus.FULL);
      return !this.chunkAbsent(var3, var4);
   }

   @Override
   public BlockGetter getChunkForLighting(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);
      ChunkHolder var5 = this.getVisibleChunkIfPresent(var3);
      if (var5 == null) {
         return null;
      }

      int var6 = CHUNK_STATUSES.size() - 1;

      while (true) {
         ChunkStatus var7 = CHUNK_STATUSES.get(var6);
         Optional<ChunkAccess> var8 = var5.getFutureIfPresentUnchecked(var7).getNow(ChunkHolder.UNLOADED_CHUNK).left();
         if (var8.isPresent()) {
            return var8.get();
         }

         if (var7 == ChunkStatus.LIGHT.getParent()) {
            return null;
         }

         var6--;
      }
   }

   public Level getLevel() {
      return this.level;
   }

   public boolean pollTask() {
      return this.mainThreadProcessor.pollTask();
   }

   boolean runDistanceManagerUpdates() {
      boolean var1 = this.distanceManager.runAllUpdates(this.chunkMap);
      boolean var2 = this.chunkMap.promoteChunkMap();
      if (!var1 && !var2) {
         return false;
      }

      this.clearCache();
      return true;
   }

   public boolean isPositionTicking(long param0) {
      ChunkHolder var3 = this.getVisibleChunkIfPresent(param0);
      if (var3 == null) {
         return false;
      }

      if (!this.level.shouldTickBlocksAt(param0)) {
         return false;
      }

      Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> var4 = var3.getTickingChunkFuture().getNow(null);
      return var4 != null && var4.left().isPresent();
   }

   public void save(boolean param0) {
      this.runDistanceManagerUpdates();
      this.chunkMap.saveAllChunks(param0);
   }

   @Override
   public void close() throws IOException {
      this.save(true);
      this.lightEngine.close();
      this.chunkMap.close();
   }

   @Override
   public void tick(BooleanSupplier param0, boolean param1) {
      this.level.getProfiler().push("purge");
      this.distanceManager.purgeStaleTickets();
      this.runDistanceManagerUpdates();
      this.level.getProfiler().popPush("chunks");
      if (param1) {
         this.tickChunks();
      }

      this.level.getProfiler().popPush("unload");
      this.chunkMap.tick(param0);
      this.level.getProfiler().pop();
      this.clearCache();
   }

   private void tickChunks() {
      long var1 = this.level.getGameTime();
      long var3 = var1 - this.lastInhabitedUpdate;
      this.lastInhabitedUpdate = var1;
      boolean var5 = this.level.isDebug();
      if (var5) {
         this.chunkMap.tick();
      } else {
         LevelData var6 = this.level.getLevelData();
         ProfilerFiller var7 = this.level.getProfiler();
         var7.push("pollingChunks");
         int var8 = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
         boolean var9 = var6.getGameTime() % 400L == 0L;
         var7.push("naturalSpawnCount");
         int var10 = this.distanceManager.getNaturalSpawnChunkCount();
         NaturalSpawner.SpawnState var11 = NaturalSpawner.createState(
            var10, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap)
         );
         this.lastSpawnState = var11;
         var7.popPush("filteringLoadedChunks");
         List<ServerChunkCache.ChunkAndHolder> var12 = Lists.newArrayListWithCapacity(var10);

         for (ChunkHolder var14 : this.chunkMap.getChunks()) {
            LevelChunk var15 = var14.getTickingChunk();
            if (var15 != null) {
               var12.add(new ServerChunkCache.ChunkAndHolder(var15, var14));
            }
         }

         var7.popPush("spawnAndTick");
         boolean var13 = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
         Collections.shuffle(var12);

         for (ServerChunkCache.ChunkAndHolder var15 : var12) {
            LevelChunk var16 = var15.chunk;
            ChunkPos var17 = var16.getPos();
            if (this.level.isNaturalSpawningAllowed(var17) && this.chunkMap.anyPlayerCloseEnoughForSpawning(var17)) {
               var16.incrementInhabitedTime(var3);
               if (var13 && (this.spawnEnemies || this.spawnFriendlies) && this.level.getWorldBorder().isWithinBounds(var17)) {
                  NaturalSpawner.spawnForChunk(this.level, var16, var11, this.spawnFriendlies, this.spawnEnemies, var9);
               }

               if (this.level.shouldTickBlocksAt(var17.toLong())) {
                  this.level.tickChunk(var16, var8);
               }
            }
         }

         var7.popPush("customSpawners");
         if (var13) {
            this.level.tickCustomSpawners(this.spawnEnemies, this.spawnFriendlies);
         }

         var7.popPush("broadcast");
         var12.forEach(l_param0 -> l_param0.holder.broadcastChanges(l_param0.chunk));
         var7.pop();
         var7.pop();
         this.chunkMap.tick();
      }
   }

   private void getFullChunk(long param0, Consumer<LevelChunk> param2) {
      ChunkHolder var4 = this.getVisibleChunkIfPresent(param0);
      if (var4 != null) {
         var4.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().ifPresent(param2);
      }
   }

   @Override
   public String gatherStats() {
      return Integer.toString(this.getLoadedChunksCount());
   }

   @VisibleForTesting
   public int getPendingTasksCount() {
      return this.mainThreadProcessor.getPendingTasksCount();
   }

   public ChunkGenerator getGenerator() {
      return this.chunkMap.generator();
   }

   public ChunkGeneratorStructureState getGeneratorState() {
      return this.chunkMap.generatorState();
   }

   public RandomState randomState() {
      return this.chunkMap.randomState();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.chunkMap.size();
   }

   public void blockChanged(BlockPos param0) {
      int var2 = SectionPos.blockToSectionCoord(param0.getX());
      int var3 = SectionPos.blockToSectionCoord(param0.getZ());
      ChunkHolder var4 = this.getVisibleChunkIfPresent(ChunkPos.asLong(var2, var3));
      if (var4 != null) {
         var4.blockChanged(param0);
      }
   }

   @Override
   public void onLightUpdate(LightLayer param0, SectionPos param1) {
      this.mainThreadProcessor.execute(() -> {
         ChunkHolder l_var3 = this.getVisibleChunkIfPresent(param1.chunk().toLong());
         if (l_var3 != null) {
            l_var3.sectionLightChanged(param0, param1.y());
         }
      });
   }

   public <T> void addRegionTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      this.distanceManager.addRegionTicket(param0, param1, param2, param3);
   }

   public <T> void removeRegionTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      this.distanceManager.removeRegionTicket(param0, param1, param2, param3);
   }

   @Override
   public void updateChunkForced(ChunkPos param0, boolean param1) {
      this.distanceManager.updateChunkForced(param0, param1);
   }

   public void move(ServerPlayer param0) {
      if (!param0.isRemoved()) {
         this.chunkMap.move(param0);
      }
   }

   public void removeEntity(Entity param0) {
      this.chunkMap.removeEntity(param0);
   }

   public void addEntity(Entity param0) {
      this.chunkMap.addEntity(param0);
   }

   public void broadcastAndSend(Entity param0, Packet<?> param1) {
      this.chunkMap.broadcastAndSend(param0, param1);
   }

   public void broadcast(Entity param0, Packet<?> param1) {
      this.chunkMap.broadcast(param0, param1);
   }

   public void setViewDistance(int param0) {
      this.chunkMap.setViewDistance(param0);
   }

   public void setSimulationDistance(int param0) {
      this.distanceManager.updateSimulationDistance(param0);
   }

   @Override
   public void setSpawnSettings(boolean param0, boolean param1) {
      this.spawnEnemies = param0;
      this.spawnFriendlies = param1;
   }

   public String getChunkDebugData(ChunkPos param0) {
      return this.chunkMap.getChunkDebugData(param0);
   }

   public DimensionDataStorage getDataStorage() {
      return this.dataStorage;
   }

   public PoiManager getPoiManager() {
      return this.chunkMap.getPoiManager();
   }

   public ChunkScanAccess chunkScanner() {
      return this.chunkMap.chunkScanner();
   }

   @Nullable
   @VisibleForDebug
   public NaturalSpawner.SpawnState getLastSpawnState() {
      return this.lastSpawnState;
   }

   public void removeTicketsOnClosing() {
      this.distanceManager.removeTicketsOnClosing();
   }

   record ChunkAndHolder(LevelChunk chunk, ChunkHolder holder) {
   }

   final class MainThreadExecutor extends BlockableEventLoop<Runnable> {
      MainThreadExecutor(Level param1) {
         super("Chunk source main thread executor for " + param1.dimension().location());
      }

      @Override
      protected Runnable wrapRunnable(Runnable param0) {
         return param0;
      }

      @Override
      protected boolean shouldRun(Runnable param0) {
         return true;
      }

      @Override
      protected boolean scheduleExecutables() {
         return true;
      }

      @Override
      protected Thread getRunningThread() {
         return ServerChunkCache.this.mainThread;
      }

      @Override
      protected void doRunTask(Runnable param0) {
         ServerChunkCache.this.level.getProfiler().incrementCounter("runTask");
         super.doRunTask(param0);
      }

      @Override
      public boolean pollTask() {
         if (ServerChunkCache.this.runDistanceManagerUpdates()) {
            return true;
         }

         ServerChunkCache.this.lightEngine.tryScheduleUpdate();
         return super.pollTask();
      }
   }
}
