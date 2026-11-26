package org.teavm.classlib.java.io;

/*
 * Second reflective fallback in Gson's UnsafeAllocator. Like TObjectStreamClass,
 * this exists only so the ldc Class.forName probe links. Gson catches the lookup
 * failure and falls back to its throwing allocator; Via never reaches it.
 */
public final class TObjectInputStream {

	private TObjectInputStream() {
	}

	public static Object newInstance(Class<?> declaringClass, Class<?> targetClass) {
		throw new UnsupportedOperationException("ObjectInputStream.newInstance is not supported in WASM-GC");
	}

}
