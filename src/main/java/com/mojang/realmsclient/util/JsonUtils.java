package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;

public class JsonUtils {
   public static <T> T getRequired(String param0, JsonObject param1, Function<JsonObject, T> param2) {
      JsonElement var3 = param1.get(param0);
      if (var3 == null || var3.isJsonNull()) {
         throw new IllegalStateException("Missing required property: " + param0);
      } else if (!var3.isJsonObject()) {
         throw new IllegalStateException("Required property " + param0 + " was not a JsonObject as espected");
      } else {
         return param2.apply(var3.getAsJsonObject());
      }
   }

   public static String getRequiredString(String param0, JsonObject param1) {
      String var2 = getStringOr(param0, param1, null);
      if (var2 == null) {
         throw new IllegalStateException("Missing required property: " + param0);
      } else {
         return var2;
      }
   }

   @Nullable
   public static String getStringOr(String param0, JsonObject param1, @Nullable String param2) {
      JsonElement var3 = param1.get(param0);
      if (var3 != null) {
         return var3.isJsonNull() ? param2 : var3.getAsString();
      } else {
         return param2;
      }
   }

   @Nullable
   public static UUID getUuidOr(String param0, JsonObject param1, @Nullable UUID param2) {
      String var3 = getStringOr(param0, param1, null);
      return var3 == null ? param2 : UUID.fromString(var3);
   }

   public static int getIntOr(String param0, JsonObject param1, int param2) {
      JsonElement var3 = param1.get(param0);
      if (var3 != null) {
         return var3.isJsonNull() ? param2 : var3.getAsInt();
      } else {
         return param2;
      }
   }

   public static long getLongOr(String param0, JsonObject param1, long param2) {
      JsonElement var4 = param1.get(param0);
      if (var4 != null) {
         return var4.isJsonNull() ? param2 : var4.getAsLong();
      } else {
         return param2;
      }
   }

   public static boolean getBooleanOr(String param0, JsonObject param1, boolean param2) {
      JsonElement var3 = param1.get(param0);
      if (var3 != null) {
         return var3.isJsonNull() ? param2 : var3.getAsBoolean();
      } else {
         return param2;
      }
   }

   public static Date getDateOr(String param0, JsonObject param1) {
      JsonElement var2 = param1.get(param0);
      return var2 != null ? new Date(Long.parseLong(var2.getAsString())) : new Date();
   }
}
