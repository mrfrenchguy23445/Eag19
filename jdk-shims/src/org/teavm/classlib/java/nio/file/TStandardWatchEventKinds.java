package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

/*
 * java.nio.file.StandardWatchEventKinds shim -- see jdk-shims/build.gradle.kts.
 *
 * The four standard kinds plus OVERFLOW, as in the JDK, because
 * PackSelectionScreen.Watcher.watchDir names three of them directly.
 *
 * OVERFLOW is not one of the three. It is here because the JDK declares it and a
 * consumer is entitled to compare against it, not because anything here can raise
 * it: with no notification mechanism there is no queue to overflow.
 */
public final class TStandardWatchEventKinds {

	public static final TWatchEvent.Kind<TPath> ENTRY_CREATE = new KindImpl<>("ENTRY_CREATE", TPath.class);
	public static final TWatchEvent.Kind<TPath> ENTRY_DELETE = new KindImpl<>("ENTRY_DELETE", TPath.class);
	public static final TWatchEvent.Kind<TPath> ENTRY_MODIFY = new KindImpl<>("ENTRY_MODIFY", TPath.class);
	public static final TWatchEvent.Kind<Object> OVERFLOW = new KindImpl<>("OVERFLOW", Object.class);

	private TStandardWatchEventKinds() {
	}

	/**
	 * A kind is compared by identity throughout the JDK -- callers write
	 * {@code event.kind() == StandardWatchEventKinds.ENTRY_CREATE} -- so identity is
	 * what has to be right, and toString has to be the declared name because it is
	 * what appears in a log line.
	 */
	private static final class KindImpl<T> implements TWatchEvent.Kind<T> {
		private final String name;
		private final Class<T> type;

		KindImpl(String name, Class<T> type) {
			this.name = name;
			this.type = type;
		}

		public String name() {
			return this.name;
		}

		public Class<T> type() {
			return this.type;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}