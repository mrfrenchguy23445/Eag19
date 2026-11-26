package org.teavm.classlib.shims;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.teavm.classlib.java.security.TKey;

/*
 * Shim for javax.crypto.Mac, reached only from com.google.common.hash.MacHashFunction, the
 * HMAC HashFunction Guava builds in Hashing.hmacMd5/hmacSha1/hmacSha256/hmacSha512. Those
 * factories are not on any single-player boot path -- they exist to hash against a shared
 * secret -- so this stays out of the way while keeping the adapter linkable. getInstance
 * refuses with NoSuchAlgorithmException, matching XCipher and XKeyGenerator.
 *
 * The full member set MacHashFunction calls is carried: getInstance, init, getMacLength,
 * getAlgorithm, the three update() overloads plus the ByteBuffer one, doFinal() and clone().
 * clone() throws, so MacHashFunction's supportsClone() probe returns false and it falls back
 * to a fresh getInstance() -- which also refuses -- rather than a broken copy.
 */
public class XMac implements Cloneable {

	private XMac() {
	}

	public static XMac getInstance(String param0) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Mac: " + param0 + " is not available on this platform");
	}

	public String getAlgorithm() {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public void init(TKey param0) throws InvalidKeyException {
		throw new InvalidKeyException("Mac: no key algorithm on this platform");
	}

	public int getMacLength() {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public void update(byte param0) {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public void update(byte[] param0) {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public void update(byte[] param0, int param1, int param2) {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public void update(ByteBuffer param0) {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	public byte[] doFinal() {
		throw new IllegalStateException("Mac: no key algorithm on this platform");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Mac: no key algorithm on this platform");
	}
}
