package com.google.common.cache;

import java.util.Map;
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
 * TeaVM-safe replacement for Guava's {@code Cache} interface.
 *
 * <p>Trimmed to the members this port actually calls. The upstream interface returns
 * {@link java.util.concurrent.ConcurrentMap} from {@link #asMap()} and takes a
 * {@link java.util.concurrent.Callable} in {@link #get(Object, Callable)}; neither type exists in
 * the WASM-GC TeaVM classlib, so both are replaced with JDK-only equivalents.
 */
public interface Cache<K, V> {

	V getIfPresent(Object key);

	V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException;

	ImmutableMap<K, V> getAllPresent(Iterable<?> keys);

	void put(K key, V value);

	void putAll(Map<? extends K, ? extends V> map);

	void invalidate(Object key);

	void invalidateAll(Iterable<?> keys);

	void invalidateAll();

	long size();

	CacheStats stats();

	ConcurrentMap<K, V> asMap();

	void cleanUp();

}