package org.apache.http.client.methods;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpRequest;

/**
 * Stub of org.apache.http.client.methods.HttpEntityEnclosingRequest (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Complete: all three methods of the real interface. getEntity/setEntity are kept whole
 * rather than folded into HttpPost because this is the real hierarchy -- HttpPost really
 * does implement this interface -- and FileUpload.setupRequest assigns a
 * CustomInputStreamEntity through it.
 */
public interface HttpEntityEnclosingRequest extends HttpRequest {
   boolean expectContinue();

   HttpEntity getEntity();

   void setEntity(HttpEntity param0);
}
