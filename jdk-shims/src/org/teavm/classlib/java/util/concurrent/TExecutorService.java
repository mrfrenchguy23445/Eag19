package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.Collection;
import java.util.List;

/**
 * Shim for {@code java.util.concurrent.ExecutorService}.
 *
 * <p>Absent from the TeaVM classlib, which models {@link Executor} but not this
 * sub-interface. It is named by Guava's {@code AbstractListeningExecutorService} and by
 * {@code net.minecraft.server.network.TextFilterClient}, and it is what
 * {@link AbstractExecutorService} implements.
 *
 * <p><b>Subset.</b> All eight lifecycle methods are declared, but none of them can mean
 * anything here: an executor service that owns no threads has nothing to shut down, no
 * queued work that outlives the caller, and no termination to wait for. The declarations are
 * kept so that {@code throws} clauses and call sites compile unchanged.
 *
 * <p>The JDK (19+) also has {@code extends AutoCloseable} with a default {@code close()}.
 * That is deliberately omitted: this shim models the Java 17 shape, which is what the rest
 * of the port is compiled against.
 */
public interface TExecutorService extends Executor {
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> org.teavm.classlib.java.util.concurrent.TFuture<T> submit(Callable<T> task);

    <T> org.teavm.classlib.java.util.concurrent.TFuture<T> submit(Runnable task, T result);

    org.teavm.classlib.java.util.concurrent.TFuture<?> submit(Runnable task);

    <T> List<org.teavm.classlib.java.util.concurrent.TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;

    <T> List<org.teavm.classlib.java.util.concurrent.TFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TTimeoutException;
}