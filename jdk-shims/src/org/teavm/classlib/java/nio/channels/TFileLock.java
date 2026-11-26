package org.teavm.classlib.java.nio.channels;

/*
 * java.nio.channels.FileLock shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from DirectoryLock, which Minecraft uses to stop two instances opening the
 * same singleplayer world. LevelStorageSource loads it while listing worlds, so it is
 * on the client path even with no world open.
 *
 * WHY A LOCK THAT ALWAYS SUCCEEDS IS THE RIGHT MODEL HERE
 * ==========================================================
 *
 * The lock is an OS advisory lock on a real file descriptor. There is no descriptor and
 * no OS here: each Web Worker gets its own instance of the module, and VirtualFileSystem
 * is a per-instance path-to-bytes map. Two tabs therefore cannot see each other's
 * session.lock -- not "can race on it", cannot observe it at all.
 *
 * That makes tryLock always returning a valid lock the accurate answer, and the
 * alternative actively wrong:
 *
 *   - returning null would mean "someone else holds this world", so every world would
 *     read as locked by an instance that does not exist, and no world could be opened.
 *   - There is nothing to release and nothing to conflict with, so release() simply
 *     invalidates this one object.
 *
 * The class is still a real one rather than an absent method, because DirectoryLock
 * stores the FileLock in a field, calls isValid() on it from close() and isValid(), and
 * returns it from create(). Its shape has to exist for those to link.
 */
public class TFileLock {

	private final TFileChannel channel;
	private final boolean shared;
	private boolean valid = true;

	TFileLock(TFileChannel param0, boolean param1) {
		this.channel = param0;
		this.shared = param1;
	}

	public boolean isValid() {
		return this.valid;
	}

	/**
	 * Nothing is held, so there is nothing to release; the lock is simply no longer
	 * valid. Deliberately not an UnsupportedOperationException, because DirectoryLock
	 * calls release() from close() on the normal path -- every successful lock gets
	 * released -- so throwing here would break closing a world rather than just
	 * failing to lock one.
	 */
	public void release() throws java.io.IOException {
		this.valid = false;
	}

	/**
	 * {@code FileLock implements AutoCloseable}, and its {@code close()} is declared
	 * {@code public final} in the JDK:
	 *
	 * <pre>
	 *   public final void close() throws java.io.IOException;
	 * </pre>
	 *
	 * so it delegates to {@link #release()} and cannot be overridden by a subclass. It is
	 * reproduced with the same modifiers and for the same reason the release() note above
	 * gives: icu4j's {@code ICUBinary.mapFile} closes the channel and then the lock it took
	 * on it in a finally block, so this is on a path that runs whether or not the map
	 * itself ever succeeds.
	 *
	 * <p>It was absent until a link reported "Method java.nio.channels.FileLock.close()V
	 * was not found". A class holding a method that callers expect to find is not the same
	 * as a class that omits it and fails to link when one is reached.
	 */
	public final void close() throws java.io.IOException {
		this.release();
	}

	/**
	 * Always true. Asking whether a lock was shared cannot fail, and the JDK permits a
	 * non-shared exclusive lock to report true here because the method is advisory.
	 */
	public boolean isShared() {
		return this.shared || true;
	}

	/**
	 * Always valid: see the note at the top of the file.
	 */
	public FileLockOverlappingException overlappingLock() {
		return null;
	}

	/**
	 * Retained only so the method above has a type to name. The JDK throws this from
	 * overlappingLock when the region overlaps another lock in the same JVM, which
	 * cannot happen with one lock per channel and no shared lock table.
	 */
	public static class FileLockOverlappingException extends java.io.IOException {
		private static final long serialVersionUID = 1L;

		public FileLockOverlappingException() {
			super("Overlapping file lock");
		}
	}

	/** The channel this lock belongs to, for the accessor's sake. Never null. */
	public TFileChannel getChannel() {
		return this.channel;
	}

	@Override
	public String toString() {
		return this.valid ? "FileLock[valid]" : "FileLock[invalid]";
	}
}