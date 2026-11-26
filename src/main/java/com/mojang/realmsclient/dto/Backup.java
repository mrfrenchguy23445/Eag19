package com.mojang.realmsclient.dto;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;

public class Backup extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String backupId;
   public Date lastModifiedDate;
   public long size;
   private boolean uploadedVersion;
   public Map<String, String> metadata = Maps.newHashMap();
   public Map<String, String> changeList = Maps.newHashMap();

   public static Backup parse(JsonElement param0) {
      JsonObject var1 = param0.getAsJsonObject();
      Backup var2 = new Backup();

      try {
         var2.backupId = JsonUtils.getStringOr("backupId", var1, "");
         var2.lastModifiedDate = JsonUtils.getDateOr("lastModifiedDate", var1);
         var2.size = JsonUtils.getLongOr("size", var1, 0L);
         if (var1.has("metadata")) {
            JsonObject var3 = var1.getAsJsonObject("metadata");

            for (Entry<String, JsonElement> var6 : var3.entrySet()) {
               if (!var6.getValue().isJsonNull()) {
                  var2.metadata.put(format(var6.getKey()), var6.getValue().getAsString());
               }
            }
         }
      } catch (Exception var3) {
         LOGGER.error("Could not parse Backup: {}", var3.getMessage());
      }

      return var2;
   }

   private static String format(String param0) {
      String[] var1 = param0.split("_");
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1) {
         if (var6 != null && var6.length() >= 1) {
            if ("of".equals(var6)) {
               var2.append(var6).append(" ");
            } else {
               char var7 = Character.toUpperCase(var6.charAt(0));
               var2.append(var7).append(var6.substring(1)).append(" ");
            }
         }
      }

      return var2.toString();
   }

   public boolean isUploadedVersion() {
      return this.uploadedVersion;
   }

   public void setUploadedVersion(boolean param0) {
      this.uploadedVersion = param0;
   }
}
