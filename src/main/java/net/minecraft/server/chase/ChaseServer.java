package net.minecraft.server.chase;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ChaseServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String serverBindAddress;
   private final int serverPort;
   private final PlayerList playerList;
   private final int broadcastIntervalMs;
   private volatile boolean wantsToRun;
   @Nullable
   private ServerSocket serverSocket;
   private final CopyOnWriteArrayList<Socket> clientSockets = new CopyOnWriteArrayList<>();

   public ChaseServer(String param0, int param1, PlayerList param2, int param3) {
      this.serverBindAddress = param0;
      this.serverPort = param1;
      this.playerList = param2;
      this.broadcastIntervalMs = param3;
   }

   public void start() throws IOException {
      if (this.serverSocket != null && !this.serverSocket.isClosed()) {
         LOGGER.warn("Remote control server was asked to start, but it is already running. Will ignore.");
      } else {
         this.wantsToRun = true;
         this.serverSocket = new ServerSocket(this.serverPort, 50, InetAddress.getByName(this.serverBindAddress));
         Thread var1 = new Thread(this::runAcceptor, "chase-server-acceptor");
         var1.setDaemon(true);
         var1.start();
         Thread var2 = new Thread(this::runSender, "chase-server-sender");
         var2.setDaemon(true);
         var2.start();
      }
   }

   private void runSender() {
      ChaseServer.PlayerPosition var1 = null;

      while (this.wantsToRun) {
         if (!this.clientSockets.isEmpty()) {
            ChaseServer.PlayerPosition var2 = this.getPlayerPosition();
            if (var2 != null && !var2.equals(var1)) {
               var1 = var2;
               byte[] var3 = var2.format().getBytes(StandardCharsets.US_ASCII);

               for (Socket var5 : this.clientSockets) {
                  if (!var5.isClosed()) {
                     // Util.ioPool() is a direct executor on WASM-GC and the Future returned by
                     // submit() was discarded here, so execute() is equivalent.
                     Util.ioPool().execute(() -> {
                        try {
                           OutputStream l_var2 = var5.getOutputStream();
                           l_var2.write(var3);
                           l_var2.flush();
                        } catch (IOException l_var2) {
                           LOGGER.info("Remote control client socket got an IO exception and will be closed", l_var2);
                           IOUtils.closeQuietly(var5);
                        }
                     });
                  }
               }
            }

            List<Socket> var3 = this.clientSockets.stream().filter(Socket::isClosed).collect(Collectors.toList());
            this.clientSockets.removeAll(var3);
         }

         if (this.wantsToRun) {
            try {
               Thread.sleep(this.broadcastIntervalMs);
            } catch (InterruptedException var6) {
            }
         }
      }
   }

   public void stop() {
      this.wantsToRun = false;
      IOUtils.closeQuietly(this.serverSocket);
      this.serverSocket = null;
   }

   private void runAcceptor() {
      try {
         while (this.wantsToRun) {
            if (this.serverSocket != null) {
               LOGGER.info("Remote control server is listening for connections on port {}", this.serverPort);
               Socket var1 = this.serverSocket.accept();
               LOGGER.info("Remote control server received client connection on port {}", var1.getPort());
               this.clientSockets.add(var1);
            }
         }
      } catch (ClosedByInterruptException var1) {
         if (this.wantsToRun) {
            LOGGER.info("Remote control server closed by interrupt");
         }
      } catch (IOException var1) {
         if (this.wantsToRun) {
            LOGGER.error("Remote control server closed because of an IO exception", var1);
         }
      } finally {
         IOUtils.closeQuietly(this.serverSocket);
      }

      LOGGER.info("Remote control server is now stopped");
      this.wantsToRun = false;
   }

   @Nullable
   private ChaseServer.PlayerPosition getPlayerPosition() {
      List<ServerPlayer> var1 = this.playerList.getPlayers();
      if (var1.isEmpty()) {
         return null;
      }

      ServerPlayer var2 = var1.get(0);
      String var3 = ChaseCommand.DIMENSION_NAMES.inverse().get(var2.getLevel().dimension());
      return var3 == null ? null : new ChaseServer.PlayerPosition(var3, var2.getX(), var2.getY(), var2.getZ(), var2.getYRot(), var2.getXRot());
   }

   record PlayerPosition(String dimensionName, double x, double y, double z, float yRot, float xRot) {
      String format() {
         return String.format(Locale.ROOT, "t %s %.2f %.2f %.2f %.2f %.2f\n", this.dimensionName, this.x, this.y, this.z, this.yRot, this.xRot);
      }
   }
}
