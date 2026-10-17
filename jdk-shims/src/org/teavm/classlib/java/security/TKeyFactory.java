package org.teavm.classlib.java.security;

import java.security.*;

import org.teavm.classlib.java.security.spec.TKeySpec;

/*
 * KeyFactory exists so net.minecraft.util.Crypt.byteToPrivateKey /
 * byteToPublicKey link. They are only ever called when parsing an online-mode
 * session key, which cannot happen in a single player build, so every operation
 * refuses rather than returning a key that would silently fail to verify.
 */
public class TKeyFactory {

	private final String algorithm;

	protected TKeyFactory(String param0) {
		this.algorithm = param0;
	}

	public static TKeyFactory getInstance(String param0) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("KeyFactory: " + param0 + " is not available on this platform");
	}

	public static TKeyFactory getInstance(String param0, String param1) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("KeyFactory: " + param0 + " is not available on this platform");
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public TPublicKey generatePublic(TKeySpec param0) throws org.teavm.classlib.java.security.spec.TInvalidKeySpecException {
		throw new org.teavm.classlib.java.security.spec.TInvalidKeySpecException("KeyFactory: no key algorithm on this platform");
	}

	public TPrivateKey generatePrivate(TKeySpec param0) throws org.teavm.classlib.java.security.spec.TInvalidKeySpecException {
		throw new org.teavm.classlib.java.security.spec.TInvalidKeySpecException("KeyFactory: no key algorithm on this platform");
	}

	public org.teavm.classlib.java.security.spec.TKeySpec getKeySpec(org.teavm.classlib.java.security.TKey param0, Class<?> param1)
		throws org.teavm.classlib.java.security.spec.TInvalidKeySpecException {
		throw new org.teavm.classlib.java.security.spec.TInvalidKeySpecException("KeyFactory: no key algorithm on this platform");
	}

	public org.teavm.classlib.java.security.TKey translateKey(org.teavm.classlib.java.security.TKey param0) throws org.teavm.classlib.java.security.TInvalidKeyException {
		throw new org.teavm.classlib.java.security.TInvalidKeyException("KeyFactory: no key algorithm on this platform");
	}
}
