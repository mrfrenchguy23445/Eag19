/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.CheckForNull;

/**
 * Contains static methods pertaining to instances of {@link Interner}.
 *
 * @author Kevin Bourrillion
 * @since 3.0
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class Interners {
  private Interners() {}

  /**
   * Builder for {@link Interner} instances.
   *
   * @since 21.0
   */
  public static class InternerBuilder {
    private final MapMaker mapMaker = new MapMaker();
    private boolean strong = true;

    private InternerBuilder() {}

    /**
     * Instructs the {@link InternerBuilder} to build a strong interner.
     *
     * @see Interners#newStrongInterner()
     */
    public InternerBuilder strong() {
      this.strong = true;
      return this;
    }

    /**
     * Instructs the {@link InternerBuilder} to build a weak interner.
     *
     * @see Interners#newWeakInterner()
     */
    @GwtIncompatible("java.lang.ref.WeakReference")
    public InternerBuilder weak() {
      this.strong = false;
      return this;
    }

    /**
     * Sets the concurrency level that will be used by the to-be-built {@link Interner}.
     *
     * @see MapMaker#concurrencyLevel(int)
     */
    public InternerBuilder concurrencyLevel(int concurrencyLevel) {
      this.mapMaker.concurrencyLevel(concurrencyLevel);
      return this;
    }

    public <E> Interner<E> build() {
      if (!strong) {
        mapMaker.weakKeys();
      }
      return new InternerImpl<>(mapMaker);
    }
  }

  /** Returns a fresh {@link InternerBuilder} instance. */
  public static InternerBuilder newBuilder() {
    return new InternerBuilder();
  }

  /**
   * Returns a new thread-safe interner which retains a strong reference to each instance it has
   * interned, thus preventing these instances from being garbage-collected. If this retention is
   * acceptable, this implementation may perform better than {@link #newWeakInterner}.
   */
  public static <E> Interner<E> newStrongInterner() {
    return newBuilder().strong().build();
  }

  /**
   * Returns a new thread-safe interner which retains a weak reference to each instance it has
   * interned, and so does not prevent these instances from being garbage-collected. This most
   * likely does not perform as well as {@link #newStrongInterner}, but is the best alternative when
   * the memory usage of that implementation is unacceptable.
   */
  @GwtIncompatible("java.lang.ref.WeakReference")
  public static <E> Interner<E> newWeakInterner() {
    return newBuilder().weak().build();
  }

  @VisibleForTesting
  static final class InternerImpl<E> implements Interner<E> {
    /**
     * Backing table for the interner.
     *
     * <p>Upstream backs this with {@link MapMakerInternalMap} built for weak-or-strong keys with
     * {@link Dummy} values. That class cannot be compiled under the WASM-GC TeaVM classlib: its
     * {@code Segment} extends {@code ReentrantLock} and its table is an {@code
     * AtomicReferenceArray}, neither of which TeaVM provides. A {@link ConcurrentHashMap} keyed on
     * {@link Equivalence#equals()} gives the same canonicalisation behaviour.
     *
     * <p>Consequence: keys are retained strongly, so {@link #newWeakInterner()} interns for the
     * lifetime of the interner rather than until collection. The only caller in this port is
     * {@code TagKey}, whose key space is the set of tag types -- small and bounded.
     */
    @VisibleForTesting final ConcurrentMap<E, Boolean> map =
        new ConcurrentHashMap<E, Boolean>();

    /**
     * @param mapMaker ignored; retained so the {@link InternerBuilder#build()} call site is
     *     unchanged. The equivalence it carried was always {@link Equivalence#equals()}, which is
     *     what {@link ConcurrentHashMap} already uses.
     */
    private InternerImpl(MapMaker mapMaker) {
    }

    @Override
    public E intern(E sample) {
      while (true) {
        // trying to read the canonical...
        Boolean present = map.get(sample);
        if (present != null) {
          // The canonical instance is the key that is already in the table. On a miss there is no
          // key to recover, so fall through and try to insert.
          E canonical = findCanonicalKey(sample);
          if (canonical != null) {
            return canonical;
          }
        }

        // didn't see it, trying to put it instead...
        Boolean sneaky = map.putIfAbsent(sample, Boolean.TRUE);
        if (sneaky == null) {
          return sample;
        } else {
          /* Someone beat us to it! Trying again...
           *
           * Technically this loop not guaranteed to terminate, so theoretically (extremely
           * unlikely) this thread might starve, but even then, there is always going to be another
           * thread doing progress here.
           */
        }
      }
    }

    /**
     * Scans the table for a key equal to {@code sample} under {@link Equivalence#equals()}.
     *
     * <p>{@link ConcurrentHashMap} matches keys with {@code equals}/{@code hashCode}, whereas the
     * interner contract is based on the caller's {@link Equivalence}. For the one caller
     * ({@code TagKey}, which keys on {@link Class}) the two agree, so a plain scan is enough; it
     * exists only to recover the canonical instance rather than the equal-but-distinct one.
     */
    @CheckForNull
    private E findCanonicalKey(E sample) {
      for (E key : map.keySet()) {
        if (key != null && Equivalence.equals().equivalent(key, sample)) {
          return key;
        }
      }
      return null;
    }
  }

  /**
   * Returns a function that delegates to the {@link Interner#intern} method of the given interner.
   *
   * @since 8.0
   */
  public static <E> Function<E, E> asFunction(Interner<E> interner) {
    return new InternerFunction<>(checkNotNull(interner));
  }

  private static class InternerFunction<E> implements Function<E, E> {

    private final Interner<E> interner;

    public InternerFunction(Interner<E> interner) {
      this.interner = interner;
    }

    @Override
    public E apply(E input) {
      return interner.intern(input);
    }

    @Override
    public int hashCode() {
      return interner.hashCode();
    }

    @Override
    public boolean equals(@CheckForNull Object other) {
      if (other instanceof InternerFunction) {
        InternerFunction<?> that = (InternerFunction<?>) other;
        return interner.equals(that.interner);
      }

      return false;
    }
  }
}
