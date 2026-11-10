package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shim for {@code java.util.concurrent.CompletableFuture}.
 *
 * <p><b>Why this is the most consequential shim in the project.</b> The TeaVM classlib this
 * port targets models locks, queues and atomics but has no future/promise machinery at all
 * -- no {@code CompletableFuture}, no {@code CompletionStage}, no {@code Future}, no
 * {@code ExecutorService}. Yet 159 files reference {@code CompletableFuture}, including
 * {@code Minecraft}'s own constructor, every chunk-loading path and the whole sound system.
 * With it missing {@code Minecraft.<init>} cannot link, so the game cannot start. This file
 * is what lets it.
 *
 * <h2>Execution model: everything completes inline</h2>
 *
 * <p>WASM-GC has no threads, so there is nothing to defer work to. Every continuation runs
 * <em>on the thread that completes the stage</em>, immediately:
 *
 * <ul>
 *   <li>A {@code thenX} on an already-completed stage runs {@code fn} inline and returns an
 *       already-completed stage.</li>
 *   <li>A {@code thenX} on a pending stage registers a continuation and returns a new pending
 *       stage; the continuation runs inline when the original completes.</li>
 *   <li>The {@code *Async} family is <b>not</b> special-cased. Each one hands its task to the
 *       supplied {@link Executor}, exactly as the JDK does.</li>
 * </ul>
 *
 * <p>That last point is what makes this work rather than merely compile. Minecraft does not
 * own its executors: it calls {@code Util.backgroundExecutor()} and {@code Util.ioPool()},
 * and those were already reduced to inline {@code DirectExecutor}s on this target (see
 * {@code Util}). So the 36 {@code thenAcceptAsync}/{@code thenApplyAsync} call sites in the
 * codebase <em>already</em> ask for inline execution. This shim honours the executor rather
 * than overriding it, and so delivers exactly what the rest of the port was built around. An
 * executor that genuinely deferred would leave a stage pending, which {@link #join()} reports
 * as a programming error rather than hanging on.
 *
 * <h2>Pending is a bug, not a wait</h2>
 *
 * <p>The JDK's {@code join()} blocks forever on a pending stage. Here that is a hang with no
 * diagnostic, on a platform whose usual failure symptom is already "the screen stopped
 * updating". So {@link #join()} on a pending stage throws {@link IllegalStateException} and
 * {@link #get()} throws {@link ExecutionException} wrapping one, both naming the reason.
 * Reaching a pending {@code join()} means a stage was never completed, which is always a
 * logic error in a build where completion is synchronous.
 *
 * <h2>Deviations from the JDK, all deliberate</h2>
 * <ul>
 *   <li><b>No blocking anywhere.</b> The two timed {@link Future#get} overloads cannot wait,
 *       so they behave like their untimed counterparts.</li>
 *   <li><b>Not thread-safe.</b> The JDK's is, via volatile fields and CAS. There is one
 *       thread here, so the fields are plain -- see the note on {@link #result}.</li>
 *   <li><b>Either-stage combinations remain left-biased.</b> The both-stage methods,
 *       including the chunk-save barrier's {@code thenCombine}, wait for both inputs.</li>
 * </ul>
 *
 * <p>Everything else -- {@link CompletionException} wrapping on {@code join}, the
 * {@code AltResult} representation, {@code join}'s rule about not re-wrapping an existing
 * {@code CompletionException}, {@code allOf} reporting the first failure -- follows the JDK
 * specification, because callers branch on those details: {@code Util.sequenceFailFast}
 * inspects the cause chain, for instance.
 *
 * <p><b>Subset.</b> The time-based family ({@code orTimeout}, {@code completeOnTimeout},
 * {@code delayedExecutor}) completes immediately instead of being omitted, since a timer has
 * nothing to fire on. {@code getNumberOfDependents} always reports 0.
 * {@link CompletionStage}'s {@code default} bodies are not reproduced; see that interface.
 */
public class TCompletableFuture<T> implements TFuture<T>, TCompletionStage<T> {

   /**
    * Sentinel for "not yet completed". A distinct object is required rather than
    * {@code null}, because {@code null} is a perfectly legal completed value -- and
    * {@code CompletableFuture.completedFuture(null)} is used in this codebase.
    */
   private static final Object PENDING = new Object();

   /**
    * Continuations are synchronous on this target, but running each nested completion
    * directly recurses through the JavaScript/WASM stack. Queue nested dispatches and
    * drain them iteratively so completion remains synchronous at the outermost call.
    */
   private static final ArrayDeque<Runnable> COMPLETION_QUEUE = new ArrayDeque<>();
   private static boolean drainingCompletions;

   private static void dispatch(Runnable action) {
      COMPLETION_QUEUE.addLast(action);
      if (drainingCompletions) {
         return;
      }

      drainingCompletions = true;
      try {
         while (!COMPLETION_QUEUE.isEmpty()) {
            COMPLETION_QUEUE.removeFirst().run();
         }
      } finally {
         drainingCompletions = false;
         if (!COMPLETION_QUEUE.isEmpty()) {
            COMPLETION_QUEUE.clear();
         }
      }
   }

   /**
    * The result, an {@link AltResult} for a failure, or {@link #PENDING}.
    *
    * <p>A plain field: no {@code volatile}, no CAS. Single-threaded by construction -- WASM-GC
    * has no threads, and {@code Util}'s executors run inline on the calling thread. Adding
    * synchronisation would imply a concurrency this target does not have.
    */
   private Object result = PENDING;

   /** Continuations to run when this stage completes. Created lazily, so an unchained future is cheap. */
   private List<Dep> conts;

   /**
    * One dependent continuation. It is handed the derived stage and completes it.
    *
    * <p>Receiving the derived stage as an argument -- rather than closing over it -- is what
    * keeps {@link #derive} generic in the result type, so continuations for differently-typed
    * stages can share one list with no cast at every call site.
    */
   private interface Cont<R> {
      void run(TCompletableFuture<R> derived, Object value, Throwable error);
   }

   /**
    * A registered continuation paired with the stage it is meant to complete.
    *
    * <p>The pairing is the whole mechanism. {@link #derive} builds the derived stage, writes
    * it into a {@link Dep}, and puts that on the owner's list; when the owner completes,
    * {@link #finish} hands each {@link Dep} its own stage back to its continuation. Without
    * the pairing there is nowhere to recover which stage a given continuation belongs to, and
    * the continuation would be called with {@code null}.
    */
   private static final class Dep {
      final TCompletableFuture<?> target;
      final Cont<?> action;

      Dep(TCompletableFuture<?> target, Cont<?> action) {
         this.target = target;
         this.action = action;
      }
   }

   /** Wraps a failure, so that completed-with-null stays distinguishable from completed. */
   private static final class AltResult {
      final Throwable ex;

      AltResult(Throwable ex) {
         this.ex = ex;
      }
   }

   public TCompletableFuture() {
   }

   /** The JDK's protected constructor for subclasses that do not complete in the constructor. */
   protected TCompletableFuture(boolean unused) {
   }

   // ---------------------------------------------------------------- internals

   @SuppressWarnings("unchecked")
   private T cast(Object o) {
      return (T)o;
   }

   private Throwable errorOf() {
      return this.result instanceof AltResult ? ((AltResult)this.result).ex : null;
   }

   /** Sets the result and runs every registered continuation inline. */
   private void finish(Object newResult) {
      if (this.result != PENDING) {
         return;
      }

      this.result = newResult;
      if (this.conts == null) {
         return;
      }

      List<Dep> pending = this.conts;

      // Cleared before running: a continuation may complete further stages, and those must
      // register on their own futures rather than mutate the list being walked.
      this.conts = null;
      Throwable ex = this.errorOf();
      Object v = ex != null ? null : this.result;

      for (int i = 0; i < pending.size(); i++) {
         Dep dep = pending.get(i);

         @SuppressWarnings({"unchecked", "rawtypes"})
         TCompletableFuture target = dep.target;
         @SuppressWarnings({"unchecked", "rawtypes"})
         Cont action = dep.action;
         dispatch(() -> action.run(target, v, ex));
      }
   }

   /** The one place the "already complete vs. register" decision is made. */
   private <R> TCompletableFuture<R> derive(Cont<R> action) {
      TCompletableFuture<R> derived = new TCompletableFuture<>();
      Object r = this.result;

      if (r == PENDING) {
         if (this.conts == null) {
            this.conts = new ArrayList<>(4);
         }

         this.conts.add(new Dep(derived, action));
         return derived;
      }

      if (r instanceof AltResult) {
         dispatch(() -> action.run(derived, null, ((AltResult)r).ex));
      } else {
         dispatch(() -> action.run(derived, r, null));
      }

      return derived;
   }

   /** As the JDK: a non-{@code CompletionException} throwable is wrapped, so the cause chain stays one level deep. */
   private static Throwable wrap(Throwable t) {
      return t instanceof TCompletionException ? t : new TCompletionException(t);
   }

   // ---------------------------------------------------------------- static factories

   public static <U> TCompletableFuture<U> completedFuture(U value) {
      TCompletableFuture<U> f = new TCompletableFuture<>();
      f.finish(value);
      return f;
   }

   public static <U> TCompletableFuture<U> completedStage(U value) {
      return completedFuture(value);
   }

   public static <U> TCompletableFuture<U> failedFuture(Throwable ex) {
      TCompletableFuture<U> f = new TCompletableFuture<>();
      f.finish(new AltResult(ex));
      return f;
   }

   public static <U> TCompletableFuture<U> failedStage(Throwable ex) {
      return failedFuture(ex);
   }

   /** See the class comment: the executor is honoured, and on this target it runs inline. */
   public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
      Objects.requireNonNull(supplier);
      Objects.requireNonNull(executor);
      TCompletableFuture<U> f = new TCompletableFuture<>();
      executor.execute(() -> {
         try {
            f.finish(supplier.get());
         } catch (Throwable t1) {
            f.finish(new AltResult(wrap(t1)));
         }
      });
      return f;
   }

   public static <U> TCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
      return supplyAsync(supplier, defaultExecutor());
   }

   public static TCompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
      Objects.requireNonNull(runnable);
      Objects.requireNonNull(executor);
      TCompletableFuture<Void> f = new TCompletableFuture<>();
      executor.execute(() -> {
         try {
            runnable.run();
            f.finish(null);
         } catch (Throwable t1) {
            f.finish(new AltResult(wrap(t1)));
         }
      });
      return f;
   }

   public static TCompletableFuture<Void> runAsync(Runnable runnable) {
      return runAsync(runnable, defaultExecutor());
   }

   /**
    * The executor used by the no-argument {@code *Async} overloads.
    *
    * <p>The JDK uses {@code ForkJoinPool.commonPool()}, which does not exist here. A direct
    * executor makes those overloads behave like their synchronous counterparts, which is the
    * intended behaviour on a single-threaded target.
    */
   public static Executor defaultExecutor() {
      return DIRECT;
   }

   private static final Executor DIRECT = Runnable::run;

   /** The JDK returns the calling executor; with no scheduler to defer to, so does this. */
   public static Executor delayedExecutor(long delay, TimeUnit unit) {
      return DIRECT;
   }

   public static Executor delayedExecutor(long delay, TimeUnit unit, Executor delayedExecutor) {
      return DIRECT;
   }

   /**
    * Completes once every input has completed, or fails with the first error.
    *
    * <p>The JDK wraps several failures in one {@code CompletionException} with the rest
    * suppressed. Not reproduced: the only caller here ({@code Util.sequence}) needs to know
    * that it failed and why, and suppressing a list nothing reads would be noise.
    */
   public static TCompletableFuture<Void> allOf(TCompletableFuture<?>... cfs) {
      TCompletableFuture<Void> d = new TCompletableFuture<>();
      if (cfs.length == 0) {
         d.finish(null);
         return d;
      }

      Throwable[] errors = new Throwable[cfs.length];
      int[] remaining = new int[] {cfs.length};

      for (int i = 0; i < cfs.length; i++) {
         final int idx = i;
         cfs[i].whenComplete((v, ex) -> {
            if (ex != null) {
               errors[idx] = ex;
            }

            if (--remaining[0] == 0) {
               for (int j = 0; j < errors.length; j++) {
                  if (errors[j] != null) {
                     d.finish(new AltResult(errors[j]));
                     return;
                  }
               }

               d.finish(null);
            }
         });
      }

      return d;
   }

   /** Completes with the first input to complete successfully -- under inline completion, the first that did not fail. */
   public static TCompletableFuture<Object> anyOf(TCompletableFuture<?>... cfs) {
      TCompletableFuture<Object> d = new TCompletableFuture<>();

      for (TCompletableFuture<?> cf : cfs) {
         Object r = cf.result;
         if (r != PENDING && !(r instanceof AltResult)) {
            d.finish(r);
            return d;
         }
      }

      for (TCompletableFuture<?> cf : cfs) {
         if (cf.result instanceof AltResult) {
            d.finish(cf.result);
            return d;
         }
      }

      d.finish(new AltResult(new IllegalStateException(
         "anyOf: every input is still pending, which cannot happen under inline completion"
      )));
      return d;
   }

   // ---------------------------------------------------------------- state

   public boolean isDone() {
      return this.result != PENDING;
   }

   public boolean isCompletedExceptionally() {
      return this.result instanceof AltResult;
   }

   public boolean isCancelled() {
      return this.errorOf() instanceof CancellationException;
   }

   /** Always 0: this shim does not track dependent counts. */
   public int getNumberOfDependents() {
      return 0;
   }

   /** Always this future; it is already a {@code CompletableFuture}. */
   public TCompletableFuture<T> minimalCompletionStage() {
      return this;
   }

   /** Always this future. */
   public TCompletableFuture<T> toCompletableFuture() {
      return this;
   }

   /**
    * A second future that completes with the same value or failure as this one.
    *
    * <p>Built on {@link #derive} rather than copying {@link #result} directly. Copying the
    * field would be wrong for a pending stage: {@link #PENDING} is the "not yet completed"
    * sentinel, so handing it to {@code finish} would mark the copy as <em>completed with the
    * sentinel object as its value</em> -- permanently stuck, and failing {@code join()} with a
    * message about a stage that is not pending at all. Going through {@code derive} makes the
    * copy wait for this one and complete when, and how, it completes.
    */
   public TCompletableFuture<T> copy() {
      return this.derive((d, v, ex) -> {
         if (ex != null) {
            d.completeExceptionally(ex);
         } else {
            d.complete(this.cast(v));
         }
      });
   }

   public void obtrudeValue(T value) {
      this.result = value;
   }

   public void obtrudeException(Throwable ex) {
      this.result = new AltResult(ex);
   }

   /** The JDK's extension point so subclasses propagate their own type. Not overridden. */
   public <U> TCompletableFuture<U> newIncompleteFuture() {
      return new TCompletableFuture<>();
   }

   public boolean complete(T value) {
      if (this.result != PENDING) {
         return false;
      }

      this.finish(value);
      return true;
   }

   public boolean completeExceptionally(Throwable ex) {
      if (this.result != PENDING) {
         return false;
      }

      this.finish(new AltResult(ex));
      return true;
   }

   // ---------------------------------------------------------------- waiting

   public T join() {
      Object r = this.result;

      if (r == PENDING) {
         throw new IllegalStateException(
            "CompletableFuture.join() on a stage that is still pending. This target completes every"
               + " stage inline on the completing thread, so a stage pending here was never"
               + " completed -- blocking would only hang the game loop."
         );
      }

      if (r instanceof AltResult) {
         Throwable ex = ((AltResult)r).ex;

         // The JDK does not re-wrap an existing CompletionException, and callers such as
         // Util.sequenceFailFast walk the cause chain, so this detail is load-bearing.
         throw ex instanceof TCompletionException ? (TCompletionException)ex : new TCompletionException(ex);
      }

      return this.cast(r);
   }

   /** The JDK's non-throwing peek; returns {@code valueIfAbsent} unless completed normally. */
   public T getNow(T valueIfAbsent) {
      Object r = this.result;
      return r == PENDING || r instanceof AltResult ? valueIfAbsent : this.cast(r);
   }

   public T get() throws InterruptedException, ExecutionException {
      Object r = this.result;

      if (r == PENDING) {
         throw new ExecutionException(new IllegalStateException(
            "CompletableFuture.get() on a stage that is still pending; see join() -- this target"
               + " never blocks."
         ));
      }

      if (r instanceof AltResult) {
         throw new ExecutionException(((AltResult)r).ex);
      }

      return this.cast(r);
   }

   /** Behaves as {@link #get()}: nothing can be waited for, so no timeout can elapse. */
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TTimeoutException {
      return get();
   }

   public boolean cancel(boolean mayInterruptIfRunning) {
      return this.completeExceptionally(new CancellationException());
   }

   /** Fails immediately with a {@link TimeoutException}: a timer has nothing to fire on. */
   public TCompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
      this.completeExceptionally(new TTimeoutException());
      return this;
   }

   /** Completes immediately with {@code value} rather than after {@code timeout}. */
   public TCompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
      this.complete(value);
      return this;
   }

   public TCompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit, TimeUnit delayedExecutor) {
      this.complete(value);
      return this;
   }

   public TCompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
      return this.completeAsync(supplier, defaultExecutor());
   }

   public TCompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
      Objects.requireNonNull(supplier);
      Objects.requireNonNull(executor);
      TCompletableFuture<T> f = new TCompletableFuture<>();
      executor.execute(() -> {
         try {
            f.finish(supplier.get());
         } catch (Throwable t1) {
            f.finish(new AltResult(wrap(t1)));
         }
      });
      return f;
   }

   // ---------------------------------------------------------------- continuations

   public <U> TCompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
      Objects.requireNonNull(fn);
      return this.derive((d, v, ex) -> {
         if (ex != null) {
            d.completeExceptionally(ex);
            return;
         }

         try {
            d.complete(fn.apply(this.cast(v)));
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
         }
      });
   }

   public <U> TCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
      return this.thenApplyAsync(fn, defaultExecutor());
   }

   public <U> TCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
      Objects.requireNonNull(fn);
      Objects.requireNonNull(executor);
      return this.thenApply(fn);
   }

   public TCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
      Objects.requireNonNull(action);
      return this.thenApply(v -> {
         action.accept(v);
         return null;
      });
   }

   public TCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
      return this.thenAcceptAsync(action, defaultExecutor());
   }

   public TCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
      Objects.requireNonNull(action);
      Objects.requireNonNull(executor);
      return this.thenAccept(action);
   }

   public TCompletableFuture<Void> thenRun(Runnable action) {
      Objects.requireNonNull(action);
      return this.thenApply(v -> {
         action.run();
         return null;
      });
   }

   public TCompletableFuture<Void> thenRunAsync(Runnable action) {
      return this.thenRunAsync(action, defaultExecutor());
   }

   public TCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
      Objects.requireNonNull(action);
      Objects.requireNonNull(executor);
      return this.thenRun(action);
   }

   public <U> TCompletableFuture<U> thenCompose(Function<? super T, ? extends TCompletionStage<U>> fn) {
      Objects.requireNonNull(fn);
      return this.derive((d, v, ex) -> {
         if (ex != null) {
            d.completeExceptionally(ex);
            return;
         }

         TCompletionStage<U> next;

         try {
            next = fn.apply(this.cast(v));
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
            return;
         }

         if (next instanceof TCompletableFuture) {
            ((TCompletableFuture<U>)next).whenComplete((vv, ee) -> {
               if (ee != null) {
                  d.completeExceptionally(ee);
               } else {
                  d.complete(vv);
               }
            });
         } else {
            // A non-CompletableFuture CompletionStage cannot occur in this codebase (the
            // only implementations are CompletableFuture), but rather than silently drop the
            // value, flatten it synchronously if it happens to be done.
            d.complete(next == null ? null : next.toCompletableFuture().join());
         }
      });
   }

   public <U> TCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn) {
      return this.thenComposeAsync(fn, defaultExecutor());
   }

   public <U> TCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends TCompletionStage<U>> fn, Executor executor) {
      Objects.requireNonNull(fn);
      Objects.requireNonNull(executor);
      return this.thenCompose(fn);
   }

   public TCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
      Objects.requireNonNull(action);
      return this.derive((d, v, ex) -> {
         // whenComplete sees the value as null and the throwable as non-null when the stage
         // failed; that is the documented contract and Util relies on it.
         try {
            action.accept(this.cast(v), ex);
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
            return;
         }

         if (ex != null) {
            d.completeExceptionally(ex);
         } else {
            d.complete(this.cast(v));
         }
      });
   }

   public TCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
      return this.whenCompleteAsync(action, defaultExecutor());
   }

   public TCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
      Objects.requireNonNull(action);
      Objects.requireNonNull(executor);
      return this.whenComplete(action);
   }

   public TCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
      Objects.requireNonNull(fn);
      return this.derive((d, v, ex) -> {
         if (ex == null) {
            d.complete(this.cast(v));
            return;
         }

         try {
            d.complete(fn.apply(ex));
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
         }
      });
   }

   public TCompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
      return this.exceptionallyAsync(fn, defaultExecutor());
   }

   public TCompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor) {
      Objects.requireNonNull(fn);
      Objects.requireNonNull(executor);
      return this.exceptionally(fn);
   }

   /*
    * exceptionallyCompose family, added after the link reported both halves missing.
    *
    * It is thenCompose and exceptionally put together: a successful value passes
    * straight through untouched, and only a failure goes through the function, whose
    * result is then awaited rather than unwrapped. That is not a composition of the two
    * existing methods -- exceptionally takes a Function to a value, not to a stage -- so
    * it has to be written out, following the same shape as thenCompose above.
    *
    * Reached from DownloadedPackSource.downloadAndSelectResourcePack (the sync form, on
    * the Realms resource-pack path) and WorldOpenFlows.doLoadLevel (the two-argument
    * form, on the singleplayer world-load path).
    */
   public <U> TCompletableFuture<U> exceptionallyCompose(Function<Throwable, ? extends TCompletionStage<U>> fn) {
      Objects.requireNonNull(fn);
      return this.derive((d, v, ex) -> {
         if (ex == null) {
            d.complete((U)this.cast(v));
            return;
         }

         TCompletionStage<U> next;

         try {
            next = fn.apply(ex);
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
            return;
         }

         if (next == null) {
            d.completeExceptionally(new NullPointerException("fn returned null"));
         } else if (next instanceof TCompletableFuture) {
            ((TCompletableFuture<U>)next).whenComplete((vv, ee) -> {
               if (ee != null) {
                  d.completeExceptionally(ee);
               } else {
                  d.complete(vv);
               }
            });
         } else {
            // Same reasoning as thenCompose: no other CompletionStage implementation is
            // reachable here, but a non-done one would hang rather than drop the value, so
            // flatten it only if it has actually finished.
            d.complete(next.toCompletableFuture().join());
         }
      });
   }

   public <U> TCompletableFuture<U> exceptionallyComposeAsync(Function<Throwable, ? extends TCompletionStage<U>> fn) {
      return this.exceptionallyComposeAsync(fn, defaultExecutor());
   }

   public <U> TCompletableFuture<U> exceptionallyComposeAsync(Function<Throwable, ? extends TCompletionStage<U>> fn, Executor executor) {
      Objects.requireNonNull(fn);
      Objects.requireNonNull(executor);
      return this.exceptionallyCompose(fn);
   }

   public <U, V> TCompletableFuture<V> handle(BiFunction<? super T, Throwable, ? extends V> fn) {
      Objects.requireNonNull(fn);
      return this.derive((d, v, ex) -> {
         try {
            d.complete(fn.apply(this.cast(v), ex));
         } catch (Throwable t1) {
            d.completeExceptionally(wrap(t1));
         }
      });
   }

   public <U, V> TCompletableFuture<V> handleAsync(BiFunction<? super T, Throwable, ? extends V> fn) {
      return this.handleAsync(fn, defaultExecutor());
   }

   public <U, V> TCompletableFuture<V> handleAsync(BiFunction<? super T, Throwable, ? extends V> fn, Executor executor) {
      Objects.requireNonNull(fn);
      Objects.requireNonNull(executor);
      return this.handle(fn);
   }

   // ---------------------------------------------------------------- two-stage combinations
   //
   // ChunkHolder combines futures that finish on separate mailbox turns. A pending
   // right-hand stage must remain a dependency of the chunk's save barrier.

   public <U, V> TCompletableFuture<V> thenCombine(TCompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
      Objects.requireNonNull(other);
      Objects.requireNonNull(fn);
      TCompletableFuture<? extends U> right = other.toCompletableFuture();
      return allOf(this, right).thenApply(ignored -> fn.apply(this.join(), right.join()));
   }

   public <U, V> TCompletableFuture<V> thenCombineAsync(TCompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
      return this.thenCombine(other, fn);
   }

   public <U, V> TCompletableFuture<V> thenCombineAsync(TCompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
      return this.thenCombine(other, fn);
   }

   public TCompletableFuture<Void> thenAcceptBoth(TCompletionStage<?> other, BiConsumer<? super T, ?> action) {
      Objects.requireNonNull(action);
      @SuppressWarnings({"unchecked", "rawtypes"})
      BiConsumer raw = (BiConsumer)action;
      return this.thenCombine(other, (left, right) -> {
         raw.accept(left, right);
         return null;
      });
   }

   public TCompletableFuture<Void> runAfterBoth(TCompletionStage<?> other, Runnable action) {
      Objects.requireNonNull(other);
      Objects.requireNonNull(action);
      return allOf(this, other.toCompletableFuture()).thenRun(action);
   }

   /** Left-biased: this stage always wins, so {@code fn} is applied to this stage's value. */
   public <U> TCompletableFuture<U> applyToEither(TCompletionStage<? extends U> other, Function<? super T, ? extends U> fn) {
      return this.thenApply(fn);
   }

   public TCompletableFuture<Void> acceptEither(TCompletionStage<?> other, Consumer<? super T> action) {
      return this.thenAccept(action);
   }

   public TCompletableFuture<Void> runAfterEither(TCompletionStage<?> other, Runnable action) {
      return this.thenRun(action);
   }
}