package org.teavm.classlib.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/*
 * Reached from io.netty.channel.nio.NioEventLoopGroup.<init>, which hands its
 * selector loop a Lock. WASM-GC is single-threaded under a Web Worker, so
 * there is never contention to arbitrate and every implementation here is
 * unconditional.
 */
public interface TLock {

	void lock();

	void lockInterruptibly() throws InterruptedException;

	boolean tryLock();

	boolean tryLock(long param0, TimeUnit param1) throws InterruptedException;

	void unlock();
}
