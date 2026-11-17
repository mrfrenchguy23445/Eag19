package net.lax1dude.eaglercraft.v1_8;

import java.math.BigInteger;

public class EaglercraftVersion {

    //////////////////////////////////////////////////////////////////////

    /// Customize these to fit your fork:

    public static final String projectForkName = "BetterEagler";
    public static final String projectForkVersion = "u27";
    public static final String projectForkVendor = "XxZixiaxX";

    public static final String projectForkURL = "https://github.com/XxFluffyAsherxX/Eaglercraft-1.19";

    /// ///////////////////////////////////////////////////////////////////

    public static final String projectOriginName = "Eaglercraft 1.19";
    public static final String projectOriginAuthor = "XxZixiaxX";
    public static final String projectOriginVersion = "u27";
    public static final String projectOriginServerVersion = "";

    public static final String projectOriginURL = "";

    // EPK Version Identifier

    public static final String EPKVersionIdentifier = null; // Set to null to disable EPK version check

    // Client brand identification system configuration

    public static final EaglercraftUUID clientBrandUUID = EagUtils.makeClientBrandUUID(projectForkName);

    public static final EaglercraftUUID legacyClientUUIDInSharedWorld = EagUtils
            .makeClientBrandUUIDLegacy(projectOriginName);

    // Miscellaneous variables:

    public static final boolean forceDemoMode = false;

    public static final String localStorageNamespace = "_eaglercraft_1.0_";

    public static final String screenRecordingFilePrefix = projectOriginName + " " + projectOriginVersion;


	/**
	 * TODO(1.19.4 port): the real update-signature RSA modulus is not present in the
	 * vendored 1.14 EaglercraftVersion. Deliberately left as ZERO so that modPow
	 * cannot produce a valid signature and update verification FAILS CLOSED.
	 * Do NOT substitute a fabricated modulus - that would accept forged updates.
	 */
	public static final BigInteger updateSignatureModulus = BigInteger.ZERO;
}