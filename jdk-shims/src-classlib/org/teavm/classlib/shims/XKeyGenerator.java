package org.teavm.classlib.shims;

import java.security.NoSuchAlgorithmException;
import org.teavm.classlib.shims.XSecretKey;

/*
 * Shim for javax.crypto.KeyGenerator, reached only from net.minecraft.util.Crypt.generateSecretKey
 * -- which produces the AES key the client hands an online-mode server in its login handshake.
 * That handshake (ClientHandshakePacketListenerImpl.handleHello) is the only caller, so a
 * refusing getInstance() keeps it linkable while failing loudly if single-player ever reached
 * it, which it cannot. Mirrors the java.security shims: getInstance refuses with
 * NoSuchAlgorithmException, the rest with an unchecked exception.
 */
public class XKeyGenerator {

	private XKeyGenerator() {
	}

	public static XKeyGenerator getInstance(String param0) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("KeyGenerator: " + param0 + " is not available on this platform");
	}

	public static XKeyGenerator getInstance(String param0, String param1) throws NoSuchAlgorithmException {
		throw new NoSuchAlgorithmException("KeyGenerator: " + param0 + " is not available on this platform");
	}

	public void init(int param0) {
		throw new IllegalStateException("KeyGenerator: no key algorithm on this platform");
	}

	public XSecretKey generateKey() {
		throw new IllegalStateException("KeyGenerator: no key algorithm on this platform");
	}
}
