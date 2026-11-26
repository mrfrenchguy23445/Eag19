package org.teavm.classlib.java.util.concurrent;

import java.util.concurrent.*;

import java.util.NavigableMap;

/**
 * Shim for {@code java.util.concurrent.ConcurrentNavigableMap}.
 *
 * <p>Absent from the TeaVM classlib, which has {@link ConcurrentMap} and
 * {@link NavigableMap} but not their intersection. It is named by gson's
 * {@code ConstructorConstructor}, which tests {@code ConcurrentNavigableMap.class
 * .isAssignableFrom(rawType)} to decide whether a requested map type is a concurrent sorted
 * map.
 *
 * <p>That {@code isAssignableFrom} call is the reason this must be an interface with exactly
 * this shape: gson's answer has to agree with the JDK's, or a {@code ConcurrentSkipListMap}
 * field in a deserialization target would silently get the wrong map implementation.
 * Declaring it as {@code extends ConcurrentMap, NavigableMap} reproduces the JDK's
 * hierarchy exactly, so the same {@code isAssignableFrom} yields the same answer.
 *
 * <p>No members are declared. Every method is inherited from {@link ConcurrentMap} or
 * {@link NavigableMap}, both of which the classlib already models, so there is nothing to
 * add and nothing that could diverge.
 */
public interface TConcurrentNavigableMap<K, V> extends ConcurrentMap<K, V>, NavigableMap<K, V> {
}