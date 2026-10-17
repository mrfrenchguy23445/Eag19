/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4J2Logger switches on Level.ordinal() and reads isMoreSpecificThan().
 */
package org.apache.logging.log4j;

public final class Level {
	public static final int OFF = 0;
	public static final int FATAL = 100;
	public static final int ERROR = 200;
	public static final int WARN = 300;
	public static final int INFO = 400;
	public static final int DEBUG = 500;
	public static final int TRACE = 600;
	public static final int ALL = Integer.MAX_VALUE;

	public static final Level OFF_LEVEL = new Level(OFF, "OFF");
	public static final Level FATAL_LEVEL = new Level(FATAL, "FATAL");
	public static final Level ERROR_LEVEL = new Level(ERROR, "ERROR");
	public static final Level WARN_LEVEL = new Level(WARN, "WARN");
	public static final Level INFO_LEVEL = new Level(INFO, "INFO");
	public static final Level DEBUG_LEVEL = new Level(DEBUG, "DEBUG");
	public static final Level TRACE_LEVEL = new Level(TRACE, "TRACE");
	public static final Level ALL_LEVEL = new Level(ALL, "ALL");

	private final int level;
	private final String name;

	private Level(int level, String name) {
		this.level = level;
		this.name = name;
	}

	public int intLevel() {
		return this.level;
	}

	public boolean isMoreSpecificThan(Level other) {
		return this.level < other.level;
	}

	public String name() {
		return this.name;
	}

	public String toString() {
		return this.name;
	}
}
