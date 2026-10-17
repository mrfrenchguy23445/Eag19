package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

public class FileUpload {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_RETRIES = 5;
   private static final String UPLOAD_PATH = "/upload";
   private final File file;
   private final long worldId;
   private final int slotId;
   private final UploadInfo uploadInfo;
   private final String sessionId;
   private final String username;
   private final String clientVersion;
   private final UploadStatus uploadStatus;
   private final AtomicBoolean cancelled = new AtomicBoolean(false);
   @Nullable
   private CompletableFuture<UploadResult> uploadTask;
   private final RequestConfig requestConfig = RequestConfig.custom()
      .setSocketTimeout((int)TimeUnit.MINUTES.toMillis(10L))
      .setConnectTimeout((int)TimeUnit.SECONDS.toMillis(15L))
      .build();

   public FileUpload(File param0, long param1, int param3, UploadInfo param4, User param5, String param6, UploadStatus param7) {
      this.file = param0;
      this.worldId = param1;
      this.slotId = param3;
      this.uploadInfo = param4;
      this.sessionId = param5.getSessionId();
      this.username = param5.getName();
      this.clientVersion = param6;
      this.uploadStatus = param7;
   }

   public void upload(Consumer<UploadResult> param0) {
      if (this.uploadTask == null) {
         this.uploadTask = CompletableFuture.supplyAsync(() -> this.requestUpload(0));
         this.uploadTask.thenAccept(param0);
      }
   }

   public void cancel() {
      this.cancelled.set(true);
      if (this.uploadTask != null) {
         this.uploadTask.cancel(false);
         this.uploadTask = null;
      }
   }

   private UploadResult requestUpload(int param0) {
      UploadResult.Builder var2 = new UploadResult.Builder();
      if (this.cancelled.get()) {
         return var2.build();
      }

      this.uploadStatus.totalBytes = this.file.length();
      HttpPost var3 = new HttpPost(this.uploadInfo.getUploadEndpoint().resolve("/upload/" + this.worldId + "/" + this.slotId));
      CloseableHttpClient var4 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();

      try {
         this.setupRequest(var3);
         HttpResponse var5 = var4.execute(var3);
         long var6 = this.getRetryDelaySeconds(var5);
         if (this.shouldRetry(var6, param0)) {
            return this.retryUploadAfter(var6, param0);
         }

         this.handleResponse(var5, var2);
      } catch (Exception var5) {
         if (!this.cancelled.get()) {
            LOGGER.error("Caught exception while uploading: ", var5);
         }
      } finally {
         this.cleanup(var3, var4);
      }

      return var2.build();
   }

   private void cleanup(HttpPost param0, @Nullable CloseableHttpClient param1) {
      param0.releaseConnection();
      if (param1 != null) {
         try {
            param1.close();
         } catch (IOException var3) {
            LOGGER.error("Failed to close Realms upload client");
         }
      }
   }

   private void setupRequest(HttpPost param0) throws FileNotFoundException {
      param0.setHeader("Cookie", "sid=" + this.sessionId + ";token=" + this.uploadInfo.getToken() + ";user=" + this.username + ";version=" + this.clientVersion);
      FileUpload.CustomInputStreamEntity var2 = new FileUpload.CustomInputStreamEntity(new FileInputStream(this.file), this.file.length(), this.uploadStatus);
      var2.setContentType("application/octet-stream");
      param0.setEntity(var2);
   }

   private void handleResponse(HttpResponse param0, UploadResult.Builder param1) throws IOException {
      int var3 = param0.getStatusLine().getStatusCode();
      if (var3 == 401) {
         LOGGER.debug("Realms server returned 401: {}", param0.getFirstHeader("WWW-Authenticate"));
      }

      param1.withStatusCode(var3);
      if (param0.getEntity() != null) {
         String var4 = EntityUtils.toString(param0.getEntity(), "UTF-8");
         if (var4 != null) {
            try {
               JsonParser var5 = new JsonParser();
               JsonElement var6 = var5.parse(var4).getAsJsonObject().get("errorMsg");
               Optional<String> var7 = Optional.ofNullable(var6).map(JsonElement::getAsString);
               param1.withErrorMessage(var7.orElse(null));
            } catch (Exception var8) {
            }
         }
      }
   }

   private boolean shouldRetry(long param0, int param2) {
      return param0 > 0L && param2 + 1 < 5;
   }

   private UploadResult retryUploadAfter(long param0, int param2) throws InterruptedException {
      Thread.sleep(Duration.ofSeconds(param0).toMillis());
      return this.requestUpload(param2 + 1);
   }

   private long getRetryDelaySeconds(HttpResponse param0) {
      return Optional.ofNullable(param0.getFirstHeader("Retry-After")).map(NameValuePair::getValue).map(Long::valueOf).orElse(0L);
   }

   public boolean isFinished() {
      return this.uploadTask.isDone() || this.uploadTask.isCancelled();
   }

   static class CustomInputStreamEntity extends InputStreamEntity {
      private final long length;
      private final InputStream content;
      private final UploadStatus uploadStatus;

      public CustomInputStreamEntity(InputStream param0, long param1, UploadStatus param3) {
         super(param0);
         this.content = param0;
         this.length = param1;
         this.uploadStatus = param3;
      }

      @Override
      public void writeTo(OutputStream param0) throws IOException {
         Args.notNull(param0, "Output stream");
         InputStream var2 = this.content;

         try {
            byte[] var3 = new byte[4096];
            int var4;
            if (this.length < 0L) {
               while ((var4 = var2.read(var3)) != -1) {
                  param0.write(var3, 0, var4);
                  this.uploadStatus.bytesWritten += var4;
               }
            } else {
               long var5 = this.length;

               while (var5 > 0L) {
                  var4 = var2.read(var3, 0, (int)Math.min(4096L, var5));
                  if (var4 == -1) {
                     break;
                  }

                  param0.write(var3, 0, var4);
                  this.uploadStatus.bytesWritten += var4;
                  var5 -= var4;
                  param0.flush();
               }
            }
         } finally {
            var2.close();
         }
      }
   }
}
