package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;

public interface AbuseReportSender {
   static AbuseReportSender create(ReportEnvironment param0, UserApiService param1) {
      return new AbuseReportSender.Services(param0, param1);
   }

   CompletableFuture<Unit> send(UUID var1, AbuseReport var2);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   class SendException extends ThrowingComponent {
      public SendException(Component param0, Throwable param1) {
         super(param0, param1);
      }
   }

   record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      @Override
      public CompletableFuture<Unit> send(UUID param0, AbuseReport param1) {
         return CompletableFuture.supplyAsync(
            () -> {
               AbuseReportRequest l_var3 = new AbuseReportRequest(
                  1, param0, param1, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo()
               );

               try {
                  this.userApiService.reportAbuse(l_var3);
                  return Unit.INSTANCE;
               } catch (MinecraftClientHttpException l_var4) {
                  Component l_var5 = this.getHttpErrorDescription(l_var4);
                  throw new CompletionException(new AbuseReportSender.SendException(l_var5, l_var4));
               } catch (MinecraftClientException l_var4) {
                  Component l_var5 = this.getErrorDescription(l_var4);
                  throw new CompletionException(new AbuseReportSender.SendException(l_var5, l_var4));
               }
            },
            Util.ioPool()
         );
      }

      @Override
      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(MinecraftClientHttpException param0) {
         return Component.translatable("gui.abuseReport.send.error_message", param0.getMessage());
      }

      private Component getErrorDescription(MinecraftClientException param0) {
         return switch (param0.getType()) {
            case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
            case HTTP_ERROR -> HTTP_ERROR_TEXT;
            case JSON_ERROR -> JSON_ERROR_TEXT;
         };
      }

      @Override
      public AbuseReportLimits reportLimits() {
         return this.userApiService.getAbuseReportLimits();
      }
   }
}
