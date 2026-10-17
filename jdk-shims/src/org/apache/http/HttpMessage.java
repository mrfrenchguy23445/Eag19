package org.apache.http;

import java.util.Locale;

/**
 * Stub of org.apache.http.HttpMessage (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Narrower than the real interface. Kept: containsHeader, getHeaders, getFirstHeader,
 * getLastHeader, getAllHeaders, getLocale, setLocale -- the header accessors Realms uses
 * to read Content-Length, Retry-After and WWW-Authenticate, plus the locale pair because
 * HttpResponse re-exports them. Dropped: every mutator (add/set/removeHeader,
 * setProtocolVersion and friends) and getProtocolVersion, which would drag in
 * ProtocolVersion and HeaderElement for no caller.
 *
 * The mutators are the interesting omission. Requests in this port are never handed to a
 * real connection manager, so there is nothing that could observe a header being set on
 * one after the fact.
 */
public interface HttpMessage {
   boolean containsHeader(String param0);

   Header[] getHeaders(String param0);

   Header getFirstHeader(String param0);

   Header getLastHeader(String param0);

   Header[] getAllHeaders();

   Locale getLocale();

   void setLocale(Locale param0);
}
