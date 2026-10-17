#!/bin/sh
# Verifies EaglerProfile's persistence round-trip and UUID rules.
#
# The repo has no JUnit source set, so this is a plain main() that exits non-zero
# on failure. It exercises write()/read() and setUsername() only -- those are
# pure and need no platform -- so it runs against the root project's classes
# without a browser or an OpenGL context.
#
# Usage: scripts/verify-profile.sh
set -e

cd "$(dirname "$0")/.."

# Match whatever JDK the build uses, so the verifier never runs against a
# different javac than the one the classes were compiled with.
if [ -n "$JAVA_HOME" ]; then
	JAVAC="$JAVA_HOME/bin/javac"
	JAVA="$JAVA_HOME/bin/java"
else
	JAVAC=javac
	JAVA=java
fi

if [ ! -d build/classes/java/main ]; then
	echo "Root classes missing. Run ./gradlew :compileJava first." >&2
	exit 1
fi

# UUIDUtil's static initialiser touches DataFixerUpper, so the verifier needs the
# real runtime classpath rather than just build/classes/java/main.
echo "Resolving runtime classpath..."
CP=$(./gradlew -q printRuntimeCp -I scripts/printcp.init.gradle --console=plain \
	| tr -d '\r' | sed -n 's/.*CPSTART\(.*\)CPEND.*/\1/p')

if [ -z "$CP" ]; then
	echo "Could not resolve runtime classpath." >&2
	exit 1
fi

OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT

echo "Compiling verifier..."
"$JAVAC" -nowarn -cp "build/classes/java/main:$CP" -d "$OUT" \
	scripts/verify/net/lax1dude/eaglercraft/v1_8/profile/EaglerProfileVerify.java

echo "Running verifier..."
"$JAVA" -cp "build/classes/java/main:$CP:$OUT" \
	net.lax1dude.eaglercraft.v1_8.profile.EaglerProfileVerify