package org.apache.http.impl.client;

import java.io.Closeable;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Stub of org.apache.http.impl.client.CloseableHttpClient (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; read HttpClientBuilder.java first -- it has
 * the full rationale and the error counts.
 *
 * The real class is an abstract base over a fifteen-overload execute() family. Only the
 * one overload this port calls is kept: execute(HttpUriRequest), which FileDownload calls
 * twice (for the world download and the resource pack) and FileUpload calls once.
 *
 * It is declared abstract and given no execute implementation on purpose: there is no
 * subclass in this jar, so the only way to obtain one is HttpClientBuilder.build(), and
 * build() throws. Declaring it abstract makes that structural rather than a matter of
 * trust -- if some future path managed to construct a CloseableHttpClient directly it
 * would not compile, which is the correct outcome.
 *
 * The real execute declares throws IOException, ClientProtocolException; this narrows to
 * IOException. That is safe in the direction that matters: the callers catch IOException
 * or Exception, never ClientProtocolException, and javac checks that against the real jar
 * anyway.
 */
public abstract class CloseableHttpClient implements Closeable {

   public abstract CloseableHttpResponse execute(HttpUriRequest param0) throws IOException;

   public abstract void close() throws IOException;
}
