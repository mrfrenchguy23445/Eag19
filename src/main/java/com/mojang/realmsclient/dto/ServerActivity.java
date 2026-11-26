package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;

public class ServerActivity extends ValueObject {
   public String profileUuid;
   public long joinTime;
   public long leaveTime;

   public static ServerActivity parse(JsonObject param0) {
      ServerActivity var1 = new ServerActivity();

      try {
         var1.profileUuid = JsonUtils.getStringOr("profileUuid", param0, null);
         var1.joinTime = JsonUtils.getLongOr("joinTime", param0, Long.MIN_VALUE);
         var1.leaveTime = JsonUtils.getLongOr("leaveTime", param0, Long.MIN_VALUE);
      } catch (Exception var3) {
      }

      return var1;
   }
}
