/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4J2Logger extends this class, so all 30 inherited InternalLogger methods must resolve here.
 */
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * netty's Log4J2Logger is declared as
 * {@code extends ExtendedLoggerWrapper implements InternalLogger}, and declares
 * only the InternalLogLevel-based log() overloads plus the (Throwable) ones. The
 * remaining 30 InternalLogger methods are inherited from here, so they have to
 * exist with exactly these signatures for netty's class to link. None of them are
 * called by Log4J2Logger itself -- it only reads getName() and getMessageFactory()
 * from the wrapped logger -- so every body here is empty.
 */
public abstract class ExtendedLoggerWrapper implements ExtendedLogger {
	protected final ExtendedLogger logger;

	protected ExtendedLoggerWrapper(ExtendedLogger logger) {
		this.logger = logger;
	}

	/**
	 * The three-argument constructor, which is the one netty actually calls.
	 *
	 * Log4J2Logger.<init> builds it as
	 * {@code super((ExtendedLogger) logger, logger.getName(), logger.getMessageFactory())}
	 * -- it needs the name and factory at construction time rather than reading them back
	 * off the wrapped logger, which is why the one-argument form was never enough.
	 *
	 * The two extra arguments are discarded: the accessors below already answer from the
	 * wrapped logger, and for a stub there is nothing for a separately captured name or
	 * factory to add.
	 */
	protected ExtendedLoggerWrapper(ExtendedLogger logger, String name, MessageFactory messageFactory) {
		this.logger = logger;
	}

	public ExtendedLogger getExtendedLogger() {
		return this.logger;
	}

	@Override
	public String getName() {
		return this.logger == null ? "" : this.logger.getName();
	}

	@Override
	public Level getLevel() {
		return this.logger == null ? null : this.logger.getLevel();
	}

	@Override
	public MessageFactory getMessageFactory() {
		return this.logger == null ? null : this.logger.getMessageFactory();
	}

	@Override
	public boolean isEnabled(Level level, Marker marker, String message) {
		return false;
	}

	@Override
	public void logIfEnabled(Level level, Marker marker, String message) {
	}

	@Override
	public void logIfEnabled(Level level, Marker marker, String format, Object arg) {
	}

	@Override
	public void logIfEnabled(Level level, Marker marker, String format, Object arg1, Object arg2) {
	}

	@Override
	public void logIfEnabled(Level level, Marker marker, String format, Object... args) {
	}

	@Override
	public void logIfEnabled(Level level, Marker marker, Throwable t) {
	}

	public void trace(String message) {}
	public void trace(String format, Object arg) {}
	public void trace(String format, Object arg1, Object arg2) {}
	public void trace(String format, Object... args) {}
	public void trace(String message, Throwable t) {}
	public void debug(String message) {}
	public void debug(String format, Object arg) {}
	public void debug(String format, Object arg1, Object arg2) {}
	public void debug(String format, Object... args) {}
	public void debug(String message, Throwable t) {}
	public void info(String message) {}
	public void info(String format, Object arg) {}
	public void info(String format, Object arg1, Object arg2) {}
	public void info(String format, Object... args) {}
	public void info(String message, Throwable t) {}
	public void warn(String message) {}
	public void warn(String format, Object arg) {}
	public void warn(String format, Object arg1, Object arg2) {}
	public void warn(String format, Object... args) {}
	public void warn(String message, Throwable t) {}
	public void error(String message) {}
	public void error(String format, Object arg) {}
	public void error(String format, Object arg1, Object arg2) {}
	public void error(String format, Object... args) {}
	public void error(String message, Throwable t) {}
	public boolean isTraceEnabled() {
		return false;
	}
	public boolean isDebugEnabled() {
		return false;
	}
	public boolean isInfoEnabled() {
		return false;
	}
	public boolean isWarnEnabled() {
		return false;
	}
	public boolean isErrorEnabled() {
		return false;
	}

	/**
	 * The Level-based {@code log} family.
	 *
	 * netty's Log4J2Logger declares most of these itself, which is why only
	 * {@code log(Level, String, Throwable)} was ever reported missing: it declares that
	 * one on a different code path. It is inherited here, so the invokevirtual on
	 * Log4J2Logger resolves to this class.
	 *
	 * The rest of the family is here so the set is not left with a hole in the middle of
	 * it. All five exist on the real ExtendedLogger, so this stays a subset; bodies are
	 * empty because, as with everything else in these stubs, the wrapped logger emits
	 * nothing.
	 */
	public void log(Level level, String message) {
	}

	public void log(Level level, String format, Object arg) {
	}

	public void log(Level level, String format, Object arg1, Object arg2) {
	}

	public void log(Level level, String format, Object... args) {
	}

	public void log(Level level, String message, Throwable t) {
	}
}
