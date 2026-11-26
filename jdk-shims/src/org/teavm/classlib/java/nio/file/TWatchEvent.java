package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

/*
 * java.nio.file.WatchEvent shim -- see jdk-shims/build.gradle.kts.
 *
 * The backing store, org.teavm.runtime.fs.VirtualFileSystem, has no notification
 * mechanism of any kind: it is a synchronous map from path to bytes. So this family
 * (WatchEvent, WatchKey, WatchService, StandardWatchEventKinds) exists as shapes
 * only, and the service never reports a change. See TWatchService for what that
 * costs and TPath.register for how a registration is answered.
 *
 * The declarations are still the real ones. Kind and Modifier are nested interfaces
 * here exactly as they are nested in the JDK, which is what makes the call sites in
 * PackSelectionScreen.Watcher.pollForChanges type-check unchanged: it declares
 * WatchEvent<?> and compares event.kind() against StandardWatchEventKinds.ENTRY_CREATE.
 */
public interface TWatchEvent<T> {

	/**
	 * The kind of change. {@code StandardWatchEventKinds} supplies the four the JDK
	 * does, plus OVERFLOW.
	 */
	interface Kind<T> {
		String name();

		Class<T> type();
	}

	/**
	 * A modifier on what is reported. The JDK has none, which is why the call sites
	 * never pass any -- they register with only kinds.
	 */
	interface Modifier {
	}

	TPath watchable();

	Kind<T> kind();

	int count();

	T context();

	boolean isDirectory();

	TPath resolve(TPath param0);
}