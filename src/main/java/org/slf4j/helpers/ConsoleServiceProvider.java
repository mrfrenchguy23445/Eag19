package org.slf4j.helpers;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J 2.0 service provider for the WASM-GC browser build.
 *
 * <p>Bound explicitly by {@code org.slf4j.LoggerFactory.findServiceProviders}
 * when no providers are discoverable via ServiceLoader (the case in this build
 * layout), so Minecraft's LOGGER and the Bootstrap-redirected System.out/err
 * actually reach the browser console instead of being swallowed by the NOP
 * fallback.
 */
public class ConsoleServiceProvider implements SLF4JServiceProvider {

	public static String REQUESTED_API_VERSION = "2.0.99";

	private final ILoggerFactory loggerFactory = new ConsoleLoggerFactory();
	private final IMarkerFactory markerFactory = new BasicMarkerFactory();
	private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequestedApiVersion() {
		return REQUESTED_API_VERSION;
	}

	@Override
	public void initialize() {
		// stateless
	}
}