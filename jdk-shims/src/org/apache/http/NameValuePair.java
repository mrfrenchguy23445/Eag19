package org.apache.http;

/**
 * Stub of org.apache.http.NameValuePair (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set that replaces the real jars on the WASM-GC
 * target. The full rationale, and the list of errors the real jars were costing, is in
 * org/apache/http/impl/client/HttpClientBuilder.java -- read that one first.
 *
 * A subset: getName() and getValue() are the whole real interface here, so this is
 * byte-for-byte the same surface, not a narrowing that would break a caller.
 */
public interface NameValuePair {
   String getName();

   String getValue();
}
