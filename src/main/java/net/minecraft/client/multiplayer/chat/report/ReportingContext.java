package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.network.chat.Component;

public final class ReportingContext {
   private static final int LOG_CAPACITY = 1024;
   private final AbuseReportSender sender;
   private final ReportEnvironment environment;
   private final ChatLog chatLog;
   @Nullable
   private ChatReportBuilder.ChatReport chatReportDraft;

   public ReportingContext(AbuseReportSender param0, ReportEnvironment param1, ChatLog param2) {
      this.sender = param0;
      this.environment = param1;
      this.chatLog = param2;
   }

   public static ReportingContext create(ReportEnvironment param0, UserApiService param1) {
      ChatLog var2 = new ChatLog(1024);
      AbuseReportSender var3 = AbuseReportSender.create(param0, param1);
      return new ReportingContext(var3, param0, var2);
   }

   public void draftReportHandled(Minecraft param0, @Nullable Screen param1, Runnable param2, boolean param3) {
      if (this.chatReportDraft != null) {
         ChatReportBuilder.ChatReport var5 = this.chatReportDraft.copy();
         param0.setScreen(
            new ConfirmScreen(
               l_param4 -> {
                  this.setChatReportDraft(null);
                  if (l_param4) {
                     param0.setScreen(new ChatReportScreen(param1, this, var5));
                  } else {
                     param2.run();
                  }
               },
               Component.translatable(param3 ? "gui.chatReport.draft.quittotitle.title" : "gui.chatReport.draft.title"),
               Component.translatable(param3 ? "gui.chatReport.draft.quittotitle.content" : "gui.chatReport.draft.content"),
               Component.translatable("gui.chatReport.draft.edit"),
               Component.translatable("gui.chatReport.draft.discard")
            )
         );
      } else {
         param2.run();
      }
   }

   public AbuseReportSender sender() {
      return this.sender;
   }

   public ChatLog chatLog() {
      return this.chatLog;
   }

   public boolean matches(ReportEnvironment param0) {
      return Objects.equals(this.environment, param0);
   }

   public void setChatReportDraft(@Nullable ChatReportBuilder.ChatReport param0) {
      this.chatReportDraft = param0;
   }

   public boolean hasDraftReport() {
      return this.chatReportDraft != null;
   }

   public boolean hasDraftReportFor(UUID param0) {
      return this.hasDraftReport() && this.chatReportDraft.isReportedPlayer(param0);
   }
}
