package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.TimeUnit;

/** A completed scheduled future for the WASM-GC inline scheduler. */
public class TInlineScheduledFuture<V> extends TCompletableFuture<V> implements TScheduledFuture<V> {

	@Override
	public long getDelay(TimeUnit unit) {
		return 0L;
	}

	@Override
	public int compareTo(TDelayed other) {
		return 0;
	}
}
