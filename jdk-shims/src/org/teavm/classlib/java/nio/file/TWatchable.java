package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;

/*
 * java.nio.file.Watchable shim -- see jdk-shims/build.gradle.kts.
 *
 * The parent Path extends in the JDK, and the reason that matters here is that it
 * carries the abstract register(WatchService, Kind<?>[], Modifier...). Watchable is
 * what lets a method take "something that can be watched" without naming Path.
 *
 * Modelled so that TPath can extend it, as the JDK's does, and so that the two
 * register forms land on the right declarations -- see TPath.
 */
public interface TWatchable {

	/**
	 * The JDK's abstract form, taking the kinds as an array. TPath's default varargs
	 * overload is what the tree actually calls, because
	 * PackSelectionScreen.Watcher.watchDir passes ENTRY_CREATE, ENTRY_DELETE and
	 * ENTRY_MODIFY as three separate arguments rather than wrapping them in a new
	 * Kind[] { ... }. Both forms exist for the same reason in the JDK, and both are
	 * declared here for the same reason.
	 */
	TWatchKey register(TWatchService param0, TWatchEvent.Kind<?>[] param1, TWatchEvent.Modifier... param2) throws IOException;
}