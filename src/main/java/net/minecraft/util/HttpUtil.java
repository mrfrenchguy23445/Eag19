package net.minecraft.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import net.minecraft.Util;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.apache.commons.io.IOUtils;
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
      return CompletableFuture.supplyAsync(() -> {
         HttpURLConnection l_var6 = null;
         InputStream l_var7 = null;
         OutputStream l_var8 = null;
         if (param4 != null) {
            param4.progressStart(Component.translatable("resourcepack.downloading"));
            param4.progressStage(Component.translatable("resourcepack.requesting"));
         }

         try {
            byte[] l_var9 = new byte[4096];
            l_var6 = (HttpURLConnection)Util.openConnection(param1, param5);
            l_var6.setInstanceFollowRedirects(true);
            float l_var10 = 0.0F;
            float l_var11 = param2.entrySet().size();

            for (Entry<String, String> l_var13 : param2.entrySet()) {
               l_var6.setRequestProperty(l_var13.getKey(), l_var13.getValue());
               if (param4 != null) {
                  param4.progressStagePercentage((int)(++l_var10 / l_var11 * 100.0F));
               }
            }

            l_var7 = l_var6.getInputStream();
            l_var11 = l_var6.getContentLength();
            int l_var12 = l_var6.getContentLength();
            if (param4 != null) {
               param4.progressStage(Component.translatable("resourcepack.progress", String.format(Locale.ROOT, "%.2f", l_var11 / 1000.0F / 1000.0F)));
            }

            if (param0.exists()) {
               long l_var13 = param0.length();
               if (l_var13 == l_var12) {
                  if (param4 != null) {
                     param4.stop();
                  }

                  return null;
               }

               LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", param0, l_var12, l_var13);
               FileUtils.deleteQuietly(param0);
            } else if (param0.getParentFile() != null) {
               param0.getParentFile().mkdirs();
            }

            l_var8 = new DataOutputStream(new FileOutputStream(param0));
            if (param3 > 0 && l_var11 > param3) {
               if (param4 != null) {
                  param4.stop();
               }

               throw new IOException("Filesize is bigger than maximum allowed (file is " + l_var10 + ", limit is " + param3 + ")");
            }

            int l_var13;
            while ((l_var13 = l_var7.read(l_var9)) >= 0) {
               l_var10 += l_var13;
               if (param4 != null) {
                  param4.progressStagePercentage((int)(l_var10 / l_var11 * 100.0F));
               }

               if (param3 > 0 && l_var10 > param3) {
                  if (param4 != null) {
                     param4.stop();
                  }

                  throw new IOException("Filesize was bigger than maximum allowed (got >= " + l_var10 + ", limit was " + param3 + ")");
               }

               if (Thread.interrupted()) {
                  LOGGER.error("INTERRUPTED");
                  if (param4 != null) {
                     param4.stop();
                  }

                  return null;
               }

               l_var8.write(l_var9, 0, l_var13);
            }

            if (param4 != null) {
               param4.stop();
            }
         } catch (Throwable l_var9) {
            LOGGER.error("Failed to download file", l_var9);
            if (l_var6 != null) {
               InputStream l_var10 = l_var6.getErrorStream();

               try {
                  LOGGER.error("HTTP response error: {}", IOUtils.toString(l_var10, StandardCharsets.UTF_8));
               } catch (IOException l_var11) {
                  LOGGER.error("Failed to read response from server");
               }
            }

            if (param4 != null) {
               param4.stop();
            }
         } finally {
            IOUtils.closeQuietly(l_var7);
            IOUtils.closeQuietly(l_var8);
         }

         return null;
      }, DOWNLOAD_EXECUTOR);
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
