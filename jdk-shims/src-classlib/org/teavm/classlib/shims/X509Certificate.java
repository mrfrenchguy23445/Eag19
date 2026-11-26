package org.teavm.classlib.shims;

/*
 * Shim for javax.security.cert.X509Certificate, reached only from
 * io.netty.util.internal.EmptyArrays' <clinit>, which does `new X509Certificate[0]`
 * for a constant no browser build ever reads. The two ssl-handler classes that also
 * name this type are unreachable on this target -- there is no JSSE here.
 *
 * WHY THIS FILE LIVES IN org.teavm.classlib.shims AND NOT IN javax/security/cert:
 *
 * The obvious placement does not work, and jdk-shims/build.gradle.kts says so: the
 * bootstrap loader answers every javax.* resource request out of the module image
 * before the classpath entry is consulted, so a class file at javax/security/cert/
 * is never read no matter which jar it is in. This jar had one such file,
 * javax/sound/sampled/AudioFormat.java, and it never resolved.
 *
 * The route out is mapClass| in this jar's own META-INF/teavm.properties. TeaVM's
 * ClasspathResourceMapper tries classMappings BEFORE packageMappings and BEFORE the
 * plain name lookup, so asking for javax.security.cert.X509Certificate is answered
 * by the target name below, which the bootstrap loader has never heard of:
 *
 *   mapClass|org.teavm.classlib.shims.X509Certificate=javax.security.cert.X509Certificate
 *
 * KEY AND VALUE ARE THE OTHER WAY ROUND FROM mapPackageHierarchy, and it is not a
 * typo in either place. loadProperties does classMappings.add(value.split("."), key)
 * and reverseClassMappings.add(key.split("."), value); apply() reads classMappings
 * and is handed the ORIGINAL name, so the original has to be the value.
 * reverseClassMappings -- read by toUnmappedClassName, which is handed the target
 * name -- is keyed by the key. Writing it backwards loads cleanly and silently
 * never matches, because classMappings then only answers for a name nobody asks.
 *
 * The target must sit OUTSIDE the org.teavm.classlib.java hierarchy on purpose.
 * toUnmappedClassName applies stripPrefixFromPackageHierarchyClasses to that
 * hierarchy first, which would strip the first character off whatever name is put
 * there and land on a class that does not exist. The shims package gets no such
 * stripping, so reverseClassMappings matches verbatim.
 *
 * A subset of the real class: it is abstract, it is a class rather than an
 * interface, and it carries no members. Nothing calls one. netty only ever counts
 * them.
 */
public abstract class X509Certificate {

	public X509Certificate() {
	}

}
