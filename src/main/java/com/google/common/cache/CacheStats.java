package com.google.common.cache;

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
 * Snapshot of cache counters.
 *
 * <p>The upstream class takes its counters from {@link LongAdder}, whose {@code Striped64} base
 * resolves a thread-local subclass through {@code Class.forName} and reflection; neither the
 * subclass lookup nor {@code java.lang.reflect} plumbing is available under TeaVM. Plain
 * {@code long} fields are used instead -- correct, because the WASM-GC client is single threaded.
 */
public final class CacheStats {

	private final long hitCount;
	private final long missCount;
	private final long loadSuccessCount;
	private final long loadExceptionCount;
	private final long totalLoadTime;
	private final long evictionCount;

	public CacheStats(long hitCount, long missCount, long loadSuccessCount, long loadExceptionCount,
			long totalLoadTime, long evictionCount) {
		this.hitCount = hitCount;
		this.missCount = missCount;
		this.loadSuccessCount = loadSuccessCount;
		this.loadExceptionCount = loadExceptionCount;
		this.totalLoadTime = totalLoadTime;
		this.evictionCount = evictionCount;
	}

	/** Number of times {@code Cache.get} or {@code LoadingCache.get} found a cached value. */
	public long hitCount() {
		return hitCount;
	}

	/** Ratio of cache requests that returned a cached value, in [0.0, 1.0]. */
	public double hitRate() {
		long requestCount = requestCount();
		return (requestCount == 0) ? 1.0 : (double) hitCount / requestCount;
	}

	/** Number of times a cached value was not found. */
	public long missCount() {
		return missCount;
	}

	/** Ratio of cache requests that were misses, in [0.0, 1.0]. */
	public double missRate() {
		long requestCount = requestCount();
		return (requestCount == 0) ? 0.0 : (double) missCount / requestCount;
	}

	/** Total number of times the cache looked up a value, hit or miss. */
	public long requestCount() {
		return hitCount + missCount;
	}

	/** Number of times the cache successfully loaded a new value. */
	public long loadCount() {
		return loadSuccessCount + loadExceptionCount;
	}

	/** Number of times the cache threw while loading a new value. */
	public long loadExceptionCount() {
		return loadExceptionCount;
	}

	/** Number of times the cache successfully loaded a new value. */
	public long loadSuccessCount() {
		return loadSuccessCount;
	}

	/** Total time spent loading new values, in nanoseconds. */
	public long totalLoadTime() {
		return totalLoadTime;
	}

	/** Number of entries evicted from the cache. */
	public long evictionCount() {
		return evictionCount;
	}

}