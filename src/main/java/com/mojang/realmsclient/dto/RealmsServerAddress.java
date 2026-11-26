package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import org.slf4j.Logger;

public class RealmsServerAddress extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String address;
   public String resourcePackUrl;
   public String resourcePackHash;

   public static RealmsServerAddress parse(String param0) {
      JsonParser var1 = new JsonParser();
      RealmsServerAddress var2 = new RealmsServerAddress();

      try {
         JsonObject var3 = var1.parse(param0).getAsJsonObject();
         var2.address = JsonUtils.getStringOr("address", var3, null);
         var2.resourcePackUrl = JsonUtils.getStringOr("resourcePackUrl", var3, null);
         var2.resourcePackHash = JsonUtils.getStringOr("resourcePackHash", var3, null);
      } catch (Exception var3) {
         LOGGER.error("Could not parse RealmsServerAddress: {}", var3.getMessage());
      }

      return var2;
   }
}
