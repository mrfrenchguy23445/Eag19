package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/*
 * Reached from io.netty.util.internal.PlatformDependent and from
 * java.lang.Class.newInstance in the classlib's own reflection helper. Neither
 * ever contends under WASM-GC; see TLock.
 *
 * Port note (wasm_gc): net.minecraft.client.color.block.BlockTintCache guards its colour
 * caches with ReentrantReadWriteLock.readLock().lock()/unlock() and writeLock()..., and the
 * classlib's own TReentrantReadWriteLock carried neither accessor -- the link reported
 *
 *   Method java.util.concurrent.locks.ReentrantReadWriteLock.writeLock()...$WriteLock; was not found
 *   Method java.util.concurrent.locks.ReentrantReadWriteLock.readLock()...$ReadLock; was not found
 *
 * Both accessors now exist and return the JDK-shaped nested ReadLock / WriteLock. Each is a
 * plain Lock that delegates to a TReentrantLock, so Striped's readLock() wrapping and the
 * tint cache's lock()/unlock() calls all link. Everything is still a no-op: WASM-GC is
 * single threaded under a Web Worker and there is never contention to arbitrate.
 *
 * The nested types are static here rather than inner classes as in the JDK. Only the binary
 * name -- ReentrantReadWriteLock$ReadLock / $WriteLock -- is what a caller resolves, and no
 * caller constructs one directly, so the outer-instance field the JDK's inner classes carry
 * is dropped.
 */
public class TReentrantReadWriteLock {
	private final TReentrantLock readLock = new TReentrantLock();
	private final TReentrantLock writeLock = new TReentrantLock();
	private final ReadLock readerLock = new ReadLock();
	private final WriteLock writerLock = new WriteLock();
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

	public ReadLock readLock() {
		return this.readerLock;
	}

	public WriteLock writeLock() {
		return this.writerLock;
	}

	public boolean isFair() {
		return this.fair;
	}

	public static class ReadLock implements TLock {
		private final TReentrantLock delegate = new TReentrantLock();

		@Override
		public void lock() {
			this.delegate.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			this.delegate.lockInterruptibly();
		}

		@Override
		public boolean tryLock() {
			return this.delegate.tryLock();
		}

		@Override
		public boolean tryLock(long param0, TimeUnit param1) throws InterruptedException {
			return this.delegate.tryLock(param0, param1);
		}

		@Override
		public void unlock() {
			this.delegate.unlock();
		}
	}

	public static class WriteLock implements TLock {
		private final TReentrantLock delegate = new TReentrantLock();

		@Override
		public void lock() {
			this.delegate.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			this.delegate.lockInterruptibly();
		}

		@Override
		public boolean tryLock() {
			return this.delegate.tryLock();
		}

		@Override
		public boolean tryLock(long param0, TimeUnit param1) throws InterruptedException {
			return this.delegate.tryLock(param0, param1);
		}

		@Override
		public void unlock() {
			this.delegate.unlock();
		}
	}
}
