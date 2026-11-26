package org.apache.http.client;

import org.apache.http.HttpMessage;

/**
 * Stub of org.apache.http.client.HttpRequest (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Extends HttpMessage, as the real one does. Narrower: the real interface adds
 * getRequestLine(), setProtocolVersion and getProtocolVersion, whose return types
 * (RequestLine, ProtocolVersion) model HTTP framing that nothing here reads.
 * isAborted()/abort() are kept because FileDownload.download calls request.abort() to
 * cancel an in-flight Realms download.
 */
public interface HttpRequest extends HttpMessage {
   boolean isAborted();

   void abort() throws UnsupportedOperationException;
}
