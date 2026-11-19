/*
 * java.nio.channels.NotYetConnectedException shim for Netty's EmbeddedChannel.
 *
 * Netty's AbstractChannel$AbstractUnsafe.flush0() throws this when a generated
 * Via packet is written before the embedded channel is registered. The classlib
 * does not model java.nio.channels at all; TeaVM's T-prefix mapping turns this
 * into the real java.* name without needing a mapClass rule.
 */
package org.teavm.classlib.java.nio.channels;

import org.teavm.classlib.java.lang.TIllegalStateException;
import org.teavm.classlib.java.lang.TThrowable;

public class TNotYetConnectedException extends TIllegalStateException {

	private static final long serialVersionUID = -4545522206153388169L;

	public TNotYetConnectedException() {
		super();
	}

	public TNotYetConnectedException(String message) {
		super(message);
	}

	public TNotYetConnectedException(String message, TThrowable cause) {
		super(message, cause);
	}

	public TNotYetConnectedException(TThrowable cause) {
		super(cause);
	}
}
