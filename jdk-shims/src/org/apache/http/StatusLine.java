package org.apache.http;

/**
 * Stub of org.apache.http.StatusLine (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Narrower than the real interface: getProtocolVersion() is dropped because it returns
 * ProtocolVersion, which exists only to describe a wire protocol this port never speaks.
 * getStatusCode() is kept and is what FileUpload.handleResponse and FileDownload.download
 * both branch on.
 */
public interface StatusLine {
   int getStatusCode();

   String getReasonPhrase();
}
