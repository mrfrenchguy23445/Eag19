package org.teavm.classlib.java.security;

import java.security.*;

/*
 * Backed by the vendored Guava instead of by java.util.zip or a native provider.
 * com.google.common.hash.Hashing already carries hand-written MD5, SHA-1 and
 * SHA-256 for this port (see PortableMessageDigest, vendored because Guava's own
 * MessageDigestHashFunction could not compile), and those are verified against the
 * published FIPS/RFC vectors.
 *
 * Input is buffered until digest() rather than streamed: MessageDigest's contract
 * is incremental, and hashing the accumulated array at the end satisfies it, but a
 * long-running stream would hold the whole message. Nothing in Minecraft streams
 * into a MessageDigest -- Crypt.digestData hashes a whole byte[] at once -- so the
 * simplification costs nothing and keeps the shim to one file.
 */
public abstract class TMessageDigest {

	private final String algorithm;

	protected TMessageDigest(String param0) {
		this.algorithm = param0;
	}

	public static TMessageDigest getInstance(String param0) throws NoSuchAlgorithmException {
		String var1 = param0.toUpperCase(java.util.Locale.ROOT).replace("-", "");
		if (var1.equals("SHA1")) {
			return new TMessageDigest(param0) {

				@Override
				protected byte[] compute(byte[] param1) {
					return com.google.common.hash.Hashing.sha1().hashBytes(param1).asBytes();
				}
			};
		}

		if (var1.equals("SHA256")) {
			return new TMessageDigest(param0) {

				@Override
				protected byte[] compute(byte[] param1) {
					return com.google.common.hash.Hashing.sha256().hashBytes(param1).asBytes();
				}
			};
		}

		if (var1.equals("MD5")) {
			return new TMessageDigest(param0) {

				@Override
				protected byte[] compute(byte[] param1) {
					return com.google.common.hash.Hashing.md5().hashBytes(param1).asBytes();
				}
			};
		}

		throw new NoSuchAlgorithmException("MessageDigest: " + param0 + " is not available on this platform");
	}

	public static TMessageDigest getInstance(String param0, String param1) throws NoSuchAlgorithmException {
		return getInstance(param0);
	}

	protected abstract byte[] compute(byte[] param0);

	public final String getAlgorithm() {
		return this.algorithm;
	}

	public final int getDigestLength() {
		try {
			return digest().length;
		} catch (TDigestException var1) {
			return 0;
		}
	}

	public void update(byte param0) {
		this.buffer.write(param0);
	}

	public void update(byte[] param0) {
		if (param0 != null) {
			this.buffer.write(param0, 0, param0.length);
		}
	}

	public void update(byte[] param0, int param1, int param2) {
		this.buffer.write(param0, param1, param2);
	}

	public void update(java.nio.ByteBuffer param0) {
		int var1 = param0.remaining();
		byte[] var2 = new byte[var1];
		param0.get(var2);
		this.buffer.write(var2, 0, var2.length);
	}

	public int getBufferLength() {
		return this.buffer.size();
	}

	// java.io.ByteArrayOutputStream.write*, not add*: the classlib's
	// TByteArrayOutputStream models the JDK class as it is declared (write/toByteArray/
	// reset/size) rather than as the Vector subclass it actually extends, so the
	// inherited add(byte) is not there. Same bytes either way.
	private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

	public byte[] digest() throws TDigestException {
		byte[] var1 = this.buffer.toByteArray();
		this.reset();
		return compute(var1);
	}

	public int digest(byte[] param0, int param1, int param2) throws TDigestException {
		byte[] var1 = digest();
		int var3 = Math.min(param2, var1.length);
		System.arraycopy(var1, 0, param0, param1, var3);
		return var3;
	}

	public byte[] digest(byte[] param0) throws TDigestException {
		update(param0);
		return digest();
	}

	public void reset() {
		this.buffer.reset();
	}
}
