package com.mojang.realmsclient.gui.task;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.TimeSource;
import org.slf4j.Logger;

public class DataFetcher {
   static final Logger LOGGER = LogUtils.getLogger();
   final Executor executor;
   final TimeUnit resolution;
   final TimeSource timeSource;

   public DataFetcher(Executor param0, TimeUnit param1, TimeSource param2) {
      this.executor = param0;
      this.resolution = param1;
      this.timeSource = param2;
   }

   public <T> DataFetcher.Task<T> createTask(String param0, Callable<T> param1, Duration param2, RepeatedDelayStrategy param3) {
      long var5 = toResolution(param2, this.resolution);
      if (var5 == 0L) {
         throw new IllegalArgumentException("Period of " + param2 + " too short for selected resolution of " + this.resolution);
      } else {
         return new DataFetcher.Task<>(param0, param1, var5, param3);
      }
   }

   /**
    * The value {@code TimeUnit.convert(Duration)} would return, spelled out.
    *
    * <p>{@code convert(Duration)} is a Java 9 addition and the WASM-GC classlib models the
    * Java 8 shape of {@code TimeUnit}, so the link reported
    * {@code Method java.util.concurrent.TimeUnit.convert(Ljava/time/Duration;)J}. The
    * JDK's implementation is a switch over the seven constants, one arm per unit, each
    * calling the matching {@code Duration} accessor; this is that switch, arm for arm, so
    * the result is identical to the call it replaces.
    *
    * <p>It cannot simply be {@code unit.toMillis(duration.toMillis())} -- that would be
    * right for {@code MILLISECONDS} and wrong for the other six, and it would silently
    * truncate where {@code convert} does not. The one resolution actually in use is
    * {@code MILLISECONDS} ({@code RealmsDataFetcher} passes it to the constructor), but the
    * field is a {@code TimeUnit} and nothing stops another caller passing a different one.
    *
    * <p>Arithmetic is left in {@code Duration}'s hands rather than funnelled through
    * {@code unit.convert(nanos)} on purpose: {@code Duration.toNanos()} throws on overflow
    * where the JDK's own arms for seconds and above never overflow, because they never form
    * a nanosecond count at all.
    */
   private static long toResolution(Duration param0, TimeUnit param1) {
      switch (param1) {
         case NANOSECONDS:
            return param0.toNanos();

         case MICROSECONDS:
            return param0.toNanos() / 1000L;

         case MILLISECONDS:
            return param0.toMillis();

         case SECONDS:
            return param0.getSeconds();

         case MINUTES:
            return param0.toMinutes();

         case HOURS:
            return param0.toHours();

         case DAYS:
            return param0.toDays();

         default:
            throw new IllegalArgumentException("Unsupported resolution: " + param1);
      }
   }

   public DataFetcher.Subscription createSubscription() {
      return new DataFetcher.Subscription();
   }

   record ComputationResult<T>(Either<T, Exception> value, long time) {
   }

   class SubscribedTask<T> {
      private final DataFetcher.Task<T> task;
      private final Consumer<T> output;
      private long lastCheckTime = -1L;

      SubscribedTask(DataFetcher.Task<T> param1, Consumer<T> param2) {
         this.task = param1;
         this.output = param2;
      }

      void update(long param0) {
         this.task.updateIfNeeded(param0);
         this.runCallbackIfNeeded();
      }

      void runCallbackIfNeeded() {
         DataFetcher.SuccessfulComputationResult<T> var1 = this.task.lastResult;
         if (var1 != null && this.lastCheckTime < var1.time) {
            this.output.accept(var1.value);
            this.lastCheckTime = var1.time;
         }
      }

      void runCallback() {
         DataFetcher.SuccessfulComputationResult<T> var1 = this.task.lastResult;
         if (var1 != null) {
            this.output.accept(var1.value);
            this.lastCheckTime = var1.time;
         }
      }

      void reset() {
         this.task.reset();
         this.lastCheckTime = -1L;
      }
   }

   public class Subscription {
      private final List<DataFetcher.SubscribedTask<?>> subscriptions = new ArrayList<>();

      public <T> void subscribe(DataFetcher.Task<T> param0, Consumer<T> param1) {
         DataFetcher.SubscribedTask<T> var3 = DataFetcher.this.new SubscribedTask<>(param0, param1);
         this.subscriptions.add(var3);
         var3.runCallbackIfNeeded();
      }

      public void forceUpdate() {
         for (DataFetcher.SubscribedTask<?> var2 : this.subscriptions) {
            var2.runCallback();
         }
      }

      public void tick() {
         for (DataFetcher.SubscribedTask<?> var2 : this.subscriptions) {
            var2.update(DataFetcher.this.timeSource.get(DataFetcher.this.resolution));
         }
      }

      public void reset() {
         for (DataFetcher.SubscribedTask<?> var2 : this.subscriptions) {
            var2.reset();
         }
      }
   }

   record SuccessfulComputationResult<T>(T value, long time) {
   }

   public class Task<T> {
      private final String id;
      private final Callable<T> updater;
      private final long period;
      private final RepeatedDelayStrategy repeatStrategy;
      @Nullable
      private CompletableFuture<DataFetcher.ComputationResult<T>> pendingTask;
      @Nullable
      DataFetcher.SuccessfulComputationResult<T> lastResult;
      private long nextUpdate = -1L;

      Task(String param1, Callable<T> param2, long param3, RepeatedDelayStrategy param5) {
         this.id = param1;
         this.updater = param2;
         this.period = param3;
         this.repeatStrategy = param5;
      }

      void updateIfNeeded(long param0) {
         if (this.pendingTask != null) {
            DataFetcher.ComputationResult<T> var3 = this.pendingTask.getNow(null);
            if (var3 == null) {
               return;
            }

            this.pendingTask = null;
            long var4 = var3.time;
            var3.value().ifLeft(l_param2 -> {
               this.lastResult = new DataFetcher.SuccessfulComputationResult<>((T)l_param2, var4);
               this.nextUpdate = var4 + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
            }).ifRight(l_param2 -> {
               long l_var4 = this.repeatStrategy.delayCyclesAfterFailure();
               DataFetcher.LOGGER.warn("Failed to process task {}, will repeat after {} cycles", this.id, l_var4, l_param2);
               this.nextUpdate = var4 + this.period * l_var4;
            });
         }

         if (this.nextUpdate <= param0) {
            this.pendingTask = CompletableFuture.supplyAsync(() -> {
               try {
                  T l_var1 = this.updater.call();
                  long l_var2 = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.left(l_var1), l_var2);
               } catch (Exception l_var1) {
                  long l_var2 = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.right(l_var1), l_var2);
               }
            }, DataFetcher.this.executor);
         }
      }

      public void reset() {
         this.pendingTask = null;
         this.lastResult = null;
         this.nextUpdate = -1L;
      }
   }
}
