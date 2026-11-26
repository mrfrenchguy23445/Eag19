/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * Log4J2Logger's constructor casts its Logger argument to this type.
 */
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

public interface ExtendedLogger extends Logger {
	boolean isEnabled(Level level, Marker marker, String message);

	void logIfEnabled(Level level, Marker marker, String message);

	void logIfEnabled(Level level, Marker marker, String format, Object arg);

	void logIfEnabled(Level level, Marker marker, String format, Object arg1, Object arg2);

	void logIfEnabled(Level level, Marker marker, String format, Object... args);

	void logIfEnabled(Level level, Marker marker, Throwable t);
}
