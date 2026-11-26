package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import org.teavm.runtime.fs.VirtualFileAccessor;

/**
 * SeekableByteChannel over a VirtualFileAccessor. Only reachable through
 * Files.newByteChannel, which in Minecraft means NativeImage.writeToFile writing a
 * screenshot or a texture back out.
 *
 * Read and write share one accessor and therefore one position, matching the JDK:
 * a channel opened for both is positioned wherever the last operation left it.
 */
final class TEaglerByteChannel implements SeekableByteChannel {

	private final VirtualFileAccessor accessor;
	private boolean open = true;

	TEaglerByteChannel(VirtualFileAccessor param0) {
		this.accessor = param0;
	}

	private void checkOpen() throws IOException {
		if (!this.open) {
			throw new java.nio.channels.ClosedChannelException();
		}
	}

	@Override
	public int read(ByteBuffer param0) throws IOException {
		this.checkOpen();
		int var1 = param0.remaining();
		if (var1 == 0) {
			return 0;
		}

		byte[] var2 = new byte[var1];
		int var3 = this.accessor.read(var2, 0, var1);
		if (var3 <= 0) {
			// VirtualFileAccessor reports end of file as "0 bytes read", but
			// ReadableByteChannel has to report it as -1.
			return -1;
		}

		param0.put(var2, 0, var3);
		return var3;
	}

	@Override
	public int write(ByteBuffer param0) throws IOException {
		this.checkOpen();
		int var1 = param0.remaining();
		if (var1 == 0) {
			return 0;
		}

		byte[] var2 = new byte[var1];
		param0.get(var2);
		this.accessor.write(var2, 0, var1);
		return var1;
	}

	@Override
	public long position() throws IOException {
		this.checkOpen();
		return this.accessor.tell();
	}

	@Override
	public TEaglerByteChannel position(long param0) throws IOException {
		this.checkOpen();
		this.accessor.seek((int)Math.max(0L, Math.min(param0, this.accessor.size())));
		return this;
	}

	@Override
	public long size() throws IOException {
		this.checkOpen();
		return this.accessor.size();
	}

	@Override
	public TEaglerByteChannel truncate(long param0) throws IOException {
		this.checkOpen();
		long var1 = Math.min(param0, this.accessor.size());
		this.accessor.resize((int)var1);
		return this;
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void close() throws IOException {
		if (this.open) {
			this.open = false;
			this.accessor.close();
		}
	}
}
