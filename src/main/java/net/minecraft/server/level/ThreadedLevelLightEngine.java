package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

public class ThreadedLevelLightEngine extends LevelLightEngine implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ProcessorMailbox<Runnable> taskMailbox;
   private final ObjectList<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> lightTasks = new ObjectArrayList<>();
   private final ChunkMap chunkMap;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> sorterMailbox;
   private volatile int taskPerBatch = 5;
   private final AtomicBoolean scheduled = new AtomicBoolean();
   // Starlight-inspired coalescing (Phase C, cooperative single-threaded port): during the
   // FEATURES stage, every qualifying block write enqueued its own mailbox task through the
   // sorter (Pair + double lambda + Util.name wrapper + queue submit + Unit ack round trip)
   // just to deliver one position. The queued sections are re-swept wholesale on the next
   // super.runUpdates call anyway, so positions for the same light section are folded into
   // a FIFO and flushed by ONE POST_UPDATE task per burst, exactly like vanilla checkBlock
   // semantics but without the per-write task overhead.
   private final Long2ObjectMap<LongArrayFIFOQueue> pendingLightChecks = new Long2ObjectOpenHashMap<>();
   private final LongSet flushingLightCheckSections = new LongOpenHashSet();

   public ThreadedLevelLightEngine(
      LightChunkGetter param0,
      ChunkMap param1,
      boolean param2,
      ProcessorMailbox<Runnable> param3,
      ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> param4
   ) {
      super(param0, true, param2);
      this.chunkMap = param1;
      this.sorterMailbox = param4;
      this.taskMailbox = param3;
   }

   @Override
   public void close() {
   }

   @Override
   public int runUpdates(int param0, boolean param1, boolean param2) {
      throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
   }

   @Override
   public void onBlockEmissionIncrease(BlockPos param0, int param1) {
      throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
   }

   @Override
   public void checkBlock(BlockPos param0) {
      int var2 = SectionPos.blockToSectionCoord(param0.getX());
      int var3 = param0.getY() >> 4;
      int var4 = SectionPos.blockToSectionCoord(param0.getZ());
      long var5 = SectionPos.asLong(var2, var3, var4);
      LongArrayFIFOQueue var6 = this.pendingLightChecks.get(var5);
      if (var6 == null) {
         var6 = new LongArrayFIFOQueue(16);
         this.pendingLightChecks.put(var5, var6);
      }

      var6.enqueue(param0.asLong());
      if (this.flushingLightCheckSections.add(var5)) {
         this.addTask(
            var2,
            var4,
            ThreadedLevelLightEngine.TaskType.POST_UPDATE,
            Util.name(() -> this.flushLightChecks(var5), () -> "checkBlocks " + new ChunkPos(var2, var4))
         );
      }
   }

   private void flushLightChecks(long param0) {
      this.flushingLightCheckSections.remove(param0);
      LongArrayFIFOQueue var3 = this.pendingLightChecks.remove(param0);
      if (var3 != null) {
         while (!var3.isEmpty()) {
            super.checkBlock(BlockPos.of(var3.dequeueLong()));
         }
      }
   }

   protected void updateChunkStatus(ChunkPos param0) {
      this.addTask(param0.x, param0.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
         super.retainData(param0, false);
         super.enableLightSources(param0, false);

         for (int l_var2 = this.getMinLightSection(); l_var2 < this.getMaxLightSection(); l_var2++) {
            super.queueSectionData(LightLayer.BLOCK, SectionPos.of(param0, l_var2), null, true);
            super.queueSectionData(LightLayer.SKY, SectionPos.of(param0, l_var2), null, true);
         }

         for (int l_var2 = this.levelHeightAccessor.getMinSection(); l_var2 < this.levelHeightAccessor.getMaxSection(); l_var2++) {
            super.updateSectionStatus(SectionPos.of(param0, l_var2), true);
         }
      }, () -> "updateChunkStatus " + param0 + " true"));
   }

   @Override
   public void updateSectionStatus(SectionPos param0, boolean param1) {
      this.addTask(
         param0.x(),
         param0.z(),
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.updateSectionStatus(param0, param1), () -> "updateSectionStatus " + param0 + " " + param1)
      );
   }

   @Override
   public void enableLightSources(ChunkPos param0, boolean param1) {
      this.addTask(
         param0.x,
         param0.z,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.enableLightSources(param0, param1), () -> "enableLight " + param0 + " " + param1)
      );
   }

   @Override
   public void queueSectionData(LightLayer param0, SectionPos param1, @Nullable DataLayer param2, boolean param3) {
      this.addTask(
         param1.x(),
         param1.z(),
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.queueSectionData(param0, param1, param2, param3), () -> "queueData " + param1)
      );
   }

   private void addTask(int param0, int param1, ThreadedLevelLightEngine.TaskType param2, Runnable param3) {
      this.addTask(param0, param1, this.chunkMap.getChunkQueueLevel(ChunkPos.asLong(param0, param1)), param2, param3);
   }

   private void addTask(int param0, int param1, IntSupplier param2, ThreadedLevelLightEngine.TaskType param3, Runnable param4) {
      this.sorterMailbox.tell(ChunkTaskPriorityQueueSorter.message(() -> {
         this.lightTasks.add(Pair.of(param3, param4));
         if (this.lightTasks.size() >= this.taskPerBatch) {
            this.runUpdate();
         }
      }, ChunkPos.asLong(param0, param1), param2));
   }

   @Override
   public void retainData(ChunkPos param0, boolean param1) {
      this.addTask(
         param0.x,
         param0.z,
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.retainData(param0, param1), () -> "retainData " + param0)
      );
   }

   public CompletableFuture<ChunkAccess> retainData(ChunkAccess param0) {
      ChunkPos var2 = param0.getPos();
      return CompletableFuture.supplyAsync(Util.name(() -> {
         super.retainData(var2, true);
         return param0;
      }, () -> "retainData: " + var2), l_param1 -> this.addTask(var2.x, var2.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, l_param1));
   }

   public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess param0, boolean param1) {
      ChunkPos var3 = param0.getPos();
      param0.setLightCorrect(false);
      this.addTask(var3.x, var3.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
         LevelChunkSection[] l_var4 = param0.getSections();

         for (int l_var5 = 0; l_var5 < param0.getSectionsCount(); l_var5++) {
            LevelChunkSection l_var6 = l_var4[l_var5];
            if (!l_var6.hasOnlyAir()) {
               int l_var7 = this.levelHeightAccessor.getSectionYFromSectionIndex(l_var5);
               super.updateSectionStatus(SectionPos.of(var3, l_var7), false);
            }
         }

         super.enableLightSources(var3, true);
         if (!param1) {
            param0.getLights().forEach(l2_param1 -> super.onBlockEmissionIncrease(l2_param1, param0.getLightEmission(l2_param1)));
         }
      }, () -> "lightChunk " + var3 + " " + param1));
      return CompletableFuture.supplyAsync(() -> {
         param0.setLightCorrect(true);
         super.retainData(var3, false);
         this.chunkMap.releaseLightTicket(var3);
         return param0;
      }, l_param1 -> this.addTask(var3.x, var3.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, l_param1));
   }

   public void tryScheduleUpdate() {
      if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
         this.taskMailbox.tell(() -> {
            this.runUpdate();
            this.scheduled.set(false);
         });
      }
   }

   private void runUpdate() {
      // ============ Starlight instrumentation (Phase C, step 1: measure first) =
      // Nanosecond bill of every light batch, split into task runnables
      // (PRE/POST_UPDATE) and the actual graph BFS (super.runUpdates), logged
      // per interval of light *activity* (idle wall time is skipped, so the
      // number read directly as "light engine cost while generating"). If the
      // BFS share is small after this lands, a full Starlight port is off.
      long l_nanos_start = System.nanoTime();
      int var1 = Math.min(this.lightTasks.size(), this.taskPerBatch);
      ObjectListIterator<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> var2 = this.lightTasks.iterator();

      int var3;
      for (var3 = 0; var2.hasNext() && var3 < var1; var3++) {
         Pair<ThreadedLevelLightEngine.TaskType, Runnable> var4 = var2.next();
         if (var4.getFirst() == ThreadedLevelLightEngine.TaskType.PRE_UPDATE) {
            var4.getSecond().run();
         }
      }

      var2.back(var3);
      long l_nanos_bfs = System.nanoTime();
      super.runUpdates(Integer.MAX_VALUE, true, true);
      long l_nanos_afterbfs = System.nanoTime();

      for (int var5 = 0; var2.hasNext() && var5 < var1; var5++) {
         Pair<ThreadedLevelLightEngine.TaskType, Runnable> var4 = var2.next();
         if (var4.getFirst() == ThreadedLevelLightEngine.TaskType.POST_UPDATE) {
            var4.getSecond().run();
         }

         var2.remove();
      }

      long l_nanos_end = System.nanoTime();
      lightDiagBatches++;
      lightDiagBatchNanos += l_nanos_end - l_nanos_start;
      lightDiagBfsNanos += l_nanos_afterbfs - l_nanos_bfs;
      lightDiagReport();
   }

   private static long lightDiagBatchNanos = 0L;
   private static long lightDiagBfsNanos = 0L;
   private static int lightDiagBatches = 0;
   private static long lightDiagWindowStart = System.nanoTime();

   private static void lightDiagReport() {
      long l_now = System.nanoTime();
      if (l_now - lightDiagWindowStart >= 5000000000L) {
         if (lightDiagBatches > 0) {
            net.lax1dude.eaglercraft.v1_8.log4j.LogManager.getLogger("Diag").info("{}",
               "LightEngine: " + lightDiagBatches + " batches in last " + (l_now - lightDiagWindowStart) / 1000000
                  + " ms wall: tasks=" + (lightDiagBatchNanos - lightDiagBfsNanos) / 1000000
                  + " ms, BFS=" + lightDiagBfsNanos / 1000000 + " ms" + " (total="
                  + lightDiagBatchNanos / 1000000 + " ms)");
         }
         lightDiagWindowStart = l_now;
         lightDiagBatchNanos = 0L;
         lightDiagBfsNanos = 0L;
         lightDiagBatches = 0;
      }
   }
   // ===================== end Starlight instrumentation =====================

   public void setTaskPerBatch(int param0) {
      this.taskPerBatch = param0;
   }

   enum TaskType {
      PRE_UPDATE,
      POST_UPDATE;
   }
}
