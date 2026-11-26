package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Crypt;
import net.minecraft.util.HttpUtil;
import org.slf4j.Logger;

public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private final ServerData serverData;
   @Nullable
   private final Screen parent;
   private final Consumer<Component> updateStatus;
   private final Connection connection;
   private GameProfile localGameProfile;
   private final boolean newWorld;
   @Nullable
   private final Duration worldLoadDuration;

   public ClientHandshakePacketListenerImpl(
      Connection param0,
      Minecraft param1,
      @Nullable ServerData param2,
      @Nullable Screen param3,
      boolean param4,
      @Nullable Duration param5,
      Consumer<Component> param6
   ) {
      this.connection = param0;
      this.minecraft = param1;
      this.serverData = param2;
      this.parent = param3;
      this.updateStatus = param6;
      this.newWorld = param4;
      this.worldLoadDuration = param5;
   }

   @Override
   public void handleHello(ClientboundHelloPacket param0) {
      Cipher var2;
      Cipher var3;
      String var4;
      ServerboundKeyPacket var5;
      try {
         SecretKey var6 = Crypt.generateSecretKey();
         PublicKey var7 = param0.getPublicKey();
         var4 = new BigInteger(Crypt.digestData(param0.getServerId(), var7, var6)).toString(16);
         var2 = Crypt.getCipher(2, var6);
         var3 = Crypt.getCipher(1, var6);
         byte[] var8 = param0.getChallenge();
         var5 = new ServerboundKeyPacket(var6, var7, var8);
      } catch (Exception var6) {
         throw new IllegalStateException("Protocol error", var6);
      }

      this.updateStatus.accept(Component.translatable("connect.authorizing"));
      HttpUtil.DOWNLOAD_EXECUTOR.submit(() -> {
         Component l_var5 = this.authenticateServer(var4);
         if (l_var5 != null) {
            if (this.serverData == null || !this.serverData.isLan()) {
               this.connection.disconnect(l_var5);
               return;
            }

            LOGGER.warn(l_var5.getString());
         }

         this.updateStatus.accept(Component.translatable("connect.encrypting"));
         this.connection.send(var5, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(var2, var3)));
      });
   }

   @Nullable
   private Component authenticateServer(String param0) {
      try {
         this.getMinecraftSessionService().joinServer(this.minecraft.getUser().getGameProfile(), this.minecraft.getUser().getAccessToken(), param0);
         return null;
      } catch (AuthenticationUnavailableException var2) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
      } catch (InvalidCredentialsException var2) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
      } catch (InsufficientPrivilegesException var2) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
      } catch (UserBannedException var2) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
      } catch (AuthenticationException var2) {
         return Component.translatable("disconnect.loginFailedInfo", var2.getMessage());
      }
   }

   private MinecraftSessionService getMinecraftSessionService() {
      return this.minecraft.getMinecraftSessionService();
   }

   @Override
   public void handleGameProfile(ClientboundGameProfilePacket param0) {
      this.updateStatus.accept(Component.translatable("connect.joining"));
      this.localGameProfile = param0.getGameProfile();
      this.connection.setProtocol(ConnectionProtocol.PLAY);
      this.connection
         .setListener(
            new ClientPacketListener(
               this.minecraft,
               this.parent,
               this.connection,
               this.serverData,
               this.localGameProfile,
               this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration)
            )
         );
   }

   @Override
   public void onDisconnect(Component param0) {
      if (this.parent != null && this.parent instanceof RealmsScreen) {
         this.minecraft.setScreen(new DisconnectedRealmsScreen(this.parent, CommonComponents.CONNECT_FAILED, param0));
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, param0));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleDisconnect(ClientboundLoginDisconnectPacket param0) {
      this.connection.disconnect(param0.getReason());
   }

   @Override
   public void handleCompression(ClientboundLoginCompressionPacket param0) {
      if (!this.connection.isMemoryConnection()) {
         this.connection.setupCompression(param0.getCompressionThreshold(), false);
      }
   }

   @Override
   public void handleCustomQuery(ClientboundCustomQueryPacket param0) {
      this.updateStatus.accept(Component.translatable("connect.negotiating"));
      this.connection.send(new ServerboundCustomQueryPacket(param0.getTransactionId(), null));
   }
}
