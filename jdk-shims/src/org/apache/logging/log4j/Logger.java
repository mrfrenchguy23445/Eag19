/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * Log4J2Logger's constructor calls getName() on this type.
 */
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.MessageFactory;

public interface Logger {
	String getName();

	Level getLevel();

	MessageFactory getMessageFactory();
}
