package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.v1_8.socket.EaglercraftHandshaker;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.slf4j.Logger;

public class ConnectScreen extends Screen {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogUtils.getLogger();
   private static final long NARRATION_DELAY_MS = 2000L;
   @Nullable
   volatile Connection connection;
   @Nullable
   volatile EaglercraftHandshaker eaglerHandshaker;
   volatile boolean aborted;
   private boolean redirected;
   final Screen parent;
   private Component status = Component.translatable("connect.connecting");
   private long lastNarration = -1L;

   private ConnectScreen(Screen param0) {
      super(GameNarrator.NO_TITLE);
      this.parent = param0;
   }

   public static void startConnecting(Screen param0, Minecraft param1, ServerAddress param2, ServerData param3) {
      ConnectScreen var4 = new ConnectScreen(param0);
      param1.clearLevel();
      param1.prepareForMultiplayer();
      param1.updateReportEnvironment(ReportEnvironment.thirdParty(param3 != null ? param3.ip : param2.getHost()));
      param1.setScreen(var4);
      var4.connect(param1, param2, param3);
   }

   private void connect(final Minecraft param0, final ServerAddress param1, @Nullable final ServerData param2) {
      final String var4 = param2 != null ? param2.ip : param1 == null ? "" : param1.toString();
      LOGGER.info("Connecting to {}", var4);
      Thread var5 = new Thread("Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()) {
         @Override
         public void run() {
            try {
               if (ConnectScreen.this.aborted) {
                  return;
               }

               boolean vanilla = var4.regionMatches(true, 0, "vanilla://", 0, 10);
               String var1 = Connection.normalizeWebSocketURI(vanilla ? var4.substring(10) : var4);

               if (vanilla) {
                  synchronized (ConnectScreen.this) {
                     if (ConnectScreen.this.aborted) {
                        return;
                     }

                     ConnectScreen.this.connection = Connection.connectToServer(var1, param0.options.useNativeTransport());
                  }

                  if (ConnectScreen.this.aborted) {
                     return;
                  }

                  ConnectScreen.this.connection
                     .setListener(
                        new ClientHandshakePacketListenerImpl(
                           ConnectScreen.this.connection, param0, param2, ConnectScreen.this, false, null, ConnectScreen.this::updateStatus
                        )
                     );
                  if (ConnectScreen.this.aborted) {
                     return;
                  }

                  ConnectScreen.this.connection
                     .send(new ClientIntentionPacket(Connection.getHostFromURI(var1), Connection.getPortFromURI(var1), ConnectionProtocol.LOGIN));
                  if (ConnectScreen.this.aborted) {
                     return;
                  }

                  ConnectScreen.this.connection
                     .send(new ServerboundHelloPacket(param0.getUser().getName(), Optional.ofNullable(param0.getUser().getProfileId())));
               } else {
                  IWebSocketClient var1x = Connection.openWebSocket(var1);
                  if (ConnectScreen.this.aborted) {
                     return;
                  }

                  synchronized (ConnectScreen.this) {
                     if (ConnectScreen.this.aborted) {
                        return;
                     }

                     ConnectScreen.this.connection = Connection.connectToServer(var1x);
                     ConnectScreen.this.eaglerHandshaker = new EaglercraftHandshaker(
                        var1x, param0, ConnectScreen.this.parent, param2, ConnectScreen.this.connection
                     );
                  }
               }
            } catch (Exception var2x) {
               if (ConnectScreen.this.aborted) {
                  return;
               }

               Exception var3;
               if (var2x.getCause() instanceof Exception var4x) {
                  var3 = var4x;
               } else {
                  var3 = var2x;
               }

               ConnectScreen.LOGGER.error("Couldn't connect to server", var2x);
               String var4xx = var4 == null ? var3.getMessage() : var3.getMessage().replace(var4, "");
               param0.execute(
                  () -> param0.setScreen(
                     new DisconnectedScreen(
                        ConnectScreen.this.parent, CommonComponents.CONNECT_FAILED, Component.translatable("disconnect.genericReason", var4xx)
                     )
                  )
               );
            }
         }
      };
      var5.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      var5.start();
   }

   private void updateStatus(Component param0) {
      this.status = param0;
   }

   @Override
   public void tick() {
      EaglercraftHandshaker var1 = this.eaglerHandshaker;
      if (var1 != null) {
         var1.tick();
         Component var2 = var1.getFailureReason();
         if (var2 != null) {
            this.eaglerHandshaker = null;
            this.minecraft.setScreen(new DisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, var2));
            return;
         }

         String var3 = var1.getRedirectAddress();
         if (var3 != null) {
            this.eaglerHandshaker = null;
            this.handleRedirect(var3);
            return;
         }

         if (var1.isFinished()) {
            this.eaglerHandshaker = null;
         }
      }

      if (this.eaglerHandshaker == null && this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }

   private void handleRedirect(String param0) {
      if (this.redirected || this.aborted) {
         return;
      }

      this.redirected = true;
      ServerData var1 = new ServerData(param0, param0, false);
      this.connect(this.minecraft, ServerAddress.parseString(param0), var1);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected void init() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> {
         this.aborted = true;
         this.eaglerHandshaker = null;
         if (this.connection != null) {
            this.connection.disconnect(Component.translatable("connect.aborted"));
         }

         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20).build());
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      long var5 = Util.getMillis();
      if (var5 - this.lastNarration > NARRATION_DELAY_MS) {
         this.lastNarration = var5;
         this.minecraft.getNarrator().sayNow(Component.translatable("narrator.joining"));
      }

      drawCenteredString(param0, this.font, this.status, this.width / 2, this.height / 2 - 50, 16777215);
      super.render(param0, param1, param2, param3);
   }
}
