package com.google.common.cache;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * TeaVM-safe replacement for Guava's CacheLoader.
 *
 * <p>The upstream class declares {@code load} overloads that reference
 * {@link java.util.concurrent.Callable} and {@link java.util.concurrent.Executor}, neither of
 * which the WASM-GC TeaVM classlib provides. Only the synchronous {@link #load(Object)} contract
 * is used by this port's call sites, so that is all that is kept here.
 */
public abstract class CacheLoader<K, V> implements CacheProvider<K, V> {

	protected CacheLoader() {
	}

	/**
	 * Computes or retrieves the value corresponding to {@code key}.
	 */
	public abstract V load(K key) throws Exception;

	/**
	 * Adapts {@link #load(Object)} to {@link CacheProvider#create(Object)}, discarding the
	 * declared checked exception. {@link LoadingCache} already unwraps loader failures into
	 * {@link java.util.concurrent.ExecutionException}-free unchecked propagation, so nothing is
	 * lost by narrowing here.
	 */
	@Override
	public V create(K key) {
		try {
			return load(key);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Bulk variant of {@link #load(Object)}. The default implementation loads keys one at a time;
	 * no loader in this port defines its own bulk behaviour.
	 */
	public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
		Map<K, V> result = new LinkedHashMap<>();
		for (K key : keys) {
			V value = load(key);
			if (value != null) {
				result.put(key, value);
			}
		}
		return result;
	}

	/**
	 * Recomputes the value for {@code key}. Returns the existing value when one is present:
	 * upstream reloads asynchronously, and every loader in this port is pure, so returning the
	 * cached entry matches observable behaviour without needing an executor.
	 */
	public V reload(K key, V oldValue) throws Exception {
		return oldValue;
	}

}