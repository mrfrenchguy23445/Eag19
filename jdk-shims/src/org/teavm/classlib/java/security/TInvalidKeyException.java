package org.teavm.classlib.java.security;

import java.security.*;

public class TInvalidKeyException extends TKeyException {

	private static final long serialVersionUID = 1L;

	public TInvalidKeyException() {
	}

	public TInvalidKeyException(String param0) {
		super(param0);
	}

	public TInvalidKeyException(String param0, Throwable param1) {
		super(param0, param1);
	}

	public TInvalidKeyException(Throwable param0) {
		super(param0);
	}
}
