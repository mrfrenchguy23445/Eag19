/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * log4j 1.2's Category.setLevel references this type via legacy CategoryUtil.
 */
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/** Reached only as a parameter type from org.apache.log4j.legacy.core.CategoryUtil. */
public class Logger extends ExtendedLoggerWrapper {
	public Logger(org.apache.logging.log4j.spi.ExtendedLogger logger) {
		super(logger);
	}
}
