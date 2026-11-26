/*
 * java.security.Key shim -- see jdk-shims/build.gradle.kts.
 *
 * Marker interface, exactly as in the JDK. Present because
 * net.minecraft.util.Crypt.rsaStringToKey types its lambda as Key, and because
 * netty's io.netty.util.internal.EmptyArrays.<clinit> declares
 * java.security.cert.Certificate[0].
 *
 * No algorithm is ever run through these types: RSA is only used to validate
 * online-mode session keys, and this build is single player. KeyFactory is kept
 * anyway so that Crypt's methods link; it refuses at the call instead.
 */

package org.teavm.classlib.java.security;

import java.security.*;

public interface TKey extends java.io.Serializable {

	String NONE = "none";

	String getAlgorithm();

	String getFormat();

	byte[] getEncoded();
}
