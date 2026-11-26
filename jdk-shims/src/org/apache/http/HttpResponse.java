package org.apache.http;

/**
 * Stub of org.apache.http.HttpResponse (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Extends HttpMessage, as the real one does. Kept: getStatusLine() and getEntity(), which
 * are the only two the two Realms call sites use -- the status code for the 200/401 and
 * Retry-After branches, and the entity for EntityUtils.toString and getContent().
 * Dropped: setStatusLine (all four overloads), setStatusCode, setReasonPhrase,
 * setEntity and setLocale -- all writers, and there is no code in this port that
 * manufactures a response.
 */
public interface HttpResponse extends HttpMessage {
   StatusLine getStatusLine();

   HttpEntity getEntity();
}
