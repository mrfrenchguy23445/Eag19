package net.minecraft.server.network;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class TextFilterClient implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
   private static final ThreadFactory THREAD_FACTORY = param0 -> {
      Thread var1 = new Thread(param0);
      var1.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
      return var1;
   };
   private static final String DEFAULT_ENDPOINT = "v1/chat";
   private final URL chatEndpoint;
   private final TextFilterClient.MessageEncoder chatEncoder;
   final URL joinEndpoint;
   final TextFilterClient.JoinOrLeaveEncoder joinEncoder;
   final URL leaveEndpoint;
   final TextFilterClient.JoinOrLeaveEncoder leaveEncoder;
   private final String authKey;
   final TextFilterClient.IgnoreStrategy chatIgnoreStrategy;
   final ExecutorService workerPool;

   private TextFilterClient(
      URL param0,
      TextFilterClient.MessageEncoder param1,
      URL param2,
      TextFilterClient.JoinOrLeaveEncoder param3,
      URL param4,
      TextFilterClient.JoinOrLeaveEncoder param5,
      String param6,
      TextFilterClient.IgnoreStrategy param7,
      int param8
   ) {
      this.authKey = param6;
      this.chatIgnoreStrategy = param7;
      this.chatEndpoint = param0;
      this.chatEncoder = param1;
      this.joinEndpoint = param2;
      this.joinEncoder = param3;
      this.leaveEndpoint = param4;
      this.leaveEncoder = param5;
      this.workerPool = Executors.newFixedThreadPool(param8, THREAD_FACTORY);
   }

   private static URL getEndpoint(URI param0, @Nullable JsonObject param1, String param2, String param3) throws MalformedURLException {
      String var4 = getEndpointFromConfig(param1, param2, param3);
      return param0.resolve("/" + var4).toURL();
   }

   private static String getEndpointFromConfig(@Nullable JsonObject param0, String param1, String param2) {
      return param0 != null ? GsonHelper.getAsString(param0, param1, param2) : param2;
   }

   @Nullable
   public static TextFilterClient createFromConfig(String param0) {
      if (Strings.isNullOrEmpty(param0)) {
         return null;
      }

      try {
         JsonObject var1 = GsonHelper.parse(param0);
         URI var2 = new URI(GsonHelper.getAsString(var1, "apiServer"));
         String var3 = GsonHelper.getAsString(var1, "apiKey");
         if (var3.isEmpty()) {
            throw new IllegalArgumentException("Missing API key");
         }

         int var4 = GsonHelper.getAsInt(var1, "ruleId", 1);
         String var5 = GsonHelper.getAsString(var1, "serverId", "");
         String var6 = GsonHelper.getAsString(var1, "roomId", "Java:Chat");
         int var7 = GsonHelper.getAsInt(var1, "hashesToDrop", -1);
         int var8 = GsonHelper.getAsInt(var1, "maxConcurrentRequests", 7);
         JsonObject var9 = GsonHelper.getAsJsonObject(var1, "endpoints", null);
         String var10 = getEndpointFromConfig(var9, "chat", "v1/chat");
         boolean var11 = var10.equals("v1/chat");
         URL var12 = var2.resolve("/" + var10).toURL();
         URL var13 = getEndpoint(var2, var9, "join", "v1/join");
         URL var14 = getEndpoint(var2, var9, "leave", "v1/leave");
         TextFilterClient.JoinOrLeaveEncoder var15 = l_param2 -> {
            JsonObject l_var3 = new JsonObject();
            l_var3.addProperty("server", var5);
            l_var3.addProperty("room", var6);
            l_var3.addProperty("user_id", l_param2.getId().toString());
            l_var3.addProperty("user_display_name", l_param2.getName());
            return l_var3;
         };
         TextFilterClient.MessageEncoder var16;
         if (var11) {
            var16 = (l_param3, l_param4) -> {
               JsonObject l_var5 = new JsonObject();
               l_var5.addProperty("rule", var4);
               l_var5.addProperty("server", var5);
               l_var5.addProperty("room", var6);
               l_var5.addProperty("player", l_param3.getId().toString());
               l_var5.addProperty("player_display_name", l_param3.getName());
               l_var5.addProperty("text", l_param4);
               l_var5.addProperty("language", "*");
               return l_var5;
            };
         } else {
            String var17 = String.valueOf(var4);
            var16 = (l_param3, l_param4) -> {
               JsonObject l_var5 = new JsonObject();
               l_var5.addProperty("rule_id", var17);
               l_var5.addProperty("category", var5);
               l_var5.addProperty("subcategory", var6);
               l_var5.addProperty("user_id", l_param3.getId().toString());
               l_var5.addProperty("user_display_name", l_param3.getName());
               l_var5.addProperty("text", l_param4);
               l_var5.addProperty("language", "*");
               return l_var5;
            };
         }

         TextFilterClient.IgnoreStrategy var17 = TextFilterClient.IgnoreStrategy.select(var7);
         String var18 = Base64.getEncoder().encodeToString(var3.getBytes(StandardCharsets.US_ASCII));
         return new TextFilterClient(var12, var16, var13, var15, var14, var15, var18, var17, var8);
      } catch (Exception var1) {
         LOGGER.warn("Failed to parse chat filter config {}", param0, var1);
         return null;
      }
   }

   void processJoinOrLeave(GameProfile param0, URL param1, TextFilterClient.JoinOrLeaveEncoder param2, Executor param3) {
      param3.execute(() -> {
         JsonObject l_var4 = param2.encode(param0);

         try {
            this.processRequest(l_var4, param1);
         } catch (Exception l_var5) {
            LOGGER.warn("Failed to send join/leave packet to {} for player {}", param1, param0, l_var5);
         }
      });
   }

   CompletableFuture<FilteredText> requestMessageProcessing(GameProfile param0, String param1, TextFilterClient.IgnoreStrategy param2, Executor param3) {
      return param1.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
         JsonObject l_var4 = this.chatEncoder.encode(param0, param1);

         try {
            JsonObject l_var5 = this.processRequestResponse(l_var4, this.chatEndpoint);
            boolean l_var6 = GsonHelper.getAsBoolean(l_var5, "response", false);
            if (l_var6) {
               return FilteredText.passThrough(param1);
            }

            String l_var7 = GsonHelper.getAsString(l_var5, "hashed", null);
            if (l_var7 == null) {
               return FilteredText.fullyFiltered(param1);
            }

            JsonArray l_var8 = GsonHelper.getAsJsonArray(l_var5, "hashes");
            FilterMask l_var9 = this.parseMask(param1, l_var8, param2);
            return new FilteredText(param1, l_var9);
         } catch (Exception l_var5) {
            LOGGER.warn("Failed to validate message '{}'", param1, l_var5);
            return FilteredText.fullyFiltered(param1);
         }
      }, param3);
   }

   private FilterMask parseMask(String param0, JsonArray param1, TextFilterClient.IgnoreStrategy param2) {
      if (param1.isEmpty()) {
         return FilterMask.PASS_THROUGH;
      }

      if (param2.shouldIgnore(param0, param1.size())) {
         return FilterMask.FULLY_FILTERED;
      }

      FilterMask var4 = new FilterMask(param0.length());

      for (int var5 = 0; var5 < param1.size(); var5++) {
         var4.setFiltered(param1.get(var5).getAsInt());
      }

      return var4;
   }

   @Override
   public void close() {
      this.workerPool.shutdownNow();
   }

   private void drainStream(InputStream param0) throws IOException {
      byte[] var2 = new byte[1024];

      while (param0.read(var2) != -1) {
      }
   }

   private JsonObject processRequestResponse(JsonObject param0, URL param1) throws IOException {
      HttpURLConnection var3 = this.makeRequest(param0, param1);

      try (InputStream var4 = var3.getInputStream()) {
         if (var3.getResponseCode() == 204) {
            return new JsonObject();
         }

         try {
            return Streams.parse(new JsonReader(new InputStreamReader(var4, StandardCharsets.UTF_8))).getAsJsonObject();
         } finally {
            this.drainStream(var4);
         }
      }
   }

   private void processRequest(JsonObject param0, URL param1) throws IOException {
      HttpURLConnection var3 = this.makeRequest(param0, param1);

      try (InputStream var4 = var3.getInputStream()) {
         this.drainStream(var4);
      }
   }

   private HttpURLConnection makeRequest(JsonObject param0, URL param1) throws IOException {
      HttpURLConnection var3 = (HttpURLConnection)param1.openConnection();
      var3.setConnectTimeout(15000);
      var3.setReadTimeout(2000);
      var3.setUseCaches(false);
      var3.setDoOutput(true);
      var3.setDoInput(true);
      var3.setRequestMethod("POST");
      var3.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      var3.setRequestProperty("Accept", "application/json");
      var3.setRequestProperty("Authorization", "Basic " + this.authKey);
      var3.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().getName());
      OutputStreamWriter var4 = new OutputStreamWriter(var3.getOutputStream(), StandardCharsets.UTF_8);

      try (JsonWriter var5 = new JsonWriter(var4)) {
         Streams.write(param0, var5);
      } catch (Throwable var11) {
         try {
            var4.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      var4.close();
      int var4x = var3.getResponseCode();
      if (var4x >= 200 && var4x < 300) {
         return var3;
      } else {
         throw new TextFilterClient.RequestFailedException(var4x + " " + var3.getResponseMessage());
      }
   }

   public TextFilter createContext(GameProfile param0) {
      return new TextFilterClient.PlayerContext(param0);
   }

   @FunctionalInterface
   public interface IgnoreStrategy {
      TextFilterClient.IgnoreStrategy NEVER_IGNORE = (param0, param1) -> false;
      TextFilterClient.IgnoreStrategy IGNORE_FULLY_FILTERED = (param0, param1) -> param0.length() == param1;

      static TextFilterClient.IgnoreStrategy ignoreOverThreshold(int param0) {
         return (l_param1, l_param2) -> l_param2 >= param0;
      }

      static TextFilterClient.IgnoreStrategy select(int param0) {
         return switch (param0) {
            case -1 -> NEVER_IGNORE;
            case 0 -> IGNORE_FULLY_FILTERED;
            default -> ignoreOverThreshold(param0);
         };
      }

      boolean shouldIgnore(String var1, int var2);
   }

   @FunctionalInterface
   interface JoinOrLeaveEncoder {
      JsonObject encode(GameProfile var1);
   }

   @FunctionalInterface
   interface MessageEncoder {
      JsonObject encode(GameProfile var1, String var2);
   }

   class PlayerContext implements TextFilter {
      private final GameProfile profile;
      private final Executor streamExecutor;

      PlayerContext(GameProfile param1) {
         this.profile = param1;
         ProcessorMailbox<Runnable> var3 = ProcessorMailbox.create(TextFilterClient.this.workerPool, "chat stream for " + param1.getName());
         this.streamExecutor = var3::tell;
      }

      @Override
      public void join() {
         TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.joinEndpoint, TextFilterClient.this.joinEncoder, this.streamExecutor);
      }

      @Override
      public void leave() {
         TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.leaveEndpoint, TextFilterClient.this.leaveEncoder, this.streamExecutor);
      }

      @Override
      public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> param0) {
         List<CompletableFuture<FilteredText>> var2 = param0.stream()
            .map(
               l_param0 -> TextFilterClient.this.requestMessageProcessing(this.profile, l_param0, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor)
            )
            .collect(ImmutableList.toImmutableList());
         return Util.sequenceFailFast(var2).exceptionally(l_param0 -> ImmutableList.of());
      }

      @Override
      public CompletableFuture<FilteredText> processStreamMessage(String param0) {
         return TextFilterClient.this.requestMessageProcessing(this.profile, param0, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor);
      }
   }

   public static class RequestFailedException extends RuntimeException {
      RequestFailedException(String param0) {
         super(param0);
      }
   }
}
