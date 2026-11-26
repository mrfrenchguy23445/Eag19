package org.apache.http.client.methods;

import java.net.URI;
import java.util.Locale;
import org.apache.http.Header;

/**
 * Stub of org.apache.http.client.methods.HttpGet (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * The real HttpGet extends HttpRequestBase, which is where setHeader/getFirstHeader/
 * releaseConnection actually live. That intermediate class is not reproduced: it carries
 * RequestLine, ProtocolVersion, Configurable and getConfig/setConfig, none of which this
 * port reads. The members the two Realms call sites use are declared here directly, with
 * the real signatures, and the class implements HttpUriRequest so it still satisfies
 * CloseableHttpClient.execute.
 *
 * abort() and isAborted() are declared here even though the real class inherits rather
 * than declares them, because FileDownload.download calls this.request.abort() on a field
 * declared as HttpGet and would not compile otherwise. They are behaviourally inert,
 * matching an aborted-never request: nothing in this port ever starts one to cancel.
 *
 * Narrowed against the real class: setHeader and addHeader are absent, because nothing
 * sets a header on a GET -- FileUpload builds the only request that carries one, and that
 * is an HttpPost. getHeaders/getFirstHeader/getLastHeader/getAllHeaders answer empty rather
 * than throwing, on the same reasoning as releaseConnection: a response header belongs to
 * the HttpResponse, and the only way to reach one was to execute this request.
 */
public class HttpGet implements HttpUriRequest {

   private final URI uri;

   public HttpGet() {
      this.uri = null;
   }

   public HttpGet(URI param0) {
      this.uri = param0;
   }

   public HttpGet(String param0) {
      this.uri = URI.create(param0);
   }

   public String getMethod() {
      return "GET";
   }

   public URI getURI() {
      return this.uri;
   }

   public boolean isAborted() {
      return false;
   }

   public void abort() throws UnsupportedOperationException {
   }

   /**
    * Releases the connection this request would have used.
    *
    * <p>In the real jar this is inherited from {@code HttpRequestBase}, which is the class
    * {@code HttpGet} actually extends. That intermediate class is not reproduced here (see
    * the header), so the method is declared directly.
    *
    * <p>It was missing here while present on {@link HttpPost}, and that single omission
    * cost twenty-three link errors -- more than every other fix in this stub set combined.
    * {@code FileDownload} calls it from three places: after a HEAD-ish content-length probe
    * ({@code contentLength}), after a finished world download ({@code lambda$download$0}),
    * and after a resource-pack download, in each case from a {@code finally} block that runs
    * whether or not the transfer succeeded. The last two are inside a lambda in a loop, which
    * is why the same missing method was reported twenty times rather than twice.
    *
    * <p>Being a {@code finally} on the failure path is what makes it worth having. Without
    * it the build does not link at all, so nothing here is a question of whether the happy
    * path works.
    */
   public void releaseConnection() {
   }

   public boolean containsHeader(String param0) {
      return false;
   }

   public Header[] getHeaders(String param0) {
      return new Header[0];
   }

   public Header getFirstHeader(String param0) {
      return null;
   }

   public Header getLastHeader(String param0) {
      return null;
   }

   public Header[] getAllHeaders() {
      return new Header[0];
   }

   public Locale getLocale() {
      return null;
   }

   public void setLocale(Locale param0) {
   }
}
