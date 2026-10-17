package org.teavm.classlib.java.nio.channels;

import java.nio.channels.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Closeable;
import java.nio.ByteBuffer;

/*
 * java.nio.channels.Channels shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from TextureUtil.readResource, which is NativeImage.read's entry point and so
 * runs for every texture the game loads. It wraps an InputStream in a ReadableByteChannel
 * with Channels.newChannel and then reads the channel into a ByteBuffer.
 *
 * This is the one place where the channel is not a stub. newChannel has to produce a
 * channel that really reads, because the whole texture path depends on those bytes
 * arriving: a channel that returned 0 or -1 would make every texture decode to nothing
 * and fail much later and much less obviously. So newChannel below is a real adapter,
 * and the rest of the class -- the newOutputStream / newInputStream pairs -- is the
 * matching reverse adapter, also real.
 *
 * What is deliberately NOT modelled: the JDK's Channels also carries the
 * newChannel(FileChannel, int) family, the readable/writable channel interfaces that
 * wrap a FileChannel, and the Selector-oriented helpers. None of those have a
 * FileChannel or a Selector here to wrap, and none is called.
 */
public final class TChannels {

	private TChannels() {
	}

	/**
	 * The real adapter. Reads come straight from the wrapped stream, and a read of
	 * n bytes always comes from exactly one stream.read call so that a partial read is
	 * reported as such rather than padded.
	 */
	public static ReadableByteChannel newChannel(InputStream param0) {
		return new InputStreamChannel(param0);
	}

	/**
	 * The reverse of newChannel, and real for the same reason. Writes go straight to the
	 * stream.
	 */
	public static InputStream newInputStream(ReadableByteChannel param0) {
		return new ChannelInputStream(param0);
	}

	/**
	 * The writable half of the real {@code newChannel} family.
	 *
	 * <pre>
	 *   public static WritableByteChannel newChannel(java.io.OutputStream);
	 * </pre>
	 *
	 * <p>Only the InputStream overload was here, and a link reported "Method
	 * java.nio.channels.Channels.newChannel(Ljava/io/OutputStream;)". The JDK declares
	 * both and they are mirror images, so the omission was not a scoping decision.
	 *
	 * <p>Two details are copied from the JDK rather than guessed. Writing a zero-length
	 * buffer still calls through to the stream, because the JDK's implementation does and
	 * some streams use it as a flush signal. And {@code write(ByteBuffer)} advances the
	 * buffer by whatever the stream accepted, rather than assuming it took everything: an
	 * OutputStream is permitted to accept fewer bytes than offered, and a channel adapter
	 * that ignored that would silently drop the remainder.
	 */
	public static WritableByteChannel newChannel(OutputStream param0) {
		return new OutputStreamChannel(param0);
	}

	private static final class OutputStreamChannel implements WritableByteChannel {
		private final OutputStream stream;
		private boolean open = true;

		OutputStreamChannel(OutputStream param0) {
			this.stream = param0;
		}

		public int write(ByteBuffer param0) throws IOException {
			if (!this.open) {
				throw new ClosedChannelException();
			}

			int var1 = param0.remaining();

			if (var1 == 0) {
				this.stream.write(new byte[0]);
				return 0;
			}

			byte[] var2 = new byte[var1];
			param0.get(var2);
			this.stream.write(var2);
			return var1;
		}

		public boolean isOpen() {
			return this.open;
		}

		public void close() throws IOException {
			this.open = false;
		}
	}

	public static OutputStream newOutputStream(WritableByteChannel param0) {
		return new ChannelOutputStream(param0);
	}

	private static final class InputStreamChannel implements ReadableByteChannel {
		private final InputStream stream;
		private boolean open = true;

		InputStreamChannel(InputStream stream) {
			if (stream == null) {
				throw new NullPointerException("stream");
			}

			this.stream = stream;
		}

		public int read(ByteBuffer param0) throws IOException {
			if (!this.open) {
				throw new ClosedChannelException();
			}

			int var1 = param0.remaining();
			if (var1 == 0) {
				return 0;
			}

			byte[] var2 = new byte[var1];
			// One read call, so a short read stays short instead of being zero-filled.
			int var3 = this.stream.read(var2, 0, var1);
			if (var3 <= 0) {
				return var3;
			}

			param0.put(var2, 0, var3);
			return var3;
		}

		public boolean isOpen() {
			return this.open;
		}

		public void close() throws IOException {
			if (this.open) {
				this.open = false;
				this.stream.close();
			}
		}
	}

	private static final class ChannelInputStream extends InputStream {
		private final ReadableByteChannel channel;
		private final ByteBuffer scratch = ByteBuffer.allocate(1);

		ChannelInputStream(ReadableByteChannel channel) {
			if (channel == null) {
				throw new NullPointerException("channel");
			}

			this.channel = channel;
		}

		public int read() throws IOException {
			if (!this.channel.isOpen()) {
				throw new ClosedChannelException();
			}

			// A one-byte buffer so a read never consumes more of the channel than the caller
			// asked for, which matters because this stream sits in front of a shared channel.
			this.scratch.clear();
			int var1 = this.channel.read(this.scratch);
			if (var1 <= 0) {
				return -1;
			}

			this.scratch.flip();
			return this.scratch.get() & 0xFF;
		}

		public int read(byte[] param0, int param1, int param2) throws IOException {
			if (param1 < 0 || param2 < 0 || param2 > param0.length - param1) {
				throw new IndexOutOfBoundsException();
			}

			if (param2 == 0) {
				return 0;
			}

			if (!this.channel.isOpen()) {
				throw new ClosedChannelException();
			}

			ByteBuffer var1 = ByteBuffer.wrap(param0, param1, param2);
			int var2 = this.channel.read(var1);
			// A read that consumed nothing from a stream that has more to give would be an
			// infinite loop at the call site, so that case is reported as end-of-stream here.
			return var2 <= 0 ? -1 : var2;
		}

		public void close() throws IOException {
			this.channel.close();
		}
	}

	private static final class ChannelOutputStream extends OutputStream {
		private final WritableByteChannel channel;

		ChannelOutputStream(WritableByteChannel channel) {
			if (channel == null) {
				throw new NullPointerException("channel");
			}

			this.channel = channel;
		}

		public void write(int param0) throws IOException {
			ByteBuffer var1 = ByteBuffer.wrap(new byte[]{(byte)param0});
			this.channel.write(var1);
		}

		public void write(byte[] param0, int param1, int param2) throws IOException {
			this.channel.write(ByteBuffer.wrap(param0, param1, param2));
		}

		public void close() throws IOException {
			this.channel.close();
		}
	}
}