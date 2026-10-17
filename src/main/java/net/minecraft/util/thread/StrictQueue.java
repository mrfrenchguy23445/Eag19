package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public interface StrictQueue<T, F> {
   @Nullable
   F pop();

   boolean push(T var1);

   boolean isEmpty();

   int size();

   final class FixedPriorityQueue implements StrictQueue<StrictQueue.IntRunnable, Runnable> {
      private final Queue<Runnable>[] queues;
      private final AtomicInteger size = new AtomicInteger();

      public FixedPriorityQueue(int param0) {
         this.queues = new Queue[param0];

         for (int var2 = 0; var2 < param0; var2++) {
            this.queues[var2] = Queues.newConcurrentLinkedQueue();
         }
      }

      @Nullable
      public Runnable pop() {
         for (Queue<Runnable> var4 : this.queues) {
            Runnable var5 = var4.poll();
            if (var5 != null) {
               this.size.decrementAndGet();
               return var5;
            }
         }

         return null;
      }

      public boolean push(StrictQueue.IntRunnable param0) {
         int var2 = param0.priority;
         if (var2 < this.queues.length && var2 >= 0) {
            this.queues[var2].add(param0);
            this.size.incrementAndGet();
            return true;
         } else {
            throw new IndexOutOfBoundsException(String.format(Locale.ROOT, "Priority %d not supported. Expected range [0-%d]", var2, this.queues.length - 1));
         }
      }

      @Override
      public boolean isEmpty() {
         return this.size.get() == 0;
      }

      @Override
      public int size() {
         return this.size.get();
      }
   }

   final class IntRunnable implements Runnable {
      final int priority;
      private final Runnable task;

      public IntRunnable(int param0, Runnable param1) {
         this.priority = param0;
         this.task = param1;
      }

      @Override
      public void run() {
         this.task.run();
      }

      public int getPriority() {
         return this.priority;
      }
   }

   final class QueueStrictQueue<T> implements StrictQueue<T, T> {
      private final Queue<T> queue;

      public QueueStrictQueue(Queue<T> param0) {
         this.queue = param0;
      }

      @Nullable
      @Override
      public T pop() {
         return this.queue.poll();
      }

      @Override
      public boolean push(T param0) {
         return this.queue.add(param0);
      }

      @Override
      public boolean isEmpty() {
         return this.queue.isEmpty();
      }

      @Override
      public int size() {
         return this.queue.size();
      }
   }
}
