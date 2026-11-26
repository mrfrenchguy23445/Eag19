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
			printThrowable(throwable);
		}
	}

	/**
	 * Prints the full cause chain, not just {@code toString()}. The wasm-gc build
	 * has no real stack walker (every frame is {@code Throwable$FakeClass}), but
	 * the *messages* of the wrapped causes are intact and are usually the only
	 * clue available for failures inside chunk generation.
	 */
	private static void printThrowable(Throwable throwable) {
		Throwable current = throwable;
		for (int depth = 0; current != null && depth < 12; depth++) {
			RAW_STREAM.println(depth == 0 ? String.valueOf(current) : "Caused by: " + current);
			StackTraceElement[] stack = null;
			try {
				stack = current.getStackTrace();
			} catch (Throwable ignored) {
			}
			if (stack != null) {
				for (int i = 0; i < stack.length && i < 24; i++) {
					RAW_STREAM.println("\tat " + stack[i]);
				}
			}
			Throwable cause;
			try {
				cause = current.getCause();
			} catch (Throwable ignored) {
				cause = null;
			}
			if (cause == current) {
				break;
			}
			current = cause;
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