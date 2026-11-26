package org.teavm.classlib.java.util.concurrent;

/**
 * Shim for {@code java.util.concurrent.ScheduledFuture}.
 *
 * <p>Absent from the TeaVM classlib, like {@link TDelayed} and
 * {@link TScheduledExecutorService}, and pulled in by the same change: netty's
 * {@code io.netty.util.concurrent.ScheduledFuture} extends it, so the name has to resolve for
 * netty's {@code EventLoop.schedule} family to be linkable at all.
 *
 * <p>The JDK's version is empty too -- {@code ScheduledFuture<V> extends Delayed, Future<V>},
 * with no members of its own -- so there is nothing to reproduce. The interesting content is
 * entirely in the two superinterfaces, and both are here: {@link TDelayed} for the delay, and
 * {@link TFuture} for the result.
 *
 * <p>As with {@link TDelayed}, the interface exists for its type. A scheduled future returned
 * from one of these executors is already complete by the time it is handed back, because the
 * work ran inline; {@code TFuture}'s own note covers what {@code get()} does in that situation.
 */
public interface TScheduledFuture<V> extends TDelayed, TFuture<V> {
}
