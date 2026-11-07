plugins {
	id("java")
}

allprojects {
	apply(plugin = "eclipse")

	// Local-only, deliberately: gradle/local-libs is a vendored Maven repository
	// holding every library and plugin this workspace resolves (TeaVM toolchain,
	// Mojang libraries, and the rest of the 1.19.4 dependency set), so the build
	// works on a machine with no network access and no Gradle cache. Do not add a
	// remote repository here without also adding the artifacts it would serve to
	// gradle/local-libs, or an offline build breaks at the first missing module.
	repositories {
		maven {
			name = "eagler-local"
			url = uri(rootDir.resolve("gradle/local-libs"))
		}
	}

	plugins.withId("java") {
		java {
			toolchain {
				languageVersion = JavaLanguageVersion.of(17)
			}
		}
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
	named("main") {
		java {
			srcDirs(
				"src/main/java",
				"src/protocol-game/java",
				"src/protocol-relay/java",
				"src/platform-api/java",
				"src/platform-shim/java"
			)
			exclude("net/optifine/**")
			exclude("dev/redstudio/**")
		}
	}
}

dependencies {
	compileOnly(libs.bundles.mc1194)

	implementation(libs.bundles.common)
}

configurations.configureEach {
	exclude(group = "com.google.code.gson", module = "gson")
	exclude(group = "org.slf4j")
	exclude(group = "org.joml") //most of the uhh other deps are vendord.
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.isFork = true
	options.forkOptions.jvmArgs = listOf("-XX:+UseSerialGC", "-Xmx6g")
}

tasks.withType<Jar> {
	entryCompression = ZipEntryCompression.STORED
	fileTree("src/platform-api/java").visit {
		if (!isDirectory) {
			if (path.endsWith(".java")) {
				exclude(path.substring(0, path.length - 5) + ".class")
			}
		}
	}
}
