package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;

public record ReportEnvironment(String clientVersion, @Nullable ReportEnvironment.Server server) {
   public static ReportEnvironment local() {
      return create(null);
   }

   public static ReportEnvironment thirdParty(String param0) {
      return create(new ReportEnvironment.Server.ThirdParty(param0));
   }

   public static ReportEnvironment realm(RealmsServer param0) {
      return create(new ReportEnvironment.Server.Realm(param0));
   }

   public static ReportEnvironment create(@Nullable ReportEnvironment.Server param0) {
      return new ReportEnvironment(getClientVersion(), param0);
   }

   public ClientInfo clientInfo() {
      return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
   }

   @Nullable
   public ThirdPartyServerInfo thirdPartyServerInfo() {
      return this.server instanceof ReportEnvironment.Server.ThirdParty var1 ? new ThirdPartyServerInfo(var1.ip) : null;
   }

   @Nullable
   public RealmInfo realmInfo() {
      return this.server instanceof ReportEnvironment.Server.Realm var1 ? new RealmInfo(String.valueOf(var1.realmId()), var1.slotId()) : null;
   }

   private static String getClientVersion() {
      StringBuilder var0 = new StringBuilder();
      var0.append("1.19.4");
      if (Minecraft.checkModStatus().shouldReportAsModified()) {
         var0.append(" (modded)");
      }

      return var0.toString();
   }

   public interface Server {
      record Realm(long realmId, int slotId) implements ReportEnvironment.Server {
         public Realm(RealmsServer param0) {
            this(param0.id, param0.activeSlot);
         }
      }

      record ThirdParty(String ip) implements ReportEnvironment.Server {
      }
   }
}
