/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4JLogger references the TRACE/DEBUG/INFO/WARN/ERROR constants.
 */
package org.apache.log4j;

public final class Level implements Priority {
	public static final int OFF_INT = Integer.MAX_VALUE;
	public static final int FATAL_INT = 100;
	public static final int ERROR_INT = 200;
	public static final int WARN_INT = 250;
	public static final int INFO_INT = 300;
	public static final int DEBUG_INT = 400;
	public static final int TRACE_INT = 500;
	public static final int ALL_INT = Integer.MIN_VALUE;

	public static final Level OFF = new Level(OFF_INT, "OFF");
	public static final Level FATAL = new Level(FATAL_INT, "FATAL");
	public static final Level ERROR = new Level(ERROR_INT, "ERROR");
	public static final Level WARN = new Level(WARN_INT, "WARN");
	public static final Level INFO = new Level(INFO_INT, "INFO");
	public static final Level DEBUG = new Level(DEBUG_INT, "DEBUG");
	public static final Level TRACE = new Level(TRACE_INT, "TRACE");
	public static final Level ALL = new Level(ALL_INT, "ALL");

	private final int priority;
	private final String name;

	private Level(int priority, String name) {
		this.priority = priority;
		this.name = name;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public boolean isGreaterOrEqual(Priority other) {
		return this.priority >= other.getPriority();
	}

	public String toString() {
		return this.name;
	}
}
