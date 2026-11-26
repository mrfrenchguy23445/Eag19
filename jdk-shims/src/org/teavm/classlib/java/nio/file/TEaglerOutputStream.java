package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.io.OutputStream;

import org.teavm.runtime.fs.VirtualFileAccessor;

final class TEaglerOutputStream extends OutputStream {

	private final VirtualFileAccessor accessor;
	private boolean closed;

	TEaglerOutputStream(VirtualFileAccessor param0) {
		this.accessor = param0;
	}

	private void checkOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
	}

	@Override
	public void write(int param0) throws IOException {
		this.checkOpen();
		byte[] var1 = new byte[] {(byte)param0};
		this.accessor.write(var1, 0, 1);
	}

	@Override
	public void write(byte[] param0, int param1, int param2) throws IOException {
		this.checkOpen();
		this.accessor.write(param0, param1, param2);
	}

	@Override
	public void flush() throws IOException {
		this.checkOpen();
		this.accessor.flush();
	}

	@Override
	public void close() throws IOException {
		if (!this.closed) {
			this.closed = true;
			this.accessor.flush();
			this.accessor.close();
		}
	}
}
