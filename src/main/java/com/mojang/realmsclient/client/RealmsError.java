package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class RealmsError {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String errorMessage;
   private final int errorCode;

   private RealmsError(String param0, int param1) {
      this.errorMessage = param0;
      this.errorCode = param1;
   }

   @Nullable
   public static RealmsError parse(String param0) {
      if (Strings.isNullOrEmpty(param0)) {
         return null;
      }

      try {
         JsonObject var1 = JsonParser.parseString(param0).getAsJsonObject();
         String var2 = JsonUtils.getStringOr("errorMsg", var1, "");
         int var3 = JsonUtils.getIntOr("errorCode", var1, -1);
         return new RealmsError(var2, var3);
      } catch (Exception var1) {
         LOGGER.error("Could not parse RealmsError: {}", var1.getMessage());
         LOGGER.error("The error was: {}", param0);
         return null;
      }
   }

   public String getErrorMessage() {
      return this.errorMessage;
   }

   public int getErrorCode() {
      return this.errorCode;
   }
}
