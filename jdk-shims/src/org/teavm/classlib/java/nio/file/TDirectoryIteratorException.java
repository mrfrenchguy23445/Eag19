package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

/**
 * Unchecked wrapper for an IOException thrown while iterating a DirectoryStream.
 * Nothing in this package throws it (see PathIterator), but FileVisitor-based
 * code in the game catches it, so the type has to exist.
 */
public class TDirectoryIteratorException extends java.io.UncheckedIOException {

	private static final long serialVersionUID = 1L;

	public TDirectoryIteratorException(java.io.IOException param0) {
		super(param0);
	}
}
