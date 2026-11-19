package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

/**
 * Shim for {@code java.util.concurrent.ThreadPoolExecutor}.
 *
 * <p>ViaVersion's protocol manager builds one of these for mapping-data
 * loading. WASM-GC has no threads, so every submitted task runs inline on the
 * submitting thread. The pool sizing arguments and the queue are accepted to
 * keep the real signature linkable, then ignored.
 */
public class TThreadPoolExecutor extends TAbstractExecutorService {

	public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
	}

	public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
	}

	public void allowCoreThreadTimeOut(boolean value) {
	}

	public void execute(Runnable command) {
		command.run();
	}
}
