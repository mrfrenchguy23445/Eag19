package org.slf4j.helpers;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * A minimal console SLF4J logger for the WASM-GC browser build.
 *
 * <p>This build ships no SLF4J provider, so LoggerFactory falls back to the
 * NOP implementation and every log record disappears - including every
 * System.out/System.err write made after Bootstrap.wrapStreams() redirects
 * them into LOGGER. ClientMain only stays visible because it caches the raw
 * stream before that swap.
 *
 * <p>We need Minecraft's LOGGER (and therefore the redirected streams) alive
 * to see where boot dies, so this logger formats the message and writes it to
 * the ORIGINAL System.out captured before Bootstrap.wrapStreams() replaced it
 * (this class is first touched by the very first getLogger() call, which
 * happens before the swap). Writing to the pre-swap stream avoids the loop
 * System.out -> LoggedPrintStream -> LOGGER -> System.out.
 */
public class ConsoleLogger extends AbstractLogger {

	private static final long serialVersionUID = 1L;

	/** The pre-Bootstrap System.out. Guaranteed raw because the first ever
	 *  getLogger() binds this provider before Bootstrap.bootStrap() runs. */
	private static final PrintStream RAW_STREAM = System.out;

	private final boolean isWarnLevel;

	public ConsoleLogger(String name) {
		this.name = name;
		this.isWarnLevel = name.contains("Warn") || name.endsWith("WARNING");
	}

	@Override
	protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
		if (arguments != null && arguments.length > 0) {
			msg = MessageFormatter.basicArrayFormat(msg, arguments);
		}
		StringBuilder sb = new StringBuilder(msg.length() + 32);
		sb.append("[console/").append(level.name()).append("][").append(name).append("]: ").append(msg);
		RAW_STREAM.println(sb.toString());
		if (throwable != null) {
			RAW_STREAM.println(String.valueOf(throwable));
		}
	}

	@Override
	protected String getFullyQualifiedCallerName() {
		return name;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return true;
	}
}