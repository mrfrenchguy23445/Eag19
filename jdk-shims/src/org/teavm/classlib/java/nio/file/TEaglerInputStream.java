package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.InputStream;
import java.io.IOException;

import org.teavm.runtime.fs.VirtualFileAccessor;

/**
 * Reads the whole accessor in one call rather than chunking: the virtual file
 * lives in the WASM heap (or in IndexedDB, already materialized as one array), so
 * a partial read would buy nothing.
 */
final class TEaglerInputStream extends InputStream {

	private final VirtualFileAccessor accessor;
	private boolean closed;

	TEaglerInputStream(VirtualFileAccessor param0) {
		this.accessor = param0;
	}

	private void checkOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
	}

	@Override
	public int read() throws IOException {
		byte[] var1 = new byte[1];
		return this.read(var1, 0, 1) == -1 ? -1 : var1[0] & 255;
	}

	@Override
	public int read(byte[] param0, int param1, int param2) throws IOException {
		this.checkOpen();
		return this.accessor.read(param0, param1, param2);
	}

	@Override
	public int available() throws IOException {
		this.checkOpen();
		return Math.max(0, this.accessor.size() - this.accessor.tell());
	}

	@Override
	public long skip(long param0) throws IOException {
		this.checkOpen();
		int var1 = (int)Math.min(param0, this.available());
		this.accessor.skip(var1);
		return var1;
	}

	@Override
	public void close() throws IOException {
		if (!this.closed) {
			this.closed = true;
			this.accessor.close();
		}
	}
}
