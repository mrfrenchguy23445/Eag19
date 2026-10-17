package org.teavm.classlib.java.util.concurrent.atomic;

/*
 * java.util.concurrent.atomic.AtomicReferenceArray shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from netty's shaded jctools, which is not optional: PlatformDependent.newLongCounter
 * builds UnpooledByteBufAllocator's metric, and the allocator is on the renderer's critical
 * path. The specific need is small -- AtomicQueueUtil.allocateRefArray allocates the slot
 * array for BaseMpscLinkedAtomicArrayQueue, and that queue family then uses get / set /
 * lazySet / length.
 *
 * WHY A PLAIN ARRAY AND NO CAS LOOP
 * ==================================
 *
 * The JDK's version is a VarHandle over an Object[] and every read-modify-write is a
 * compare-and-swap retry loop. Under TeaVM there is no VarHandle, no Unsafe, and no shared
 * mutable heap between workers: each Web Worker gets its own instance of the module, so
 * an instance is only ever reached by the thread that created it. A retry loop would
 * always succeed on its first try and its whole cost would be the extra indirection.
 *
 * This is not the same as claiming jctools is safe to run concurrently here. It is: the
 * queues are used one-per-thread as the JDK intends, and what would be unsafe is two
 * threads reaching one instance, which cannot happen. Where a single-threaded read is
 * genuinely enough the body is a plain array read or write.
 *
 * The array is final and never resized: length() is fixed at construction, as in the JDK.
 */
public class TAtomicReferenceArray<E> {

	private final Object[] array;

	public TAtomicReferenceArray(int param0) {
		if (param0 < 0) {
			throw new IllegalArgumentException("array size must be non-negative");
		}

		this.array = new Object[param0];
	}

	public int length() {
		return this.array.length;
	}

	public E get(int param0) {
		return (E)this.array[this.checkIndex(param0)];
	}

	public void set(int param0, E param1) {
		this.array[this.checkIndex(param0)] = param1;
	}

	/**
	 * Bounds check shared by every accessor, kept separate from the read so that get()
	 * is a plain array read and the others can use the index directly.
	 */
	private int checkIndex(int param0) {
		if (param0 < 0 || param0 >= this.array.length) {
			throw new IndexOutOfBoundsException(param0);
		}

		return param0;
	}

	/**
	 * A plain store rather than a release store. There is no memory ordering to honour
	 * when only one thread can observe this instance -- see the note at the top of the
	 * file -- and the JDK documents this method as offering no ordering guarantees at all.
	 */
	public void lazySet(int param0, E param1) {
		this.array[this.checkIndex(param0)] = param1;
	}

	public E getAndSet(int param0, E param1) {
		int var2 = this.checkIndex(param0);
		Object var3 = this.array[var2];
		this.array[var2] = param1;
		return (E)var3;
	}

	public boolean compareAndSet(int param0, E param1, E param2) {
		int var3 = this.checkIndex(param0);
		if (this.array[var3] == param1) {
			this.array[var3] = param2;
			return true;
		}

		return false;
	}

	/**
	 * Spelled the same as compareAndSet and behaving the same. The JDK distinguishes
	 * them only by whether the compare is guaranteed not to fail spuriously; with no
	 * other thread mutating the array the question does not arise. jctools' padded slot
	 * loop calls the weak form.
	 */
	public boolean weakCompareAndSet(int param0, E param1, E param2) {
		return this.compareAndSet(param0, param1, param2);
	}


	@Override
	public String toString() {
		StringBuilder var1 = new StringBuilder();
		var1.append('[');
		for (int var2 = 0; var2 < this.array.length; ++var2) {
			if (var2 > 0) {
				var1.append(", ");
			}

			var1.append(this.array[var2]);
		}

		return var1.append(']').toString();
	}
}