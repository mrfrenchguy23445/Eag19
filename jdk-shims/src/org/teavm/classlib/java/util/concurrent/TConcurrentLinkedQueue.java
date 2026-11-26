package org.teavm.classlib.java.util.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/*
 * java.util.concurrent.ConcurrentLinkedQueue shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from ProcessorMailbox.create, which wraps one in a StrictQueue and hands it
 * to ProcessorMailbox.<init>. ProcessorMailbox is Minecraft's mailbox: the single-
 * threaded-task mailbox the netty event loop drains. ServerList.<clinit> builds one,
 * so this is on the client's boot path.
 *
 * The JDK's version is a Michael-Scott non-blocking linked queue built on
 * AtomicReference.compareAndSet, with a self-linking sentinel head node and a
 * wait-free poll loop that must retry forever if another thread interleaves. That
 * machinery is not reproduced and the body is a plain intrusive singly-linked list.
 *
 * The reason is the same one as for TAtomicReferenceArray: each Web Worker has its own
 * copy of the module and its own heap, so a queue instance is only reachable from the
 * thread that made it and the CAS retry loop can never fail to make progress. Two
 * threads sharing one ProcessorMailbox -- which is what the JDK's queue is for -- cannot
 * happen here, because the event loop that drains it is the only holder.
 *
 * This does change the class's thread-safety contract: an instance is not safe for
 * concurrent producers here, where the real one is. The queue's interface is the JDK's
 * in full, so nothing at a call site can tell.
 */
public class TConcurrentLinkedQueue<E> extends AbstractQueue<E> {

	/**
	 * Node of the list. E is carried in the node rather than in a separate field so that
	 * a node and its payload are one allocation, which is what the JDK's AtomicReference
	 * node does too.
	 */
	private static final class Node<E> {
		final E value;
		Node<E> next;

		Node(E value) {
			this.value = value;
		}
	}

	private final Node<E> head = new Node<>(null);
	private final AtomicReference<Node<E>> tail = new AtomicReference<>(this.head);

	public TConcurrentLinkedQueue() {
	}

	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	@Override
	public int size() {
		int var1 = 0;
		for (Node<E> var2 = this.head.next; var2 != null; var2 = var2.next) {
			++var1;
		}

		return var1;
	}

	@Override
	public boolean isEmpty() {
		return this.head.next == null;
	}

	@Override
	public boolean offer(E param0) {
		if (param0 == null) {
			throw new NullPointerException();
		}

		Node<E> var1 = new Node<>(param0);
		// Appending at the tail rather than at the head. The JDK does the same: it keeps
		// the head and the tail one node apart so that offer needs no CAS on the common
		// path, and the single-writer argument here means the CAS is not needed anyway.
		for (Node<E> var2 = this.tail.get();; var2 = this.tail.get()) {
			Node<E> var3 = var2.next;
			if (var3 == null) {
				var2.next = var1;
				this.tail.set(var1);
				return true;
			}
		}
	}

	@Override
	public E poll() {
		for (Node<E> var1 = this.head; var1 != null; var1 = var1.next) {
			Node<E> var2 = var1.next;
			if (var2 != null) {
				var1.next = var2.next;
				if (var2 == this.tail.get()) {
					// The node just removed was the tail, so the tail has to fall back to
					// the head. Compare-and-set rather than a plain set because this is the
					// one place two writers could collide, and in the JDK it is a real CAS.
					this.tail.compareAndSet(var2, var1);
				}

				return var2.value;
			}
		}

		return null;
	}

	@Override
	public E peek() {
		Node<E> var1 = this.head.next;
		return var1 == null ? null : var1.value;
	}

	public E element() {
		E var1 = this.peek();
		if (var1 == null) {
			throw new NoSuchElementException();
		}

		return var1;
	}

	@Override
	public boolean remove(Object param0) {
		if (param0 == null) {
			return false;
		}

		for (Node<E> var1 = this.head; var1.next != null; var1 = var1.next) {
			if (var1.next.value.equals(param0)) {
				var1.next = var1.next.next;
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean contains(Object param0) {
		if (param0 == null) {
			return false;
		}

		for (Node<E> var1 = this.head.next; var1 != null; var1 = var1.next) {
			if (param0.equals(var1.value)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Object[] toArray() {
		Object[] var1 = new Object[this.size()];
		int var2 = 0;
		for (Node<E> var3 = this.head.next; var3 != null; var3 = var3.next) {
			var1[var2++] = var3.value;
		}

		return var1;
	}

	@Override
	public String toString() {
		return this.toArray().toString();
	}

	/** Weakly consistent, as in the JDK: it holds no lock and never throws. */
	private final class Itr implements Iterator<E> {
		private Node<E> next = TConcurrentLinkedQueue.this.head.next;
		private Node<E> lastReturned;

		public boolean hasNext() {
			return this.next != null;
		}

		public E next() {
			Node<E> var1 = this.next;
			if (var1 == null) {
				throw new NoSuchElementException();
			}

			this.next = var1.next;
			this.lastReturned = var1;
			return var1.value;
		}

		public void remove() {
			Node<E> var1 = this.lastReturned;
			if (var1 == null) {
				throw new IllegalStateException();
			}

			this.lastReturned = null;
			for (Node<E> var2 = TConcurrentLinkedQueue.this.head; var2.next != null; var2 = var2.next) {
				if (var2.next == var1) {
					var2.next = var1.next;
					return;
				}
			}

			throw new NoSuchElementException();
		}
	}
}