package org.apache.http.client.methods;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * Stub of org.apache.http.client.methods.HttpPost (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Unlike HttpGet, this one has to carry real state. FileUpload.setupRequest builds the
 * request before execute() is ever reached -- and FileUpload is constructed on the boot
 * path, so its setupRequest runs before any network is attempted -- and sets a Cookie
 * header and the request body on it. A stub that dropped setHeader/setEntity would fail
 * to compile against the real jar, so these are kept, and they store what they are given
 * so that getFirstHeader can answer.
 *
 * The real HttpPost extends HttpEntityEnclosingRequestBase, which supplies setHeader and
 * getFirstHeader. That class is not reproduced; the members are declared here with the
 * real signatures. See HttpGet for the same note on the header accessors.
 */
public class HttpPost implements HttpEntityEnclosingRequest {

   private final URI uri;
   private final Map<String, String> headers = new LinkedHashMap<>();
   private HttpEntity entity;
   private boolean aborted;

   public HttpPost() {
      this.uri = null;
   }

   public HttpPost(URI param0) {
      this.uri = param0;
   }

   public HttpPost(String param0) {
      this.uri = URI.create(param0);
   }

   public String getMethod() {
      return "POST";
   }

   public URI getURI() {
      return this.uri;
   }

   public boolean isAborted() {
      return this.aborted;
   }

   public void abort() throws UnsupportedOperationException {
      this.aborted = true;
   }

   public boolean expectContinue() {
      return false;
   }

   public HttpEntity getEntity() {
      return this.entity;
   }

   public void setEntity(HttpEntity param0) {
      this.entity = param0;
   }

   public void setHeader(String param0, String param1) {
      this.headers.put(param0, param1);
   }

   public void releaseConnection() {
   }

   public boolean containsHeader(String param0) {
      return this.headers.containsKey(param0);
   }

   public Header[] getHeaders(String param0) {
      String var1 = this.headers.get(param0);
      return var1 == null ? new Header[0] : new Header[]{new BasicHeader(param0, var1)};
   }

   public Header getFirstHeader(String param0) {
      String var1 = this.headers.get(param0);
      return var1 == null ? null : new BasicHeader(param0, var1);
   }

   public Header getLastHeader(String param0) {
      return this.getFirstHeader(param0);
   }

   public Header[] getAllHeaders() {
      Header[] var1 = new Header[this.headers.size()];
      int var2 = 0;

      for (Map.Entry<String, String> var3 : this.headers.entrySet()) {
         var1[var2++] = new BasicHeader(var3.getKey(), var3.getValue());
      }

      return var1;
   }

   public Locale getLocale() {
      return null;
   }

   public void setLocale(Locale param0) {
   }

   /**
    * The one value type here. The real httpcore has BasicHeader, which is exactly this
    * constructor and exactly these two accessors; it is reproduced privately rather than
    * stubbed because nothing outside this file can observe it.
    */
   private static final class BasicHeader implements Header {

      private final String name;
      private final String value;

      BasicHeader(String param0, String param1) {
         this.name = param0;
         this.value = param1;
      }

      @Override
      public String getName() {
         return this.name;
      }

      @Override
      public String getValue() {
         return this.value;
      }

      @Override
      public String toString() {
         return this.name + ": " + this.value;
      }
   }
}
