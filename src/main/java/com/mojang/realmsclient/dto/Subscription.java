package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import org.slf4j.Logger;

public class Subscription extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public long startDate;
   public int daysLeft;
   public Subscription.SubscriptionType type = Subscription.SubscriptionType.NORMAL;

   public static Subscription parse(String param0) {
      Subscription var1 = new Subscription();

      try {
         JsonParser var2 = new JsonParser();
         JsonObject var3 = var2.parse(param0).getAsJsonObject();
         var1.startDate = JsonUtils.getLongOr("startDate", var3, 0L);
         var1.daysLeft = JsonUtils.getIntOr("daysLeft", var3, 0);
         var1.type = typeFrom(JsonUtils.getStringOr("subscriptionType", var3, Subscription.SubscriptionType.NORMAL.name()));
      } catch (Exception var2) {
         LOGGER.error("Could not parse Subscription: {}", var2.getMessage());
      }

      return var1;
   }

   private static Subscription.SubscriptionType typeFrom(String param0) {
      try {
         return Subscription.SubscriptionType.valueOf(param0);
      } catch (Exception var1) {
         return Subscription.SubscriptionType.NORMAL;
      }
   }

   public enum SubscriptionType {
      NORMAL,
      RECURRING;
   }
}
