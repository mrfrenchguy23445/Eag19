package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.Closeable;
import java.util.List;

/*
 * java.nio.file.WatchService shim -- see jdk-shims/build.gradle.kts.
 *
 * WHAT THIS COSTS, PRECISELY
 * ==========================
 *
 * VirtualFileSystem is a synchronous path-to-bytes map. It cannot notify, so this
 * service never reports anything: poll() is always null.
 *
 * The one caller in the tree is PackSelectionScreen.Watcher, and it is built so that
 * a silent service is merely inert rather than wrong. Its pollForChanges loops while
 * watcher.poll() returns non-null and returns false when the loop never runs, so a
 * null poll means "no changes" -- the same answer it gives for an idle directory. The
 * screen therefore stops noticing packs dropped into the packs directory while it is
 * open, and still sees them when it is next opened. The refresh button and reopening
 * the screen both work; only the live tail is gone.
 *
 * If that ever needs to be real, it is a diff, not an interface change: record the
 * directory listing and each entry's BasicFileAttributes.lastModifiedTime at register
 * time, re-read it on poll, and emit ENTRY_CREATE / ENTRY_DELETE / ENTRY_MODIFY from
 * the difference. That is the whole mechanism. Nothing about the shapes here would
 * have to change, which is why they are the JDK's shapes rather than something
 * narrower that would need replacing later.
 */
public interface TWatchService extends Closeable {

	/**
	 * The next batch of changes, or null when there are none. Always null here -- see
	 * the note above.
	 */
	TWatchKey poll();

	@Override
	void close() throws java.io.IOException;
}