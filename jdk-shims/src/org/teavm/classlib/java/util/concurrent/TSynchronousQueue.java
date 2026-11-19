package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.SynchronousQueue}.
 *
 * <p>Only needed so ViaVersion's {@code ProtocolManagerImpl} constructor can be
 * linked under WASM-GC. That constructor hands this queue to a
 * {@link ThreadPoolExecutor} which the WASM-GC shim runs every task inline and
 * never queues onto, so the queue's hand-off behaviour is not observable.
 */
public class TSynchronousQueue<E> extends TLinkedBlockingQueue<E> {
	public TSynchronousQueue() {
	}

	public TSynchronousQueue(boolean fair) {
	}
}
