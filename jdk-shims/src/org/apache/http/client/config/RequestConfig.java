package org.apache.http.client.config;

/**
 * Stub of org.apache.http.client.config.RequestConfig (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Unlike the rest of the set this one is fully functional rather than refusing, because
 * it is the one httpclient type that runs before any network is attempted. Both
 * FileUpload and FileDownload build one in a field initialiser, and FileUpload is
 * constructed on the boot path (Minecraft.<init> -> ProfileKeyPairManager.create ->
 * ... -> new FileUpload). So this is real code executing during startup, and a stub that
 * threw here would break the boot rather than only break Realms.
 *
 * The two timeouts it carries are stored and returned faithfully. They are then discarded:
 * nothing ever reads them, because the only thing that would have applied them is
 * HttpClientBuilder.build(), which refuses.
 *
 * Narrowed to the two timeouts out of the real config's sixteen-odd fields. The rest
 * (proxy, redirects, auth schemes, cookie spec, max redirects) describe HTTP connection
 * behaviour this port has no equivalent for -- there is no connection manager to configure
 * -- and their types would drag in HttpHost and InetAddress, which is precisely the part of
 * httpclient that cost 100 link errors.
 */
public class RequestConfig {

   public static final RequestConfig DEFAULT = new RequestConfig(-1, -1);

   private final int connectTimeout;
   private final int socketTimeout;

   private RequestConfig(int param0, int param1) {
      this.connectTimeout = param0;
      this.socketTimeout = param1;
   }

   public int getConnectTimeout() {
      return this.connectTimeout;
   }

   public int getSocketTimeout() {
      return this.socketTimeout;
   }

   public static Builder custom() {
      return new Builder();
   }

   /**
    * The real Builder's constructor is package-private, and is left package-private here
    * for the same reason: custom() is the only way to get one.
    */
   public static class Builder {

      private int connectTimeout = -1;
      private int socketTimeout = -1;

      Builder() {
      }

      public Builder setConnectTimeout(int param0) {
         this.connectTimeout = param0;
         return this;
      }

      public Builder setSocketTimeout(int param0) {
         this.socketTimeout = param0;
         return this;
      }

      public RequestConfig build() {
         return new RequestConfig(this.connectTimeout, this.socketTimeout);
      }
   }
}
