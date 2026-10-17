package org.teavm.classlib.java.lang.management;

import java.util.Collections;
import java.util.List;

/*
 * Reached from DebugScreenOverlay$AllocationRateCalculator.<clinit>, which calls
 * getGarbageCollectorMXBeans() to derive the allocation-rate line on the F3
 * screen. The WASM-GC heap is one fixed WebAssembly.Memory that the GC never
 * resizes, so there are no GC counters to report: the list is empty and the
 * overlay renders the field as zero rather than a real rate.
 */
public final class TManagementFactory {

	private TManagementFactory() {
	}

	public static List<Object> getGarbageCollectorMXBeans() {
		return Collections.emptyList();
	}

	public static Object getMemoryMXBean() {
		return null;
	}

	public static Object getRuntimeMXBean() {
		return null;
	}

	public static Object getThreadMXBean() {
		return null;
	}

	public static Object getPlatformMBeanServer() {
		return null;
	}
}
