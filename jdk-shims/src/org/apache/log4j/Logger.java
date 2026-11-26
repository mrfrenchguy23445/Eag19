/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4JLoggerFactory calls org.apache.log4j.Logger.getLogger(String).
 */
package org.apache.log4j;

public class Logger extends Category {
	private Logger(String name) {
		super(name);
	}

	public static Logger getLogger(String name) {
		return new Logger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}
}
