plugins {
	id("java")
}

allprojects {
	apply(plugin = "eclipse")
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
	exclude(group = "org.joml") //deps vendored
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
