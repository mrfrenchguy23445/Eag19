package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.util.Collections;
import java.util.List;

/*
 * java.nio.file.WatchKey shim -- see jdk-shims/build.gradle.kts.
 *
 * The registration handle Path.register hands back. Every key here is immediately
 * invalid and never yields an event, which is what makes a WatchService that cannot
 * notify still safe to hand out: the caller's loop terminates on poll() returning
 * null rather than spinning on a live key.
 *
 * InvalidKey is a single shared instance, as in the JDK, so that identity comparison
 * against WatchKey.isValid() and against the return of reset() behaves as callers
 * expect.
 */
public interface TWatchKey {

	/**
	 * The shared invalid key: the registration either failed or was cancelled, and it
	 * will never become valid again.
	 */
	TWatchKey INVALID_KEY = new Invalid();

	/**
	 * The changes queued for this registration. Always empty here, and reset() below is
	 * what keeps that from looking like an error to a caller that loops on it.
	 */
	List<TWatchEvent<?>> pollEvents();

	/**
	 * Re-arms the key, reporting whether it is still usable.
	 *
	 * <p>The return type here is load-bearing and was wrong until it was checked against
	 * {@code javap java.nio.file.WatchKey}: the JDK declares {@code boolean reset()}, and an
	 * earlier version of this shim returned {@code TWatchKey} instead. A shim is only
	 * allowed to be a subset of the real class, never a variant of it -- returning a
	 * different type does not type-check and then fail to link, it fails to link on its own,
	 * and it did: "Method java.nio.file.WatchKey.reset()Z was not found".
	 *
	 * <p>The value is the validity directly, which is what the JDK returns here: a key that
	 * has been cancelled or whose watchable has gone away reports false, and every key from
	 * this shim is already invalid.
	 */
	boolean reset();

	boolean isValid();

	/**
	 * The registration this key came from, as a {@link TWatchable} rather than as the
	 * concrete {@link TPath}. The JDK declares {@code Watchable watchable()}; returning the
	 * narrower {@code Path} is a covariant narrowing that TeaVM does not accept, for the
	 * same reason as reset() above.
	 */
	TWatchable watchable();

	void cancel();

	/**
	 * The shared invalid key. Package-private class rather than an anonymous one so
	 * that it cannot be instantiated from outside, matching the JDK's guarantee that
	 * INVALID_KEY is the only invalid key there is.
	 */
	final class Invalid implements TWatchKey {
		Invalid() {
		}

		public List<TWatchEvent<?>> pollEvents() {
			return Collections.emptyList();
		}

		public boolean reset() {
			return false;
		}

		public boolean isValid() {
			return false;
		}

		public TWatchable watchable() {
			return null;
		}

		public void cancel() {
		}

		@Override
		public String toString() {
			return "closed key";
		}
	}
}