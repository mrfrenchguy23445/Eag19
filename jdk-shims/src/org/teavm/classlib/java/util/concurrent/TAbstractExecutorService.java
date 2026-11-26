package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Shim for {@code java.util.concurrent.AbstractExecutorService}.
 *
 * <p>Absent from the TeaVM classlib. Guava's {@code AbstractListeningExecutorService}
 * extends it and declares nothing but {@code execute}, so it is the base of every Guava
 * listening-executor class reachable on this target.
 *
 * <p><b>What survives.</b> The interesting part of the JDK's version is not the lifecycle
 * methods -- it is the derivation of {@code submit}, {@code invokeAll} and {@code invokeAny}
 * from the single abstract primitive, {@code execute(Runnable)}. That derivation is
 * reproduced here in full, because it is what makes an {@code ExecutorService} usable at all
 * and because Guava subclasses inherit it rather than reimplementing it.
 *
 * <p>Tasks returned by {@code submit} are shimmed {@link CompletableFuture}s rather than the
 * JDK's {@code FutureTask}, because {@code FutureTask} is not modelled by this classlib
 * either. That is a safe substitution: {@code CompletableFuture} already implements
 * {@link Future}, and its {@code get()} semantics here (immediate value, or
 * {@code ExecutionException} on failure) match what {@code FutureTask} would produce once the
 * task has run -- which, with inline execution, is before {@code submit} returns.
 *
 * <p><b>What cannot.</b> {@code awaitTermination} cannot wait, so it reports the terminal
 * state directly. {@code shutdown} is recorded so {@code isShutdown} can answer truthfully,
 * but there is nothing to stop: every task here has already finished running by the time
 * {@code submit} hands back its future.
 */
public abstract class TAbstractExecutorService implements TExecutorService {
   private volatile boolean shutdown;

   /**
    * Wraps a task in a future and schedules it through {@link #execute(Runnable)}.
    *
    * <p>This is the JDK's {@code newTaskFor(Callable)} reduced to what is needed here: the
    * {@code Runnable}-and-result overload is not referenced by any reachable subclass, so it
    * is omitted rather than carried as dead code.
    */
   protected <T> org.teavm.classlib.java.util.concurrent.TFuture<T> newTaskFor(Callable<T> param0) {
      TCompletableFuture<T> var1 = new TCompletableFuture<>();
      this.execute(() -> {
         try {
            var1.complete(param0.call());
         } catch (Throwable var2) {
            var1.completeExceptionally(var2);
         }
      });
      return var1;
   }

   public <T> org.teavm.classlib.java.util.concurrent.TFuture<T> submit(Callable<T> param0) {
      if (param0 == null) {
         throw new NullPointerException();
      }

      return this.newTaskFor(param0);
   }

   public <T> org.teavm.classlib.java.util.concurrent.TFuture<T> submit(Runnable param0, T param1) {
      return this.submit(() -> {
         param0.run();
         return param1;
      });
   }

   public org.teavm.classlib.java.util.concurrent.TFuture<?> submit(Runnable param0) {
      return this.submit(param0, null);
   }

   /**
    * Submits every task and returns their futures.
    *
    * <p>The JDK applies the timeout to the whole batch and cancels what has not started. Here
    * every task runs to completion inside this method, so the timeout is accepted and
    * ignored: there is no batch that could overrun, because nothing is still running by the
    * time this returns.
    */
   public <T> List<org.teavm.classlib.java.util.concurrent.TFuture<T>> invokeAll(Collection<? extends Callable<T>> param0) throws InterruptedException {
      List<org.teavm.classlib.java.util.concurrent.TFuture<T>> var1 = new ArrayList<>(param0.size());

      for (Callable<T> var2 : param0) {
         var1.add(this.newTaskFor(var2));
      }

      return var1;
   }

   public <T> List<org.teavm.classlib.java.util.concurrent.TFuture<T>> invokeAll(Collection<? extends Callable<T>> param0, long param1, TimeUnit param2)
         throws InterruptedException {
      return this.invokeAll(param0);
   }

   /** Returns the first successful result, or throws the last failure if none succeeded. */
   public <T> T invokeAny(Collection<? extends Callable<T>> param0) throws InterruptedException, ExecutionException {
      List<org.teavm.classlib.java.util.concurrent.TFuture<T>> var1 = this.invokeAll(param0);
      ExecutionException var2 = null;

      for (org.teavm.classlib.java.util.concurrent.TFuture<T> var3 : var1) {
         try {
            return var3.get();
         } catch (ExecutionException var4) {
            if (var2 == null) {
               var2 = var4;
            }
         }
      }

      throw var2 != null ? var2 : new ExecutionException(new IllegalStateException("invokeAny: no tasks"));
   }

   public <T> T invokeAny(Collection<? extends Callable<T>> param0, long param1, TimeUnit param2)
         throws InterruptedException, ExecutionException, TTimeoutException {
      return this.invokeAny(param0);
   }

   public void shutdown() {
      this.shutdown = true;
   }

   public List<Runnable> shutdownNow() {
      this.shutdown = true;
      // The JDK returns whatever had not yet started. Every task here has already run, so
      // the list is empty -- which is the truthful answer, not a stub value.
      return Collections.<Runnable>emptyList();
   }

   public boolean isShutdown() {
      return this.shutdown;
   }

   /** Always {@code true}: with inline execution nothing is ever left running. */
   public boolean isTerminated() {
      return true;
   }

   /** Returns immediately, reporting {@link #isTerminated()}. See the class comment. */
   public boolean awaitTermination(long param0, TimeUnit param1) throws InterruptedException {
      return this.isTerminated();
   }

   /**
    * The concrete executor the {@link Executors} factories return.
    *
    * <p>Every task runs inline on the submitting thread, which is the same contract
    * {@code Util}'s {@code DirectExecutor} already established for Minecraft's own pools.
    * The interesting detail is {@link #shutdown()}: it deliberately does <b>not</b> reject
    * later submissions. Rejection is the JDK's behaviour, but a rejected task here would
    * silently drop work that in the JDK would have been queued and run -- and with no worker
    * thread, "queued" and "run" are the same instant anyway.
    */
   public static class Inline extends TAbstractExecutorService {
      public void execute(Runnable param0) {
         param0.run();
      }
   }
}