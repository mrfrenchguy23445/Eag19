package org.teavm.classlib.java.security.cert;

/*
 * Reached only from io.netty.util.internal.EmptyArrays.<clinit>, which declares
 * EMPTY_CERTIFICATES = new X509Certificate[0] for ResourceLeakDetector. Nothing
 * in Minecraft or netty ever parses an x509 certificate; this exists purely so
 * that empty array can be typed. All verification methods are left off: they are
 * declared on TCertificate already and nothing calls them.
 */
public interface TX509Certificate extends TCertificate {
}
