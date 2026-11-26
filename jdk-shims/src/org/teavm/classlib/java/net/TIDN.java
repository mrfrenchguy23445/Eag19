package org.teavm.classlib.java.net;

/*
 * java.net.IDN shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from ServerAddress.getHost, which is how every server address typed into the
 * multiplayer screen is normalised before the connection is attempted.
 *
 * WHAT IS ACTUALLY MISSING AND WHY A STUB IS CORRECT
 * ===================================================
 *
 * The JDK's IDN is a full Unicode IDNA implementation: Punycode, the whole
 * Unicode 3.2 mapping tables, the Bidi rule set and nameprep. Reproducing that is a
 * large amount of table data for no gain here, and the gain really is zero.
 *
 * IDN.toASCII exists to turn a Unicode host name into the ASCII form that a DNS
 * resolver accepts. Eaglercraft never resolves anything: a multiplayer host is turned
 * into a WebSocket URL and handed to the browser, and the browser's own networking
 * stack does IDNA itself if the name needs it. So for a name that is already ASCII --
 * which is every host a player can meaningfully type -- toASCII is the identity
 * function, and that is what this returns.
 *
 * The JDK is also careful to throw IllegalArgumentException for input that is out of
 * range or malformed, and ServerAddress.getHost catches exactly that and returns "". So
 * the one behaviour worth keeping is the throwing one, and it is here: a label longer
 * than 63 characters, or a whole name longer than 255, is rejected the same way.
 * Anything else passes through unchanged.
 *
 * toUnicode is the identity for the same reason. It exists to reverse toASCII, and if
 * toASCII did nothing there is nothing to reverse.
 */
public final class TIDN {

	private TIDN() {
	}

	public static String toASCII(String param0) {
		// The JDK answers ALLOW_UNASSIGNED here too; the difference is only in which
		// unassigned code points that option permits, and neither list is modelled.
		return toASCII(param0, 0);
	}

	public static String toASCII(String param0, int param1) {
		if (param0 == null) {
			throw new IllegalArgumentException("input can not be null");
		}

		checkLength(param0);
		return param0;
	}

	public static String toUnicode(String param0) {
		return toUnicode(param0, 0);
	}

	public static String toUnicode(String param0, int param1) {
		if (param0 == null) {
			throw new IllegalArgumentException("input can not be null");
		}

		checkLength(param0);
		return param0;
	}

	/**
	 * The limits the JDK enforces, and the only part of its validation that is about
	 * structure rather than about Unicode tables. A non-ASCII name is exempt in the JDK
	 * too, because it is measured after conversion; since nothing is converted here,
	 * only an all-ASCII name is measured, which is the same judgement for the input that
	 * reaches this.
	 */
	private static void checkLength(String param0) {
		if (isAscii(param0)) {
			if (param0.length() > 255) {
				throw new IllegalArgumentException("Input too long: " + param0.length() + " chars");
			}

			for (String var1 : param0.split("\\.", -1)) {
				if (var1.length() > 63) {
					throw new IllegalArgumentException("Label too long: " + var1.length() + " chars");
				}
			}
		}
	}

	private static boolean isAscii(String param0) {
		for (int var1 = 0; var1 < param0.length(); ++var1) {
			if (param0.charAt(var1) > 0x7F) {
				return false;
			}
		}

		return true;
	}
}