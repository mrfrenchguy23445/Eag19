import com.resentclient.oss.eaglercraft.build.impl.wasm
import java.io.File
import org.teavm.gradle.api.OptimizationLevel

plugins {
	id("java")
	id("org.teavm") version "0.12.1-EAGLER-R3"
	id("com.resentclient.oss.eaglercraft.build") version "0.0.0"
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
	named("main") {
		java.srcDirs(
			"../src/main/java",
			"../src/protocol-game/java",
			"../src/protocol-relay/java",
			"../src/platform-shim/java",
			"../src/wasm-gc-teavm/java"
		)
		java.exclude("net/optifine/**")
		java.exclude("dev/redstudio/**")
		resources.srcDirs(
			"../src/teavm/resources"
		)
	}
}



configurations.configureEach {
	exclude(group = "com.google.code.gson", module = "gson")
	exclude(group = "org.slf4j")
	exclude(group = "org.joml")
	exclude(group = "com.mojang", module = "logging")
	exclude(group = "org.apache.logging.log4j")
	exclude(group = "com.google.guava")
	exclude(group = "com.mojang", module = "text2speech")
	exclude(group = "com.github.oshi")
	exclude(group = "net.java.dev.jna")
}

dependencies {
	teavm(teavm.libs.jso)
	teavm(teavm.libs.jsoApis)
	compileOnly("org.teavm:teavm-core:0.17.0")
	implementation(libs.jorbis)
	implementation(libs.bundles.common)
	implementation(libs.bundles.mc1194)
	implementation(libs.bundles.via)
	implementation("com.google.errorprone:error_prone_annotations:2.53.0")
	implementation("org.checkerframework:checker-qual:3.12.0")
	implementation("com.google.j2objc:j2objc-annotations:1.3")
	implementation(project(":jdk-shims"))
}

val wasmFolder = "javascript"
val wasmOutputFileName = "classes.wasm"

teavm {
	wasmGC {
		targetFileName = "../$wasmOutputFileName"
		optimization = OptimizationLevel.AGGRESSIVE
		outOfProcess = true
		fastGlobalAnalysis = false
		processMemory = 6144
		mainClass = "net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.MainClass"
		outputDir = file(wasmFolder)
		properties = mapOf("java.util.TimeZone.autodetect" to "true")
		obfuscated = true
		sourceMap = false
		debugInformation = false
		minDirectBuffersSize = 32
		maxDirectBuffersSize = 512
		disassembly = false
	}
}


val shimJar = project(":jdk-shims").layout.buildDirectory.file("libs/jdk-shims.jar")
val wasmOutputFile = file("$wasmFolder/$wasmOutputFileName")
val optimizeWasmScript = rootProject.file("scripts/optimize-wasm.mjs")
tasks.named<org.teavm.gradle.tasks.GenerateWasmGCTask>("generateWasmGC") {
	classpath = files(shimJar) + sourceSets["main"].output + classpath
	dependsOn(":jdk-shims:jar", "classes")

	doFirst {
		file("$wasmFolder/$wasmOutputFileName.teadbg").delete()
		file("$wasmFolder/$wasmOutputFileName.wat").delete()
	}


	doLast {
		val nodeName = if (System.getProperty("os.name").lowercase().contains("windows")) "node.exe" else "node"
		val node = System.getenv("PATH").orEmpty()
			.split(File.pathSeparator)
			.map { file(it).resolve(nodeName) }
			.firstOrNull { it.exists() }
		if (node == null || !optimizeWasmScript.exists()) {
			logger.lifecycle("wasm-opt skipped: node or scripts/optimize-wasm.mjs unavailable")
		} else {
			project.exec {
				workingDir(rootProject.projectDir)
				commandLine(node.absolutePath, optimizeWasmScript.absolutePath, wasmOutputFile.absolutePath, "--in-place")
			}
		}
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

eaglercraftBuild {
	suites {
		val mainSuite = wasm("main") {
			val srcFolder = "../src/wasm-gc-teavm/js"
			closureCompiler.set(file("buildtools/closure-compiler.jar"))
			closureMainClass.set("com.google.javascript.jscomp.CommandLineRunner")
			closureInputFiles.setFrom(files(
				"$srcFolder/externs.js",
				"$srcFolder/eagruntime_util.js",
				"$srcFolder/eagruntime_main.js",
				"$srcFolder/platformApplication.js",
				"$srcFolder/platformAssets.js",
				"$srcFolder/platformAudio.js",
				"$srcFolder/platformFilesystem.js",
				"$srcFolder/platformInput.js",
				"$srcFolder/platformNetworking.js",
				"$srcFolder/platformOpenGL.js",
				"$srcFolder/platformRuntime.js",
				"$srcFolder/platformScreenRecord.js",
				"$srcFolder/platformVoiceClient.js",
				"$srcFolder/platformWebRTC.js",
				"$srcFolder/platformWebView.js",
				"$srcFolder/clientPlatformSingleplayer.js",
				"$srcFolder/serverPlatformSingleplayer.js",
				"$srcFolder/WASMGCBufferAllocator.js",
				"$srcFolder/fix-webm-duration.js",
				"$srcFolder/teavm_runtime.js",
				"$srcFolder/eagruntime_entrypoint.js"
			)			)
			runtimeOutput.set(file("javascript/eagruntime.js"))
			epwSource.set(file("$wasmFolder/epw_src.txt"))
			epwMeta.set(file("$wasmFolder/epw_meta.txt"))
			epwSearchDirectory.set(file(wasmFolder))
			clientBundleOutputDir.set(file("javascript_dist"))
		}
		mainSuite.epkSources.set(file("../desktopRuntime/resources"))
		mainSuite.epkOutput.set(file("javascript/assets.epk"))
		mainSuite.languageMetadataInput.set(file("../target_teavm_javascript/javascript/lang"))
		mainSuite.languageEpkOutput.set(file("javascript/lang.tmp.epk"))
		mainSuite.sourceGeneratorTaskName.set("generateWasmGC")
	}
}
