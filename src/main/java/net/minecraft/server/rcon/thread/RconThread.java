package net.minecraft.server.rcon.thread;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.slf4j.Logger;

public class RconThread extends GenericThread {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerSocket socket;
   private final String rconPassword;
   private final List<RconClient> clients = Lists.newArrayList();
   private final ServerInterface serverInterface;

   private RconThread(ServerInterface param0, ServerSocket param1, String param2) {
      super("RCON Listener");
      this.serverInterface = param0;
      this.socket = param1;
      this.rconPassword = param2;
   }

   private void clearClients() {
      this.clients.removeIf(l_param0 -> !l_param0.isRunning());
   }

   @Override
   public void run() {
      try {
         while (this.running) {
            try {
               Socket var1 = this.socket.accept();
               RconClient var2 = new RconClient(this.serverInterface, this.rconPassword, var1);
               var2.start();
               this.clients.add(var2);
               this.clearClients();
            } catch (SocketTimeoutException var1) {
               this.clearClients();
            } catch (IOException var1) {
               if (this.running) {
                  LOGGER.info("IO exception: ", var1);
               }
            }
         }
      } finally {
         this.closeSocket(this.socket);
      }
   }

   @Nullable
   public static RconThread create(ServerInterface param0) {
      DedicatedServerProperties var1 = param0.getProperties();
      String var2 = param0.getServerIp();
      if (var2.isEmpty()) {
         var2 = "0.0.0.0";
      }

      int var3 = var1.rconPort;
      if (0 < var3 && 65535 >= var3) {
         String var4 = var1.rconPassword;
         if (var4.isEmpty()) {
            LOGGER.warn("No rcon password set in server.properties, rcon disabled!");
            return null;
         }

         try {
            ServerSocket var5 = new ServerSocket(var3, 0, InetAddress.getByName(var2));
            var5.setSoTimeout(500);
            RconThread var6 = new RconThread(param0, var5, var4);
            if (!var6.start()) {
               return null;
            }

            LOGGER.info("RCON running on {}:{}", var2, var3);
            return var6;
         } catch (IOException var5) {
            LOGGER.warn("Unable to initialise RCON on {}:{}", var2, var3, var5);
            return null;
         }
      } else {
         LOGGER.warn("Invalid rcon port {} found in server.properties, rcon disabled!", var3);
         return null;
      }
   }

   @Override
   public void stop() {
      this.running = false;
      this.closeSocket(this.socket);
      super.stop();

      for (RconClient var2 : this.clients) {
         if (var2.isRunning()) {
            var2.stop();
         }
      }

      this.clients.clear();
   }

   private void closeSocket(ServerSocket param0) {
      LOGGER.debug("closeSocket: {}", param0);

      try {
         param0.close();
      } catch (IOException var2) {
         LOGGER.warn("Failed to close socket", var2);
      }
   }
}
