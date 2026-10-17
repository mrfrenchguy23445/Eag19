package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class WorldTemplate extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String id = "";
   public String name = "";
   public String version = "";
   public String author = "";
   public String link = "";
   @Nullable
   public String image;
   public String trailer = "";
   public String recommendedPlayers = "";
   public WorldTemplate.WorldTemplateType type = WorldTemplate.WorldTemplateType.WORLD_TEMPLATE;

   public static WorldTemplate parse(JsonObject param0) {
      WorldTemplate var1 = new WorldTemplate();

      try {
         var1.id = JsonUtils.getStringOr("id", param0, "");
         var1.name = JsonUtils.getStringOr("name", param0, "");
         var1.version = JsonUtils.getStringOr("version", param0, "");
         var1.author = JsonUtils.getStringOr("author", param0, "");
         var1.link = JsonUtils.getStringOr("link", param0, "");
         var1.image = JsonUtils.getStringOr("image", param0, null);
         var1.trailer = JsonUtils.getStringOr("trailer", param0, "");
         var1.recommendedPlayers = JsonUtils.getStringOr("recommendedPlayers", param0, "");
         var1.type = WorldTemplate.WorldTemplateType.valueOf(JsonUtils.getStringOr("type", param0, WorldTemplate.WorldTemplateType.WORLD_TEMPLATE.name()));
      } catch (Exception var2) {
         LOGGER.error("Could not parse WorldTemplate: {}", var2.getMessage());
      }

      return var1;
   }

   public enum WorldTemplateType {
      WORLD_TEMPLATE,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
