#!/bin/sh
# Rebuild the patched TeaVM jars that gradle/local-libs serves.
#
# Why this exists: gradle/local-libs/org/teavm/** holds jar files that are the
# *upstream* jars with seven .class files swapped out. Committing only the jars
# means losing the ability to change the patches, and committing only the jars
# with no recipe means losing the ability to reproduce them. So both the patch
# sources and this recipe live here.
#
# The pristine upstream jars are NOT vendored -- they are already in the Gradle
# cache (this build resolves them from mavenLocal/remote on first sync, sha1
# 1840d468... for teavm-core and 7b6990b3... for teavm-classlib), so rebuild
# needs only `./gradlew --refresh-dependencies` to have run once.
#
# usage: teavm-patches/rebuild.sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/teavm-patches/src"
OUT="$ROOT/teavm-patches/out"
LOCAL="$ROOT/gradle/local-libs/org/teavm"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
TEAVM_VER="0.12.1-EAGLER-R3"

[ -d "$CACHE" ] || { echo "no gradle cache at $CACHE" >&2; exit 1; }

# Pristine jar to patch INTO: the binary jar, not -sources/-javadoc.
find_upstream() {
	artifact="$1"
	found="$(find "$CACHE/org.teavm/$artifact/$TEAVM_VER" \
		-name "$artifact-$TEAVM_VER.jar" ! -name '*-sources.jar' \
		! -name '*-javadoc.jar' 2>/dev/null | head -1)"
	[ -n "$found" ] || { echo "upstream $artifact jar not in cache" >&2; exit 1; }
	printf '%s' "$found"
}

# Compile classpath: every cached jar EXCEPT other TeaVM versions. Two TeaVM
# versions on one javac classpath silently shadow each other's types, which is
# how the first patch attempt produced classes that compiled but did nothing.
rm -rf "$OUT"
mkdir -p "$OUT/classes"
find "$CACHE" -name '*.jar' \
	! -name '*-sources.jar' ! -name '*-javadoc.jar' \
	! -path '*/org.teavm/*/0.9.2/*' \
	-print | tr '\n' ':' | sed 's/:$//' > "$OUT/cp.txt"

find "$SRC" -name '*.java' | awk '{printf "\"%s\"\n", $0}' > "$OUT/sources.txt"
echo "==> javac $(wc -l < "$OUT/sources.txt") sources"
"$JAVA_HOME/bin/javac" -nowarn -encoding UTF-8 \
	-classpath "$(cat "$OUT/cp.txt")" \
	-d "$OUT/classes" @"$OUT/sources.txt"

# Each jar gets only its own patched classes -- never the other jar's. Build
# into a scratch file and swap at the end: writing straight into the live jar
# means a failure part-way through leaves an *unpatched* jar in place, which
# builds fine and then silently reintroduces the wasm errors this whole
# directory exists to remove -- the worst possible failure mode, since it costs
# a 12-minute link to notice.
#
# Usage: patch_into <artifact> <class entry>...
# Each entry's own package directory is copied out of $OUT/classes, so a jar
# can carry several patched classes. The directory is used rather than the bare
# class file because sibling inner classes ($1, $2, ...) have to travel with
# their outer class -- the outer class names them in its constant pool, and a
# stale inner class is a NoSuchMethodError at run time, not a build failure.
patch_into() {
	artifact="$1"
	shift
	target="$LOCAL/$artifact/$TEAVM_VER/$artifact-$TEAVM_VER.jar"
	[ -f "$target" ] || { echo "missing $target -- run the wasm build once" >&2; exit 1; }
	staged="$OUT/$artifact.jar"
	rm -f "$staged"
	cp "$(find_upstream "$artifact")" "$staged"
	for class_entry in "$@"; do
		"$JAVA_HOME/bin/jar" uf "$staged" -C "$OUT/classes" "$(dirname "$class_entry")"
	done
	# Prove every patched class actually landed before touching the real jar.
	for class_entry in "$@"; do
		"$JAVA_HOME/bin/jar" tf "$staged" | grep -qx "$class_entry" || {
			echo "$staged is missing $class_entry -- refusing to install" >&2
			exit 1
		}
	done
	cp "$staged" "$target"
	echo "==> $artifact sha1=$(sha1sum "$target" | cut -c1-8) ($(du -h "$target" | cut -f1))"
}

patch_into teavm-core \
	org/teavm/backend/wasm/generate/gc/methods/WasmGCMethodGenerator.class \
	org/teavm/model/util/RegisterAllocator.class \
	org/teavm/vm/TeaVM.class
patch_into teavm-classlib \
	org/teavm/classlib/impl/record/ObjectMethodsSubstitutor.class \
	org/teavm/classlib/java/io/TBufferedReader.class \
	org/teavm/classlib/java/lang/TString.class \
	org/teavm/classlib/java/util/TSpliterators.class

echo "done. gradle picks jars up in place on the next resolve (artifact lookup"
echo "is by content sha1, so no --refresh-dependencies needed)."
