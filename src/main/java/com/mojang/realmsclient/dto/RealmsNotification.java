package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RealmsNotification {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String NOTIFICATION_UUID = "notificationUuid";
   private static final String DISMISSABLE = "dismissable";
   private static final String SEEN = "seen";
   private static final String TYPE = "type";
   private static final String VISIT_URL = "visitUrl";
   final UUID uuid;
   final boolean dismissable;
   final boolean seen;
   final String type;

   RealmsNotification(UUID param0, boolean param1, boolean param2, String param3) {
      this.uuid = param0;
      this.dismissable = param1;
      this.seen = param2;
      this.type = param3;
   }

   public boolean seen() {
      return this.seen;
   }

   public boolean dismissable() {
      return this.dismissable;
   }

   public UUID uuid() {
      return this.uuid;
   }

   public static List<RealmsNotification> parseList(String param0) {
      List<RealmsNotification> var1 = new ArrayList<>();

      try {
         for (JsonElement var4 : JsonParser.parseString(param0).getAsJsonObject().get("notifications").getAsJsonArray()) {
            var1.add(parse(var4.getAsJsonObject()));
         }
      } catch (Exception var2) {
         LOGGER.error("Could not parse list of RealmsNotifications", var2);
      }

      return var1;
   }

   private static RealmsNotification parse(JsonObject param0) {
      UUID var1 = JsonUtils.getUuidOr("notificationUuid", param0, null);
      if (var1 == null) {
         throw new IllegalStateException("Missing required property notificationUuid");
      }

      boolean var2 = JsonUtils.getBooleanOr("dismissable", param0, true);
      boolean var3 = JsonUtils.getBooleanOr("seen", param0, false);
      String var4 = JsonUtils.getRequiredString("type", param0);
      RealmsNotification var5 = new RealmsNotification(var1, var2, var3, var4);
      return "visitUrl".equals(var4) ? RealmsNotification.VisitUrl.parse(var5, param0) : var5;
   }

   public static class VisitUrl extends RealmsNotification {
      private static final String URL = "url";
      private static final String BUTTON_TEXT = "buttonText";
      private static final String MESSAGE = "message";
      private final String url;
      private final RealmsText buttonText;
      private final RealmsText message;

      private VisitUrl(RealmsNotification param0, String param1, RealmsText param2, RealmsText param3) {
         super(param0.uuid, param0.dismissable, param0.seen, param0.type);
         this.url = param1;
         this.buttonText = param2;
         this.message = param3;
      }

      public static RealmsNotification.VisitUrl parse(RealmsNotification param0, JsonObject param1) {
         String var2 = JsonUtils.getRequiredString("url", param1);
         RealmsText var3 = JsonUtils.getRequired("buttonText", param1, RealmsText::parse);
         RealmsText var4 = JsonUtils.getRequired("message", param1, RealmsText::parse);
         return new RealmsNotification.VisitUrl(param0, var2, var3, var4);
      }

      public Component getMessage() {
         return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
      }

      public Button buildOpenLinkButton(Screen param0) {
         Component var2 = this.buttonText.createComponent(Component.translatable("mco.notification.visitUrl.buttonText.default"));
         return Button.builder(var2, ConfirmLinkScreen.confirmLink(this.url, param0, true)).build();
      }
   }
}
