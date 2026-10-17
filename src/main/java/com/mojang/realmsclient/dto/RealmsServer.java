package com.mojang.realmsclient.dto;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

public class RealmsServer extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public long id;
   public String remoteSubscriptionId;
   public String name;
   public String motd;
   public RealmsServer.State state;
   public String owner;
   public String ownerUUID;
   public List<PlayerInfo> players;
   public Map<Integer, RealmsWorldOptions> slots;
   public boolean expired;
   public boolean expiredTrial;
   public int daysLeft;
   public RealmsServer.WorldType worldType;
   public int activeSlot;
   public String minigameName;
   public int minigameId;
   public String minigameImage;
   public RealmsServerPing serverPing = new RealmsServerPing();

   public String getDescription() {
      return this.motd;
   }

   public String getName() {
      return this.name;
   }

   public String getMinigameName() {
      return this.minigameName;
   }

   public void setName(String param0) {
      this.name = param0;
   }

   public void setDescription(String param0) {
      this.motd = param0;
   }

   public void updateServerPing(RealmsServerPlayerList param0) {
      List<String> var2 = Lists.newArrayList();
      int var3 = 0;

      for (String var5 : param0.players) {
         if (!var5.equals(Minecraft.getInstance().getUser().getUuid())) {
            String var6 = "";

            try {
               var6 = RealmsUtil.uuidToName(var5);
            } catch (Exception var7) {
               LOGGER.error("Could not get name for {}", var5, var7);
               continue;
            }

            var2.add(var6);
            var3++;
         }
      }

      this.serverPing.nrOfPlayers = String.valueOf(var3);
      this.serverPing.playerList = Joiner.on('\n').join(var2);
   }

   public static RealmsServer parse(JsonObject param0) {
      RealmsServer var1 = new RealmsServer();

      try {
         var1.id = JsonUtils.getLongOr("id", param0, -1L);
         var1.remoteSubscriptionId = JsonUtils.getStringOr("remoteSubscriptionId", param0, null);
         var1.name = JsonUtils.getStringOr("name", param0, null);
         var1.motd = JsonUtils.getStringOr("motd", param0, null);
         var1.state = getState(JsonUtils.getStringOr("state", param0, RealmsServer.State.CLOSED.name()));
         var1.owner = JsonUtils.getStringOr("owner", param0, null);
         if (param0.get("players") != null && param0.get("players").isJsonArray()) {
            var1.players = parseInvited(param0.get("players").getAsJsonArray());
            sortInvited(var1);
         } else {
            var1.players = Lists.newArrayList();
         }

         var1.daysLeft = JsonUtils.getIntOr("daysLeft", param0, 0);
         var1.expired = JsonUtils.getBooleanOr("expired", param0, false);
         var1.expiredTrial = JsonUtils.getBooleanOr("expiredTrial", param0, false);
         var1.worldType = getWorldType(JsonUtils.getStringOr("worldType", param0, RealmsServer.WorldType.NORMAL.name()));
         var1.ownerUUID = JsonUtils.getStringOr("ownerUUID", param0, "");
         if (param0.get("slots") != null && param0.get("slots").isJsonArray()) {
            var1.slots = parseSlots(param0.get("slots").getAsJsonArray());
         } else {
            var1.slots = createEmptySlots();
         }

         var1.minigameName = JsonUtils.getStringOr("minigameName", param0, null);
         var1.activeSlot = JsonUtils.getIntOr("activeSlot", param0, -1);
         var1.minigameId = JsonUtils.getIntOr("minigameId", param0, -1);
         var1.minigameImage = JsonUtils.getStringOr("minigameImage", param0, null);
      } catch (Exception var2) {
         LOGGER.error("Could not parse McoServer: {}", var2.getMessage());
      }

      return var1;
   }

   private static void sortInvited(RealmsServer param0) {
      param0.players
         .sort(
            (l_param0, l_param1) -> ComparisonChain.start()
               .compareFalseFirst(l_param1.getAccepted(), l_param0.getAccepted())
               .compare(l_param0.getName().toLowerCase(Locale.ROOT), l_param1.getName().toLowerCase(Locale.ROOT))
               .result()
         );
   }

   private static List<PlayerInfo> parseInvited(JsonArray param0) {
      List<PlayerInfo> var1 = Lists.newArrayList();

      for (JsonElement var3 : param0) {
         try {
            JsonObject var4 = var3.getAsJsonObject();
            PlayerInfo var5 = new PlayerInfo();
            var5.setName(JsonUtils.getStringOr("name", var4, null));
            var5.setUuid(JsonUtils.getStringOr("uuid", var4, null));
            var5.setOperator(JsonUtils.getBooleanOr("operator", var4, false));
            var5.setAccepted(JsonUtils.getBooleanOr("accepted", var4, false));
            var5.setOnline(JsonUtils.getBooleanOr("online", var4, false));
            var1.add(var5);
         } catch (Exception var6) {
         }
      }

      return var1;
   }

   private static Map<Integer, RealmsWorldOptions> parseSlots(JsonArray param0) {
      Map<Integer, RealmsWorldOptions> var1 = Maps.newHashMap();

      for (JsonElement var3 : param0) {
         try {
            JsonObject var5 = var3.getAsJsonObject();
            JsonParser var6 = new JsonParser();
            JsonElement var7 = var6.parse(var5.get("options").getAsString());
            RealmsWorldOptions var4;
            if (var7 == null) {
               var4 = RealmsWorldOptions.createDefaults();
            } else {
               var4 = RealmsWorldOptions.parse(var7.getAsJsonObject());
            }

            int var8 = JsonUtils.getIntOr("slotId", var5, -1);
            var1.put(var8, var4);
         } catch (Exception var9) {
         }
      }

      for (int var2 = 1; var2 <= 3; var2++) {
         if (!var1.containsKey(var2)) {
            var1.put(var2, RealmsWorldOptions.createEmptyDefaults());
         }
      }

      return var1;
   }

   private static Map<Integer, RealmsWorldOptions> createEmptySlots() {
      Map<Integer, RealmsWorldOptions> var0 = Maps.newHashMap();
      var0.put(1, RealmsWorldOptions.createEmptyDefaults());
      var0.put(2, RealmsWorldOptions.createEmptyDefaults());
      var0.put(3, RealmsWorldOptions.createEmptyDefaults());
      return var0;
   }

   public static RealmsServer parse(String param0) {
      try {
         return parse(new JsonParser().parse(param0).getAsJsonObject());
      } catch (Exception var1) {
         LOGGER.error("Could not parse McoServer: {}", var1.getMessage());
         return new RealmsServer();
      }
   }

   private static RealmsServer.State getState(String param0) {
      try {
         return RealmsServer.State.valueOf(param0);
      } catch (Exception var1) {
         return RealmsServer.State.CLOSED;
      }
   }

   private static RealmsServer.WorldType getWorldType(String param0) {
      try {
         return RealmsServer.WorldType.valueOf(param0);
      } catch (Exception var1) {
         return RealmsServer.WorldType.NORMAL;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
   }

   @Override
   public boolean equals(Object param0) {
      if (param0 == null) {
         return false;
      }

      if (param0 == this) {
         return true;
      }

      if (param0.getClass() != this.getClass()) {
         return false;
      }

      RealmsServer var2 = (RealmsServer)param0;
      return new EqualsBuilder()
         .append(this.id, var2.id)
         .append(this.name, var2.name)
         .append(this.motd, var2.motd)
         .append(this.state, var2.state)
         .append(this.owner, var2.owner)
         .append(this.expired, var2.expired)
         .append(this.worldType, this.worldType)
         .isEquals();
   }

   public RealmsServer clone() {
      RealmsServer var1 = new RealmsServer();
      var1.id = this.id;
      var1.remoteSubscriptionId = this.remoteSubscriptionId;
      var1.name = this.name;
      var1.motd = this.motd;
      var1.state = this.state;
      var1.owner = this.owner;
      var1.players = this.players;
      var1.slots = this.cloneSlots(this.slots);
      var1.expired = this.expired;
      var1.expiredTrial = this.expiredTrial;
      var1.daysLeft = this.daysLeft;
      var1.serverPing = new RealmsServerPing();
      var1.serverPing.nrOfPlayers = this.serverPing.nrOfPlayers;
      var1.serverPing.playerList = this.serverPing.playerList;
      var1.worldType = this.worldType;
      var1.ownerUUID = this.ownerUUID;
      var1.minigameName = this.minigameName;
      var1.activeSlot = this.activeSlot;
      var1.minigameId = this.minigameId;
      var1.minigameImage = this.minigameImage;
      return var1;
   }

   public Map<Integer, RealmsWorldOptions> cloneSlots(Map<Integer, RealmsWorldOptions> param0) {
      Map<Integer, RealmsWorldOptions> var2 = Maps.newHashMap();

      for (Entry<Integer, RealmsWorldOptions> var4 : param0.entrySet()) {
         var2.put(var4.getKey(), var4.getValue().clone());
      }

      return var2;
   }

   public String getWorldName(int param0) {
      return this.name + " (" + this.slots.get(param0).getSlotName(param0) + ")";
   }

   public ServerData toServerData(String param0) {
      return new ServerData(this.name, param0, false);
   }

   public static class McoServerComparator implements Comparator<RealmsServer> {
      private final String refOwner;

      public McoServerComparator(String param0) {
         this.refOwner = param0;
      }

      public int compare(RealmsServer param0, RealmsServer param1) {
         return ComparisonChain.start()
            .compareTrueFirst(param0.state == RealmsServer.State.UNINITIALIZED, param1.state == RealmsServer.State.UNINITIALIZED)
            .compareTrueFirst(param0.expiredTrial, param1.expiredTrial)
            .compareTrueFirst(param0.owner.equals(this.refOwner), param1.owner.equals(this.refOwner))
            .compareFalseFirst(param0.expired, param1.expired)
            .compareTrueFirst(param0.state == RealmsServer.State.OPEN, param1.state == RealmsServer.State.OPEN)
            .compare(param0.id, param1.id)
            .result();
      }
   }

   public enum State {
      CLOSED,
      OPEN,
      UNINITIALIZED;
   }

   public enum WorldType {
      NORMAL,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
