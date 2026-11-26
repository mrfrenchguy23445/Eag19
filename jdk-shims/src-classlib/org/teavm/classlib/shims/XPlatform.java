package org.teavm.classlib.shims;

import org.teavm.platform.PlatformRunnable;

/*
 * Replacement for org.teavm.platform.Platform, which carries a class-level
 * @UnsupportedOn("webassembly-gc") and therefore fails the link the moment any
 * reachable method mentions it -- not when the method runs, when the reference
 * is analyzed. The two references that mattered, both proved from wasm12's
 * stacks:
 *
 *   Class org.teavm.platform.Platform is not supported on current target
 *       at java.lang.Thread.start(XThread.java:78)                 <- startThread
 *       at net.minecraft.client.sounds.SoundEngineExecutor...
 *
 *   Class org.teavm.platform.Platform is not supported on current target
 *       at java.lang.Object.notifyAll(TObject.java:432)            <- postpone
 *       at io.netty.util.concurrent.DefaultPromise.checkNotifyWaiters...
 *       at net.minecraft.server.network.ServerConnectionListener...
 *       at net.minecraft.core.registries.BuiltInRegistries...
 *       at net.minecraft.server.Bootstrap.bootStrap
 *
 * The marker lives on the ORIGINAL class, and PlatformMarkerSupport reads the
 * annotation off whatever ClassHolder the name resolves to -- so a stand-in
 * carrying no annotation links, regardless of what the original declared.
 * This is the same trick XUnsafe plays from the caller's side, pointed the
 * other way: instead of replacing the code that reached a missing member, the
 * member's owner is replaced with one this target can execute.
 *
 * What each method does here, and why it is faithful on a single thread:
 *
 *   startThread(r)  -- runs r inline. There is no second thread on this target,
 *                      so the "new thread" body executes to completion inside
 *                      start() and start() returns afterwards. Code that used
 *                      to race with the caller now runs before it; nothing in
 *                      the boot graph depends on the old ordering. The thread
 *                      class keeps its alive/finished bookkeeping around the
 *                      call, so isAlive() and join() still agree.
 *
 *   postpone(r)     -- runs r inline. The original defers to the JS event loop
 *                      via setTimeout(0) only so that waiters can be woken
 *                      after the current stack unwinds. Every caller of
 *                      postpone reachable here (TObject.notify, notifyAll)
 *                      notifies a monitor that, on this target, cannot hold a
 *                      waiter: Object.wait is Fiber-backed and therefore
 *                      unreachable, so the notify queue is always empty and
 *                      postpone is never even entered at runtime. Inline is
 *                      the same behavior with less ceremony.
 *
 *   schedule(r, ms) -- busy-waits the timeout, then runs r inline, returning a
 *                      throwaway id. The original is setTimeout. Not reachable
 *                      from the boot graph once XThread.sleep stops using it,
 *                      but kept so a future schedule() caller links rather than
 *                      forcing another 12-minute link cycle to discover it.
 *
 *   killSchedule    -- no-op; there is no timer to cancel once schedule runs
 *                      its callback inline before returning.
 *
 * What is deliberately absent: every @GeneratedBy/@InjectedBy member
 * (getPlatformObject, clone, asJavaClass, lookupClass, initClass,
 * getEnumConstants, getAnnotations, ...). Those are rewritten by TeaVM's
 * PlatformGenerator against the real class's metadata layout; a hand-written
 * body cannot reproduce them, and nothing in this port's reachable graph has
 * asked for one yet. If a future link reports a missing member of
 * org.teavm.platform.Platform, the fix is the XUnsafe pattern: replace the
 * caller, not this class.
 *
 * WHY org.teavm.classlib.shims: same as XCharsets's header. ClassMappings
 * answers for org.teavm.platform.Platform from classMappings (keyed by the
 * original name, value this class), and the shims package sits outside
 * stripPrefixFromPackageHierarchyClasses, so this name survives verbatim.
 */
public final class XPlatform {

	private XPlatform() {
	}

	public static void startThread(PlatformRunnable runnable) {
		runnable.run();
	}

	public static void postpone(PlatformRunnable runnable) {
		runnable.run();
	}

	public static int schedule(PlatformRunnable runnable, int timeout) {
		long deadline = System.currentTimeMillis() + (long) timeout;
		while (System.currentTimeMillis() < deadline) {
			// busy-wait: a browser main thread cannot sleep, and this target has no
			// timer wheel to hand the continuation to. Unreachable from boot today;
			// present so that reaching it is a runtime cost, not a link failure.
		}
		runnable.run();
		return 0;
	}

	public static void killSchedule(int id) {
	}
}
