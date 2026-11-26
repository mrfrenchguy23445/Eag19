package org.teavm.classlib.shims;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.SortedMap;
import java.util.TreeMap;

/*
 * Shim for org.apache.commons.io.Charsets, commons-io 2.11.0, byte-for-byte
 * faithful except for one line.
 *
 * WHAT BREAKS, AND WHY IT IS NOT AN IOUtils PROBLEM:
 *
 *   Method java.util.Collections.unmodifiableSortedMap(Ljava/util/SortedMap;)...
 *       at org.apache.commons.io.Charsets.<clinit>(Charsets.java:73)
 *       at org.apache.commons.io.IOUtils.copy(IOUtils.java:1158)
 *       at org.apache.commons.io.IOUtils.toString(IOUtils.java:3168)
 *       at net.minecraft.util.HttpUtil.lambda$downloadTo$0(HttpUtil.java:139)
 *
 * The stack is a red herring in both directions: IOUtils itself resolves fine,
 * and rewriting the HttpUtil call site would not have helped, because every
 * reachable caller of IOUtils.copy/toString pulls in Charsets.<clinit>, and it is
 * <clinit> that fails. Line 73 of Charsets is
 *
 *     STANDARD_CHARSET_MAP = Collections.unmodifiableSortedMap(standardCharsetMap);
 *
 * and the classlib's TCollections has no unmodifiableSortedMap -- it stops at
 * unmodifiableMap, unmodifiableList, unmodifiableSet. Adding the method would mean
 * copying TCollections and its 16 anonymous classes; replacing this one class does
 * the same job for one file.
 *
 * The ONLY change from the original: the TreeMap is assigned directly instead of
 * being wrapped. Nothing in commons-io mutates STANDARD_CHARSET_MAP after <clinit>,
 * so it is never written again, and requiredCharsets() hands out the same live
 * TreeMap the original handed out through its unmodifiable view. All other members
 * -- the six public charset constants, requiredCharsets(), both toCharset()
 * overloads and the constructor -- are copied unchanged so that every reference to
 * org.apache.commons.io.Charsets in the program keeps resolving.
 *
 * WHY org.teavm.classlib.shims: same as X509Certificate's header. ClassMappings
 * answers for the original name from classMappings, and toUnmappedClassName
 * answers for this name from reverseClassMappings, which matches verbatim here
 * because the shims package is outside the stripPrefixFromPackageHierarchyClasses
 * hierarchy. See META-INF/teavm.properties for the key/value direction, which is
 * the opposite of mapPackageHierarchy.
 *
 * A flat class: commons-io ships no Charsets$* inner classes, so there is nothing
 * else to rename alongside it.
 */
public class XCharsets {

	private static final SortedMap<String, Charset> STANDARD_CHARSET_MAP;

	public static final Charset ISO_8859_1;

	public static final Charset US_ASCII;

	public static final Charset UTF_16;

	public static final Charset UTF_16BE;

	public static final Charset UTF_16LE;

	public static final Charset UTF_8;

	public XCharsets() {
	}

	public static SortedMap<String, Charset> requiredCharsets() {
		return STANDARD_CHARSET_MAP;
	}

	public static Charset toCharset(final Charset charset) {
		return charset != null ? charset : Charset.defaultCharset();
	}

	public static Charset toCharset(final String charsetName) throws UnsupportedCharsetException {
		return charsetName != null ? Charset.forName(charsetName) : Charset.defaultCharset();
	}

	static {
		final SortedMap<String, Charset> standardCharsetMap = new TreeMap<String, Charset>(String.CASE_INSENSITIVE_ORDER);
		standardCharsetMap.put(StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1);
		standardCharsetMap.put(StandardCharsets.US_ASCII.name(), StandardCharsets.US_ASCII);
		standardCharsetMap.put(StandardCharsets.UTF_16.name(), StandardCharsets.UTF_16);
		standardCharsetMap.put(StandardCharsets.UTF_16BE.name(), StandardCharsets.UTF_16BE);
		standardCharsetMap.put(StandardCharsets.UTF_16LE.name(), StandardCharsets.UTF_16LE);
		standardCharsetMap.put(StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8);
		STANDARD_CHARSET_MAP = standardCharsetMap;
		ISO_8859_1 = StandardCharsets.ISO_8859_1;
		US_ASCII = StandardCharsets.US_ASCII;
		UTF_16 = StandardCharsets.UTF_16;
		UTF_16BE = StandardCharsets.UTF_16BE;
		UTF_16LE = StandardCharsets.UTF_16LE;
		UTF_8 = StandardCharsets.UTF_8;
	}

}
