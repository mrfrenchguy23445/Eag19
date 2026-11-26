package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.StringUtils;

public class BanNoticeScreen {
   private static final Component TEMPORARY_BAN_TITLE = Component.translatable("gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
   private static final Component PERMANENT_BAN_TITLE = Component.translatable("gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);

   public static ConfirmLinkScreen create(BooleanConsumer param0, BanDetails param1) {
      return new ConfirmLinkScreen(
         param0, getBannedTitle(param1), getBannedScreenText(param1), "https://aka.ms/mcjavamoderation", CommonComponents.GUI_ACKNOWLEDGE, true
      );
   }

   private static Component getBannedTitle(BanDetails param0) {
      return isTemporaryBan(param0) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
   }

   private static Component getBannedScreenText(BanDetails param0) {
      return Component.translatable(
         "gui.banned.description", getBanReasonText(param0), getBanStatusText(param0), Component.literal("https://aka.ms/mcjavamoderation")
      );
   }

   private static Component getBanReasonText(BanDetails param0) {
      String var1 = param0.reason();
      String var2 = param0.reasonMessage();
      if (StringUtils.isNumeric(var1)) {
         int var3 = Integer.parseInt(var1);
         BanReason var4 = BanReason.byId(var3);
         Component var5;
         if (var4 != null) {
            var5 = ComponentUtils.mergeStyles(var4.title().copy(), Style.EMPTY.withBold(true));
         } else if (var2 != null) {
            var5 = Component.translatable("gui.banned.description.reason_id_message", var3, var2).withStyle(ChatFormatting.BOLD);
         } else {
            var5 = Component.translatable("gui.banned.description.reason_id", var3).withStyle(ChatFormatting.BOLD);
         }

         return Component.translatable("gui.banned.description.reason", var5);
      } else {
         return Component.translatable("gui.banned.description.unknownreason");
      }
   }

   private static Component getBanStatusText(BanDetails param0) {
      if (isTemporaryBan(param0)) {
         Component var1 = getBanDurationText(param0);
         return Component.translatable(
            "gui.banned.description.temporary", Component.translatable("gui.banned.description.temporary.duration", var1).withStyle(ChatFormatting.BOLD)
         );
      } else {
         return Component.translatable("gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
      }
   }

   private static Component getBanDurationText(BanDetails param0) {
      Duration var1 = Duration.between(Instant.now(), param0.expires());
      long var2 = var1.toHours();
      if (var2 > 72L) {
         return CommonComponents.days(var1.toDays());
      } else {
         return var2 < 1L ? CommonComponents.minutes(var1.toMinutes()) : CommonComponents.hours(var1.toHours());
      }
   }

   private static boolean isTemporaryBan(BanDetails param0) {
      return param0.expires() != null;
   }
}
