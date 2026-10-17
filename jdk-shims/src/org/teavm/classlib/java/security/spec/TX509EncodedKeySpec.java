package org.teavm.classlib.java.security.spec;

import java.security.spec.*;

/*
 * X.509 SubjectPublicKeyInfo, which is the format net.minecraft.util.Crypt
 * stringToRsaPublicKey decodes. Carried only so that method links; nothing reads
 * the bytes back, because KeyFactory refuses on this platform.
 */
public class TX509EncodedKeySpec extends TEncodedKeySpec {

	public TX509EncodedKeySpec(byte[] param0) {
		super(param0);
	}

	@Override
	public String getFormat() {
		return "X.509";
	}
}
