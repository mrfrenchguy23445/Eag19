package org.teavm.classlib.java.io;

/*
 * Via's shaded Gson probes java.io.ObjectStreamClass reflectively from
 * UnsafeAllocator.create(). The probe is followed by Class.forName("...") checks
 * whose failures are caught, but TeaVM still needs the class to exist at link
 * time. The only two methods Gson looks up are supplied here; they are never
 * expected to run because every deserialized type in Via has a normal
 * constructor or is handled by a dedicated TypeAdapter.
 */
public final class TObjectStreamClass {

	private TObjectStreamClass() {
	}

	public static int getConstructorId(Class<?> type) {
		return 0;
	}

	public static Object newInstance(Class<?> type, int constructorId)
			throws InstantiationException, IllegalAccessException {
		throw new UnsupportedOperationException("ObjectStreamClass.newInstance is not supported in WASM-GC");
	}

}
