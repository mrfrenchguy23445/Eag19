package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.ThreadFactory}.
 *
 * <p>Absent from the TeaVM classlib, and named by Guava's
 * {@code AbstractScheduledService.ThreadFactoryImpl}, {@code JdkFutureAdapters} and
 * {@code MoreExecutors}, all of which pass a factory to {@link Executors} factories.
 *
 * <p>Kept as a real interface rather than being erased, because it appears in method
 * signatures in those Guava classes and in {@code TextFilterClient}. Erasing it would mean
 * editing every one of them.
 */
public interface TThreadFactory {
    Thread newThread(Runnable r);
}