import java.io.File

pluginManagement {
	repositories {
		// eagler-local is the ONLY repository, and must stay that way. The TeaVM
		// plugin pulls teavm-core in as a *plugin* dependency, and it is that
		// classpath -- not the project's `teavm` configuration, which only holds the
		// classlib and the JS/JSO runtime jars -- that the wasm-GC backend classes
		// are loaded from: WasmGCMethodGenerator and RegisterAllocator both live in
		// teavm-core. With a remote fork repository listed, Gradle resolved the
		// pristine teavm-core from it and every patch in gradle/local-libs was
		// silently discarded, which is what a build reports as extra
		// "local.set[0] expected type (ref null N), found ... i32" bodies that V8
		// refuses to compile (teavm-patches/README.md, root cause B).
		//
		// gradle/local-libs is vendored: every plugin and every library this
		// workspace resolves -- TeaVM toolchain, Mojang libraries, the rest of
		// 1.19.4's dependency set -- is in it, so the build needs no network. To
		// refresh it after changing a dependency, see scripts/build-offline-repo.sh.
		maven {
			name = "eagler-local"
			url = uri(File(rootDir, "gradle/local-libs"))
		}
	}
}

rootProject.name = "eaglercraft-workspace"

include("target_lwjgl_desktop")
include("target_teavm_javascript")
include("target_teavm_wasm_gc")
include("jdk-shims")
