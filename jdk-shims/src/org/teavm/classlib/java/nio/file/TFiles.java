package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import org.teavm.classlib.java.nio.file.attribute.TFileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;

/**
 * java.nio.file.Files shim -- see jdk-shims/build.gradle.kts.
 *
 * Backed by org.teavm.runtime.fs.VirtualFileSystem, the same store java.io.File
 * already uses, so a File and a Path naming the same thing see the same bytes.
 *
 * Deviations from the JDK, all of them forced by there being one filesystem and no
 * symlinks:
 *   - attribute arguments are accepted and ignored;
 *   - walk/find/list materialise the whole tree eagerly instead of lazily, because
 *     the backing store has no directory handle to keep open between elements, and
 *     an eager list cannot produce a DirectoryIteratorException;
 *   - walk never follows symlinks because there are none;
 *   - copy of a directory is refused rather than silently doing nothing;
 *   - createTemp* uses a "tmp" directory under the filesystem's user dir, since
 *     java.io.tmpdir is not meaningful here.
 */
public final class TFiles {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private TFiles() {
	}

	private static TEaglerPath p(TPath param0) {
		if (param0 == null) {
			throw new NullPointerException("path");
		}

		if (!(param0 instanceof TEaglerPath)) {
			throw new ProviderMismatchException("Not an Eaglercraft path: " + param0);
		}

		return (TEaglerPath)param0;
	}

	// ------------------------------------------------------------------ queries

	public static boolean exists(TPath param0, TLinkOption... param1) {
		TEaglerPath var1 = p(param0);
		return var1.exists();
	}

	public static boolean notExists(TPath param0, TLinkOption... param1) {
		return !exists(param0);
	}

	public static boolean isDirectory(TPath param0, TLinkOption... param1) {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		return var2 != null && var2.isDirectory();
	}

	public static boolean isRegularFile(TPath param0, TLinkOption... param1) {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		return var2 != null && var2.isFile();
	}

	public static boolean isReadable(TPath param0) {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		return var2 != null && var2.canRead();
	}

	public static boolean isWritable(TPath param0, TLinkOption... param1) {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		return var2 != null && var2.canWrite();
	}

	public static boolean isSameFile(TPath param0, TPath param1) throws IOException {
		TEaglerPath var1 = p(param0);
		TEaglerPath var2 = p(param1);
		VirtualFile var3 = var1.vf();
		VirtualFile var4 = var2.vf();
		if (var3 == null || var4 == null) {
			throw new NoSuchFileException(var3 == null ? var1.resolved() : var2.resolved());
		}

		return var1.resolved().equals(var2.resolved());
	}

	public static long size(TPath param0) throws IOException {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		if (var2 == null || !var2.isFile()) {
			throw new NoSuchFileException(var1.resolved());
		}

		return var2.length();
	}

	@SuppressWarnings("unchecked")
	public static <A extends BasicFileAttributes> A readAttributes(TPath param0, Class<A> param1, TLinkOption... param2)
			throws IOException {
		TEaglerPath var2 = p(param0);
		VirtualFile var3 = var2.vf();
		if (var3 == null || (!var3.isFile() && !var3.isDirectory())) {
			throw new NoSuchFileException(var2.resolved());
		}

		return (A)new TEaglerFileAttributes(var3);
	}

	public static TPath setLastModifiedTime(TPath param0, FileTime param1) throws IOException {
		TEaglerPath var2 = p(param0);
		VirtualFile var3 = var2.vf();
		if (var3 == null || (!var3.isFile() && !var3.isDirectory())) {
			throw new NoSuchFileException(var2.resolved());
		}

		var3.setLastModified(param1.toMillis());
		return var2;
	}

	// -------------------------------------------------------------- directories

	public static TPath createDirectory(TPath param0, TFileAttribute<?>... param1) throws IOException {
		TEaglerPath var2 = p(param0);
		if (var2.exists()) {
			throw new FileAlreadyExistsException(var2.resolved());
		}

		VirtualFile var3 = var2.parentVf();
		if (var3 == null || !var3.isDirectory()) {
			throw new NoSuchFileException(var2.resolved() + " (parent directory does not exist)");
		}

		if (!var3.createDirectory(var2.name())) {
			throw new IOException("Could not create directory " + var2.resolved());
		}

		return var2;
	}

	/**
	 * Walks the name elements from the root (or from the filesystem's user dir for
	 * a relative path) creating each level, rather than asking the store to make a
	 * whole tree at once -- VirtualFile.createDirectory only ever makes one entry.
	 */
	public static TPath createDirectories(TPath param0, TFileAttribute<?>... param1) throws IOException {
		TEaglerPath var2 = p(param0);
		TEaglerPath var3 = var2.isAbsolute() ? new TEaglerPath("/") : new TEaglerPath("");
		for (String var4 : var2.names()) {
			var3 = TEaglerPath.e(var3.resolve(var4));
			if (!var3.exists()) {
				createDirectory(var3);
			}
		}

		if (!isDirectory(var3)) {
			throw new FileAlreadyExistsException(var3.resolved());
		}

		return var3;
	}

	public static TPath createFile(TPath param0, TFileAttribute<?>... param1) throws IOException {
		TEaglerPath var2 = p(param0);
		if (var2.exists()) {
			throw new FileAlreadyExistsException(var2.resolved());
		}

		VirtualFile var3 = var2.parentVf();
		if (var3 == null || !var3.isDirectory()) {
			throw new NoSuchFileException(var2.resolved() + " (parent directory does not exist)");
		}

		if (!var3.createFile(var2.name())) {
			throw new IOException("Could not create file " + var2.resolved());
		}

		return var2;
	}

	public static void delete(TPath param0) throws IOException {
		TEaglerPath var1 = p(param0);
		VirtualFile var2 = var1.vf();
		if (var2 == null || (!var2.isFile() && !var2.isDirectory())) {
			throw new NoSuchFileException(var1.resolved());
		}

		if (!var2.isDirectory()) {
			var2.delete();
			return;
		}

		List<TPath> var3 = var1.listChildren();
		if (var3 != null) {
			for (TPath var4 : var3) {
				delete(var4);
			}
		}

		var2.delete();
	}

	public static boolean deleteIfExists(TPath param0) throws IOException {
		TEaglerPath var1 = p(param0);
		if (!var1.exists()) {
			return false;
		}

		delete(var1);
		return true;
	}

	public static TPath copy(TPath param0, TPath param1, TCopyOption... param2) throws IOException {
		TEaglerPath var3 = p(param0);
		TEaglerPath var4 = p(param1);
		if (isDirectory(var3)) {
			throw new IOException("copy: directory copy is not supported (source " + var3.resolved() + ")");
		}

		boolean var5 = false;
		for (TCopyOption var6 : param2) {
			if (var6 == TStandardCopyOption.REPLACE_EXISTING) {
				var5 = true;
			}
		}

		if (var4.exists()) {
			if (!var5) {
				throw new FileAlreadyExistsException(var4.resolved());
			}

			delete(var4);
		}

		byte[] var7 = readAllBytes(var3);
		TPath var8 = var4.getParent();
		if (var8 != null) {
			createDirectories(var8);
		}

		write(var4, var7);
		return var4;
	}

	public static TPath move(TPath param0, TPath param1, TCopyOption... param2) throws IOException {
		TEaglerPath var3 = p(param0);
		TEaglerPath var4 = p(param1);
		copy(var3, var4, TStandardCopyOption.REPLACE_EXISTING);
		delete(var3);
		return var4;
	}

	// ------------------------------------------------------------------ streams

	public static InputStream newInputStream(TPath param0, OpenOption... param1) throws IOException {
		return new TEaglerInputStream(p(param0).open(true, false, false));
	}

	public static OutputStream newOutputStream(TPath param0, OpenOption... param1) throws IOException {
		boolean var2 = false; // APPEND
		boolean var3 = false; // create the file when it does not exist yet
		boolean var4 = false; // discard any content the file already has

		// The JDK defines an empty option set as CREATE + TRUNCATE_EXISTING + WRITE.
		if (param1.length == 0) {
			var3 = true;
			var4 = true;
		}

		for (OpenOption var5 : param1) {
			if (var5 == StandardOpenOption.APPEND) {
				var2 = true;
			} else if (var5 == StandardOpenOption.CREATE || var5 == StandardOpenOption.CREATE_NEW) {
				var3 = true;
			} else if (var5 == StandardOpenOption.TRUNCATE_EXISTING) {
				var3 = true;
				var4 = true;
			}
		}

		TEaglerPath var6 = p(param0);
		if (var3 && !var6.exists()) {
			createFile(var6);
		}

		if (var2) {
			return new TEaglerOutputStream(var6.open(false, true, true));
		}

		// The VFS truncates writable, non-append accessors. WRITE without
		// TRUNCATE_EXISTING must instead preserve the contents and rewind.
		VirtualFileAccessor var7 = var6.open(false, true, !var4);
		if (var4) {
			var7.resize(0);
		} else {
			var7.seek(0);
		}

		return new TEaglerOutputStream(var7);
	}

	public static SeekableByteChannel newByteChannel(TPath param0, OpenOption... param1) throws IOException {
		return newByteChannel(param0, new HashSet<OpenOption>(Arrays.asList(param1)));
	}

	public static SeekableByteChannel newByteChannel(TPath param0, Set<? extends OpenOption> param1,
			TFileAttribute<?>... param2) throws IOException {
		boolean var3 = false;
		boolean var4 = false;
		boolean var5 = false;

		for (OpenOption var6 : param1) {
			if (var6 == StandardOpenOption.READ) {
				var3 = true;
			} else if (var6 == StandardOpenOption.WRITE) {
				var4 = true;
			} else if (var6 == StandardOpenOption.APPEND) {
				var5 = true;
			}
		}

		if (!var3 && !var4) {
			var3 = true;
		}

		TEaglerPath var7 = p(param0);
		if (param1.contains(StandardOpenOption.CREATE) && !var7.exists()) {
			createFile(var7);
		}

		if (var7.exists() && var5 && var4) {
			return new TEaglerByteChannel(var7.open(var3, true, true));
		}

		return new TEaglerByteChannel(var7.open(var3, var4, false));
	}

	public static Reader newReader(TPath param0, OpenOption... param1) throws IOException {
		return new InputStreamReader(newInputStream(param0, param1), UTF8);
	}

	public static Writer newWriter(TPath param0, OpenOption... param1) throws IOException {
		return new OutputStreamWriter(newOutputStream(param0, param1), UTF8);
	}

	public static BufferedReader newBufferedReader(TPath param0) throws IOException {
		return new BufferedReader(newReader(param0));
	}

	public static BufferedReader newBufferedReader(TPath param0, Charset param1) throws IOException {
		return new BufferedReader(new InputStreamReader(newInputStream(param0), param1));
	}

	public static BufferedWriter newBufferedWriter(TPath param0) throws IOException {
		return new BufferedWriter(newWriter(param0));
	}

	public static BufferedWriter newBufferedWriter(TPath param0, Charset param1) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(newOutputStream(param0), param1));
	}

	/**
	 * The {@code OpenOption...} overload with no charset, added because
	 * {@code TextureAtlas.dumpSpriteNames} calls exactly this shape and the link reported
	 * {@code Method java.nio.file.Files.newBufferedWriter(Ljava/nio/file/Path;
	 * [Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;}. The three overloads above all
	 * take a charset explicitly, which is why this one was missing rather than merely
	 * unreachable.
	 *
	 * <p>Option handling is whatever {@link #newOutputStream(TPath, OpenOption...)} does, and
	 * nothing more: {@link StandardOpenOption#APPEND} selects append mode over truncate,
	 * {@link StandardOpenOption#CREATE} / {@link #CREATE_NEW} / {@link #TRUNCATE_EXISTING}
	 * create the file if it is missing, and any other option is ignored rather than rejected.
	 * That last point is looser than the JDK, which throws
	 * {@link UnsupportedOperationException} for an option its provider does not support --
	 * this port has one provider, so there is nothing for an option to be unsupported
	 * <em>relative to</em>, and throwing would only turn a caller's harmless extra argument
	 * into a crash. The one caller, {@code TextureAtlas.dumpSpriteNames}, passes
	 * {@code CREATE} and {@code TRUNCATE_EXISTING} and is unaffected either way.
	 */
	public static BufferedWriter newBufferedWriter(TPath param0, OpenOption... param1) throws IOException {
		return new BufferedWriter(newWriter(param0, param1));
	}

	public static BufferedWriter newBufferedWriter(TPath param0, Charset param1, OpenOption... param2)
			throws IOException {
		return new BufferedWriter(new OutputStreamWriter(newOutputStream(param0, param2), param1));
	}

	// ------------------------------------------------------------------ content

	public static byte[] readAllBytes(TPath param0) throws IOException {
		InputStream var1 = newInputStream(param0);
		java.io.ByteArrayOutputStream var2 = new java.io.ByteArrayOutputStream();
		byte[] var3 = new byte[8192];
		int var4 = var1.read(var3);

		// The JDK's own read(InputStream, int) also guards with > 0 rather than
		// >= 0 so that a stream which reports 0 without being at EOF cannot spin.
		while (var4 > 0) {
			var2.write(var3, 0, var4);
			var4 = var1.read(var3);
		}

		var1.close();
		return var2.toByteArray();
	}

	public static String readString(TPath param0) throws IOException {
		return readString(param0, UTF8);
	}

	public static String readString(TPath param0, Charset param1) throws IOException {
		return new String(readAllBytes(param0), param1);
	}

	public static TPath write(TPath param0, byte[] param1, OpenOption... param2) throws IOException {
		OutputStream var3 = newOutputStream(param0, param2);
		var3.write(param1);
		var3.close();
		return param0;
	}

	public static TPath writeString(TPath param0, CharSequence param1, OpenOption... param2) throws IOException {
		return write(param0, param1.toString().getBytes(UTF8), param2);
	}

	public static List<String> readAllLines(TPath param0) throws IOException {
		return readAllLines(param0, UTF8);
	}

	public static List<String> readAllLines(TPath param0, Charset param1) throws IOException {
		List<String> var2 = new ArrayList<>();
		BufferedReader var3 = newBufferedReader(param0, param1);
		String var4 = var3.readLine();

		while (var4 != null) {
			var2.add(var4);
			var4 = var3.readLine();
		}

		var3.close();
		return var2;
	}

	public static TPath write(TPath param0, Iterable<? extends CharSequence> param1, OpenOption... param2)
			throws IOException {
		BufferedWriter var3 = newBufferedWriter(param0, UTF8, param2);
		for (CharSequence var4 : param1) {
			var3.write(var4.toString());
			var3.newLine();
		}

		var3.close();
		return param0;
	}

	public static Stream<String> lines(TPath param0) throws IOException {
		return readAllLines(param0).stream();
	}

	// ---------------------------------------------------------------- traversal

	public static TDirectoryStream<TPath> newDirectoryStream(TPath param0) throws IOException {
		TEaglerPath var1 = p(param0);
		List<TPath> var2 = var1.listChildren();
		if (var2 == null) {
			throw new NotDirectoryException(var1.resolved());
		}

		return new TPathIterator(var2.iterator());
	}

	public static TDirectoryStream<TPath> newDirectoryStream(TPath param0, TFileVisitOption... param1) throws IOException {
		return newDirectoryStream(param0);
	}

	public static Stream<TPath> list(TPath param0) throws IOException {
		TEaglerPath var1 = p(param0);
		List<TPath> var2 = var1.listChildren();
		if (var2 == null) {
			throw new NotDirectoryException(var1.resolved());
		}

		return var2.stream();
	}

	/**
	 * Breadth-first over the name elements, parents before children, which is the
	 * order the JDK's walk guarantees. maxDepth counts name elements below the
	 * start, so maxDepth 0 is the start itself and Integer.MAX_VALUE is unlimited.
	 *
	 * The store is walked eagerly rather than lazily: there is no directory handle
	 * to hold open between elements, and a snapshot is also why a walk can never
	 * report an entry deleted underneath it.
	 */
	private static List<TPath> walk(TEaglerPath param0, int param1) {
		List<TPath> var2 = new ArrayList<>();
		var2.add(param0);
		if (param1 == 0) {
			return var2;
		}

		for (int var3 = 0; var3 < var2.size(); ++var3) {
			List<TPath> var4 = ((TEaglerPath)var2.get(var3)).listChildren();
			if (var4 != null) {
				var2.addAll(var4);
			}
		}

		return var2;
	}

	public static Stream<TPath> walk(TPath param0, int param1, TFileVisitOption... param2) throws IOException {
		return walk(p(param0), param1).stream();
	}

	/**
	 * The unlimited-depth overload the JDK also offers, as a distinct method rather than
	 * by defaulting the varargs one. Java resolves walk(path) to this, not to
	 * walk(path, int, FileVisitOption...), because a lone Path cannot widen to int; so
	 * without it the call does not compile at link time at all.
	 *
	 * Reached from CreateWorldScreen.removeTempDataPackDir and
	 * CreateWorldScreen.createTempDataPackDirFromExistingWorld, which walk a directory
	 * of temporary data packs without a depth limit.
	 */
	public static Stream<TPath> walk(TPath param0, TFileVisitOption... param1) throws IOException {
		return walk(p(param0), Integer.MAX_VALUE).stream();
	}

	public static Stream<TPath> find(TPath param0, int param1, BiPredicate<TPath, BasicFileAttributes> param2,
			TFileVisitOption... param3) throws IOException {
		List<TPath> var4 = new ArrayList<>();
		for (TPath var5 : walk(p(param0), param1)) {
			if (param2.test(var5, readAttributes(var5, BasicFileAttributes.class))) {
				var4.add(var5);
			}
		}

		return var4.stream();
	}

	public static TPath walkFileTree(TPath param0, TFileVisitor<? super TPath> param1) throws IOException {
		return walkFileTree(param0, new HashSet<TFileVisitOption>(), Integer.MAX_VALUE, param1);
	}

	public static TPath walkFileTree(TPath param0, Set<TFileVisitOption> param1, int param2,
			TFileVisitor<? super TPath> param3) throws IOException {
		TEaglerPath var4 = p(param0);
		walkTree(var4, param2, param3);
		return var4;
	}

	private static TFileVisitResult walkTree(TEaglerPath param0, int param1, TFileVisitor<? super TPath> param2)
			throws IOException {
		BasicFileAttributes var3 = readAttributes(param0, BasicFileAttributes.class);
		if (param1 == 0 || !var3.isDirectory()) {
			return param2.visitFile(param0, var3);
		}

		TFileVisitResult var4 = param2.preVisitDirectory(param0, var3);
		if (var4 == TFileVisitResult.TERMINATE) {
			return var4;
		}

		if (var4 != TFileVisitResult.SKIP_SUBTREE) {
			List<TPath> var5 = param0.listChildren();
			if (var5 != null) {
				for (TPath var6 : var5) {
					if (walkTree((TEaglerPath)var6, param1 - 1, param2).isTerminate()) {
						return TFileVisitResult.TERMINATE;
					}
				}
			}
		}

		return param2.postVisitDirectory(param0, null);
	}

	// ---------------------------------------------------------------------- tmp

	private static TEaglerPath tmpRoot() throws IOException {
		TEaglerPath var1 = new TEaglerPath("/tmp");
		if (!var1.exists()) {
			createDirectories(var1);
		}

		return var1;
	}

	public static TPath createTempDirectory(String param0, TFileAttribute<?>... param1) throws IOException {
		return createTempDirectory(param0 == null ? "" : param0, tmpRoot());
	}

	public static TPath createTempDirectory(TPath param0, String param1, TFileAttribute<?>... param2)
			throws IOException {
		return createTempDirectory(param1 == null ? "" : param1, param0);
	}

	static TPath createTempDirectory(String param0, TPath param1) throws IOException {
		TEaglerPath var3 = p(param1);
		for (int var4 = 0; var4 < 10000; ++var4) {
			TEaglerPath var5 = TEaglerPath.e(var3.resolve(param0 + Long.toString(TEMP_COUNTER.getAndIncrement())));
			if (var5.exists()) {
				continue;
			}

			createDirectory(var5);
			return var5;
		}

		throw new IOException("Could not create a temporary directory under " + var3.resolved());
	}

	public static TPath createTempFile(String param0, String param1, TFileAttribute<?>... param2) throws IOException {
		return createTempFile(param0, param1, tmpRoot(), param2);
	}

	public static TPath createTempFile(TPath param0, String param1, String param2, TFileAttribute<?>... param3)
			throws IOException {
		return createTempFile(param1, param2, p(param0), param3);
	}

	static TPath createTempFile(String param0, String param1, TPath param2, TFileAttribute<?>... param3)
			throws IOException {
		String var4 = param0 == null ? "" : param0;
		String var5 = param1 == null ? ".tmp" : param1;
		TEaglerPath var6 = p(param2);
		for (int var7 = 0; var7 < 10000; ++var7) {
			TEaglerPath var8 = TEaglerPath.e(
					var6.resolve(var4 + Long.toString(TEMP_COUNTER.getAndIncrement()) + var5));
			if (var8.exists()) {
				continue;
			}

			createFile(var8);
			return var8;
		}

		throw new IOException("Could not create a temporary file under " + var6.resolved());
	}

	private static final java.util.concurrent.atomic.AtomicLong TEMP_COUNTER =
			new java.util.concurrent.atomic.AtomicLong(System.nanoTime() & 0xFFFFFFFL);
}
