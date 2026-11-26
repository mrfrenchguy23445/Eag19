package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

public class TFileSystemNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TFileSystemNotFoundException() {
	}

	public TFileSystemNotFoundException(String param0) {
		super(param0);
	}
}
