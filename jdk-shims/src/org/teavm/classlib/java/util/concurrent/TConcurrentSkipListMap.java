package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.TreeMap;

/**
 * Shim for {@code java.util.concurrent.ConcurrentSkipListMap}.
 *
 * <p>Absent from the TeaVM classlib, and constructed by gson's
 * {@code ConstructorConstructor} ({@code return (T) new ConcurrentSkipListMap<>();}) when a
 * deserialization target asks for a concurrent sorted map.
 *
 * <p><b>Backed by {@link TreeMap}.</b> The JDK's version is a skip list, chosen for lock-free
 * concurrent updates. The ordering and range semantics are what callers observe, and
 * {@code TreeMap} provides identical ones; the lock-free structure is an implementation
 * detail that buys concurrency this target does not have. Extending {@code TreeMap} (rather
 * than wrapping one) means every inherited {@code NavigableMap} method -- {@code headMap},
 * {@code tailMap}, {@code subMap}, {@code descendingMap}, the {@code floor}/{@code ceiling}/
 * {@code higher}/{@code lower} family -- is the real implementation rather than a
 * hand-written copy that could drift.
 *
 * <p>The only JDK methods not inherited are the {@code java.util.concurrent} extras
 * ({@code addAndGet}, {@code computeIfAbsent}, {@code merge}, {@code replace}, {@code putIfAbsent}).
 * Those are all default methods on {@link ConcurrentMap} in modern JDKs, so they are
 * inherited too, and gson -- the only caller -- uses none of them.
 *
 * <p>Not thread-safe, which is the honest description: this is a plain {@code TreeMap} and
 * there is one thread.
 */
public class TConcurrentSkipListMap<K, V> extends TreeMap<K, V> implements TConcurrentNavigableMap<K, V> {
    private static final long serialVersionUID = 1L;

    public TConcurrentSkipListMap() {
      super();
   }

   public TConcurrentSkipListMap(java.util.SortedMap<K, ? extends V> param0) {
      super(param0);
   }

   public TConcurrentSkipListMap(java.util.Map<K, ? extends V> param0) {
      super(param0);
   }

   /**
    * Conditional remove, and the one method {@link ConcurrentMap} leaves abstract rather than
    * defaulting. {@code TreeMap} has no counterpart, so it is implemented here.
    *
    * <p>Written to match {@code ConcurrentMap}'s contract exactly: the entry is removed only
    * if it is currently mapped to {@code value}, using {@code equals} for the comparison.
    * The two {@code containsKey}/{@code equals} checks rather than a single compare-and-clear
    * are what make the "absent key" case return false instead of treating {@code null} as a
    * match.
    */
   @Override
   public boolean remove(Object key, Object value) {
      if (!this.containsKey(key) || !java.util.Objects.equals(this.get(key), value)) {
         return false;
      }

      this.remove(key);
      return true;
   }
}