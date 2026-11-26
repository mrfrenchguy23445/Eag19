package org.teavm.classlib.shims;

import org.teavm.classlib.java.security.spec.TAlgorithmParameterSpec;

/*
 * Shim for javax.crypto.spec.IvParameterSpec, reached only from net.minecraft.util.getCipher
 * -- the AES/CFB8 initialisation vector, which is the key's own bytes again. It is the
 * argument to Cipher.init(int, Key, AlgorithmParameterSpec) and nothing ever reads it, so
 * this is a plain holder. The real class is not final and exposes getIV(); both are kept
 * so the shim stays a faithful subset.
 */
public class XIvParameterSpec implements TAlgorithmParameterSpec {

	private final byte[] iv;

	public XIvParameterSpec(byte[] param0) {
		if (param0 == null) {
			throw new NullPointerException("iv");
		}

		this.iv = param0.clone();
	}

	public XIvParameterSpec(byte[] param0, int param1, int param2) {
		if (param0 == null) {
			throw new NullPointerException("iv");
		}

		this.iv = param0.clone();
	}

	public byte[] getIV() {
		return this.iv.clone();
	}
}
