# Building this workspace without network access

Every library and plugin this workspace resolves is vendored in
`gradle/local-libs`, which is a plain Maven repository on disk:

- the patched TeaVM toolchain (`org.teavm:teavm-*`, the EAGLER fork),
- Mojang's own libraries (`com.mojang:*`, from libraries.minecraft.net),
- the rest of the 1.19.4 dependency set (netty, lwjgl, jetty, guava, ...),
- the build plugins and their own dependencies.

`settings.gradle.kts` (`pluginManagement`) and `build.gradle.kts`
(`allprojects`) point at that directory and at nothing else, so Gradle never
opens a connection. There is no `--offline` requirement and no Gradle cache to
copy: a fresh `GRADLE_USER_HOME` resolves everything from `gradle/local-libs`.

## Requirements

- JDK 17 or newer (`java -version`)
- The Gradle 8.14 distribution. `gradle/wrapper/gradle-wrapper.properties`
  still points at services.gradle.org, because that download is separate from
  dependency resolution. If the machine cannot reach it, install Gradle 8.14
  once and run `gradle` instead of `./gradlew`, or drop
  `gradle-8.14-all.zip` into `~/.gradle/wrapper/dists/gradle-8.14-all/`
  next to the hash directory and let the wrapper unpack it.

## Build

    ./gradlew :target_teavm_wasm_gc:generateWasmGC

That is the meaningful target: the wasm-gc client. `:jdk-shims:jar` and
`:target_teavm_wasm_gc:compileJava` both build against the vendored repository
with an empty Gradle home and `--offline`.

Two targets do not compile here for reasons that have nothing to do with
dependencies, and did not before this repository was vendored either:

- `:target_lwjgl_desktop` -- see the note in MOD_PORT_PLAN.md ("Desktop runtime
  ... is UNCOMPILABLE in this workspace").
- `:target_teavm_javascript` -- a leftover 1.8-era source set: it compiles
  `src/teavm/java`, which imports `net.minecraft.util.MathHelper`, while this
  branch (official 1.19.4 names) has `net.minecraft.util.Mth`.

## Refreshing the repository after a dependency change

    ./gradlew -I scripts/dump-offline-deps.init.gradle dumpOfflineDeps > deps.txt
    scripts/build-offline-repo.sh deps.txt

`build-offline-repo.sh` needs network once, on the machine doing the refresh: it
copies the artifacts from the Gradle cache and fetches the POM/module metadata
that the cache does not keep. If a build then reports a module it cannot find,
`scripts/bootstrap-offline-repo.sh` (see its header) vendors it.
