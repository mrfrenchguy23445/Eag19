package net.minecraft.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import org.slf4j.Logger;

public class HttpUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ListeningExecutorService DOWNLOAD_EXECUTOR = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(
         new ThreadFactoryBuilder()
            .setDaemon(true)
            .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
            .setNameFormat("Downloader %d")
            .build()
      )
   );

   private HttpUtil() {
   }

   public static CompletableFuture<?> downloadTo(
      File param0, URL param1, Map<String, String> param2, int param3, @Nullable ProgressListener param4, Proxy param5
   ) {
      // Port note (wasm19): remote file download is refused in this build, for the same reason
      // HttpTexture's is. Upstream this ran on DOWNLOAD_EXECUTOR and opened param1 through
      // Util.openConnection, streaming the body into param0; on the WASM-GC classlib that resolves
      // to an XHR-backed HttpURLConnection whose performRequest parks on org.teavm.runtime.Fiber,
      // which this target does not provide, so the method failed the link:
      //
      //   Class org.teavm.runtime.Fiber is not supported on current target
      //       at java.net.impl.XHRURLConnection.performRequest
      //       at net.minecraft.util.HttpUtil.downloadTo(HttpUtil.java:71)
      //       at net.minecraft.client.resources.DownloadedPackSource.downloadAndSelectResourcePack(...)
      //
      // HttpURLConnection is XHR under the hood here and XHR's performRequest is @Async, so this is
      // not a missing class a shim could supply: making the call at all is what requires the Fiber
      // continuation machinery, and no reachable HTTP call survives without it. That is the same
      // conclusion Realms32BitWarningStatus reached for the Realms request path.
      //
      // The one caller is DownloadedPackSource.downloadAndSelectResourcePack, which asks for a
      // server's resource pack. Nothing here can fetch one: this port's downloads run through
      // XHR-backed connections, and there is no path that turns an XHR response into a file in
      // serverPackDir. Refusing with a failed future rather than a silent completed one leaves the
      // caller's own failure path intact -- clear the pack, delete the half-downloaded file, show
      // the "pack application failed" prompt -- and gives its log line a cause instead of a hash
      // mismatch against a file that was never written. The progress listener is stopped first so
      // the download screen does not sit at "requesting" behind the prompt.
      if (param4 != null) {
         param4.stop();
      }

      LOGGER.warn("Not downloading {}: remote file download is disabled in this build", param1);
      return CompletableFuture.failedFuture(
         new IOException("Remote file download is disabled in this build; requested " + param1)
      );
   }

   public static int getAvailablePort() {
      // Both of these used to open a java.net.ServerSocket, which does not exist in the
      // WASM-GC TeaVM classlib -- java.net there is URL and URI handling only, with no
      // Socket, InetAddress or InetSocketAddress. That was 8 "Class java.net.ServerSocket
      // was not found" errors, all of them from here.
      //
      // Asking the OS for an ephemeral port has no meaning in a browser anyway. There is
      // no host TCP stack to ask: a client's "port" is a field in a WebSocket URL, and
      // whether anything answers on it is decided by the JavaScript peer, not by a socket
      // this code could bind and release.
      //
      // 25564 is not a new number. It is what the vanilla method already returned on its
      // failure path -- new ServerSocket(0) throwing IOException -- so this preserves the
      // value the caller would have seen had the class merely been absent rather than
      // unmodellable. It is also 25565 (the vanilla default server port) minus one, which
      // is the same relationship that fallback has in vanilla.
      return 25564;
   }

   public static boolean isPortAvailable(int param0) {
      // Range check is kept; the probe is not. See getAvailablePort above for why there is
      // nothing to bind.
      //
      // This answers true for any port in range, where vanilla would have answered false
      // for every one of them (its catch block returns false). That is deliberate and is
      // the more useful of the two answers: this port never binds the number, so nothing
      // can be occupying it, and answering false would leave the Open-to-LAN screen
      // permanently showing "port unavailable" for every value the player could type.
      return param0 >= 1024 && param0 <= 65535;
   }
}
