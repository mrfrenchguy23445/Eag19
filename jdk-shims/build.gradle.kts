plugins {
	id("java")
}

/*
 * JDK SHIMS FOR THE WASM-GC TEAVM TARGET
 * ======================================
 *
 * The WASM-GC TeaVM classlib (teavm-classlib-0.12.1-EAGLER-R3.jar) models a
 * strict subset of the JDK: 1552 entries, of which the java.* packages carry
 * only what Eaglercraft's own runtime needs. Everything 1.19.4 reaches for and
 * the subset does not have simply reads "Class X was not found" at link time --
 * java.nio.file.Path and java.nio.file.Paths, java.security.Key/PublicKey/
 * PrivateKey/KeyFactory, java.security.spec.PKCS8EncodedKeySpec/
 * X509EncodedKeySpec, java.security.cert.Certificate, java.security.MessageDigest,
 * java.lang.Module and more.
 *
 * WHAT CANNOT BE SHIMMED THIS WAY: javax.*
 * ========================================
 *
 * The org.teavm.classlib.java relocation is a workaround for ONE exclusion rule
 * (includePackageHierarchy|java=false). It does not generalise, and one class in
 * this jar was supplied the obvious way and did not work:
 *
 *   javax/sound/sampled/AudioFormat.java
 *
 * It has been deleted. It genuinely did not resolve, for the structural reason: javax is
 * answered by the bootstrap loader out of the module image before the classpath entry is
 * consulted, so the jar file is never read. javax.sound.sampled.AudioFormat has been
 * replaced in game code by com.mojang.blaze3d.audio.SoundFormat instead, which is the
 * fix that actually works. Do not restore the file.
 *
 * javax.security.cert.X509Certificate was the same dead end, and it is still needed --
 * netty's EmptyArrays.<clinit> counts instances of it. The way around is NOT a class file
 * under javax/ but a mapClass| rule in this jar's own META-INF/teavm.properties pointing
 * at org.teavm.classlib.shims.X509Certificate, which sits outside that namespace so the
 * bootstrap loader has never heard of it. classMappings are the first thing TeaVM's
 * resolver tries, so that route can win where the class file could not. See the header on
 * src-classlib/org/teavm/classlib/shims/X509Certificate.java.
 *
 * WHY THIS JAR SHIPS A teavm.properties AFTER ALL
 * ===============================================
 *
 * An earlier revision of this header said no properties file should be added here and
 * none would be. That was the right conclusion from a wrong premise: it assumed one file
 * is read and a second would displace the classlib's. TeaVM's ClasspathResourceMapper
 * does classLoader.getResources("META-INF/teavm.properties") and merges every file it
 * finds, each into a fresh Properties object, so a second file is purely additive.
 * src/main/resources/META-INF/teavm.properties carries the mapClass| rules described
 * above and is load-bearing; nothing in it conflicts with the classlib's own keys.
 *
 * SUN.MISC.UNSAFE IS INERT -- DO NOT DELETE IT ANYWAY
 * ====================================================
 *
 * src-sunmisc/sun/misc/Unsafe.java exists but is never the copy that runs. Proven, not
 * inferred: ClassLoader.getSystemResource("sun/misc/Unsafe.class") answers
 * jrt:/jdk.unsupported/sun/misc/Unsafe.class, 17134 bytes, rather than this jar's 84-line
 * file. The bootstrap loader is asked first and wins, exactly as it does for javax.*.
 *
 * The link error associated with it reads
 *
 *   Method java.lang.Class.isHidden()Z was not found
 *       at sun.misc.Unsafe.objectFieldOffset(Unsafe.java:644)
 *
 * and lines 644 and 647 are the HOST JDK 17's numbering -- this repository's Unsafe is 84
 * lines long and calls nothing that would produce those frames. It is the host's own
 * sun.misc.Unsafe being parsed, and the host's version calls Class.isHidden and
 * Class.isRecord, which the classlib does not have. The shim's own header analyses this
 * correctly; an earlier version of this note claimed the shim was the live copy and was
 * wrong. The shims stay because making them win would be worse: netty reaches for 43
 * distinct members of Unsafe and this jar supplies a handful of stubs, so a jar that did
 * win would turn a link error into silent garbage at runtime.
 *
 * The residual Class.isHidden/isRecord pair is therefore a separate and still-open
 * problem, reached through the host's Unsafe and not through game code. The two ways out
 * are to stop netty's PlatformDependent0.objectFieldOffset being reached at all, or to
 * name a class carrying all 43 members netty asks for and route to it with a mapClass|
 * rule. Note that the claim once made here -- that the classlib's TClass cannot be
 * recompiled from source, citing a note in docs/PORT_STATUS.md that was never written --
 * is false: the X* overrides in src-classlib/ do exactly that. java.lang.Class still
 * cannot be replaced this way, for the different reason recorded in
 * src/main/resources/META-INF/teavm.properties.
 *
 * WHY THESE CLASSES LIVE IN org.teavm.classlib.java.*, NOT java.*
 * ============================================================
 *
 * Shipping them as plain class files under java/ does not work, and did not work
 * until this was found: every one of them was reported missing while sitting in
 * this jar on the TeaVM classpath. The cause is in the classlib's own
 * META-INF/teavm.properties:
 *
 *   includePackageHierarchy|java=false
 *   includePackageHierarchy|javax.xml=false
 *   stripPrefixFromPackageHierarchyClasses|org.teavm.classlib.java=T
 *   mapPackageHierarchy|org.teavm.classlib.java=java
 *
 * The first rule makes TeaVM DISCARD every class in the java.* namespace taken
 * from an ordinary jar, which is why nothing here ever resolved. It is also why
 * no jar on the classpath holds a single java.lang.String class file: the
 * classlib ships its 1199 JDK classes relocated under org.teavm.classlib.java
 * with a T prefix on each one, and the last two rules map them back onto the
 * real java.* names.
 *
 * Those rules are a relocation pipeline that runs on any jar, not a private
 * classlib channel. So each shim is declared as
 *
 *   package org.teavm.classlib.java.security.spec;
 *   public class TX509EncodedKeySpec extends TEncodedKeySpec
 *
 * and arrives as neither excluded nor misnamed. TeaVM strips the T and maps the
 * package back, registering it as java.security.spec.X509EncodedKeySpec. Nothing
 * extra has to be configured for these: the classlib's properties already do it.
 * (The one exception is the X* overrides, which cannot ride the T-prefix rule --
 * see WHY THIS JAR SHIPS A teavm.properties above.)
 *
 * Two consequences worth keeping in mind:
 *
 *  - The T prefix is what stops the relocated shims colliding with the real JDK
 *    classes now reachable via the `import java.<pkg>.*;` line each file carries.
 *    That wildcard import restores the implicit same-package access the shims had
 *    before they moved (java.nio.file.OpenOption and friends), which relocation
 *    otherwise breaks.
 *
 *  - javax.sound.sampled.AudioFormat used to get a third bullet here, saying that only
 *    javax.xml is excluded so it resolved under its real name and needed no mapping. That
 *    was wrong. It did not resolve, javax being answered by the bootstrap loader ahead of
 *    the classpath, and it has been replaced by com.mojang.blaze3d.audio.SoundFormat. The
 *    third-party stubs in src/ -- org.apache.log4j, org.apache.logging.log4j,
 *    org.apache.http, oshi, com.mojang.text2speech -- do fall under neither exclusion rule,
 *    which is why they are packaged under their real names.
 *
 * WHY src-classlib/ IS A SEPARATE SOURCE SET AND NOT PART OF src/
 * ===============================================================
 *
 * src/ is patched into java.base, and that has a consequence which is easy to miss: once
 * java.base owns the package org.teavm.classlib.java.lang -- src/ already held
 * org/teavm/classlib/java/lang/TModule.java, so it did before this changed -- javac
 * resolves every name in that package from the module and stops there. A package may not
 * be split between a named module and the classpath, so teavm-classlib.jar's own TObject,
 * TString, TRunnable and the rest are shadowed and invisible. Anything in src/ that names
 * a same-package sibling fails with "cannot find symbol" on a class that is plainly on the
 * classpath.
 *
 * src-classlib/ carries no patch flag, so it is an ordinary classpath package with no
 * module ownership to shadow anyone. javac sees teavm-classlib.jar exactly as it would any
 * third-party library, while the explicitly-listed sources still take precedence over the
 * jar's class files of the same name -- which is the whole mechanism: copy the classlib's
 * own sources here, add the member that is missing, and compile them against the real
 * classlib rather than against a rewritten copy of it.
 *
 * Do not fold the two source sets together. Doing so would silently reintroduce the
 * shadowing for every class in org.teavm.classlib.java.lang and break the shims in src/
 * that never had the problem (TModule reaches for no sibling). The split is what makes the
 * override route work at all.
 *
 * Only the target sees this jar. It is deliberately NOT on the root project's
 * compile classpath: javac would shadow java.base with these subsets while
 * type-checking the game and reject calls the real JDK would allow, so the game
 * must keep compiling against java.base and the subsets are validated only by
 * TeaVM, which reports any member it cannot link.
 *
 * Consequence to keep in mind: every shim here must be a SUBSET of the real JDK
 * class, never a superset and never a different signature. Anything wider would
 * type-check against the real class and then fail to link.
 */

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

sourceSets {
	named("main") {
		// src/, not the plugin default src/main/java: the tree here mirrors the JDK's own
		// package layout (java/lang, java/nio/file, ...) with no "main" level of its own.
		java.srcDirs("src")
	}

	// sun.misc.Unsafe is the one shim that must keep its real package name.
	//
	// Every other shim here is relocated into org.teavm.classlib.java.* with a T prefix,
	// because the classlib's META-INF/teavm.properties maps that namespace back onto java.*
	// and discards java.* from every other jar. sun is not covered by any of those rules,
	// so a class packaged under its real name is indexed under that name directly. It
	// cannot go in src/ for a second reason: it needs a different --patch-module, and
	// patching java.base with a sun.misc source collides with the jdk.unsupported module
	// that already exports that package.
	//
	// See src-sunmisc/sun/misc/Unsafe.java for why the class exists at all.
	create("sunmisc") {
		java.srcDirs("src-sunmisc")
		compileClasspath += sourceSets["main"].output
		runtimeClasspath += output + compileClasspath
	}

	// The java.lang overrides in src-classlib/, and the one reason this is a separate
	// source set at all: it is NOT compiled with --patch-module.
	//
	// Patching src/ into java.base is what makes the shims there compile, but it has a
	// consequence that is easy to miss: once java.base owns the package
	// org.teavm.classlib.java.lang, javac resolves every name in that package from the
	// module and stops -- teavm-classlib.jar's own TObject, TString, TRunnable and the
	// rest are shadowed, because a package may not be split between a named module and
	// the classpath. src/ already held org/teavm/classlib/java/lang/TModule.java, so the
	// package was already owned before this change; the overrides simply made it visible
	// by being the first thing in that package to reach for a sibling.
	//
	// src-classlib/ has no patch flag, so it is an ordinary classpath package with no
	// module ownership and no shadowing. javac finds teavm-classlib.jar exactly as it
	// would for any third-party library, and the explicitly-listed sources take
	// precedence over the jar's class files of the same name. That is the whole point of
	// copying the classlib's own sources here: they compile against the real classlib.
	//
	// src-classlib/ is deliberately not folded into src-sunmisc either -- that one is
	// patched into jdk.unsupported, and these types have nothing to do with it.
	//
	// compileClasspath also carries src/'s output, for exactly one family of classes:
	// the javax.crypto shims in org/teavm/classlib/shims/ extend TKey (java.security.Key)
	// and java.security.spec.AlgorithmParameterSpec, both of which are shims shipped by
	// src/ rather than by the classlib jar. Nothing in the classlib jar shares a name
	// with them, so adding src/'s output can only resolve types and never shadows one.
	create("classlib") {
		java.srcDirs("src-classlib")
		compileClasspath += sourceSets["main"].output
	}
}

// No project-level repositories: the root build's allprojects block points every
// project at gradle/local-libs, which vendors teavm-core and the classlib.

dependencies {
	// Only java.security.MessageDigest reaches out, and only to reuse the vendored
	// Guava digests (com.google.common.hash.Hashing.sha1/sha256/md5), which already
	// carry hand-written MD5/SHA-1/SHA-256 for this port. compileOnly, because the
	// root project must never see this jar -- see the note above.
	compileOnly(rootProject)

	// java.nio.file is built on org.teavm.runtime.fs.VirtualFileSystem, which lives
	// in teavm-core (not the classlib) and is the same store java.io.File's
	// implementation is written against. compileOnly again: the target already has
	// teavm-core, and duplicating it here would only risk a version skew.
	compileOnly("org.teavm:teavm-core:0.17.0")

	// The overrides under src-classlib/org/teavm/classlib/java/lang/X*.java are the
	// classlib's own sources, copied line for line and then given one member each --
	// so they import what the classlib imports. teavm-classlib supplies
	// org.teavm.classlib.PlatformDetector and org.teavm.classlib.impl.*; the two jso
	// jars supply org.teavm.jso.*; interop is org.teavm.interop.*. Everything else
	// they reach for (org.teavm.dependency, .platform, .runtime, .backend.*) is in
	// teavm-core.
	//
	// Scoped to the classlib source set, not main: nothing in src/ imports any of these,
	// and the classlib set is the one that needs teavm-classlib.jar on its classpath to
	// resolve the same-package siblings the patch flag hides from src/.
	//
	// compileOnly, for the same reason as every other entry here: this jar must never
	// export anything, or the target would link two copies of the classlib.
	"classlibCompileOnly"("org.teavm:teavm-core:0.17.0")
	"classlibCompileOnly"("org.teavm:teavm-classlib:0.12.1-EAGLER-R3")
	"classlibCompileOnly"("org.teavm:teavm-interop:0.12.1-EAGLER-R3")
	"classlibCompileOnly"("org.teavm:teavm-jso:0.12.1-EAGLER-R3")
	"classlibCompileOnly"("org.teavm:teavm-jso-apis:0.12.1-EAGLER-R3")
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.isFork = true
	options.forkOptions.jvmArgs = listOf("-Xmx1g")
}

// --patch-module is per source set, not global: it says which module the files on
// disk belong to, and the two sets belong to different ones.
tasks.named<JavaCompile>("compileJava") {
	// Patching these sources into java.base is what lets javac accept them at all.
	//
	// For the relocated org.teavm.classlib.java.* shims it is unavoidable: they reference
	// java.base internals and java.security.MessageDigest, which patches them into the
	// module that owns both.
	//
	// The third-party stubs added later -- org.apache.log4j, org.apache.logging.log4j,
	// org.apache.http, oshi, com.mojang.text2speech -- do not strictly need it; an
	// ordinary package would compile fine. They are covered by the same argument rather than
	// a second compile task, because one rule is easier to keep true than two.
	//
	// javax.sound.sampled.AudioFormat used to live here too, which is what the older version
	// of this comment said these flags were for: javac refuses a javax.sound source with
	// "package exists in another module: java.desktop", and patching it into java.base
	// sidesteps that. That shim has been deleted, because it did not work -- javax.* is
	// answered by the bootstrap loader before the classpath is consulted, so the class file
	// was never read and the type stayed missing. See the note at the top of this file and
	// com/mojang/blaze3d/audio/SoundFormat.java, which replaced it in game code.
	//
	// --add-reads is the other half: patching makes these classes part of java.base, which
	// reads nothing on the classpath by default, so without it java.nio.file cannot see
	// org.teavm.runtime.fs nor java.security.MessageDigest's com.google.common.hash.Hashing.
	options.compilerArgs.addAll(
		listOf(
			"--patch-module", "java.base=${projectDir}/src",
			"--add-reads", "java.base=ALL-UNNAMED"
		)
	)
}

tasks.named<JavaCompile>("compileSunmiscJava") {
	// sun.misc belongs to jdk.unsupported, so that is the module it is patched into.
	// No --add-reads is needed: it references only java.base and java.lang.reflect.
	options.compilerArgs.addAll(
		listOf("--patch-module", "jdk.unsupported=${projectDir}/src-sunmisc")
	)
}

tasks.named<Jar>("jar") {
	from(sourceSets["sunmisc"].output)
	from(sourceSets["classlib"].output)
}
