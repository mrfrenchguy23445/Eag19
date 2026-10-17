package net.lax1dude.eaglercraft.v1_8;

import net.lax1dude.eaglercraft.v1_8.log4j.LogManager;
import net.lax1dude.eaglercraft.v1_8.log4j.Logger;

/**
 * Boot-phase diagnostic helper. Emits a marker on all three console channels at
 * once so we can tell which channel survives into the browser console:
 * System.out, System.err, and the Eaglercraft log4j-ish logger (which itself
 * ultimately prints on System.out/System.err but adds the timestamped
 * [main/LEVEL][name] prefix the browser console has been shown to carry).
 *
 * The Minecraft SLF4J LOGGER is a NOP in this build ("No SLF4J providers were
 * found"), so this is how boot progress reaches the console.
 */
public class BootDiag {

	private static final Logger EAGLOG = LogManager.getLogger("Diag");

	public static void probe(String msg) {
		try {
			System.out.println("[probe][out] " + msg);
		} catch (Throwable t) {
			System.out.println("[probe][out-fail] " + msg + ": " + t);
		}
		try {
			System.err.println("[probe][err] " + msg);
		} catch (Throwable t) {
			System.err.println("[probe][err-fail] " + msg + ": " + t);
		}
		try {
			EAGLOG.info("[probe] {}", msg);
		} catch (Throwable t) {
			try {
				System.err.println("[probe][eag-fail] " + msg + ": " + t);
			} catch (Throwable t2) {
				// nothing left to try
			}
		}
	}
}