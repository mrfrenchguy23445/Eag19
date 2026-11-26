package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import org.slf4j.Logger;

public class RealmsNews extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String newsLink;

   public static RealmsNews parse(String param0) {
      RealmsNews var1 = new RealmsNews();

      try {
         JsonParser var2 = new JsonParser();
         JsonObject var3 = var2.parse(param0).getAsJsonObject();
         var1.newsLink = JsonUtils.getStringOr("newsLink", var3, null);
      } catch (Exception var2) {
         LOGGER.error("Could not parse RealmsNews: {}", var2.getMessage());
      }

      return var1;
   }
}
