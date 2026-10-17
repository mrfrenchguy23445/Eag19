package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

/**
 * The single Path implementation. Paths are held as a normalized-syntax string
 * ("/" separated, duplicate and trailing slashes collapsed, "." and ".." left
 * alone) and are only collapsed to a concrete location when they reach the store.
 *
 * That mirrors the JDK closely enough for Minecraft, with one deliberate
 * difference: the virtual filesystem has no working directory, so
 * toAbsolutePath() prefixes VirtualFileSystem.getUserDir() rather than asking the
 * OS, and a relative Path keeps resolving against that same base for its whole
 * life instead of tracking a mutable per-thread cwd.
 */
public final class TEaglerPath implements TPath {

	final String path;

	TEaglerPath(String param0) {
		this.path = param0;
	}

	static VirtualFileSystem vfs() {
		return VirtualFileSystemProvider.getInstance();
	}

	/**
	 * Collapses "." and ".." lexically, optionally anchoring a relative path at the user dir.
	 *
	 * <p>Split out from {@link #resolved} because {@link #normalize} must NOT anchor: it is a
	 * purely lexical operation in the JDK, and returning an absolute path from it would
	 * silently change what a relative path means -- "a/b" would stop being usable as a path
	 * relative to whatever the caller set as the user dir.
	 *
	 * @param var1 whether a relative path should be anchored at the user directory
	 */
	String collapsed(boolean var1) {
		String var2 = this.path;
		boolean var3 = var2.startsWith("/");
		Deque<String> var4 = new ArrayDeque<>();
		for (String var5 : var2.split("/")) {
			if (var5.isEmpty() || var5.equals(".")) {
				continue;
			}

			if (var5.equals("..")) {
				// Only collapse a ".." that has something to remove. A leading ".." on a
				// relative path has to survive, otherwise ".." would silently become "."
				// and a relative path could never climb above its base.
				if (!var4.isEmpty() && !var4.peekLast().equals("..")) {
					var4.removeLast();
				} else if (!var3) {
					var4.addLast("..");
				}

				continue;
			}

			var4.addLast(var5);
		}

		StringBuilder var6 = new StringBuilder();
		if (var3) {
			var6.append('/');
		} else if (var1) {
			String var7 = vfs().getUserDir();
			if (var7 != null && !var7.isEmpty()) {
				var6.append(TEaglerFileSystem.normalizeSyntax(var7));
				if (var6.charAt(var6.length() - 1) != '/') {
					var6.append('/');
				}
			}
		}

		for (String var8 : var4) {
			var6.append(var8).append('/');
		}

		if (var6.length() > 1 && var6.charAt(var6.length() - 1) == '/') {
			var6.setLength(var6.length() - 1);
		}

		return var6.toString();
	}

	/** Collapses "." and ".." lexically and anchors a relative path at the user dir. */
	String resolved() {
		return this.collapsed(true);
	}

	VirtualFile vf() {
		return vfs().getFile(this.resolved());
	}

	@Override
	public TFileSystem getFileSystem() {
		return TFileSystems.getDefault();
	}

	@Override
	public boolean isAbsolute() {
		return this.path.startsWith("/");
	}

	@Override
	public TPath getRoot() {
		return this.path.startsWith("/") ? new TEaglerPath("/") : null;
	}

	/**
	 * The last element as a RELATIVE path, or null for the root.
	 *
	 * <p>Relative even when this path is absolute: that is the JDK contract, and it is what
	 * lets {@code getName(0)} and {@code subpath(...)} be composed without ever producing an
	 * absolute path the caller did not ask for.
	 */
	@Override
	public TPath getFileName() {
		if (this.path.equals("/") || this.path.isEmpty()) {
			return null;
		}

		int var1 = this.path.lastIndexOf('/');
		return var1 == -1 ? this : new TEaglerPath(this.path.substring(var1 + 1));
	}

	/** The containing directory, or null when there is none -- including for the root itself. */
	@Override
	public TPath getParent() {
		int var1 = this.path.lastIndexOf('/');
		if (var1 <= 0) {
			// No separator: a bare name, which has no parent. var1 == 0 means the path *is*
			// the root, and the JDK reports the root's parent as null too.
			return null;
		}

		return new TEaglerPath(this.path.substring(0, var1));
	}

	List<String> names() {
		List<String> var1 = new ArrayList<>();
		if (this.path.equals("/")) {
			return var1;
		}

		for (String var2 : this.path.split("/")) {
			if (!var2.isEmpty()) {
				var1.add(var2);
			}
		}

		return var1;
	}

	@Override
	public int getNameCount() {
		return this.names().size();
	}

	/**
	 * One element by index, always as a RELATIVE path.
	 *
	 * <p>The JDK documents this explicitly: {@code getName} returns a relative path even when
	 * the receiver is absolute, so {@code Paths.get("/a/b").getName(0)} is {@code a}, not
	 * {@code /a}. Reinstating the receiver's leading slash here would make every caller that
	 * walks a tree and then writes the name somewhere else silently target the filesystem root.
	 */
	@Override
	public TPath getName(int param0) {
		List<String> var1 = this.names();
		if (param0 < 0 || param0 >= var1.size()) {
			throw new IllegalArgumentException("Invalid name index " + param0);
		}

		return new TEaglerPath(var1.get(param0));
	}

	/** A contiguous run of names, always RELATIVE, per the same JDK contract as getName. */
	@Override
	public TPath subpath(int param0, int param1) {
		List<String> var2 = this.names();
		if (param0 < 0 || param1 > var2.size() || param0 >= param1) {
			throw new IllegalArgumentException("Invalid subpath range " + param0 + ".." + param1);
		}

		StringBuilder var3 = new StringBuilder();
		for (int var4 = param0; var4 < param1; ++var4) {
			if (var3.length() > 0) {
				var3.append('/');
			}

			var3.append(var2.get(var4));
		}

		return new TEaglerPath(var3.toString());
	}

	@Override
	public boolean startsWith(TPath param0) {
		String var1 = this.resolved();
		String var2 = e(param0).resolved();
		return var1.equals(var2) || var1.startsWith(var2 + "/");
	}

	@Override
	public boolean startsWith(String param0) {
		return this.startsWith(TFileSystems.getDefault().getPath(param0));
	}

	@Override
	public boolean endsWith(TPath param0) {
		String var1 = e(param0).resolved();
		String var2 = this.resolved();
		if (var1.equals(var2)) {
			return true;
		}

		if (!var2.endsWith(var1)) {
			return false;
		}

		int var3 = var2.length() - var1.length();
		return var3 > 0 && var2.charAt(var3 - 1) == '/';
	}

	@Override
	public boolean endsWith(String param0) {
		return this.endsWith(TFileSystems.getDefault().getPath(param0));
	}

	/**
	 * Collapses redundant separators and "." / ".." lexically, without touching the filesystem.
	 *
	 * <p>Deliberately does NOT anchor at the user directory. The JDK's normalize() is purely
	 * lexical and preserves the receiver's absoluteness, so normalizing "a/b" must still return
	 * a relative path. This previously had two identical branches, both returning the anchored
	 * form -- so normalize() quietly promoted every relative path to absolute, which is a
	 * different path, not a tidier one.
	 */
	@Override
	public TPath normalize() {
		return new TEaglerPath(this.collapsed(false));
	}

	@Override
	public TPath resolve(TPath param0) {
		if (param0.isAbsolute()) {
			return param0;
		}

		String var1 = e(param0).path;
		if (this.path.endsWith("/")) {
			return new TEaglerPath(this.path + var1);
		}

		return new TEaglerPath(this.path + "/" + var1);
	}

	@Override
	public TPath resolve(String param0) {
		return this.resolve(TFileSystems.getDefault().getPath(param0));
	}

	@Override
	public TPath resolveSibling(TPath param0) {
		TPath var1 = this.getParent();
		return var1 == null ? param0 : var1.resolve(param0);
	}

	@Override
	public TPath resolveSibling(String param0) {
		return this.resolveSibling(TFileSystems.getDefault().getPath(param0));
	}

	@Override
	public TPath relativize(TPath param0) {
		List<String> var1 = this.names();
		List<String> var2 = e(param0).names();
		int var3 = 0;
		while (var3 < var1.size() && var3 < var2.size() && var1.get(var3).equals(var2.get(var3))) {
			++var3;
		}

		StringBuilder var4 = new StringBuilder();
		for (int var5 = var3; var5 < var1.size(); ++var5) {
			var4.append("../");
		}

		for (int var6 = var3; var6 < var2.size(); ++var6) {
			if (var6 > var3) {
				var4.append('/');
			}

			var4.append(var2.get(var6));
		}

		return TFileSystems.getDefault().getPath(var4.length() == 0 ? "" : var4.substring(0, var4.length() - 1));
	}

	@Override
	public URI toUri() {
		try {
			return new URI("file", null, this.path.isEmpty() ? "/" : this.path, null);
		} catch (java.net.URISyntaxException var1) {
			throw new IllegalArgumentException("Cannot build a file: URI from " + this.path, var1);
		}
	}

	@Override
	public TPath toAbsolutePath() {
		return this.isAbsolute() ? this : new TEaglerPath(this.resolved());
	}

	@Override
	public TPath toRealPath(TLinkOption... param0) throws IOException {
		return this.normalize();
	}

	@Override
	public File toFile() {
		return new File(this.resolved());
	}

	/**
	 * There is nothing to register, because VirtualFileSystem cannot notify. The
	 * registration is dropped and the caller gets the shared invalid key.
	 *
	 * Reached from PackSelectionScreen.Watcher.watchDir on the client's resource-pack
	 * screen, which is why this and the WatchService shape exist at all. The invalid key
	 * rather than an exception is what keeps that screen working: its poll loop reads a
	 * null poll as "nothing changed", and an invalid key resets to itself without
	 * queueing an event. See TWatchService for what is lost.
	 */
	@Override
	public TWatchKey register(TWatchService param0, TWatchEvent.Kind<?>[] param1, TWatchEvent.Modifier... param2) throws IOException {
		return TWatchKey.INVALID_KEY;
	}

	/**
	 * Compares the resolved form, not the stored one, so that "a/./b" and "a/b"
	 * are the same path -- which is what the JDK promises and what Minecraft relies
	 * on when it puts a normalized Path into a map keyed by Paths.get(...) elsewhere.
	 */
	@Override
	public boolean equals(Object param0) {
		return param0 instanceof TEaglerPath && this.resolved().equals(((TEaglerPath)param0).resolved());
	}

	@Override
	public int hashCode() {
		return this.resolved().hashCode();
	}

	@Override
	public String toString() {
		return this.path;
	}

	@Override
	public Iterator<TPath> iterator() {
		List<TPath> var1 = new ArrayList<>();

		for (String var2 : this.names()) {
			var1.add(new TEaglerPath(var2));
		}

		return var1.iterator();
	}

	@Override
	public int compareTo(TPath param0) {
		return this.resolved().compareTo(e(param0).resolved());
	}

	/**
	 * The only Path implementation exists, so any Path reaching this class came
	 * from EaglerFileSystem and is an EaglerPath. Anything else would mean a second
	 * provider had been installed, which FileSystems does not allow.
	 */
	static TEaglerPath e(TPath param0) {
		return (TEaglerPath)param0;
	}

	boolean exists() {
		VirtualFile var1 = this.vf();
		return var1 != null && (var1.isFile() || var1.isDirectory());
	}

	VirtualFile parentVf() {
		int var1 = this.resolved().lastIndexOf('/');
		return vfs().getFile(var1 <= 0 ? "/" : this.resolved().substring(0, var1));
	}

	String name() {
		String var1 = this.resolved();
		int var2 = var1.lastIndexOf('/');
		return var2 == -1 ? var1 : var1.substring(var2 + 1);
	}

	VirtualFileAccessor open(boolean param0, boolean param1, boolean param2) throws IOException {
		VirtualFile var1 = this.vf();
		if (var1 == null || !var1.isFile()) {
			throw new NoSuchFileException(this.resolved());
		}

		return var1.createAccessor(param0, param1, param2);
	}

	/** Directory listing as Paths, or null when this is not a directory. */
	List<TPath> listChildren() {
		VirtualFile var1 = this.vf();
		if (var1 == null || !var1.isDirectory()) {
			return null;
		}

		String[] var2 = var1.listFiles();
		List<TPath> var3 = new ArrayList<>();
		if (var2 != null) {
			for (String var4 : var2) {
				var3.add(this.resolve(var4));
			}
		}

		return var3;
	}
}
