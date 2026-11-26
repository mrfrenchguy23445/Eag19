package org.apache.http.impl.client;

import org.apache.http.client.config.RequestConfig;

/**
 * Stub of org.apache.http.impl.client.HttpClientBuilder (httpclient 4.5.13), replacing
 * the real jar. This is the file to read to understand why Apache HttpClient is stubbed
 * rather than linked.
 *
 * WHY THE REAL JARS ARE GONE
 * ==========================
 *
 * httpclient was added as an implementation dependency to take 51 of 183 link errors out,
 * on the reasoning recorded in target_teavm_wasm_gc/build.gradle.kts: "httpclient is
 * reached from AccountProfileKeyPairManager.<init> on the boot path ... Both are dead
 * weight for a port that talks over Eaglercraft's own websocket transport, but they are
 * reachable code, and the linker resolves a reachable callee even when the branch is never
 * taken."
 *
 * Every clause of that is true, and the conclusion drawn from it was still wrong. Declaring
 * the jar does resolve the game's own references -- and then hands the linker 51 new
 * dependencies to resolve underneath. httpclient 4's transport is java.net.Socket,
 * javax.net.ssl and java.security, and on the WASM-GC classlib none of them exist.
 *
 * Measured on the next full link (255 errors), grouping the failures by the deepest frame
 * in each stack:
 *
 *   java.net.Socket ......................... 30   all 30 from BHttpConnectionBase /
 *                                                  DefaultHttpClientConnectionOperator
 *   java.util.concurrent.locks.ReadWriteLock  12   BasicCookieStore
 *   javax.net.ssl.SSLSocket .................. 10   SSLConnectionSocketFactory
 *   java.net.InetAddress ....................  8
 *   java.security.Provider ..................  8
 *   java.util.concurrent.atomic
 *     .AtomicMarkableReference ..............  7   FileDownload.contentLength
 *   javax.crypto.Cipher .....................  4   NTLMEngineImpl -- NTLM proxy auth
 *   java.net.InetSocketAddress ..............  3
 *   java.net.PasswordAuthentication .........  3
 *   javax.net.ssl.SSLContext .................  3
 *   java.io.ObjectOutputStream ..............  1   AuthenticationStrategyImpl, which
 *                                                  java-serialises credentials
 *   java.io.ObjectInputStream ...............  1
 *   java.lang.StackWalker ...................  1   via sun.security.util.Debug
 *   java.lang.ref.PhantomReference ..........  1
 *   javax.security.cert.X509Certificate .....  1
 *   java.security.Principal .................  2
 *   java.net.UnknownHostException ...........  1
 *   java.net.ConnectException ...............  1
 *   java.net.MulticastSocket ................  1
 *   java.net.SocketFactory ..................  1
 *   java.net.ProxySelector ..................  1
 *   java.security.Security ..................  1
 *   java.security.KeyManagementException ...  1
 *   java.security.cert
 *     .CertificateEncodingException .........  1
 *   javax.net.ssl.SSLSocketFactory ..........  2   (fields, Authenticator$RequestorType)
 *   javax.net.ssl.SSLSession .................  1
 *   javax.crypto.spec.SecretKeySpec .........  1
 *   java.util.concurrent
 *     .locks.Condition ......................  4
 *
 * Around 105 of the 255 errors -- 41% -- came from one dependency that was added to reduce
 * the count. Adding text2speech in the same commit cost another 39 (see Narrator.java).
 * The five jars added together took the build from 183 errors to 255.
 *
 * Note what is NOT on that list: java.net.Socket is absent from Netty entirely. Every one
 * of the 30 was reached through org.apache.http, checked per-stack rather than assumed.
 *
 * WHAT THESE STUBS DO
 * ===================
 *
 * Two different answers, split by whether the type does I/O.
 *
 * The value objects are real. RequestConfig, HttpGet, HttpPost, InputStreamEntity, Args and
 * EntityUtils all genuinely run, because FileUpload and FileDownload construct and populate
 * them during startup -- FileUpload's field initialiser builds a RequestConfig, and its
 * setupRequest sets a Cookie header and the request body, all on the boot path before any
 * transfer is attempted. A stub that refused at the value objects would break the boot.
 *
 * The transfer is refused, and refused loudly. build() is the point where a real
 * CloseableHttpClient would be assembled -- connection manager, SSL context, retry
 * handler, auth strategy -- and every one of those needs java.net.Socket. It throws
 * UnsupportedOperationException naming the operation.
 *
 * Refusing is safe here, not merely acceptable, because every call site already handles it:
 *
 *   FileUpload.requestUpload  wraps the whole body in catch (Exception) and returns an
 *                              UploadResult carrying the failure
 *   FileDownload.contentLength wraps in catch (Throwable) and returns 0L
 *   FileDownload.download      catches (Exception) and sets this.error = true
 *
 * and in both methods the client variable is null-guarded in the finally block, so an
 * exception out of build() skips close() rather than dereferencing null.
 *
 * What this costs: Realms world upload and download, which is Mojang's paid backup
 * service. It needs a Mojang account and a reachable Realms endpoint, neither of which
 * exists for an offline-mode Eaglercraft client, and it never worked here -- it did not
 * link. Nothing on the path to the title screen or a singleplayer world is affected.
 *
 * SUBSET RULE
 * ===========
 *
 * Every member of every file in this set is a subset of the real class, with the real
 * signature, real modifiers and the same throws clauses. Nothing is added and nothing is
 * widened, because the root project still compiles the game against the real jars
 * (compileOnly(libs.bundles.mc1194)) and TeaVM links against these. A superset would
 * type-check against the real class and then fail to link, which is the failure mode the
 * log4j stubs in this same source tree are written to avoid.
 */
public class HttpClientBuilder {

   private RequestConfig defaultRequestConfig;

   public static HttpClientBuilder create() {
      return new HttpClientBuilder();
   }

   /**
    * Stores the config and returns this, as the real builder does. The value is kept rather
    * than discarded so that setDefaultRequestConfig -> build() reads the way it did; nothing
    * consults it, because build() does not return.
    */
   public final HttpClientBuilder setDefaultRequestConfig(RequestConfig param0) {
      this.defaultRequestConfig = param0;
      return this;
   }

   /**
    * Always throws. This is where the real builder assembles a connection manager, an SSL
    * context, a retry handler and an authentication strategy -- the java.net.Socket and
    * javax.net.ssl stack enumerated in the header above.
    *
    * @throws UnsupportedOperationException always
    */
   public CloseableHttpClient build() {
      throw new UnsupportedOperationException(
         "Apache HttpClient is not linked in this port. build() would create a connection manager "
            + "over java.net.Socket, which the WASM-GC TeaVM classlib does not provide; this is the "
            + "Realms transfer path (world upload/download), which needs a Mojang account this client "
            + "does not have. The linked-in replacement for ordinary HTTP is "
            + "net.minecraft.util.HttpUtil.downloadTo, which goes through the browser's own stack."
            + (this.defaultRequestConfig == null ? "" : " Requested timeouts: connect="
            + this.defaultRequestConfig.getConnectTimeout() + "ms, socket="
            + this.defaultRequestConfig.getSocketTimeout() + "ms."));
   }
}
