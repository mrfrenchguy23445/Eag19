package org.teavm.classlib.java.nio.file;

/*
 * Thrown by TFiles.isDirectory/isRegularFile/isReadable/isWritable/isExecutable
 * when the Path's FileSystemProvider is not the one they were reached through.
 * They all funnel into the same private p(Path, Class) guard.
 */
public class TProviderMismatchException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public TProviderMismatchException() {
	}

	public TProviderMismatchException(String param0) {
		super(param0);
	}
}
