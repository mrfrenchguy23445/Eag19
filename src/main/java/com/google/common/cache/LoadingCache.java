package com.google.common.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableMap;

/**
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * TeaVM-safe replacement for Guava's {@code LoadingCache}.
 *
 * <p>The upstream implementation is {@link LocalCache}, whose {@code Segment} extends
 * {@code ReentrantLock} and whose table is an {@code AtomicReferenceArray}. Neither class exists
 * in the WASM-GC TeaVM classlib, and neither is needed: the browser/WASM client is single
 * threaded, so the striped, lock-guarded table reduces to a plain map. This mirrors what upstream
 * Eaglercraft does -- its cache package is just a {@code HashMap} plus a {@link CacheProvider}.
 *
 * <p>Expiry ({@code expireAfterAccess} / {@code expireAfterWrite}) and {@code maximumSize} are
 * accepted and honoured on a lazy basis: entries carry a deadline and are dropped when they are
 * next touched or when the map exceeds its cap. This keeps the call sites' semantics without the
 * reference-counting and eviction machinery of upstream Guava.
 *
 * <p>Keys are held strongly. Upstream {@code weakKeys()} call sites (VoxelShape, Class) pass here
 * through the no-op builder method; those objects are few and bounded in practice, so this is a
 * deliberate trade of exact GC behaviour for buildability.
 */
public class LoadingCache<K, V> implements Cache<K, V> {

	/**
	 * Held as {@code CacheLoader<? super K, V>} rather than {@link CacheProvider} so a builder
	 * call passing a {@code CacheLoader<? super K1, V1>} can be forwarded without a capture
	 * conversion.
	 */
	private final CacheLoader<? super K, V> provider;
	private final Map<K, Entry<V>> cacheMap;
	private long maximumSize;
	private long expireAfterWriteNanos;
	private long expireAfterAccessNanos;

	private long hitCount;
	private long missCount;
	private long loadSuccessCount;
	private long loadExceptionCount;
	private long evictionCount;

	public LoadingCache(CacheLoader<? super K, V> provider) {
		this.provider = provider;
		this.cacheMap = new LinkedHashMap<>();
	}

	// --- configuration, applied by CacheBuilder before the cache is published ---

	void setMaximumSize(long maximumSize) {
		this.maximumSize = maximumSize;
	}

	void setExpireAfterWriteNanos(long nanos) {
		this.expireAfterWriteNanos = nanos;
	}

	void setExpireAfterAccessNanos(long nanos) {
		this.expireAfterAccessNanos = nanos;
	}

	// --- access ---

	/**
	 * Returns the cached value for {@code key}, loading and storing it on a miss.
	 *
	 * @throws ExecutionException wrapping any checked exception thrown by the loader, matching
	 *         upstream's contract for this method
	 */
	public V get(K key) throws ExecutionException {
		Entry<V> entry = lookup(key);
		if (entry != null) {
			entry.touch();
			hitCount++;
			return entry.value;
		}
		missCount++;
		try {
			V value = provider.create(key);
			loadSuccessCount++;
			store(key, value);
			return value;
		} catch (Throwable t) {
			loadExceptionCount++;
			throw new ExecutionException(t);
		}
	}

	/**
	 * Returns the cached value for {@code key}, loading it on a miss, and wraps loader failures in
	 * an unchecked exception. This is the {@code getUnchecked} counterpart of {@link #get(Object)}.
	 */
	@SuppressWarnings("unchecked")
	public V getUnchecked(K key) {
		try {
			return get(key);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@SuppressWarnings("unchecked")
	public V getIfPresent(Object key) {
		Entry<V> entry = lookup((K) key);
		if (entry == null) {
			return null;
		}
		entry.touch();
		hitCount++;
		return entry.value;
	}

	public V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
		V existing = getIfPresent(key);
		if (existing != null) {
			return existing;
		}
		try {
			V value = loader.load(key);
			loadSuccessCount++;
			store(key, value);
			return value;
		} catch (Throwable t) {
			loadExceptionCount++;
			throw new ExecutionException(t);
		}
	}

	public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
		ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
		for (Object key : keys) {
			V value = getIfPresent(key);
			if (value != null) {
				builder.put((K) key, value);
			}
		}
		return builder.build();
	}

	/** Bulk variant of {@link #getUnchecked(Object)}; loads every key that is not yet cached. */
	public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) {
		ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
		for (K key : keys) {
			builder.put(key, getUnchecked(key));
		}
		return builder.build();
	}

	// --- mutation ---

	public void put(K key, V value) {
		store(key, value);
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
			store(e.getKey(), e.getValue());
		}
	}

	public void refresh(K key) {
		try {
			V value = getUnchecked(key);
			store(key, value);
		} catch (RuntimeException e) {
			// upstream logs and swallows on refresh failure; nothing to log to here
		}
	}

	public void invalidate(Object key) {
		cacheMap.remove(key);
	}

	public void invalidateAll(Iterable<?> keys) {
		for (Object key : keys) {
			cacheMap.remove(key);
		}
	}

	public void invalidateAll() {
		cacheMap.clear();
	}

	public long size() {
		return cacheMap.size();
	}

	public CacheStats stats() {
		return new CacheStats(hitCount, missCount, loadSuccessCount, loadExceptionCount, 0L, evictionCount);
	}

	public ConcurrentMap<K, V> asMap() {
		ConcurrentMap<K, V> view = new ConcurrentHashMap<>();
		for (Map.Entry<K, Entry<V>> e : cacheMap.entrySet()) {
			view.put(e.getKey(), e.getValue().value);
		}
		return view;
	}

	public void cleanUp() {
		removeExpired(System.nanoTime());
	}

	/** Removes every entry whose write or access deadline has passed. */
	private void removeExpired(long now) {
		Iterator<Map.Entry<K, Entry<V>>> it = cacheMap.entrySet().iterator();
		while (it.hasNext()) {
			if (it.next().getValue().expired(now)) {
				it.remove();
			}
		}
	}

	// --- internals ---

	private void store(K key, V value) {
		if (value == null) {
			cacheMap.remove(key);
			return;
		}
		cacheMap.put(key, new Entry(value, System.nanoTime()));
		evictIfNeeded();
	}

	private Entry<V> lookup(K key) {
		Entry<V> entry = cacheMap.get(key);
		if (entry == null) {
			return null;
		}
		if (entry.expired(System.nanoTime())) {
			cacheMap.remove(key);
			return null;
		}
		return entry;
	}

	/**
	 * Drops expired entries, then trims the oldest insertions until the map is back within
	 * {@code maximumSize}. {@link LinkedHashMap} iterates in insertion order, so the first entries
	 * are the least recently created -- close enough to Guava's LRU for these caches.
	 */
	private void evictIfNeeded() {
		if (expireAfterWriteNanos > 0L || expireAfterAccessNanos > 0L) {
			removeExpired(System.nanoTime());
		}
		if (maximumSize > 0L && cacheMap.size() > maximumSize) {
			int toRemove = cacheMap.size() - (int) maximumSize;
			Iterator<Map.Entry<K, Entry<V>>> it = cacheMap.entrySet().iterator();
			while (toRemove-- > 0 && it.hasNext()) {
				it.next();
				it.remove();
				evictionCount++;
			}
		}
	}

	/**
	 * A cached value plus its write and access deadlines.
	 *
	 * <p>Non-static so it can read the enclosing cache's expiry settings directly; the
	 * {@code V} parameter keeps the raw value type visible at each use site.
	 */
	private final class Entry<V> {

		private final V value;
		private long writtenNanos;
		private long accessedNanos;

		Entry(V value, long now) {
			this.value = value;
			this.writtenNanos = now;
			this.accessedNanos = now;
		}

		void touch() {
			accessedNanos = System.nanoTime();
		}

		boolean expired(long now) {
			if (expireAfterWriteNanos > 0L && now - writtenNanos >= expireAfterWriteNanos) {
				return true;
			}
			return expireAfterAccessNanos > 0L && now - accessedNanos >= expireAfterAccessNanos;
		}

	}

}