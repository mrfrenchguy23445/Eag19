package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * The classlib does not model java.nio.file.SimpleFileVisitor, so this shim is the
 * one that exists. Every hook continues, which is what a visitor that only cares
 * about the leaves wants.
 */
public class TSimpleFileVisitor<T> implements TFileVisitor<T> {

	@Override
	public TFileVisitResult preVisitDirectory(T param0, BasicFileAttributes param1) throws IOException {
		return TFileVisitResult.CONTINUE;
	}

	@Override
	public TFileVisitResult visitFile(T param0, BasicFileAttributes param1) throws IOException {
		return TFileVisitResult.CONTINUE;
	}

	@Override
	public TFileVisitResult visitFileFailed(T param0, IOException param1) throws IOException {
		throw param1;
	}

	@Override
	public TFileVisitResult postVisitDirectory(T param0, IOException param1) throws IOException {
		if (param1 != null) {
			throw param1;
		}

		return TFileVisitResult.CONTINUE;
	}
}
