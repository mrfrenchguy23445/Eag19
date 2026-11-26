/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4JLogger calls isTraceEnabled/isDebugEnabled/isInfoEnabled and log(String,Priority,Object,Throwable) on this type via Logger.
 */
package org.apache.log4j;

import org.apache.log4j.legacy.core.CategoryUtil;

public class Category {
	private String name;
	private Level level;

	protected Category(String name) {
		this.name = name;
		this.level = null;
	}

	public String getName() {
		return this.name;
	}

	public Level getLevel() {
		return this.level;
	}

	public synchronized void setLevel(Level level) {
		this.level = level;
		CategoryUtil.setLevel(null, this);
	}

	public boolean isTraceEnabled() {
		return false;
	}

	/**
	 * Present because log4j 1.2 declares it on Category, and netty's Log4JLogger calls
	 * it eight times through org.apache.log4j.Logger: once per level, in
	 * isTraceEnabled/isDebugEnabled/isInfoEnabled/isWarnEnabled/isErrorEnabled and the
	 * three enabled(InternalLogLevel) forms.
	 *
	 * Declared here rather than on Logger so that the call site resolves the way it does
	 * against the real jar -- Logger inherits it, and an invokevirtual on Logger finds
	 * an inherited method. Answering false is consistent with the five
	 * isXxxEnabled() above it, and is what makes the rest of netty's log path inert.
	 */
	public boolean isEnabledFor(Priority level) {
		if (level == null) {
			throw new IllegalArgumentException("You must pass a non-null priority level.");
		}

		return false;
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isInfoEnabled() {
		return false;
	}

	public void log(String fqcn, Priority level, Object message, Throwable t) {
	}

	public void log(String fqcn, Priority level, Object message, Throwable t, Object arg) {
	}
}
