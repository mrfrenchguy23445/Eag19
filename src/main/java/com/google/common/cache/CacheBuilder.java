package com.google.common.cache;

import java.util.concurrent.TimeUnit;

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
 * TeaVM-safe replacement for Guava's {@code CacheBuilder}.
 *
 * <p>The upstream builder carries roughly forty configuration methods and hands off to
 * {@link LocalCache}, the lock-striped cache that cannot compile under TeaVM. This port only ever
 * calls {@code newBuilder()}, {@code weakKeys()}, {@code maximumSize(long)},
 * {@code expireAfterAccess(long, TimeUnit)}, {@code expireAfterWrite(long, TimeUnit)} and
 * {@code build(CacheLoader)}, so those are what is implemented here.
 *
 * <p>{@link #weakKeys()} is accepted and ignored: see {@link LoadingCache} for why strong
 * references are an acceptable substitute here.
 */
public final class CacheBuilder<K, V> {

	private long maximumSize;
	private long expireAfterWriteNanos;
	private long expireAfterAccessNanos;

	private CacheBuilder() {
	}

	public static CacheBuilder<Object, Object> newBuilder() {
		return new CacheBuilder<>();
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> weakKeys() {
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> weakValues() {
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> maximumSize(long size) {
		this.maximumSize = size;
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> expireAfterWrite(long duration, TimeUnit unit) {
		this.expireAfterWriteNanos = unit.toNanos(duration);
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> expireAfterAccess(long duration, TimeUnit unit) {
		this.expireAfterAccessNanos = unit.toNanos(duration);
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> refreshAfterWrite(long duration, TimeUnit unit) {
		// no-op: refreshAfterWrite only forces background reloads, which a single-threaded
		// cache has no way to schedule without an executor
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> initialCapacity(int size) {
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> concurrencyLevel(int level) {
		// no-op: the replacement cache is not striped
		return (CacheBuilder<K1, V1>) this;
	}

	@SuppressWarnings("unchecked")
	public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> recordStats() {
		// counters are always maintained, so this is already the behaviour
		return (CacheBuilder<K1, V1>) this;
	}

	/**
	 * Builds the cache.
	 *
	 * <p>The type variables are re-declared here rather than inherited from this builder's
	 * {@code K}/{@code V}. That mirrors upstream Guava and is required for the call sites:
	 * {@code CacheBuilder.newBuilder()} yields a {@code CacheBuilder<Object, Object>}, so the
	 * key and value types have to be recovered from the loader argument instead of from the
	 * builder.
	 */
	public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(CacheLoader<? super K1, V1> loader) {
		LoadingCache<K1, V1> cache = new LoadingCache<K1, V1>(loader);
		cache.setMaximumSize(maximumSize);
		cache.setExpireAfterWriteNanos(expireAfterWriteNanos);
		cache.setExpireAfterAccessNanos(expireAfterAccessNanos);
		return cache;
	}

	public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build() {
		return build(new CacheLoader<K1, V1>() {

			@Override
			public V1 load(K1 key) {
				return null;
			}

		});
	}

}