package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/*
 * Reached from net.minecraft.client.Minecraft.<init> (the DownloadedPackSource
 * filesystem lock) and from Minecraft.clearLevel. WASM-GC has no threads, so
 * tryLock always succeeds and the hold count stays at zero; see TLock.
 */
public class TReentrantLock implements TLock {
	private final boolean fair;

	public TReentrantLock() {
		this(false);
	}

	public TReentrantLock(boolean param0) {
		this.fair = param0;
	}

	public boolean isFair() {
		return this.fair;
	}

	@Override
	public void lock() {
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
	}

	@Override
	public boolean tryLock() {
		return true;
	}

	@Override
	public boolean tryLock(long param0, TimeUnit param1) throws InterruptedException {
		return true;
	}

	@Override
	public void unlock() {
	}

	public boolean isLocked() {
		return false;
	}

	public int getHoldCount() {
		return 0;
	}

	public boolean isHeldByCurrentThread() {
		return false;
	}
}
