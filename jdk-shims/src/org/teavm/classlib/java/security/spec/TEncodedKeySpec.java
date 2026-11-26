package org.teavm.classlib.java.security.spec;

import java.security.spec.*;

public abstract class TEncodedKeySpec implements TKeySpec {

	private final byte[] key;

	protected TEncodedKeySpec(byte[] param0) {
		if (param0 == null) {
			throw new NullPointerException("encoded key material");
		}

		this.key = param0.clone();
	}

	public byte[] getEncoded() {
		return this.key.clone();
	}

	public abstract String getFormat();
}
