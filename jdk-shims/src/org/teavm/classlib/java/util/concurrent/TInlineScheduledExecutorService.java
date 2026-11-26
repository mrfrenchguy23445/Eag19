package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Inline {@link TScheduledExecutorService} for WASM-GC.
 *
 * <p>Delays cannot be honoured without threads or timers, so scheduled work is
 * run immediately. This is the same substitution already made for every other
 * executor in this shim set.
 */
public class TInlineScheduledExecutorService extends TAbstractExecutorService implements TScheduledExecutorService {

	@Override
	public void execute(Runnable command) {
		command.run();
	}

	@Override
	public TScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		command.run();
		TInlineScheduledFuture<Object> future = new TInlineScheduledFuture<>();
		future.complete(null);
		return future;
	}

	@Override
	public <V> TScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		TInlineScheduledFuture<V> future = new TInlineScheduledFuture<>();
		try {
			future.complete(callable.call());
		} catch (Throwable t) {
			future.completeExceptionally(t);
		}
		return future;
	}

	@Override
	public TScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return schedule(command, initialDelay, unit);
	}

	@Override
	public TScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return schedule(command, initialDelay, unit);
	}
}
