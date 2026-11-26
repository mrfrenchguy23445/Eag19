package org.apache.http.client.methods;

import java.net.URI;
import org.apache.http.client.HttpRequest;

/**
 * Stub of org.apache.http.client.methods.HttpUriRequest (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Complete: all four methods of the real interface, same signatures. It has to be,
 * because it is the parameter type of CloseableHttpClient.execute, and both call sites
 * pass an HttpGet and an HttpPost through it.
 */
public interface HttpUriRequest extends HttpRequest {
   String getMethod();

   URI getURI();

   boolean isAborted();

   void abort() throws UnsupportedOperationException;
}
