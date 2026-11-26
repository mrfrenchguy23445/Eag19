package org.teavm.classlib.java.util.concurrent.atomic;

/*
 * java.util.concurrent.atomic.LongAdder shim -- see jdk-shims/build.gradle.kts.
 *
 * Reached from netty's LongAdderCounter, which *extends* this class and implements
 * LongCounter on top of it. PlatformDependent.newLongCounter builds it from
 * UnpooledByteBufAllocator.<init>, so this is on the client's boot path.
 *
 * Being extended rather than merely called is why the shape matters. netty's subclass
 * declares only value(), and value() is written in netty as
 *
 *   public long value() { return longValue(); }
 *
 * so longValue() has to resolve on this type. The JDK gets it from Number, which
 * LongAdder extends; that is reproduced here rather than by declaring longValue()
 * directly, so that the remaining Number conversions come for free and the class stays
 * a subset of the real one.
 *
 * The JDK's LongAdder is a Striped64: cells are spread over a table indexed by a thread
 * hash to avoid contention, and the cells are CAS'd. None of that is reproduced. A
 * single long is the honest model here, because the contention LongAdder exists to
 * avoid cannot arise -- see the note on TAtomicReferenceArray about one worker owning
 * one instance. sum() adding the cell table is just adding one value.
 */
public class TLongAdder extends Number {

	private long value;

	public TLongAdder() {
	}

	public void add(long param0) {
		this.value += param0;
	}

	public void increment() {
		++this.value;
	}

	public void decrement() {
		--this.value;
	}

	/**
	 * The sum. With no cells, sum() and this are the same read in the JDK too -- the
	 * difference is only that the JDK would then add up a table.
	 */
	public long sum() {
		return this.value;
	}

	/**
	 * Read and clear, which is the JDK's sumThenReset. Straightforward here for the same
	 * reason sum() is: there is only the one value.
	 */
	public long sumThenReset() {
		long var1 = this.value;
		this.value = 0L;
		return var1;
	}

	/**
	 * Reset is void in the JDK and takes no argument, unlike sumThenReset. Kept as a
	 * separate method because the two are not interchangeable and a caller can tell them
	 * apart by signature alone.
	 */
	public void reset() {
		this.value = 0L;
	}

	@Override
	public int intValue() {
		return (int)this.longValue();
	}

	@Override
	public long longValue() {
		return this.value;
	}

	@Override
	public float floatValue() {
		return (float)this.longValue();
	}

	@Override
	public double doubleValue() {
		return (double)this.longValue();
	}

	@Override
	public String toString() {
		return Long.toString(this.value);
	}
}