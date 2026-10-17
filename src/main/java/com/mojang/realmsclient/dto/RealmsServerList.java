package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;

public class RealmsServerList extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public List<RealmsServer> servers;

   public static RealmsServerList parse(String param0) {
      RealmsServerList var1 = new RealmsServerList();
      var1.servers = Lists.newArrayList();

      try {
         JsonParser var2 = new JsonParser();
         JsonObject var3 = var2.parse(param0).getAsJsonObject();
         if (var3.get("servers").isJsonArray()) {
            JsonArray var4 = var3.get("servers").getAsJsonArray();
            Iterator<JsonElement> var5 = var4.iterator();

            while (var5.hasNext()) {
               var1.servers.add(RealmsServer.parse(var5.next().getAsJsonObject()));
            }
         }
      } catch (Exception var2) {
         LOGGER.error("Could not parse McoServerList: {}", var2.getMessage());
      }

      return var1;
   }
}
