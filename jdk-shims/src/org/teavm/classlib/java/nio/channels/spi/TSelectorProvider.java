package org.teavm.classlib.java.nio.channels.spi;

import java.io.IOException;

/*
 * Reached from io.netty.channel.nio.NioEventLoopGroup.<init>, which calls
 * SelectorProvider.provider(). The WASM-GC transport does not use java.nio
 * channels at all -- netty runs on its own JS-backed transport -- so this only
 * has to exist and hand back a provider. It cannot hand back a real one:
 * java.nio.channels.Selector is not modelled by the classlib either.
 */
public abstract class TSelectorProvider {

	public static TSelectorProvider provider() {
		throw new UnsupportedOperationException("java.nio.channels is not available under WASM-GC");
	}

	public abstract Object openSelector() throws IOException;

	public Object getDefaultLookup() {
		return null;
	}
}
