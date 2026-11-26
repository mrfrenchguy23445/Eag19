package org.teavm.classlib.java.security;

import java.security.*;

/*
 * Only the RSA/SHA-256 signatures Mojang's own server list uses are ever
 * requested, and only from net.minecraft.client.multiplayer.ServerMetadata /
 * com.mojang.authlib's ServicesKeyInfo path -- online mode, which this build
 * does not have. Refusing from getInstance keeps SignatureValidator.from(...)
 * linkable while making the failure loud if it is ever reached.
 */
public class TSignature {

	public static final int SHA1withRSA = 1;
	public static final int SHA256withRSA = 2;
	public static final int SHA512withRSA = 3;
	public static final int NONEwithRSA = 4;

	private final String algorithm;

	protected TSignature(String param0) {
		this.algorithm = param0;
	}

	public static TSignature getInstance(String param0) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Signature: " + param0 + " is not available on this platform");
	}

	public static TSignature getInstance(String param0, String param1) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("Signature: " + param0 + " is not available on this platform");
	}

	public final String getAlgorithm() {
		return this.algorithm;
	}

	public final void initVerify(TPublicKey param0) throws TInvalidKeyException {
		throw new TInvalidKeyException("Signature: no key algorithm on this platform");
	}

	public final void initSign(TPrivateKey param0) throws TInvalidKeyException {
		throw new TInvalidKeyException("Signature: no key algorithm on this platform");
	}

	public final void update(byte param0) throws TSignatureException {
		throw new TSignatureException("Signature: no key algorithm on this platform");
	}

	public final void update(byte[] param0) throws TSignatureException {
		throw new TSignatureException("Signature: no key algorithm on this platform");
	}

	public final void update(byte[] param0, int param1, int param2) throws TSignatureException {
		throw new TSignatureException("Signature: no key algorithm on this platform");
	}

	public final byte[] sign() throws TSignatureException {
		throw new TSignatureException("Signature: no key algorithm on this platform");
	}

	public final boolean verify(byte[] param0) throws TSignatureException {
		throw new TSignatureException("Signature: no key algorithm on this platform");
	}

	public final void setParameter(String param0, Object param1) throws java.security.InvalidParameterException {
		throw new java.security.InvalidParameterException("Signature: no key algorithm on this platform");
	}
}
