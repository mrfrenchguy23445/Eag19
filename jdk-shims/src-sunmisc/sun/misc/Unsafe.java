package sun.misc;

/*
 * NOT relocated into org.teavm.classlib.*, and NOT a T-prefixed name.
 *
 * TeaVM's classlib does not model sun.misc.Unsafe at all, and no jar on the
 * target's classpath supplies one, so the linker falls through to the host JDK's
 * own copy. That copy is compiled as if it were ordinary game code, and the JDK
 * 17 implementation of objectFieldOffset(Field) calls Class.isHidden() and
 * Class.isRecord() -- neither of which exists on the classlib's TClass. The
 * failure is reported against netty:
 *
 *   Method java.lang.Class.isHidden()Z was not found
 *       at sun.misc.Unsafe.objectFieldOffset(Unsafe.java:644)
 *       at io.netty.util.internal.PlatformDependent0.objectFieldOffset(...)
 *
 * sun is not in the classlib's exclusion rules (only java and javax.xml are), so
 * a class packaged under its real name here is indexed and used instead of the
 * host JDK's. That is what this is for.
 *
 * DO NOT DELETE THIS FILE BECAUSE OF THAT STACK TRACE.
 * ===================================================
 *
 * The two lines above make it look inert: they name Unsafe.java:644 and :647,
 * which is the HOST JDK 17's line numbering, while this file is 63 lines long
 * and has no line 644. Those frames are TeaVM reconstructing where the missing
 * method would have come from in a stock JDK, so that the error points at
 * something recognisable. They are not a record of what executed. The evidence
 * that this shim is live and working is that after it was added, a full link
 * stopped reporting:
 *
 *   Class sun.misc.Unsafe was not found
 *   Method sun.misc.Unsafe.objectFieldOffset(Ljava/lang/reflect/Field;)J
 *
 * It never reported either, while java.lang.Class.isHidden and isRecord were
 * still reported. Removing this file would restore the host copy, bring back
 * both of those errors, and cost three of the errors this one currently costs.
 * (The Class.isHidden/isRecord pair is a separate, still-open problem: it needs
 * netty's objectFieldOffset to not be reached at all, or TClass extended, and
 * neither has been done.)
 *
 * Nothing here works, and that is deliberate. netty's
 * PlatformDependent0.objectFieldOffset is the entry point to its whole
 * UnsafeFieldAccessor machinery; it only needs the call to fail, because
 * PlatformDependent then falls back to reflection-based field access, which is
 * correct and merely slower. Throwing UnsupportedOperationException rather than
 * returning a fabricated offset matters: a plausible-looking wrong offset would
 * corrupt memory silently, while the exception costs one accessor strategy.
 *
 * Compiled via a second source set patched into jdk.unsupported, because javac
 * refuses a plain sun.misc source with "package exists in another module:
 * jdk.unsupported". See jdk-shims/build.gradle.kts.
 */
public final class Unsafe {

	private static final Unsafe THE_UNSAFE = new Unsafe();

	private Unsafe() {
	}

	public static Unsafe getUnsafe() {
		return THE_UNSAFE;
	}

	public long objectFieldOffset(java.lang.reflect.Field param0) {
		throw new UnsupportedOperationException("no Unsafe under WASM-GC");
	}

	public int arrayBaseOffset(Class<?> param0) {
		throw new UnsupportedOperationException("no Unsafe under WASM-GC");
	}

	public int arrayIndexScale(Class<?> param0) {
		throw new UnsupportedOperationException("no Unsafe under WASM-GC");
	}

	public Object allocateInstance(Class<?> param0) {
		throw new UnsupportedOperationException("no Unsafe under WASM-GC");
	}

	public void compareAndSwapObject(Object param0, long param1, Object param2, Object param3) {
		throw new UnsupportedOperationException("no Unsafe under WASM-GC");
	}
}
