package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.Future}.
 *
 * <p>Absent from the TeaVM classlib: this classlib models locks and queues but not the
 * future abstraction, and {@link CompletableFuture} -- which <em>is</em> needed, by 159
 * files -- implements {@code Future} in the real JDK. Without this type the shimmed
 * {@code CompletableFuture} could not declare its own supertype, and every
 * {@code CompletableFuture f = ...; Future f2 = f;} assignment would fail to link.
 *
 * <p><b>Subset note.</b> All six JDK methods are declared, but the two timed variants
 * cannot honour their contract: there are no threads, so nothing can block and no timeout
 * can elapse. The shimmed {@link CompletableFuture} implements them by completing
 * immediately (or, if the future is still pending, failing immediately -- see there). The
 * declarations are kept anyway because {@code throws} clauses on this interface are what
 * callers compile against; dropping them would change what compiles.
 */
public interface TFuture<V> {
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TTimeoutException;
}