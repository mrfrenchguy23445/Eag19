/*
 * java.nio.file.Path shim -- see jdk-shims/build.gradle.kts.
 *
 * The WASM-GC classlib ships no java.nio.file at all, but the EAGLER-R3 fork's
 * classlib does ship a complete virtual filesystem (org.teavm.runtime.fs) which
 * java.io.File is already implemented on top of. So this is the same store the
 * game already sees through File, wrapped in the JDK interface.
 *
 * Only the single-provider case is modelled: every Path belongs to
 * FileSystems.getDefault(), there is no provider lookup, and the path syntax is
 * POSIX ('/') because VirtualFileSystem.canonicalize/isWindows report a browser
 * filesystem, never NTFS.
 */

package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/*
 * TPath extends TWatchable, as java.nio.file.Path extends java.nio.file.Watchable in
 * the JDK. That is not cosmetic: Watchable is what declares the abstract register
 * form, and the convenience overload below overrides nothing -- it is a default method
 * on Path itself, which is why it has to be declared on this interface rather than
 * inherited.
 */
public interface TPath extends Comparable<TPath>, Iterable<TPath>, TWatchable {

	static TPath of(String param0, String... param1) {
		return TPaths.get(param0, param1);
	}

	static TPath of(URI param0) {
		return TPaths.get(param0);
	}

	TFileSystem getFileSystem();

	boolean isAbsolute();

	TPath getRoot();

	TPath getFileName();

	TPath getParent();

	int getNameCount();

	TPath getName(int param0);

	TPath subpath(int param0, int param1);

	boolean startsWith(TPath param0);

	boolean startsWith(String param0);

	boolean endsWith(TPath param0);

	boolean endsWith(String param0);

	TPath normalize();

	TPath resolve(TPath param0);

	TPath resolve(String param0);

	TPath resolveSibling(TPath param0);

	TPath resolveSibling(String param0);

	TPath relativize(TPath param0);

	URI toUri();

	TPath toAbsolutePath();

	TPath toRealPath(TLinkOption... param0) throws IOException;

	File toFile();

	/**
	 * The JDK's abstract form. There is nothing to register: VirtualFileSystem has no
	 * notification, so the registration is recorded nowhere and the caller is handed
	 * the shared invalid key.
	 *
	 * An invalid key rather than null, and not an exception, because that is what a
	 * rejected registration is in the JDK -- a directory that is already being watched
	 * returns the existing key, and one that cannot be watched returns an invalid one.
	 * Returning the invalid key means the caller's reset() loop behaves: PackSelectionScreen
	 * .Watcher.pollForChanges resets each key it polls, and an invalid key resets to
	 * itself without queueing anything.
	 */
	@Override
	TWatchKey register(TWatchService param0, TWatchEvent.Kind<?>[] param1, TWatchEvent.Modifier... param2) throws IOException;

	/**
	 * The JDK's default overload, taking the kinds as varargs.
	 *
	 * This is the form the tree calls: PackSelectionScreen.Watcher.watchDir writes
	 *
	 *   path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
	 *
	 * with three separate arguments, which cannot bind to the array form -- a Kind is
	 * not a Kind[] -- so it binds here instead.
	 */
	default TWatchKey register(TWatchService param0, TWatchEvent.Kind<?>... param1) throws IOException {
		return this.register(param0, param1, new TWatchEvent.Modifier[0]);
	}

	@Override
	boolean equals(Object param0);

	@Override
	int hashCode();

	@Override
	String toString();

	@Override
	Iterator<TPath> iterator();

	@Override
	int compareTo(TPath param0);
}
