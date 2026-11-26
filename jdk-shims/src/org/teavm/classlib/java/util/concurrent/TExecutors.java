package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.Executors}.
 *
 * <p>Absent from the TeaVM classlib, and named directly by {@code net.minecraft.Util} (in a
 * comment describing the pools that were replaced), {@code HttpUtil},
 * {@code TextFilterClient} and Guava's {@code JdkFutureAdapters} --
 * {@code Executors.newCachedThreadPool(threadFactory)} and
 * {@code Executors.newFixedThreadPool(n, threadFactory)} are the two that are actually
 * called.
 *
 * <p><b>Every factory returns the same inline executor.</b> All of the JDK's factories differ
 * only in how many threads they run tasks on and how they queue them; with no threads the
 * distinction has no observable effect beyond how many workers would have existed, and every
 * task completes before the submitting call returns either way. This is the same substitution
 * {@code Util} already made for Minecraft's own background and IO pools, so the two halves of
 * the port agree.
 *
 * <p>The {@code n} and {@code ThreadFactory} arguments are still validated -- a non-positive
 * pool size is a programming error and should fail loudly -- and then discarded.
 *
 * <p><b>Subset.</b> Only the non-scheduled factories are provided. The scheduled ones need a
 * {@code ScheduledExecutorService} and a real timer to mean anything, and nothing reachable
 * here calls them. The Java-19 virtual-thread factory is omitted because
 * {@code newVirtualThreadPerTaskExecutor} does not exist in the Java 17 shape the port
 * targets. {@code newWorkStealingPool}'s overload is omitted for the same reason as the
 * scheduled factories: its parameter type is {@code ForkJoinPool}, which this classlib does
 * not model, so the signature could not even be expressed.
 */
public class TExecutors {

   /** Shared instance: the returned services hold no state, so one is enough. */
   private static final TExecutorService INLINE = new TAbstractExecutorService.Inline();

   /** Scheduled variant, reached from ViaVersion's TaskScheduler constructor. */
   private static final TScheduledExecutorService INLINE_SCHEDULED = new TInlineScheduledExecutorService();

   private static void checkPoolSize(int param0) {
      if (param0 <= 0) {
         throw new IllegalArgumentException("poolSize must be greater than 0");
      }
   }

   public static TExecutorService newFixedThreadPool(int param0) {
      checkPoolSize(param0);
      return INLINE;
   }

   public static TExecutorService newFixedThreadPool(int param0, TThreadFactory param1) {
      checkPoolSize(param0);
      return INLINE;
   }

   public static TExecutorService newSingleThreadExecutor() {
      return INLINE;
   }

   public static TExecutorService newSingleThreadExecutor(TThreadFactory param0) {
      return INLINE;
   }

   public static TExecutorService newCachedThreadPool() {
      return INLINE;
   }

   public static TExecutorService newCachedThreadPool(TThreadFactory param0) {
      return INLINE;
   }

   public static TScheduledExecutorService newScheduledThreadPool(int param0) {
      checkPoolSize(param0);
      return INLINE_SCHEDULED;
   }

   public static TScheduledExecutorService newScheduledThreadPool(int param0, TThreadFactory param1) {
      checkPoolSize(param0);
      return INLINE_SCHEDULED;
   }

   public static TScheduledExecutorService newSingleThreadScheduledExecutor() {
      return INLINE_SCHEDULED;
   }

   public static TScheduledExecutorService newSingleThreadScheduledExecutor(TThreadFactory param0) {
      return INLINE_SCHEDULED;
   }

   public static TExecutorService newWorkStealingPool() {
      return INLINE;
   }

   /**
    * The JDK's default thread factory, reduced to a factory that refuses.
    *
    * Reached from Guava's ThreadFactoryBuilder.doBuild, which substitutes it when no
    * backing factory was configured. That happens on the client boot path, from
    * HttpUtil.<clinit>. The factory it builds is only ever handed back to one of the
    * factories above, all of which discard their ThreadFactory, so in practice nothing
    * calls newThread on it.
    *
    * It throws rather than returning null or a thread that would never be scheduled: a
    * null would turn into an NPE somewhere further away, and a real Thread here would
    * quietly reintroduce the threading this whole class exists to avoid. If it is ever
    * called the message says exactly where.
    */
   public static TThreadFactory defaultThreadFactory() {
      return new TThreadFactory() {
         public Thread newThread(Runnable param0) {
            throw new UnsupportedOperationException(
               "Thread creation is not available under WASM-GC; reached via Executors.defaultThreadFactory()");
         }
      };
   }

   /**
    * Wraps a task that returns nothing as one that returns a fixed value.
    *
    * Reached from io.netty.util.concurrent.GlobalEventExecutor.<init>, which wraps a
    * ScheduledFutureTask and passes null as the result. The wrapper is stored and only
    * reached if the executor is asked to run it, which the inline executor above does
    * inline, so the task does run -- just on the submitting thread rather than on a
    * timer, and with its result discarded rather than delivered to a Future nobody is
    * holding.
    */
   public static <T> Callable<T> callable(Runnable param0, T param1) {
      if (param0 == null) {
         throw new NullPointerException("task");
      }

      return new Callable<T>() {
         public T call() {
            param0.run();
            return param1;
         }
      };
   }
}