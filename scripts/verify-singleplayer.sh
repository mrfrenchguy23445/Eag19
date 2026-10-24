#!/bin/sh
# Verify the integrated server transport without starting a window or browser.
set -eu
cd "$(dirname "$0")/.."
./gradlew :compileJava :jdk-shims:compileJava --console=plain
CP=$(./gradlew -q :target_teavm_wasm_gc:printRuntimeCp -I scripts/printcp.init.gradle --console=plain | sed -n 's/^CPSTART\(.*\)CPEND$/\1/p')
[ -n "$CP" ] || { echo 'Could not resolve verification classpath' >&2; exit 1; }
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
javac -cp "build/classes/java/main:$CP" -d "$OUT" scripts/verify/net/minecraft/network/LocalConnectionVerify.java
java -cp "build/classes/java/main:$CP:$OUT" net.minecraft.network.LocalConnectionVerify
javac -d "$OUT" jdk-shims/src/org/teavm/classlib/java/nio/channels/TFileChannel.java jdk-shims/src/org/teavm/classlib/java/nio/channels/TFileLock.java scripts/verify/org/teavm/classlib/java/nio/channels/RegionChannelVerify.java
java -cp "$OUT" org.teavm.classlib.java.nio.channels.RegionChannelVerify
CORE=gradle/local-libs/org/teavm/teavm-core/0.12.1-EAGLER-R3/teavm-core-0.12.1-EAGLER-R3.jar
PERSIST=net/lax1dude/eaglercraft/v1_8/internal/wasm_gc_teavm
javac -cp "build/classes/java/main:$CP:$CORE" -d "$OUT" "src/wasm-gc-teavm/java/$PERSIST/PersistentVirtualFileSystem.java" "scripts/verify/$PERSIST/PersistentFilesystemVerify.java"
java -cp "$OUT:build/classes/java/main:$CP:$CORE" net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm.PersistentFilesystemVerify
FUTURES=jdk-shims/src/org/teavm/classlib/java/util/concurrent
javac -d "$OUT" "$FUTURES/TTimeoutException.java" "$FUTURES/TCompletionException.java" "$FUTURES/TFuture.java" "$FUTURES/TCompletionStage.java" "$FUTURES/TCompletableFuture.java" scripts/verify/org/teavm/classlib/java/util/concurrent/ChunkSaveBarrierVerify.java
java -cp "$OUT" org.teavm.classlib.java.util.concurrent.ChunkSaveBarrierVerify
CLASSLIB=gradle/local-libs/org/teavm/teavm-classlib/0.12.1-EAGLER-R3/teavm-classlib-0.12.1-EAGLER-R3.jar
javac -cp "$CLASSLIB" -d "$OUT" scripts/verify/DoubleSpliteratorVerify.java
java -cp "$OUT:$CLASSLIB" DoubleSpliteratorVerify
