package org.teavm.classlib.shims;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.teavm.classlib.java.security.TKey;
import org.teavm.classlib.java.security.spec.TAlgorithmParameterSpec;

/*
 * Shim for javax.crypto.Cipher, reached from net.minecraft.util.Crypt (getCipher /
 * setupCipher) and from net.minecraft.network.Connection.applyCipher plus the two
 * CipherEncoder/CipherDecoder wrappers. This is the online-mode connection encryption and
 * nothing else: the only path that ever hands a live Cipher to the pipeline is
 * ServerLoginPacketListenerImpl.handleKey, which the integrated server never reaches (see
 * its usesAuthentication() && !isMemoryConnection() gate). A refusing getInstance() therefore
 * keeps every one of those sites linkable without a byte ever being encrypted.
 *
 * The members mirror exactly what the game calls -- getInstance, the two init() overloads,
 * getOutputSize, both update() overloads and doFinal -- and no more. getInstance refuses
 * with NoSuchAlgorithmException and init with InvalidKeyException, matching how the other
 * java.security shims here fail loudly; the pure data members refuse with an unchecked
 * IllegalStateException because nothing can reach them once init has thrown.
 */
public class XCipher {

	private XCipher() {
	}

	public static XCipher getInstance(String param0) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Cipher: " + param0 + " is not available on this platform");
	}

	public static XCipher getInstance(String param0, String param1) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Cipher: " + param0 + " is not available on this platform");
	}

	public void init(int param0, TKey param1) throws InvalidKeyException {
		throw new InvalidKeyException("Cipher: no encryption algorithm on this platform");
	}

	public void init(int param0, TKey param1, TAlgorithmParameterSpec param2) throws InvalidKeyException {
		throw new InvalidKeyException("Cipher: no encryption algorithm on this platform");
	}

	public int getOutputSize(int param0) {
		throw new IllegalStateException("Cipher: no encryption algorithm on this platform");
	}

	public int update(byte[] param0, int param1, int param2, byte[] param3) {
		throw new IllegalStateException("Cipher: no encryption algorithm on this platform");
	}

	public int update(byte[] param0, int param1, int param2, byte[] param3, int param4) {
		throw new IllegalStateException("Cipher: no encryption algorithm on this platform");
	}

	public byte[] doFinal(byte[] param0) {
		throw new IllegalStateException("Cipher: no encryption algorithm on this platform");
	}
}
