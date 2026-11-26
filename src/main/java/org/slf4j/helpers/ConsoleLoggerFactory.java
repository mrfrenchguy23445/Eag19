package org.slf4j.helpers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * ILoggerFactory that returns per-name {@link ConsoleLogger} instances.
 */
public class ConsoleLoggerFactory implements ILoggerFactory {

	private final Map<String, Logger> loggers = new HashMap<>();

	@Override
	public Logger getLogger(String name) {
		synchronized (loggers) {
			Logger logger = loggers.get(name);
			if (logger == null) {
				logger = new ConsoleLogger(name);
				loggers.put(name, logger);
			}
			return logger;
		}
	}
}