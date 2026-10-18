package org.teavm.classlib.java.security.spec;

/*
 * java.security.spec.AlgorithmParameterSpec shim -- see jdk-shims/build.gradle.kts.
 *
 * Marker interface, exactly as in the JDK. The classlib ships nothing under
 * java/security/spec/ at all, so java.security.spec.KeySpec / EncodedKeySpec /
 * PKCS8EncodedKeySpec / X509EncodedKeySpec / InvalidKeySpecException were hand-shipped
 * here by the same reasoning. AlgorithmParameterSpec is the last one net.minecraft.util.Crypt
 * needs: it appears only in Cipher.init(int, Key, AlgorithmParameterSpec), which is the
 * AES/CFB8 initialisation in Crypt.getCipher -- the online-mode connection encryption.
 * No parameter object is ever read, only declared as a parameter type, so the interface
 * is left empty.
 */
public interface TAlgorithmParameterSpec {
}
