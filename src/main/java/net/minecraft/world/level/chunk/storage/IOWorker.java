package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicBoolean shutdownRequested = new AtomicBoolean();
   private final ProcessorMailbox<StrictQueue.IntRunnable> mailbox;
   private final RegionFileStorage storage;
   private final Map<ChunkPos, IOWorker.PendingStore> pendingWrites = Maps.newLinkedHashMap();
   private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> regionCacheForBlender = new Long2ObjectLinkedOpenHashMap<>();
   private static final int REGION_CACHE_SIZE = 1024;

   protected IOWorker(Path param0, boolean param1, String param2) {
      this.storage = new RegionFileStorage(param0, param1);
      this.mailbox = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(IOWorker.Priority.values().length), Util.ioPool(), "IOWorker-" + param2);
   }

   public boolean isOldChunkAround(ChunkPos param0, int param1) {
      ChunkPos var3 = new ChunkPos(param0.x - param1, param0.z - param1);
      ChunkPos var4 = new ChunkPos(param0.x + param1, param0.z + param1);

      for (int var5 = var3.getRegionX(); var5 <= var4.getRegionX(); var5++) {
         for (int var6 = var3.getRegionZ(); var6 <= var4.getRegionZ(); var6++) {
            BitSet var7 = this.getOrCreateOldDataForRegion(var5, var6).join();
            if (!var7.isEmpty()) {
               ChunkPos var8 = ChunkPos.minFromRegion(var5, var6);
               int var9 = Math.max(var3.x - var8.x, 0);
               int var10 = Math.max(var3.z - var8.z, 0);
               int var11 = Math.min(var4.x - var8.x, 31);
               int var12 = Math.min(var4.z - var8.z, 31);

               for (int var13 = var9; var13 <= var11; var13++) {
                  for (int var14 = var10; var14 <= var12; var14++) {
                     int var15 = var14 * 32 + var13;
                     if (var7.get(var15)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private CompletableFuture<BitSet> getOrCreateOldDataForRegion(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);
      synchronized (this.regionCacheForBlender) {
         CompletableFuture<BitSet> var6 = this.regionCacheForBlender.getAndMoveToFirst(var3);
         if (var6 == null) {
            var6 = this.createOldDataForRegion(param0, param1);
            this.regionCacheForBlender.putAndMoveToFirst(var3, var6);
            if (this.regionCacheForBlender.size() > 1024) {
               this.regionCacheForBlender.removeLast();
            }
         }

         return var6;
      }
   }

   private CompletableFuture<BitSet> createOldDataForRegion(int param0, int param1) {
      return CompletableFuture.supplyAsync(() -> {
         ChunkPos l_var3 = ChunkPos.minFromRegion(param0, param1);
         ChunkPos l_var4 = ChunkPos.maxFromRegion(param0, param1);
         BitSet l_var5 = new BitSet();
         ChunkPos.rangeClosed(l_var3, l_var4).forEach(l2_param1 -> {
            CollectFields l2_var3 = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data"));

            try {
               this.scanChunk(l2_param1, l2_var3).join();
            } catch (Exception l2_var4) {
               LOGGER.warn("Failed to scan chunk {}", l2_param1, l2_var4);
               return;
            }

            if (l2_var3.getResult() instanceof CompoundTag l2_var5 && this.isOldChunk(l2_var5)) {
               int l2_var6 = l2_param1.getRegionLocalZ() * 32 + l2_param1.getRegionLocalX();
               l_var5.set(l2_var6);
            }
         });
         return l_var5;
      }, Util.backgroundExecutor());
   }

   private boolean isOldChunk(CompoundTag param0) {
      return param0.contains("DataVersion", 99) && param0.getInt("DataVersion") >= 3088 ? param0.contains("blending_data", 10) : true;
   }

   public CompletableFuture<Void> store(ChunkPos param0, @Nullable CompoundTag param1) {
      return this.<CompletableFuture<Void>>submitTask(() -> {
         IOWorker.PendingStore l_var3 = this.pendingWrites.computeIfAbsent(param0, l2_param1 -> new IOWorker.PendingStore(param1));
         l_var3.data = param1;
         return Either.left(l_var3.result);
      }).thenCompose(Function.identity());
   }

   public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos param0) {
      return this.submitTask(() -> {
         IOWorker.PendingStore l_var2 = this.pendingWrites.get(param0);
         if (l_var2 != null) {
            return Either.left(Optional.ofNullable(l_var2.data));
         }

         try {
            CompoundTag l_var3 = this.storage.read(param0);
            return Either.left(Optional.ofNullable(l_var3));
         } catch (Exception l_var3) {
            LOGGER.warn("Failed to read chunk {}", param0, l_var3);
            return Either.right(l_var3);
         }
      });
   }

   public CompletableFuture<Void> synchronize(boolean param0) {
      CompletableFuture<Void> var2 = this.<CompletableFuture<Void>>submitTask(
            () -> Either.left(
               CompletableFuture.allOf(this.pendingWrites.values().stream().map(l2_param0 -> l2_param0.result).toArray(CompletableFuture[]::new))
            )
         )
         .thenCompose(Function.identity());
      return param0 ? var2.thenCompose(l_param0 -> this.submitTask(() -> {
         try {
            this.storage.flush();
            return Either.left(null);
         } catch (Exception l2_var1) {
            LOGGER.warn("Failed to synchronize chunks", l2_var1);
            return Either.right(l2_var1);
         }
      })) : var2.thenCompose(l_param0 -> this.submitTask(() -> Either.left(null)));
   }

   @Override
   public CompletableFuture<Void> scanChunk(ChunkPos param0, StreamTagVisitor param1) {
      return this.submitTask(() -> {
         try {
            IOWorker.PendingStore l_var3 = this.pendingWrites.get(param0);
            if (l_var3 != null) {
               if (l_var3.data != null) {
                  l_var3.data.acceptAsRoot(param1);
               }
            } else {
               this.storage.scanChunk(param0, param1);
            }

            return Either.left(null);
         } catch (Exception l_var3) {
            LOGGER.warn("Failed to bulk scan chunk {}", param0, l_var3);
            return Either.right(l_var3);
         }
      });
   }

   private <T> CompletableFuture<T> submitTask(Supplier<Either<T, Exception>> param0) {
      return this.mailbox.askEither(l_param1 -> new StrictQueue.IntRunnable(IOWorker.Priority.FOREGROUND.ordinal(), () -> {
         if (!this.shutdownRequested.get()) {
            l_param1.tell(param0.get());
         }

         this.tellStorePending();
      }));
   }

   private void storePendingChunk() {
      if (!this.pendingWrites.isEmpty()) {
         Iterator<Entry<ChunkPos, IOWorker.PendingStore>> var1 = this.pendingWrites.entrySet().iterator();
         Entry<ChunkPos, IOWorker.PendingStore> var2 = var1.next();
         var1.remove();
         this.runStore(var2.getKey(), var2.getValue());
         this.tellStorePending();
      }
   }

   private void tellStorePending() {
      this.mailbox.tell(new StrictQueue.IntRunnable(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
   }

   private void runStore(ChunkPos param0, IOWorker.PendingStore param1) {
      try {
         this.storage.write(param0, param1.data);
         param1.result.complete(null);
      } catch (Exception var3) {
         LOGGER.error("Failed to store chunk {}", param0, var3);
         param1.result.completeExceptionally(var3);
      }
   }

   @Override
   public void close() throws IOException {
      if (this.shutdownRequested.compareAndSet(false, true)) {
         this.mailbox.ask(l_param0 -> new StrictQueue.IntRunnable(IOWorker.Priority.SHUTDOWN.ordinal(), () -> l_param0.tell(Unit.INSTANCE))).join();
         this.mailbox.close();

         try {
            this.storage.close();
         } catch (Exception var1) {
            LOGGER.error("Failed to close storage", var1);
         }
      }
   }

   static class PendingStore {
      @Nullable
      CompoundTag data;
      final CompletableFuture<Void> result = new CompletableFuture<>();

      public PendingStore(@Nullable CompoundTag param0) {
         this.data = param0;
      }
   }

   enum Priority {
      FOREGROUND,
      BACKGROUND,
      SHUTDOWN;
   }
}
