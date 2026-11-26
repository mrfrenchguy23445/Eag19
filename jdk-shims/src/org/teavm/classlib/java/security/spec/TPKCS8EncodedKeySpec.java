package org.teavm.classlib.java.security.spec;

import java.security.spec.*;

/*
 * PKCS#8 PrivateKeyInfo, the format net.minecraft.util.Crypt
 * stringToPemRsaPrivateKey decodes. See X509EncodedKeySpec.
 */
public class TPKCS8EncodedKeySpec extends TEncodedKeySpec {

	public TPKCS8EncodedKeySpec(byte[] param0) {
		super(param0);
	}

	@Override
	public String getFormat() {
		return "PKCS#8";
	}
}
