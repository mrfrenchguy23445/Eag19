package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class UploadInfo extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_SCHEMA = "http://";
   private static final int DEFAULT_PORT = 8080;
   private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");
   private final boolean worldClosed;
   @Nullable
   private final String token;
   private final URI uploadEndpoint;

   private UploadInfo(boolean param0, @Nullable String param1, URI param2) {
      this.worldClosed = param0;
      this.token = param1;
      this.uploadEndpoint = param2;
   }

   @Nullable
   public static UploadInfo parse(String param0) {
      try {
         JsonParser var1 = new JsonParser();
         JsonObject var2 = var1.parse(param0).getAsJsonObject();
         String var3 = JsonUtils.getStringOr("uploadEndpoint", var2, null);
         if (var3 != null) {
            int var4 = JsonUtils.getIntOr("port", var2, -1);
            URI var5 = assembleUri(var3, var4);
            if (var5 != null) {
               boolean var6 = JsonUtils.getBooleanOr("worldClosed", var2, false);
               String var7 = JsonUtils.getStringOr("token", var2, null);
               return new UploadInfo(var6, var7, var5);
            }
         }
      } catch (Exception var1) {
         LOGGER.error("Could not parse UploadInfo: {}", var1.getMessage());
      }

      return null;
   }

   @Nullable
   @VisibleForTesting
   public static URI assembleUri(String param0, int param1) {
      Matcher var2 = URI_SCHEMA_PATTERN.matcher(param0);
      String var3 = ensureEndpointSchema(param0, var2);

      try {
         URI var4 = new URI(var3);
         int var5 = selectPortOrDefault(param1, var4.getPort());
         return var5 != var4.getPort()
            ? new URI(var4.getScheme(), var4.getUserInfo(), var4.getHost(), var5, var4.getPath(), var4.getQuery(), var4.getFragment())
            : var4;
      } catch (URISyntaxException var4) {
         LOGGER.warn("Failed to parse URI {}", var3, var4);
         return null;
      }
   }

   private static int selectPortOrDefault(int param0, int param1) {
      if (param0 != -1) {
         return param0;
      } else {
         return param1 != -1 ? param1 : 8080;
      }
   }

   private static String ensureEndpointSchema(String param0, Matcher param1) {
      return param1.find() ? param0 : "http://" + param0;
   }

   public static String createRequest(@Nullable String param0) {
      JsonObject var1 = new JsonObject();
      if (param0 != null) {
         var1.addProperty("token", param0);
      }

      return var1.toString();
   }

   @Nullable
   public String getToken() {
      return this.token;
   }

   public URI getUploadEndpoint() {
      return this.uploadEndpoint;
   }

   public boolean isWorldClosed() {
      return this.worldClosed;
   }
}
