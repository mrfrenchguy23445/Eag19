package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.ScheduledExecutorService}.
 *
 * <p><b>Why this exists, which is the least obvious thing in the shim set.</b> The classlib models
 * {@link Executor} and nothing above it, and {@code jdk-shims} had filled the gap up to
 * {@link TExecutorService} for Guava. Netty then needed the gap filled one level further, and the
 * symptom looked unrelated. With {@code ScheduledExecutorService} unresolvable, netty's
 *
 * <pre>public interface EventExecutorGroup extends java.util.concurrent.ScheduledExecutorService,
 *                                                 java.lang.Iterable&lt;EventExecutor&gt;
 * </pre>
 *
 * had no resolvable supertype at all -- not even {@code Executor}. Every member netty reached
 * through that interface therefore failed, and the linker reported each one against the netty
 * interface that had inherited it rather than against the missing JDK type:
 *
 * <pre>Method io.netty.util.concurrent.EventExecutor.isTerminated()Z was not found
 *     at io.netty.util.concurrent.MultithreadEventExecutorGroup.&lt;init&gt;(...:98)
 * Method io.netty.util.concurrent.EventExecutor.awaitTermination(JLjava/util/concurrent/TimeUnit;)Z was not found
 *     at io.netty.util.concurrent.MultithreadEventExecutorGroup.&lt;init&gt;(...:99)
 * Method io.netty.util.concurrent.EventExecutor.execute(Ljava/lang/Runnable;)V was not found
 *     at io.netty.util.concurrent.DefaultPromise.safeExecute(...)
 * </pre>
 *
 * All three, on a path the client cannot avoid: {@code ServerConnectionListener} builds a
 * {@code LocalEventLoopGroup} for the integrated server, and {@code LocalEventLoopGroup} reaches
 * {@code DefaultEventLoopGroup} which reaches {@code MultithreadEventExecutorGroup}, whose
 * constructor walks its children calling {@code isTerminated} and {@code awaitTermination}.
 * The name that fixed it is not in any of those frames.
 *
 * <p>Note what this is and is not. It does not add scheduling. It restores the inheritance chain
 * so that netty's own executor types can be resolved, which is all the linker was asking for.
 *
 * <p><b>What survives.</b> The four {@code schedule} methods are declared with the JDK's exact
 * signatures. They are not implemented here, because nothing implements this interface on this
 * target: {@link TAbstractExecutorService} implements {@link TExecutorService} only, and
 * {@link TExecutors} has no {@code newSingleThreadScheduledExecutor}. Guava's
 * {@code AbstractScheduledService} is the one place the game names this type, and it is not
 * reachable from the client, so nothing needs the factory.
 *
 * <p><b>What cannot.</b> A delay of any kind. Inline execution means a scheduled task runs now,
 * so these four methods have no meaning on this target -- the same reason {@link TDelayed} has
 * no answer for {@code getDelay}, and the same reason every other executor here is direct.
 */
public interface TScheduledExecutorService extends TExecutorService {
    TScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    <V> TScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    TScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    TScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
