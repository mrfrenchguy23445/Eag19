package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.util.Iterator;

/**
 * The whole directory is snapshotted by Files.newDirectoryStream before the
 * iterator is handed out, so there is no lazy, throwing cursor to model here --
 * unlike the JDK, which is why DirectoryIteratorException exists and is not
 * declared anywhere in this package.
 */
public class TPathIterator implements TDirectoryStream<TPath>, Iterator<TPath> {

	private final Iterator<TPath> delegate;

	TPathIterator(Iterator<TPath> param0) {
		this.delegate = param0;
	}

	@Override
	public Iterator<TPath> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return this.delegate.hasNext();
	}

	@Override
	public TPath next() {
		return this.delegate.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
	}
}
