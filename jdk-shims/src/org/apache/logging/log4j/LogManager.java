/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * Reachable from netty's InternalLoggerFactory; see gen_log4j.py.
 */
package org.apache.logging.log4j;

import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * Entry point netty's Log4J2LoggerFactory calls: {@code LogManager.getLogger(String)},
 * whose result it then casts to ExtendedLogger.
 *
 * <p>The real LogManager.<clinit> is the single most expensive thing on netty's logging
 * path under this target. It reads a system property through
 * {@code java.util.concurrent.locks.ReentrantLock}, walks the classpath through
 * {@code ProviderUtil}, builds a properties snapshot through {@code PropertiesUtil} (which
 * wants {@code MethodHandles.lookup()}) and initialises a {@code StatusLogger} behind a
 * {@code ReentrantReadWriteLock}. None of those classes are modelled by the WASM-GC
 * classlib, and the browser client has nowhere to log to anyway.
 */
public class LogManager {

	private LogManager() {
	}

	public static Logger getLogger(String name) {
		return new SimpleLogger(name);
	}

	public static Logger getLogger() {
		return getLogger(LogManager.class.getName());
	}

	/**
	 * The minimum an ExtendedLogger has to be for netty: a name, and nothing else.
	 * ExtendedLoggerWrapper supplies every logging method as a no-op.
	 */
	private static final class SimpleLogger extends ExtendedLoggerWrapper {

		private final String name;

		SimpleLogger(String name) {
			super(null);
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}
	}
}