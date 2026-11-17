import com.resentclient.oss.eaglercraft.build.impl.wasm
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.api.WasmDebugInfoLevel
import org.teavm.gradle.api.WasmDebugInfoLocation

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

// No project-level repositories: the root build's allprojects block points every
// project at gradle/local-libs, which vendors the patched TeaVM toolchain.

configurations.configureEach {
	// The game vendors both Gson and SLF4J under src/main/java; pulling the
	// desktop jars in as well makes TeaVM resolve the wrong copies and drags in
	// reflection that this classlib cannot link.
	exclude(group = "com.google.code.gson", module = "gson")
	exclude(group = "org.slf4j")
	exclude(group = "org.joml")
	// Same shadowing mechanism for three more vendored libraries:
	//  - com.mojang:logging carries its own LogUtils (StackWalker-based caller
	//    lookup) which shadows the vendored WASM-GC-safe copy on the TeaVM
	//    classpath, and drags log4j-slf4j2-impl/log4j-api/log4j-core in with
	//    it. Every user of that package (com.mojang.logging.LogUtils) resolves
	//    the vendored copy, and jdk-shims supplies the small log4j2-API surface
	//    netty's Log4J2Logger needs.
	//  - org.apache.logging.log4j:* is only referenced by the excluded bridge
	//    and by netty, which reaches it through the jdk-shims stubs above.
	//  - guava 31.0.1-jre (via authlib/datafixerupper/patchy) shadows the
	//    vendored com.google.common.** copies -- the real Queues
	//    returns ConcurrentLinkedQueue while callers compiled against the
	//    vendored shim expect the LinkedList descriptor, which TeaVM then
	//    reports as "Method ... was not found". The vendored copy wins once
	//    the real jars are gone.
	//  - text2speech is a jna/osgi stack with platform narrator
	//    implementations (NarratorMac/NarratorWindows/NarratorLinux all drag
	//    jna reflection that the classlib cannot link). jdk-shims supplies
	//    the Narrator interface both callers need; excluding the jar makes
	//    that copy win the same way it wins in the desktop target.
	exclude(group = "com.mojang", module = "logging")
	exclude(group = "org.apache.logging.log4j")
	exclude(group = "com.google.guava")
	exclude(group = "com.mojang", module = "text2speech")
	//  - oshi-core (its only TeaVM-visible consumer, the CPU metrics sampler,
	//    is ported to a WASM-safe stub) and jna (which oshi and text2speech
	//    reach): both are jna-native library-detection stacks that cannot link
	//    against this classlib and cannot run in a browser.
	exclude(group = "com.github.oshi")
	exclude(group = "net.java.dev.jna")
}

dependencies {
	teavm(teavm.libs.jso)
	teavm(teavm.libs.jsoApis)
	compileOnly("org.teavm:teavm-core:0.12.1-EAGLER-R3")
	implementation(libs.jorbis)
	implementation(libs.bundles.common)
	implementation(libs.bundles.mc1194)
	// The vendored guava sources compile against these annotation-only jars;
	// they used to arrive transitively through the real guava that is now
	// excluded above, so they are pinned here at the versions guava 31.0.1
	// declares.
	implementation("com.google.errorprone:error_prone_annotations:2.7.1")
	implementation("org.checkerframework:checker-qual:3.12.0")
	implementation("com.google.j2objc:j2objc-annotations:1.3")
	// The classlib overrides and java.* shims this target links against.
	implementation(project(":jdk-shims"))
}

val wasmFolder = "javascript"
val wasmOutputFileName = "classes.wasm"

teavm {
	wasmGC {
		targetFileName = "../$wasmOutputFileName"
		optimization = OptimizationLevel.AGGRESSIVE
		// Link in a dedicated compiler JVM: the aggressive 1.19.4 link does not fit
		// in the Gradle daemon's default 1.5g heap.
		outOfProcess = true
		fastGlobalAnalysis = false
		// 6g: the 4g link OOM'd (13.9 GB machine; the 1.5 g Gradle daemon eats
		// into the same physical budget). -Xmx is passed through verbatim by
		// BuildDaemon.start.
		processMemory = 6144
		mainClass = "net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.MainClass"
		outputDir = file(wasmFolder)
		properties = mapOf("java.util.TimeZone.autodetect" to "true")
		debugInformation = true
		debugInfoLocation = WasmDebugInfoLocation.EXTERNAL
		debugInfoLevel = WasmDebugInfoLevel.DEOBFUSCATION
		minDirectBuffersSize = 32
		maxDirectBuffersSize = 512
		disassembly = true
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
