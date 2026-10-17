package org.teavm.classlib.java.security.cert;

import java.security.cert.*;

import org.teavm.classlib.java.security.TKey;

/*
 * Reached only from io.netty.util.internal.EmptyArrays.<clinit>, which declares
 * EMPTY_CERTIFICATES = new Certificate[0]. Nothing in Minecraft parses x509
 * certificates, so this is the marker half of the JDK interface with the
 * verification methods kept at their real signatures minus the provider
 * exceptions (javax.security / java.security.NoSuchProviderException are not
 * modelled and nothing here would ever raise them).
 */
public interface TCertificate extends TKey {

	void verify(org.teavm.classlib.java.security.TPublicKey param0) throws TCertificateException;

	void verify(org.teavm.classlib.java.security.TPublicKey param0, String param1) throws TCertificateException;

	org.teavm.classlib.java.security.TPublicKey getPublicKey();

	void checkValidity();

	void checkValidity(java.util.Date param0);

	int getType();
}
