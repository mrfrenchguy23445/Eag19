package org.teavm.classlib.java.util.concurrent.locks;

/*
 * Reached from io.netty.util.internal.PlatformDependent and from
 * java.lang.Class.newInstance in the classlib's own reflection helper. Neither
 * ever contends under WASM-GC; see TLock.
 */
public class TReentrantReadWriteLock {
	private final TReentrantLock readLock = new TReentrantLock();
	private final TReentrantLock writeLock = new TReentrantLock();
	private final boolean fair;

	public TReentrantReadWriteLock() {
		this(false);
	}

	public TReentrantReadWriteLock(boolean param0) {
		this.fair = param0;
	}

	public TLock getReadLock() {
		return this.readLock;
	}

	public TLock getWriteLock() {
		return this.writeLock;
	}

	public boolean isFair() {
		return this.fair;
	}
}
