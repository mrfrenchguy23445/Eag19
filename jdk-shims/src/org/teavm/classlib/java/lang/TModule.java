/*
 * java.lang.Module shim -- see jdk-shims/build.gradle.kts.
 *
 * The WASM-GC classlib models java.lang.Class.getModule() but ships no
 * java.lang.Module, so the one place netty reaches it,
 *
 *   io.netty.util.internal.PlatformDependent.<clinit>
 *     -> PlatformDependent.addressSize
 *     -> PlatformDependent0.addressSize
 *     -> sun.misc.Unsafe.<clinit>
 *     -> jdk.internal.reflect.Reflection.<clinit>   ("private static final Module baseModule = ...")
 *
 * fails to link. Nothing ever calls through this type: it is a marker, read only
 * to decide whether a member is exported to a module, and the browser client has
 * no modules. getName() therefore returns null, which is the same answer the
 * classlib's own TModule-based paths give for the unnamed module.
 */

package org.teavm.classlib.java.lang;

public final class TModule {

	private TModule() {
	}

	public String getName() {
		return null;
	}

	public boolean isNamed() {
		return false;
	}

	public boolean isUnnamed() {
		return true;
	}

	public ClassLoader getClassLoader() {
		return null;
	}

	public boolean isExported(String param0) {
		return true;
	}

	public boolean isExported(String param0, TModule param1) {
		return true;
	}

	public boolean canRead(TModule param0) {
		return true;
	}

	@Override
	public String toString() {
		return "unnamed module";
	}
}
