package org.teavm.classlib.java.security;

import java.security.*;

public class TSignatureException extends GeneralSecurityException {

	private static final long serialVersionUID = 1L;

	public TSignatureException() {
	}

	public TSignatureException(String param0) {
		super(param0);
	}

	public TSignatureException(String param0, Throwable param1) {
		super(param0, param1);
	}

	public TSignatureException(Throwable param0) {
		super(param0);
	}
}
