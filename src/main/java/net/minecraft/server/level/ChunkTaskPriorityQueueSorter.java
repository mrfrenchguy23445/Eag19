package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class ChunkTaskPriorityQueueSorter implements ChunkHolder.LevelChangeListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<ProcessorHandle<?>, ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>>> queues;
   private final Set<ProcessorHandle<?>> sleeping;
   private final ProcessorMailbox<StrictQueue.IntRunnable> mailbox;

   public ChunkTaskPriorityQueueSorter(List<ProcessorHandle<?>> param0, Executor param1, int param2) {
      this.queues = param0.stream()
         .collect(Collectors.toMap(Function.identity(), l_param1 -> new ChunkTaskPriorityQueue<>(l_param1.name() + "_queue", param2)));
      this.sleeping = Sets.newHashSet(param0);
      this.mailbox = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(4), param1, "sorter");
   }

   public boolean hasWork() {
      return this.mailbox.hasWork() || this.queues.values().stream().anyMatch(ChunkTaskPriorityQueue::hasWork);
   }

   public static <T> ChunkTaskPriorityQueueSorter.Message<T> message(Function<ProcessorHandle<Unit>, T> param0, long param1, IntSupplier param3) {
      return new ChunkTaskPriorityQueueSorter.Message<>(param0, param1, param3);
   }

   public static ChunkTaskPriorityQueueSorter.Message<Runnable> message(Runnable param0, long param1, IntSupplier param3) {
      return new ChunkTaskPriorityQueueSorter.Message<>(l_param1 -> () -> {
         // Cooperative-port fix: if the task throws, the Unit ack was never sent back to
         // the ask-future, so the sorter's allOf re-arm was lost and the whole task
         // queue for that processor froze forever with messages still pending. Always
         // send the ack, and log the failure instead of dying silently.
         try {
            param0.run();
         } catch (Throwable l_var2) {
            LOGGER.error("Chunk task at {} threw", new ChunkPos(param1), l_var2);
         } finally {
            l_param1.tell(Unit.INSTANCE);
         }
      }, param1, param3);
   }

   public static ChunkTaskPriorityQueueSorter.Message<Runnable> message(ChunkHolder param0, Runnable param1) {
      return message(param1, param0.getPos().toLong(), param0::getQueueLevel);
   }

   public static <T> ChunkTaskPriorityQueueSorter.Message<T> message(ChunkHolder param0, Function<ProcessorHandle<Unit>, T> param1) {
      return message(param1, param0.getPos().toLong(), param0::getQueueLevel);
   }

   public static ChunkTaskPriorityQueueSorter.Release release(Runnable param0, long param1, boolean param3) {
      return new ChunkTaskPriorityQueueSorter.Release(param0, param1, param3);
   }

   public <T> ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<T>> getProcessor(ProcessorHandle<T> param0, boolean param1) {
      return this.mailbox
         .<ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<T>>>ask(
            l_param2 -> new StrictQueue.IntRunnable(
               0,
               () -> {
                  this.getQueue(param0);
                  l_param2.tell(
                     ProcessorHandle.of(
                        "chunk priority sorter around " + param0.name(),
                        l3_param2 -> this.submit(param0, l3_param2.task, l3_param2.pos, l3_param2.level, param1)
                     )
                  );
               }
            )
         )
         .join();
   }

   public ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> getReleaseProcessor(ProcessorHandle<Runnable> param0) {
      return this.mailbox
         .<ProcessorHandle<ChunkTaskPriorityQueueSorter.Release>>ask(
            l_param1 -> new StrictQueue.IntRunnable(
               0,
               () -> l_param1.tell(
                  ProcessorHandle.of(
                     "chunk priority sorter around " + param0.name(), l3_param1 -> this.release(param0, l3_param1.pos, l3_param1.task, l3_param1.clearQueue)
                  )
               )
            )
         )
         .join();
   }

   @Override
   public void onLevelChange(ChunkPos param0, IntSupplier param1, int param2, IntConsumer param3) {
      this.mailbox.tell(new StrictQueue.IntRunnable(0, () -> {
         int l_var5 = param1.getAsInt();
         this.queues.values().forEach(l2_param3 -> l2_param3.resortChunkTasks(l_var5, param0, param2));
         param3.accept(param2);
      }));
   }

   private <T> void release(ProcessorHandle<T> param0, long param1, Runnable param3, boolean param4) {
      this.mailbox.tell(new StrictQueue.IntRunnable(1, () -> {
         ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> l_var6 = this.getQueue(param0);
         l_var6.release(param1, param4);
         if (this.sleeping.remove(param0)) {
            this.pollTask(l_var6, param0);
         }

         param3.run();
      }));
   }

   private <T> void submit(ProcessorHandle<T> param0, Function<ProcessorHandle<Unit>, T> param1, long param2, IntSupplier param4, boolean param5) {
      this.mailbox.tell(new StrictQueue.IntRunnable(2, () -> {
         ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> l_var7 = this.getQueue(param0);
         int l_var8 = param4.getAsInt();
         l_var7.submit(Optional.of(param1), param2, l_var8);
         if (param5) {
            l_var7.submit(Optional.empty(), param2, l_var8);
         }

         if (this.sleeping.remove(param0)) {
            this.pollTask(l_var7, param0);
         }
      }));
   }

   private <T> void pollTask(ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> param0, ProcessorHandle<T> param1) {
      this.mailbox.tell(new StrictQueue.IntRunnable(3, () -> {
         Stream<Either<Function<ProcessorHandle<Unit>, T>, Runnable>> l_var3 = param0.pop();
         if (l_var3 == null) {
            this.sleeping.add(param1);
         } else {
            // Cooperative-port fix: a failing batch used to be dropped on the floor,
            // killing the re-arm loop and leaving queued messages unpopped forever.
            // Handle the failure, then re-arm regardless so the queue keeps draining.
            CompletableFuture
               .allOf(l_var3.map(l2_param1 -> l2_param1.map(param1::ask, l3_param0 -> {
                  l3_param0.run();
                  return CompletableFuture.completedFuture(Unit.INSTANCE);
               })).toArray(CompletableFuture[]::new))
               .handle((l2_r, l2_ex) -> {
                  if (l2_ex != null) {
                     LOGGER.error("Chunk task batch failed", l2_ex);
                  }
                  this.pollTask(param0, param1);
                  return null;
               });
         }
      }));
   }

   private <T> ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> getQueue(ProcessorHandle<T> param0) {
      ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>> var2 = this.queues.get(param0);
      if (var2 == null) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("No queue for: " + param0));
      } else {
         return (ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>>)var2;
      }
   }

   @VisibleForTesting
   public String getDebugStatus() {
      return this.queues
            .entrySet()
            .stream()
            .map(
               l_param0 -> l_param0.getKey().name()
                  + "=["
                  + l_param0.getValue().getAcquired().stream().map(l2_param0 -> l2_param0 + ":" + new ChunkPos(l2_param0)).collect(Collectors.joining(","))
                  + "]"
            )
            .collect(Collectors.joining(","))
         + ", s="
         + this.sleeping.size();
   }

   /** Cooperative-port diagnostics: pending counts, head levels and the sleeping set. */
   public String describeQueues() {
      StringBuilder var2 = new StringBuilder();
      for (Map.Entry<ProcessorHandle<?>, ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>>> var3 : this.queues.entrySet()) {
         if (var2.length() != 0) {
            var2.append(' ');
         }

         ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>> var4 = var3.getValue();
         var2.append(var3.getKey().name()).append(":q=").append(var4.totalQueuedTasks())
            .append("@").append(var4.firstQueueLevel())
            .append("/acq=").append(var4.acquiredCount());
      }

      var2.append(" sleeping=");
      for (ProcessorHandle<?> var5 : this.sleeping) {
         var2.append(var5.name()).append(',');
      }

      return var2.append(" sortMb=").append(this.mailbox.size()).toString();
   }

   @Override
   public void close() {
      this.queues.keySet().forEach(ProcessorHandle::close);
   }

   public static final class Message<T> {
      final Function<ProcessorHandle<Unit>, T> task;
      final long pos;
      final IntSupplier level;

      Message(Function<ProcessorHandle<Unit>, T> param0, long param1, IntSupplier param3) {
         this.task = param0;
         this.pos = param1;
         this.level = param3;
      }
   }

   public static final class Release {
      final Runnable task;
      final long pos;
      final boolean clearQueue;

      Release(Runnable param0, long param1, boolean param3) {
         this.task = param0;
         this.pos = param1;
         this.clearQueue = param3;
      }
   }
}
