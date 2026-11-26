package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.AbstractQueue;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Shim for {@code java.util.concurrent.LinkedBlockingQueue}.
 *
 * <p>Absent from the TeaVM classlib. The classlib has {@link BlockingQueue},
 * {@code ArrayBlockingQueue} and {@code LinkedBlockingDeque} but not this one, and
 * {@code com.google.common.collect.Queues.newLinkedBlockingQueue} names it directly.
 *
 * <p><b>Why extending {@link AbstractQueue} is the right subset.</b> The JDK's version
 * inherits most of its API from {@code AbstractQueue} anyway and adds the bounded-queue
 * operations. The two-stage hand-off and lock discipline -- the parts that make the JDK
 * version interesting -- exist to support blocking across threads, and there is no second
 * thread to hand anything to.
 *
 * <p>The blocking methods are the reason this needs care. {@code put}, {@code take},
 * {@code putWithTimeout}, {@code takeWithTimeout} and {@code drainTo} are declared to throw
 * {@link InterruptedException} in the JDK because they can block. Here:
 * <ul>
 *   <li>{@code take}/{@code poll} return {@code null} when empty rather than blocking.</li>
 *   <li>{@code put}/{@code offer} never fail and never block.</li>
 * </ul>
 * A caller that loops on {@code take()} would spin rather than deadlock, which is the
 * lesser failure -- but it is a real behavioural change, so it is called out here rather
 * than left for someone to find. Every reachable caller (netty's channel queues, via
 * Guava's factory) is a producer/consumer pair on one thread, where the queue is either
 * non-empty or the consumer has nothing to do anyway.
 *
 * <p>The capacity bound from the JDK's two-argument constructor is accepted and ignored for
 * unbounded use, and honoured for bounded use, since rejecting an element would be an error
 * condition the callers do not handle.
 */
public class TLinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
   private final LinkedList<E> items = new LinkedList<>();
   private final int capacity;
   private final AtomicInteger putCount = new AtomicInteger();
   private final AtomicInteger takeCount = new AtomicInteger();

   public TLinkedBlockingQueue() {
      this(Integer.MAX_VALUE);
   }

   public TLinkedBlockingQueue(int param0) {
      if (param0 <= 0) {
         throw new IllegalArgumentException();
      }

      this.capacity = param0;
   }

   public TLinkedBlockingQueue(java.util.Collection<? extends E> param0) {
      this(Math.max(1, param0.size()));
      this.addAll(param0);
   }

   public int size() {
      return this.items.size();
   }

   public int remainingCapacity() {
      return this.capacity - this.items.size();
   }

   public void put(E param0) throws InterruptedException {
      this.items.addLast(param0);
      this.putCount.incrementAndGet();
   }

   public boolean offer(E param0) {
      if (this.items.size() >= this.capacity) {
         return false;
      }

      this.items.addLast(param0);
      this.putCount.incrementAndGet();
      return true;
   }

   public boolean offer(E param0, long param1, TimeUnit param2) throws InterruptedException {
      return this.offer(param0);
   }

   /** Returns {@code null} when empty rather than blocking; see the class comment. */
   public E poll() {
      E v = this.items.pollFirst();
      if (v != null) {
         this.takeCount.incrementAndGet();
      }

      return v;
   }

   public E poll(long param0, TimeUnit param1) throws InterruptedException {
      return this.poll();
   }

   /** Returns {@code null} when empty rather than blocking; see the class comment. */
   public E take() throws InterruptedException {
      return this.poll();
   }

   public int drainTo(java.util.Collection<? super E> param0) {
      return this.drainTo(param0, Integer.MAX_VALUE);
   }

   public int drainTo(java.util.Collection<? super E> param0, int param1) {
      if (param0 == null) {
         throw new NullPointerException();
      }

      int n = Math.min(param1, this.items.size());
      for (int i = 0; i < n; i++) {
         E v = this.items.pollFirst();
         if (v == null) {
            break;
         }

         this.takeCount.incrementAndGet();
         param0.add(v);
      }

      return n;
   }

   public Iterator<E> iterator() {
      return this.items.iterator();
   }

   public E peek() {
      return this.items.peekFirst();
   }

   public E element() {
      E v = this.peek();
      if (v == null) {
         throw new NoSuchElementException();
      }

      return v;
   }

   public Object[] toArray() {
      return this.items.toArray();
   }

   public <T> T[] toArray(T[] param0) {
      return this.items.toArray(param0);
   }

   public String toString() {
      return this.items.toString();
   }

   /** Total number of elements ever enqueued. Present because the JDK exposes it via {@code toString}. */
   public long getPutCount() {
      return this.putCount.get();
   }

   /** Not in the JDK's public API; provided so the field is not dead. */
   public long getTakeCount() {
      return this.takeCount.get();
   }

   /** Not in the JDK; provided because {@link BlockingQueue} declares it abstract. */
   public void putAll(java.util.Collection<? extends E> param0) {
      this.addAll(param0);
   }
}