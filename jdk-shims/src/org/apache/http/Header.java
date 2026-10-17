package org.apache.http;

/**
 * Stub of org.apache.http.Header (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Narrower than the real interface on purpose: the real Header also declares
 * getElements() -> HeaderElement[], and neither HeaderElement nor its ParseException
 * return type is reachable from anything this port calls. A subset is the rule for every
 * stub in this tree; a superset would type-check against the real jar and then fail to
 * link. Realms reads headers through NameValuePair.getValue(), which it inherits, so this
 * narrowing costs nothing.
 */
public interface Header extends NameValuePair {
}
