import java.io.File

pluginManagement {
	repositories {
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
