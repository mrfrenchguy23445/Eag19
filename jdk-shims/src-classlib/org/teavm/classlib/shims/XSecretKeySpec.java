package org.teavm.classlib.shims;

/*
 * Shim for javax.crypto.spec.SecretKeySpec, reached from net.minecraft.util.Crypt (the
 * SecretKeySpec wrapping the AES key decrypted out of an online-mode handshake) and from
 * com.google.common.hash.Hashing (the HMAC key material). The real class is a concrete
 * holder: a key byte array plus an algorithm name. Nothing is ever encrypted with it in
 * this build -- see XSecretKey -- but the constructor and the three Key accessors link
 * without running a single byte of crypto, so they are all that is carried here.
 *
 * The constructor does not throw in the real API either, so it does not throw here.
 */
public class XSecretKeySpec implements XSecretKey {

	private final byte[] key;
	private final String algorithm;

	public XSecretKeySpec(byte[] param0, String param1) {
		if (param0 == null) {
			throw new NullPointerException("key material");
		}

		this.key = param0.clone();
		this.algorithm = param1;
	}

	@Override
	public String getAlgorithm() {
		return this.algorithm;
	}

	@Override
	public String getFormat() {
		return "RAW";
	}

	@Override
	public byte[] getEncoded() {
		return this.key.clone();
	}
}
