package org.teavm.classlib.shims;

import org.teavm.classlib.java.security.TKey;

/*
 * Shim for javax.crypto.SecretKey, reached from net.minecraft.util.Crypt.digestData /
 * getCipher / decryptByteToSecretKey and from net.minecraft.network.protocol.login.
 * ServerboundKeyPacket. Every one of those is online-mode login encryption, which this
 * single-player build never runs, so the interface carries no behaviour.
 *
 * It extends TKey (java.security.Key), exactly as the real SecretKey extends Key, because
 * Crypt.getCipher(int, Key) and Cipher.init(int, Key, ...) are handed a SecretKey. Dropping
 * the Key relationship would make the shim a superset that fails to type-check at the call.
 *
 * The javax.crypto route itself is the mapClass| one in META-INF/teavm.properties -- see the
 * header on X509Certificate for why a javax.* class file can never be read directly.
 */
public interface XSecretKey extends TKey {

}
