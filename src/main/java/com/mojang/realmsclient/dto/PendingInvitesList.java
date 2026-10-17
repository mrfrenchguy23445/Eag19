package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;

public class PendingInvitesList extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public List<PendingInvite> pendingInvites = Lists.newArrayList();

   public static PendingInvitesList parse(String param0) {
      PendingInvitesList var1 = new PendingInvitesList();

      try {
         JsonParser var2 = new JsonParser();
         JsonObject var3 = var2.parse(param0).getAsJsonObject();
         if (var3.get("invites").isJsonArray()) {
            Iterator<JsonElement> var4 = var3.get("invites").getAsJsonArray().iterator();

            while (var4.hasNext()) {
               var1.pendingInvites.add(PendingInvite.parse(var4.next().getAsJsonObject()));
            }
         }
      } catch (Exception var2) {
         LOGGER.error("Could not parse PendingInvitesList: {}", var2.getMessage());
      }

      return var1;
   }
}
