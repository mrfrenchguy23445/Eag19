package org.teavm.classlib.java.util.concurrent.atomic;

/*
 * Reached from Via's shaded Gson: Gson 2.10 installs a TypeAdapter for
 * java.util.concurrent.atomic.AtomicIntegerArray in Gson.<init>. The adapter
 * only calls (int), length(), get(int) and set(int,int). The remaining methods
 * are the JDK-shaped operations callers could reasonably expect; WASM-GC runs
 * one worker per module, so plain array access is sufficient.
 */
public class TAtomicIntegerArray {

	private final int[] array;

	public TAtomicIntegerArray(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("array size must be non-negative");
		}
		this.array = new int[length];
	}

	public int length() {
		return array.length;
	}

	public int get(int index) {
		return array[checkIndex(index)];
	}

	public void set(int index, int value) {
		array[checkIndex(index)] = value;
	}

	public void lazySet(int index, int value) {
		array[checkIndex(index)] = value;
	}

	public int getAndSet(int index, int value) {
		int i = checkIndex(index);
		int previous = array[i];
		array[i] = value;
		return previous;
	}

	public boolean compareAndSet(int index, int expect, int update) {
		int i = checkIndex(index);
		if (array[i] != expect) {
			return false;
		}
		array[i] = update;
		return true;
	}

	public boolean weakCompareAndSet(int index, int expect, int update) {
		return compareAndSet(index, expect, update);
	}

	public int getAndIncrement(int index) {
		int i = checkIndex(index);
		return array[i]++;
	}

	public int getAndDecrement(int index) {
		int i = checkIndex(index);
		return array[i]--;
	}

	public int getAndAdd(int index, int delta) {
		int i = checkIndex(index);
		int previous = array[i];
		array[i] = previous + delta;
		return previous;
	}

	public int incrementAndGet(int index) {
		int i = checkIndex(index);
		return ++array[i];
	}

	public int decrementAndGet(int index) {
		int i = checkIndex(index);
		return --array[i];
	}

	public int addAndGet(int index, int delta) {
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
