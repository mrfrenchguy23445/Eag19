package org.teavm.classlib.java.security;

import java.security.*;

public class TKeyPair {

	private final TPublicKey publicKey;
	private final TPrivateKey privateKey;

	public TKeyPair(TPublicKey param0, TPrivateKey param1) {
		this.publicKey = param0;
		this.privateKey = param1;
	}

	public TPublicKey getPublic() {
		return this.publicKey;
	}

	public TPrivateKey getPrivate() {
		return this.privateKey;
	}
}
