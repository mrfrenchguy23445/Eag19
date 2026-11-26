package org.teavm.classlib.java.util.concurrent.locks;

/*
 * Reached from the blocking-queue paths in TCompletableFuture and the sound
 * system. There is no scheduler to park against in a Web Worker, so unpark is
 * the only call with an effect and it has none.
 */
public final class TLockSupport {

	private TLockSupport() {
	}

	public static void park() {
	}

	/**
	 * The blocker-carrying forms. Both are the same no-op as park(): there is no
	 * scheduler to block against in a Web Worker, so a park cannot be made to wait
	 * and unpark has nothing to release.
	 *
	 * Reached from java.util.concurrent's own machinery -- TCompletableFuture's blocking
	 * queue paths park with a timeout -- which is why they cannot simply be assumed
	 * absent. Kept as separate methods rather than folded into park() because the
	 * parameter types differ and Java would not substitute one for the other.
	 */
	public static void park(Object param0) {
	}

	public static void parkNanos(Object param0, long param1) {
	}

	public static void parkUntil(Object param0, long param1) {
	}

	public static void parkNanos(long param0) {
	}

	public static void parkUntil(long param0) {
	}

	public static void unpark(Thread param0) {
	}

	public static boolean isHeldByCurrentThread() {
		return true;
	}
}
