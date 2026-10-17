package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.Delayed}.
 *
 * <p>Absent from the TeaVM classlib, and absent from {@code jdk-shims} until netty needed the
 * whole chain. See {@link TScheduledExecutorService} for why that was the last link rather than
 * this one.
 *
 * <p>This is the JDK's marker-plus-one-method interface, reproduced exactly:
 * {@code Delayed extends Comparable<Delayed>} and declares {@code getDelay(TimeUnit)}. Netty's
 * {@code io.netty.util.concurrent.ScheduledFuture} extends it, so without this name a call to
 * {@code getDelay} through a scheduled future could not be resolved either.
 *
 * <p><b>What it means here.</b> Nothing, and deliberately so. {@code getDelay} asks a task how much
 * longer it should wait before running, which presupposes a scheduler that holds tasks and runs
 * them later. Every executor on this target runs its work inline, immediately, on the calling
 * thread -- see {@link TExecutors}, which documents that decision at length -- so there is no
 * pending queue and no delay to report. Any implementation that answered this honestly would say
 * zero, and any that answered with a real number would be describing a queue that does not exist.
 * The declaration is here for the type, not for the semantics.
 */
public interface TDelayed extends Comparable<TDelayed> {
    long getDelay(TimeUnit unit);
}
