/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * log4j 1.2's Logger.getLogger() constructs via a Hierarchy; netty's Log4JLoggerFactory reaches Hierarchy$PrivateLoggerAdapter.
 */
package org.apache.log4j;

public class Hierarchy {
	private static final Hierarchy INSTANCE = new Hierarchy();

	protected Hierarchy() {
	}

	public static Hierarchy getInstance() {
		return INSTANCE;
	}
}
