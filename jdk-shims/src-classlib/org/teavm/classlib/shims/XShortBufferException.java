package org.teavm.classlib.shims;

import java.security.GeneralSecurityException;

/*
 * Shim for javax.crypto.ShortBufferException, named by net.minecraft.network.CipherBase's
 * throws clauses. A throws clause is not part of a method descriptor and is never resolved
 * at link time, so this class only has to exist if some site ever catches it -- none does.
 * It is carried anyway so that adding CipherBase to the reachable set cannot produce a new
 * missing-type report. Exactly the JDK shape: a GeneralSecurityException with no members.
 */
public class XShortBufferException extends GeneralSecurityException {

	private static final long serialVersionUID = 1L;

	public XShortBufferException() {
	}

	public XShortBufferException(String param0) {
		super(param0);
	}
}
