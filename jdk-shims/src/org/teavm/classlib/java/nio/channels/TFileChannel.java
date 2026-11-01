package org.teavm.classlib.java.nio.channels;

import java.nio.channels.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

/*
 * java.nio.channels.FileChannel shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from DirectoryLock, which is on the client's boot path: LevelStorageSource
 * loads every level summary on startup, and each summary opens session.lock to check
 * whether another instance holds the world.
 *
 * A REAL CHANNEL, NOT A STUB
 * ===========================
 *
 * Unlike most of this tree, the read and write bodies here do real work. DirectoryLock
 * writes three bytes to the lock file and then reads nothing back, so a channel that
 * discarded writes would be indistinguishable -- until it was not: the lock file has to
 * exist afterwards, because that is what makes the next isLocked() call open it instead
 * of taking the NoSuchFileException branch. A write-dropping channel would make every
 * world look unlocked forever.
 *
 * It is also correct for the region-file and NBT paths, which reach it through
 * TFiles.newByteChannel and are byte-exact either way.
 *
 * MODEL: WHOLE FILE IN, WHOLE FILE OUT
 * =====================================
 *
 * The JDK's FileChannel is a positional view onto a file descriptor with an OS page
 * cache behind it. Here the file is a byte[] inside a GC'd heap, so the channel holds
 * the contents as a byte[] plus a position, and flushes by writing the whole array
 * back. That is not an approximation of a page cache -- it is what TFiles already does
 * for readAllBytes and write, and VirtualFileSystem has no notion of a partial write.
 *
 * THE OPTION SEMANTICS ARE THE PART THAT MATTERS
 * ===============================================
 *
 * open() has to distinguish CREATE from no CREATE, and specifically:
 *
 *   open(path, WRITE) on a missing file throws NoSuchFileException.
 *
 * DirectoryLock.isLocked depends on exactly that. It opens session.lock with WRITE
 * alone, and catches NoSuchFileException to conclude "no lock file, therefore nobody
 * holds this world". A channel that created the file on open, or that returned a
 * channel for a missing file, would silently change that answer from false to true and
 * make every world appear permanently locked.
 */
public class TFileChannel implements SeekableByteChannel {

	private final Path path;
	private final Set<StandardOpenOption> options;
	/** Backing storage. Its length is the capacity, not the file size -- see {@link #length}. */
	private byte[] contents;
	/**
	 * The logical file size, which is not {@code contents.length}.
	 *
	 * <p>{@link #grow(int)} over-allocates so that appending stays amortised O(1); reporting
	 * the capacity as the size would make a region file look far longer than it is and the
	 * region header's sector bounds checks would accept sectors that hold nothing.
	 */
	private int length;
	private long position;
	private boolean open = true;
	private TFileLock lock;
	/** Contents in memory differ from the file on disk. */
	private boolean dirty;
	/** The file already existed when this channel was opened, or has been written since. */
	private boolean onDisk;

	private TFileChannel(Path param0, Set<StandardOpenOption> param1) {
		this.path = param0;
		this.options = param1;
	}

	/**
	 * Loads the file, honouring CREATE and READ.
	 *
	 * The option set is validated the same way the JDK validates it, including the rule
	 * that READ and WRITE with neither APPEND nor TRUNCATE is rejected as "no useful
	 * option" -- DirectoryLock never hits that case, but a caller that did should be
	 * told rather than handed a channel that does nothing.
	 */
	public static TFileChannel open(Path param0, OpenOption... param1) throws IOException {
		if (param0 == null) {
			throw new NullPointerException("path");
		}

		if (param1 == null) {
			throw new NullPointerException("options");
		}

		Set<StandardOpenOption> var2 = EnumSet.noneOf(StandardOpenOption.class);
		for (OpenOption var3 : param1) {
			if (var3 == null) {
				throw new NullPointerException("options contains a null entry");
			}

			if (var3 instanceof StandardOpenOption) {
				var2.add((StandardOpenOption)var3);
			}
		}

		if (var2.isEmpty()) {
			throw new IllegalArgumentException("No options present");
		}

		TFileChannel var4 = new TFileChannel(param0, var2);
		if (!var2.contains(StandardOpenOption.READ) && !var2.contains(StandardOpenOption.WRITE)) {
			throw new IllegalArgumentException("No read or write option present");
		}

		var4.load();
		return var4;
	}

	private void load() throws IOException {
		boolean var0 = this.options.contains(StandardOpenOption.CREATE);
		if (Files.exists(this.path) && Files.isRegularFile(this.path)) {
			byte[] var1 = Files.readAllBytes(this.path);
			if (this.options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
				var1 = new byte[0];
			}

			this.contents = var1;
			this.length = var1.length;
			this.onDisk = true;
			return;
		}

		if (!var0) {
			// The NoSuchFileException that DirectoryLock.isLocked relies on.
			throw new NoSuchFileException(this.path.toString());
		}

		this.contents = new byte[0];
		this.length = 0;
		this.onDisk = false;
	}

	/**
	 * Writes the whole file back, but only when there is something to write.
	 *
	 * <p>This is the expensive operation -- it hands the entire array to the virtual
	 * filesystem, which for the browser storage backend is a full array put. {@link #write}
	 * no longer calls it for every write; see the note there.
	 */
	private void flush() throws IOException {
		if (!this.dirty) {
			return;
		}

		if (this.options.contains(StandardOpenOption.WRITE)) {
			byte[] var0 = this.length == this.contents.length ? this.contents : java.util.Arrays.copyOf(this.contents, this.length);
			Files.write(this.path, var0, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			this.onDisk = true;
		}

		this.dirty = false;
	}

	@Override
	public int read(ByteBuffer param0) throws IOException {
		this.checkOpen();
		if (!this.options.contains(StandardOpenOption.READ)) throw new NonReadableChannelException();
		if (!param0.hasRemaining()) {
			return 0;
		}

		if (this.position >= this.length) {
			return -1;
		}

		int var1 = (int)Math.min(param0.remaining(), this.length - this.position);
		for (int var2 = 0; var2 < var1; ++var2) {
			param0.put(this.contents[(int)this.position + var2]);
		}

		this.position += var1;
		return var1;
	}

	@Override
	public int write(ByteBuffer param0) throws IOException {
		this.checkOpen();
		if (!param0.hasRemaining()) {
			return 0;
		}

		if (!this.options.contains(StandardOpenOption.WRITE)) throw new NonWritableChannelException();
		if (this.options.contains(StandardOpenOption.APPEND)) this.position = this.length;
		long end = this.position + param0.remaining();
		if (end > Integer.MAX_VALUE) throw new IOException("File too large for browser storage");
		this.grow((int)end);

		int var2 = param0.remaining();
		param0.get(this.contents, (int)this.position, var2);
		this.position += var2;
		if (end > this.length) {
			this.length = (int)end;
		}

		this.dirty = true;
		// A write that CREATES the file still goes through immediately, because
		// DirectoryLock.isLocked() answers by opening session.lock and taking the
		// NoSuchFileException branch; a deferred create would make every world look
		// unlocked. Everything after that stays in memory and goes out on flush()/
		// close(), which is what the JDK's page cache does. Writing the whole file per
		// write made saving chunk N cost two full copies of a region file that grows to
		// tens of megabytes -- quadratic in the number of chunks saved.
		if (!this.onDisk) {
			this.flush();
		}

		return var2;
	}

	/** Region files access sectors without changing the sequential channel position. */
	public int read(ByteBuffer buffer, long offset) throws IOException {
		if (offset < 0) throw new IllegalArgumentException("Negative position");
		long saved = this.position;
		try {
			this.position = offset;
			return this.read(buffer);
		} finally {
			this.position = saved;
		}
	}

	public int write(ByteBuffer buffer, long offset) throws IOException {
		if (offset < 0) throw new IllegalArgumentException("Negative position");
		long saved = this.position;
		try {
			this.position = offset;
			return this.write(buffer);
		} finally {
			this.position = saved;
		}
	}

	/**
	 * Grows the buffer geometrically so a sequence of appends is amortised O(1) per byte.
	 *
	 * <p>Allocating exactly {@code param0} reallocated and copied the entire file on every
	 * growth, so appending chunk payloads to a region file copied the whole thing once per
	 * 4 KiB sector allocated. Only {@link #length} is the file size; the slack is capacity.
	 */
	private void grow(int param0) {
		if (param0 <= this.contents.length) {
			return;
		}

		byte[] var1 = new byte[Math.max(param0, this.contents.length + (this.contents.length >> 1) + 8192)];
		System.arraycopy(this.contents, 0, var1, 0, this.length);
		this.contents = var1;
	}

	@Override
	public long position() throws IOException {
		this.checkOpen();
		return this.position;
	}

	@Override
	public TFileChannel position(long param0) throws IOException {
		this.checkOpen();
		if (param0 < 0) {
			throw new IllegalArgumentException("Negative position");
		}

		this.position = param0;
		return this;
	}

	@Override
	public long size() throws IOException {
		this.checkOpen();
		return this.length;
	}

	/**
	 * Truncates to the given size.
	 *
	 * <p>The return type is {@link TFileChannel}, not {@link SeekableByteChannel}, and that
	 * is not a narrowing choice. {@code javap java.nio.channels.FileChannel} shows the real
	 * class declares {@code public abstract FileChannel truncate(long)} alongside the
	 * covariant {@code SeekableByteChannel} bridge that satisfies the interface; TeaVM links
	 * against the declared one. An earlier version of this shim returned
	 * {@code SeekableByteChannel}, which is the bridge's type rather than the real one, and
	 * it failed to link: "Method java.nio.channels.FileChannel.truncate(J)".
	 *
	 * <p>The behaviour matches the JDK: a size past the end is not an error and is a no-op,
	 * and a negative one throws.
	 */
	@Override
	public TFileChannel truncate(long param0) throws IOException {
		this.checkOpen();
		if (param0 < 0) {
			throw new IllegalArgumentException("Negative size");
		}

		if (param0 < this.length) {
			this.length = (int)param0;
			this.position = Math.min(this.position, param0);
			this.dirty = true;
			this.flush();
		}

		return this;
	}

	/**
	 * The subset of {@code FileChannel.MapMode} that naming a memory mapping needs, reached
	 * from icu4j's {@code ICUBinary.mapFile}.
	 *
	 * <p>Real as a nested enum, and relocated with its outer class, so it arrives as
	 * {@code java.nio.channels.FileChannel$MapMode} rather than as a top-level type. Only
	 * READ_ONLY and READ_WRITE are declared, because those are the only two constants the
	 * real enum has that this port can name; WRITE and APPEND use the same numeric layout
	 * and differ only in the behaviour of map() itself, which nothing calls.
	 *
	 * <p>There is no {@code map()} method here. A WASM-GC module has no virtual memory to
	 * map a file into -- {@code Memory} is a linear-memory object with no host file
	 * behind it -- so the JDK's answer cannot be produced at any level of effort, and
	 * declaring map() would only mean choosing a wrong one. See ICUBinary's only use,
	 * which reads the file it maps and throws when there is no data directory; that is a
	 * path this port already fails earlier, at the .dat file not being present.
	 */
	public enum MapMode {
		READ_ONLY,
		READ_WRITE,
		WRITE,
		APPEND;

		/** Whether this mode permits the channel to be written through. Matches the JDK. */
		public boolean isWritable() {
			return this != MapMode.READ_ONLY;
		}
	}

	/**
	 * Nothing to flush. Writes are already through to the store on the way out, so
	 * there is no deferred state for this to push; metaData is likewise a no-op because
	 * the store records no timestamps a caller could ask for.
	 */
	public void force(boolean param0) throws IOException {
		this.checkOpen();
	}

	/**
	 * Always a valid exclusive lock -- see TFileLock for why refusing would be wrong.
	 *
	 * The region arguments are accepted and ignored. The JDK rejects a zero or negative
	 * region before returning null, and that check is kept because it is cheap and is
	 * about the arguments rather than about the store.
	 */
	public TFileLock tryLock(long param0, long param1, boolean param2) throws IOException {
		this.checkOpen();
		if (param0 < 0 || param1 <= 0) {
			throw new IllegalArgumentException("Size must be positive");
		}

		if (this.lock == null || !this.lock.isValid()) {
			this.lock = new TFileLock(this, !param2);
			return this.lock;
		}

		// A second overlapping request on the same channel. The JDK would return null
		// when the JVM already holds it; here the existing lock is handed back, which
		// keeps the "is there a lock?" question answerable at one place.
		return this.lock;
	}

	public TFileLock tryLock() throws IOException {
		return this.tryLock(0L, Long.MAX_VALUE, false);
	}

	public TFileLock lock() throws IOException {
		return this.tryLock();
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void close() throws IOException {
		if (!this.open) {
			return;
		}

		this.flush();
		if (this.lock != null && this.lock.isValid()) {
			this.lock.release();
		}

		this.open = false;
	}

	private void checkOpen() throws ClosedChannelException {
		if (!this.open) {
			throw new ClosedChannelException();
		}
	}

	@Override
	public String toString() {
		return "FileChannel[" + this.path + (this.open ? "]" : ", closed]");
	}
}