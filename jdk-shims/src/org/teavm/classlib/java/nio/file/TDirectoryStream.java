package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.Closeable;
import java.util.Iterator;

public interface TDirectoryStream<T> extends Iterable<T>, Closeable {

	@Override
	Iterator<T> iterator();

	@Override
	void close() throws java.io.IOException;
}
