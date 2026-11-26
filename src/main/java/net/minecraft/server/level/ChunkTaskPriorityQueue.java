package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;

public class ChunkTaskPriorityQueue<T> {
   public static final int PRIORITY_LEVEL_COUNT = ChunkMap.MAX_CHUNK_DISTANCE + 2;
   private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> taskQueue = IntStream.range(0, PRIORITY_LEVEL_COUNT)
      .mapToObj(l_param0 -> new Long2ObjectLinkedOpenHashMap<List<Optional<T>>>())
      .collect(Collectors.toList());
   private volatile int firstQueue = PRIORITY_LEVEL_COUNT;
   private final String name;
   private final LongSet acquired = new LongOpenHashSet();
   private final int maxTasks;

   public ChunkTaskPriorityQueue(String param0, int param1) {
      this.name = param0;
      this.maxTasks = param1;
   }

   protected void resortChunkTasks(int param0, ChunkPos param1, int param2) {
      if (param0 < PRIORITY_LEVEL_COUNT) {
         Long2ObjectLinkedOpenHashMap<List<Optional<T>>> var4 = this.taskQueue.get(param0);
         List<Optional<T>> var5 = var4.remove(param1.toLong());
         if (param0 == this.firstQueue) {
            while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
               this.firstQueue++;
            }
         }

         if (var5 != null && !var5.isEmpty()) {
            this.taskQueue.get(param2).computeIfAbsent(param1.toLong(), l_param0 -> Lists.newArrayList()).addAll(var5);
            this.firstQueue = Math.min(this.firstQueue, param2);
         }
      }
   }

   protected void submit(Optional<T> param0, long param1, int param3) {
      this.taskQueue.get(param3).computeIfAbsent(param1, l_param0 -> Lists.newArrayList()).add(param0);
      this.firstQueue = Math.min(this.firstQueue, param3);
   }

   protected void release(long param0, boolean param2) {
      for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> var5 : this.taskQueue) {
         List<Optional<T>> var6 = var5.get(param0);
         if (var6 != null) {
            if (param2) {
               var6.clear();
            } else {
               var6.removeIf(l_param0 -> !l_param0.isPresent());
            }

            if (var6.isEmpty()) {
               var5.remove(param0);
            }
         }
      }

      while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
         this.firstQueue++;
      }

      this.acquired.remove(param0);
   }

   private Runnable acquire(long param0) {
      return () -> this.acquired.add(param0);
   }

   @Nullable
   public Stream<Either<T, Runnable>> pop() {
      if (this.acquired.size() >= this.maxTasks) {
         return null;
      }

      if (!this.hasWork()) {
         return null;
      }

      int var1 = this.firstQueue;
      Long2ObjectLinkedOpenHashMap<List<Optional<T>>> var2 = this.taskQueue.get(var1);
      long var3 = var2.firstLongKey();
      List<Optional<T>> var5 = var2.removeFirst();

      while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
         this.firstQueue++;
      }

      return var5.stream().map(l_param2 -> l_param2.<Either<T, Runnable>>map(Either::left).orElseGet(() -> Either.right(this.acquire(var3))));
   }

   public boolean hasWork() {
      return this.firstQueue < PRIORITY_LEVEL_COUNT;
   }

   /** Cooperative-port diagnostics: total pending messages across every priority bucket. */
   public int totalQueuedTasks() {
      int var2 = 0;
      for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> var3 : this.taskQueue) {
         for (List<Optional<T>> var5 : (Iterable<List<Optional<T>>>)var3.values()) {
            var2 += var5.size();
         }
      }
      return var2;
   }

   /** Cooperative-port diagnostics: the current head priority bucket index. */
   public int firstQueueLevel() {
      return this.firstQueue;
   }

   /** Cooperative-port diagnostics: how many positions are currently acquired. */
   public int acquiredCount() {
      return this.acquired.size();
   }

   @Override
   public String toString() {
      return this.name + " " + this.firstQueue + "...";
   }

   @VisibleForTesting
   LongSet getAcquired() {
      return new LongOpenHashSet(this.acquired);
   }
}
