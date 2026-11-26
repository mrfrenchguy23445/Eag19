package org.teavm.classlib.java.util.concurrent;

import java.util.*;
import java.util.concurrent.*;

/*
 * java.util.concurrent.PriorityBlockingQueue shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from Guava's Queues.newPriorityBlockingQueue, which ChunkRenderDispatcher's
 * constructor uses for the high-priority end of its chunk-compile queue. That puts it
 * on the renderer's critical path, one frame after a chunk becomes visible, so it is
 * not an optional class.
 *
 * WHICH PART OF THE API IS HERE
 * =============================
 *
 * This implements Queue, not BlockingQueue. The real class implements the latter, so
 * this is a strict subset -- which is the safe direction for a shim, since a call site
 * written against the real class either uses a method declared here or was already a
 * link error. Nothing that compiles and links against the real class can fail here for
 * want of the blocking half, because the half that is missing is exactly the half that
 * cannot work: there are no threads to block and no notifier to wake.
 *
 * The four methods the caller actually uses are all implemented faithfully --
 * ChunkRenderDispatcher reads size() and isEmpty() to keep toBatchCount straight, and
 * offer()s and poll()s to build and drain its batches.
 *
 * This is a binary heap, as the JDK's is, and NOT an unbounded array with a
 * ConcurrentSkipListSet of "out of order" nodes. The JDK's structure exists to let
 * producers that are not the consumer proceed concurrently; there is no such second
 * thread here (see TConcurrentLinkedQueue for why), so a plain heap is the honest
 * model and is also cheaper.
 */
public class TPriorityBlockingQueue<E> extends AbstractQueue<E> {

	private Object[] heap;
	private int size;
	private final java.util.Comparator<? super E> comparator;

	public TPriorityBlockingQueue() {
		this(11, null);
	}

	public TPriorityBlockingQueue(int param0) {
		this(param0, null);
	}

	/**
	 * The JDK's constructor validates the initial capacity up front; doing it here too
	 * means a bad capacity fails at the same point rather than on first insertion.
	 */
	public TPriorityBlockingQueue(int param0, java.util.Comparator<? super E> param1) {
		if (param0 < 1) {
			throw new IllegalArgumentException("initialCapacity must be at least 1");
		}

		this.heap = new Object[param0];
		this.comparator = param1;
	}

	public TPriorityBlockingQueue(int param0, java.util.Comparator<? super E> param1,
			java.util.Collection<? extends E> param2) {
		this(param0, param1);
		if (param2 != null) {
			for (E var3 : param2) {
				this.offer(var3);
			}
		}
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean offer(E param0) {
		if (param0 == null) {
			throw new NullPointerException();
		}

		int var1 = this.size;
		if (var1 == this.heap.length) {
			this.grow();
		}

		this.heap[var1] = param0;
		this.siftUp(var1);
		++this.size;
		return true;
	}

	@Override
	public E poll() {
		if (this.size == 0) {
			return null;
		}

		Object var1 = this.heap[0];
		this.heap[0] = this.heap[--this.size];
		this.heap[this.size] = null;
		if (this.size > 0) {
			this.siftDown(0);
		}

		return (E)var1;
	}

	@Override
	public E peek() {
		return this.size == 0 ? null : (E)this.heap[0];
	}

	/**
	 * Identity comparison, not equals, which is what the JDK does and matters here:
	 * the elements are ChunkCompileTask instances, and an equals-based contains would
	 * depend on a value class that the queue never promised anything about.
	 */
	@Override
	public boolean contains(Object param0) {
		return param0 != null && this.indexOf(param0) >= 0;
	}

	@Override
	public boolean remove(Object param0) {
		int var1 = param0 == null ? -1 : this.indexOf(param0);
		if (var1 < 0) {
			return false;
		}

		this.removeAt(var1);
		return true;
	}

	private int indexOf(Object param0) {
		for (int var1 = 0; var1 < this.size; ++var1) {
			if (this.heap[var1] == param0) {
				return var1;
			}
		}

		return -1;
	}

	/** Removes by position: swap with the last element, then re-establish the heap. */
	private void removeAt(int param0) {
		--this.size;
		if (param0 != this.size) {
			this.heap[param0] = this.heap[this.size];
			this.siftDown(param0);
			this.siftUp(param0);
		}

		this.heap[this.size] = null;
	}

	@Override
	public void clear() {
		for (int var1 = 0; var1 < this.size; ++var1) {
			this.heap[var1] = null;
		}

		this.size = 0;
	}

	private void grow() {
		Object[] var1 = new Object[this.heap.length << 1];
		System.arraycopy(this.heap, 0, var1, 0, this.heap.length);
		this.heap = var1;
	}

	private boolean less(Object param0, Object param1) {
		if (this.comparator != null) {
			return this.comparator.compare((E)param0, (E)param1) < 0;
		}

		return ((Comparable<Object>)param0).compareTo(param1) < 0;
	}

	private void siftUp(int param0) {
		Object var1 = this.heap[param0];
		while (param0 > 0) {
			int var2 = (param0 - 1) >>> 1;
			Object var3 = this.heap[var2];
			if (!this.less(var1, var3)) {
				break;
			}

			this.heap[param0] = var3;
			param0 = var2;
		}

		this.heap[param0] = var1;
	}

	private void siftDown(int param0) {
		int var1 = this.size;
		Object var2 = this.heap[param0];
		int var3;
		while ((var3 = (param0 << 1) + 1) < var1) {
			Object var4 = this.heap[var3];
			if (var3 + 1 < var1 && this.less(this.heap[var3 + 1], var4)) {
				var4 = this.heap[++var3];
			}

			if (!this.less(var4, var2)) {
				break;
			}

			this.heap[param0] = var4;
			param0 = var3;
		}

		this.heap[param0] = var2;
	}

	/**
	 * Heap-array order, not sorted order, exactly as the JDK's iterator: it walks the
	 * backing array, so it is weakly consistent and does not promise priority order.
	 *
	 * remove() is unsupported, as in the JDK -- a removal has to reposition other
	 * elements, which an iterator over the raw array cannot express. Callers that need
	 * to remove go through remove(Object), which can.
	 */
	@Override
	public java.util.Iterator<E> iterator() {
		return new java.util.Iterator<E>() {
			private int cursor;

			public boolean hasNext() {
				return this.cursor < TPriorityBlockingQueue.this.size;
			}

			public E next() {
				if (this.cursor >= TPriorityBlockingQueue.this.size) {
					throw new NoSuchElementException();
				}

				return (E)TPriorityBlockingQueue.this.heap[this.cursor++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		return this.toArray().toString();
	}
}