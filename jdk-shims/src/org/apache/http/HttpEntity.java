package org.apache.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stub of org.apache.http.HttpEntity (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Complete: every method of the real interface is declared, with the real signature and
 * the same throws clauses. It is not narrowed, because InputStreamEntity -- which the game
 * subclasses in FileUpload.CustomInputStreamEntity -- has to satisfy it, and a partial
 * interface would make that subclass fail to compile against the real jar while appearing
 * to work here.
 */
public interface HttpEntity {
   boolean isRepeatable();

   boolean isChunked();

   long getContentLength();

   Header getContentType();

   Header getContentEncoding();

   InputStream getContent() throws IOException, UnsupportedOperationException;

   void writeTo(OutputStream param0) throws IOException;

   boolean isStreaming();

   void consumeContent() throws IOException;
}
