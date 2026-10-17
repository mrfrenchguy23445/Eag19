package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import org.slf4j.Logger;

public class ServerStatusPinger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CANT_CONNECT_MESSAGE = Component.translatable("multiplayer.status.cannot_connect")
      .withStyle(param0 -> param0.withColor(-65536));
   private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public void pingServer(final ServerData param0, final Runnable param1) throws UnknownHostException {
      final String var3 = Connection.normalizeWebSocketURI(param0.ip);
      param0.motd = Component.translatable("multiplayer.status.pinging");
      param0.ping = -1L;
      param0.playerList = Collections.emptyList();
      final Connection var4 = Connection.connectToServer(var3, Minecraft.getInstance().options.useNativeTransport());
      this.connections.add(var4);
      var4.setListener(new ClientStatusPacketListener() {
         private boolean success;

         @Override
         public void handleStatusResponse(ClientboundStatusResponsePacket param0x) {
            if (this.success) {
               var4.disconnect(Component.translatable("multiplayer.status.old"));
            } else {
               this.success = true;
               var4.send(new ServerboundPingRequestPacket(Util.getMillis()));
               ServerStatus var2 = param0x.status();
               param0.motd = var2.description();
               param0.protocol = var2.version().map(ServerStatus.Version::protocol).orElse(param0.protocol);
               param0.version = var2.version().<Component>map(param0y -> Component.literal(param0y.name())).orElse(param0.version);
               param0.players = var2.players().orElse(null);
               if (param0.players != null) {
                  param0.status = formatPlayerCount(param0.players.online(), param0.players.max());
                  List<Component> var3x = Lists.newArrayList();
                  List<GameProfile> var4x = param0.players.sample();

                  for (int var5 = 0; var5 < var4x.size(); var5++) {
                     if (var5 >= 5) {
                        var3x.add(Component.translatable("multiplayer.status.and_more", var4x.size() - var5));
                        break;
                     }

                     var3x.add(Component.literal(var4x.get(var5).getName()));
                  }

                  param0.playerList = var3x;
               } else {
                  param0.status = CommonComponents.EMPTY;
               }

               var2.favicon().ifPresent(param0y -> param0.setIconBytes(param0y.iconBytes()));
               param0.setEnforcesSecureChat(var2.enforcesSecureChat());
            }
         }

         @Override
         public void handlePongResponse(ClientboundPongResponsePacket param0x) {
            param0.ping = Util.getMillis() - param0x.getTime();
            var4.disconnect(Component.translatable("multiplayer.status.finished"));
            param1.run();
         }

         @Override
         public void onDisconnect(Component param0x) {
            if (!this.success) {
               ServerStatusPinger.this.onPingFailed(param0x, param0);
            }
         }

         @Override
         public boolean isAcceptingMessages() {
            return var4.isConnected();
         }
      });
      var4.send(new ClientIntentionPacket(Connection.getHostFromURI(var3), Connection.getPortFromURI(var3), ConnectionProtocol.STATUS));
      var4.send(new ServerboundStatusRequestPacket());
   }

   void onPingFailed(Component param0, ServerData param1) {
      LOGGER.error("Can't ping {}: {}", param1.ip, param0.getString());
      param1.motd = CANT_CONNECT_MESSAGE;
      param1.status = CommonComponents.EMPTY;
      param1.players = null;
      param1.playerList = Collections.emptyList();
      param1.ping = -1L;
   }

   static Component formatPlayerCount(int param0, int param1) {
      return Component.literal(Integer.toString(param0))
         .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
         .append(Integer.toString(param1))
         .withStyle(ChatFormatting.GRAY);
   }

   public void tick() {
      synchronized (this.connections) {
         Iterator<Connection> var2 = this.connections.iterator();

         while (var2.hasNext()) {
            Connection var3 = var2.next();
            if (var3.isConnected()) {
               var3.tick();
            } else {
               var2.remove();
               var3.handleDisconnection();
            }
         }
      }
   }

   public void removeAll() {
      synchronized (this.connections) {
         Iterator<Connection> var2 = this.connections.iterator();

         while (var2.hasNext()) {
            Connection var3 = var2.next();
            if (var3.isConnected()) {
               var2.remove();
               var3.disconnect(Component.translatable("multiplayer.status.cancelled"));
            }
         }
      }
   }
}
