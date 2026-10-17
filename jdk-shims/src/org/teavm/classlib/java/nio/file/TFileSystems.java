package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/*
 * There is exactly one filesystem, matching java.io.File's: the classlib's
 * org.teavm.runtime.fs.VirtualFileSystem, which VirtualFileSystemProvider hands
 * out and which the wasm target replaces with its IndexedDB-backed instance.
 * newFileSystem is not offered; there is nothing to install a second provider.
 */
public final class TFileSystems {

	private static final TFileSystem DEFAULT = new TEaglerFileSystem();

	private TFileSystems() {
	}

	public static TFileSystem getDefault() {
		return DEFAULT;
	}
}
