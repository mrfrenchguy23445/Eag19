package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.teavm.runtime.fs.VirtualFile;

/**
 * Implements the classlib's own BasicFileAttributes interface (java.nio.file
 * .attribute.BasicFileAttributes is TBasicFileAttributes, and that one does model
 * the full method set, so there is no need to shim the interface itself).
 *
 * The virtual filesystem tracks exactly three facts -- is it a file, is it a
 * directory, how long is it -- plus lastModified. Everything else reports the
 * fixed answer the JDK gives for a filesystem that cannot supply it: fileKey() is
 * null, access time equals modification time, isOther() is false, and a symbolic
 * link is never possible.
 */
final class TEaglerFileAttributes implements BasicFileAttributes {

	private final boolean directory;
	private final long size;
	private final long lastModified;

	TEaglerFileAttributes(VirtualFile param0) {
		this.directory = param0.isDirectory();
		this.size = this.directory ? 0L : param0.length();
		this.lastModified = param0.lastModified();
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.fromMillis(this.lastModified);
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.fromMillis(this.lastModified);
	}

	@Override
	public FileTime creationTime() {
		return FileTime.fromMillis(this.lastModified);
	}

	@Override
	public boolean isRegularFile() {
		return !this.directory;
	}

	@Override
	public boolean isDirectory() {
		return this.directory;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public Object fileKey() {
		return null;
	}

	@Override
	public long size() {
		return this.size;
	}
}
