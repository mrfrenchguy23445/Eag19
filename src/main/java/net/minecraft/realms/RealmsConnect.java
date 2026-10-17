package net.minecraft.realms;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsServer;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.slf4j.Logger;

public class RealmsConnect {
   static final Logger LOGGER = LogUtils.getLogger();
   final Screen onlineScreen;
   volatile boolean aborted;
   @Nullable
   Connection connection;

   public RealmsConnect(Screen param0) {
      this.onlineScreen = param0;
   }

   public void connect(final RealmsServer param0, ServerAddress param1) {
      final Minecraft var3 = Minecraft.getInstance();
      var3.setConnectedToRealms(true);
      var3.prepareForMultiplayer();
      var3.getNarrator().sayNow(Component.translatable("mco.connect.success"));
      final String var4 = param1.getHost();
      final int var5 = param1.getPort();
      (new Thread("Realms-connect-task") {
            @Override
            public void run() {
               InetSocketAddress l_var1 = null;

               try {
                  l_var1 = new InetSocketAddress(var4, var5);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection = Connection.connectToServer(l_var1, var3.options.useNativeTransport());
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection
                     .setListener(
                        new ClientHandshakePacketListenerImpl(
                           RealmsConnect.this.connection, var3, param0.toServerData(var4), RealmsConnect.this.onlineScreen, false, null, l2_param0 -> {}
                        )
                     );
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.send(new ClientIntentionPacket(var4, var5, ConnectionProtocol.LOGIN));
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  String l_var2 = var3.getUser().getName();
                  UUID l_var3 = var3.getUser().getProfileId();
                  RealmsConnect.this.connection.send(new ServerboundHelloPacket(l_var2, Optional.ofNullable(l_var3)));
                  var3.updateReportEnvironment(ReportEnvironment.realm(param0));
               } catch (Exception l_var2) {
                  var3.getDownloadedPackSource().clearServerPack();
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.LOGGER.error("Couldn't connect to world", l_var2);
                  String l_var3 = l_var2.toString();
                  if (l_var1 != null) {
                     String l_var4 = l_var1 + ":" + var5;
                     l_var3 = l_var3.replaceAll(l_var4, "");
                  }

                  DisconnectedRealmsScreen l_var4 = new DisconnectedRealmsScreen(
                     RealmsConnect.this.onlineScreen, CommonComponents.CONNECT_FAILED, Component.translatable("disconnect.genericReason", l_var3)
                  );
                  var3.execute(() -> var3.setScreen(l_var4));
               }
            }
         })
         .start();
   }

   public void abort() {
      this.aborted = true;
      if (this.connection != null && this.connection.isConnected()) {
         this.connection.disconnect(Component.translatable("disconnect.genericReason"));
         this.connection.handleDisconnection();
      }
   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }
}
