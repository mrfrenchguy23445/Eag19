package org.teavm.classlib.java.util.concurrent.atomic;

/*
 * Reached from Via's shaded Gson: Gson.<init> installs an AtomicLongArray
 * adapter next to the AtomicIntegerArray one. It only calls (int), length(),
 * get(int) and set(int,long); the rest keeps the JDK shape for callers.
 */
public class TAtomicLongArray {

	private final long[] array;

	public TAtomicLongArray(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("array size must be non-negative");
		}
		this.array = new long[length];
	}

	public int length() {
		return array.length;
	}

	public long get(int index) {
		return array[checkIndex(index)];
	}

	public void set(int index, long value) {
		array[checkIndex(index)] = value;
	}

	public void lazySet(int index, long value) {
		array[checkIndex(index)] = value;
	}

	public long getAndSet(int index, long value) {
		int i = checkIndex(index);
		long previous = array[i];
		array[i] = value;
		return previous;
	}

	public boolean compareAndSet(int index, long expect, long update) {
		int i = checkIndex(index);
		if (array[i] != expect) {
			return false;
		}
		array[i] = update;
		return true;
	}

	public boolean weakCompareAndSet(int index, long expect, long update) {
		return compareAndSet(index, expect, update);
	}

	public long getAndIncrement(int index) {
		int i = checkIndex(index);
		return array[i]++;
	}

	public long getAndDecrement(int index) {
		int i = checkIndex(index);
		return array[i]--;
	}

	public long getAndAdd(int index, long delta) {
		int i = checkIndex(index);
		long previous = array[i];
		array[i] = previous + delta;
		return previous;
	}

	public long incrementAndGet(int index) {
		int i = checkIndex(index);
		return ++array[i];
	}

	public long decrementAndGet(int index) {
		int i = checkIndex(index);
		return --array[i];
	}

	public long addAndGet(int index, long delta) {
		int i = checkIndex(index);
		array[i] += delta;
		return array[i];
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(array[i]);
		}
		return builder.append(']').toString();
	}

	private int checkIndex(int index) {
		if (index < 0 || index >= array.length) {
			throw new IndexOutOfBoundsException(index);
		}
		return index;
	}

}
