package net.minecraft.server.chase;

import com.google.common.base.Charsets;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ChaseClient {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int RECONNECT_INTERVAL_SECONDS = 5;
   private final String serverHost;
   private final int serverPort;
   private final MinecraftServer server;
   private volatile boolean wantsToRun;
   @Nullable
   private Socket socket;
   @Nullable
   private Thread thread;

   public ChaseClient(String param0, int param1, MinecraftServer param2) {
      this.serverHost = param0;
      this.serverPort = param1;
      this.server = param2;
   }

   public void start() {
      if (this.thread != null && this.thread.isAlive()) {
         LOGGER.warn("Remote control client was asked to start, but it is already running. Will ignore.");
      }

      this.wantsToRun = true;
      this.thread = new Thread(this::run, "chase-client");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   public void stop() {
      this.wantsToRun = false;
      IOUtils.closeQuietly(this.socket);
      this.socket = null;
      this.thread = null;
   }

   public void run() {
      String var1 = this.serverHost + ":" + this.serverPort;

      while (this.wantsToRun) {
         try {
            LOGGER.info("Connecting to remote control server {}", var1);
            this.socket = new Socket(this.serverHost, this.serverPort);
            LOGGER.info("Connected to remote control server! Will continuously execute the command broadcasted by that server.");

            try (BufferedReader var2 = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), Charsets.US_ASCII))) {
               while (this.wantsToRun) {
                  String var3 = var2.readLine();
                  if (var3 == null) {
                     LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", var1, 5);
                     break;
                  }

                  this.handleMessage(var3);
               }
            } catch (IOException var2) {
               LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", var1, 5);
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to connect to remote control server {}. Will retry in {}s.", var1, 5);
         }

         if (this.wantsToRun) {
            try {
               Thread.sleep(5000L);
            } catch (InterruptedException var5) {
            }
         }
      }
   }

   private void handleMessage(String param0) {
      try (Scanner var2 = new Scanner(new StringReader(param0))) {
         var2.useLocale(Locale.ROOT);
         String var3 = var2.next();
         if ("t".equals(var3)) {
            this.handleTeleport(var2);
         } else {
            LOGGER.warn("Unknown message type '{}'", var3);
         }
      } catch (NoSuchElementException var2) {
         LOGGER.warn("Could not parse message '{}', ignoring", param0);
      }
   }

   private void handleTeleport(Scanner param0) {
      this.parseTarget(param0)
         .ifPresent(
            l_param0 -> this.executeCommand(
               String.format(
                  Locale.ROOT,
                  "execute in %s run tp @s %.3f %.3f %.3f %.3f %.3f",
                  l_param0.level.location(),
                  l_param0.pos.x,
                  l_param0.pos.y,
                  l_param0.pos.z,
                  l_param0.rot.y,
                  l_param0.rot.x
               )
            )
         );
   }

   private Optional<ChaseClient.TeleportTarget> parseTarget(Scanner param0) {
      ResourceKey<Level> var2 = ChaseCommand.DIMENSION_NAMES.get(param0.next());
      if (var2 == null) {
         return Optional.empty();
      }

      float var3 = param0.nextFloat();
      float var4 = param0.nextFloat();
      float var5 = param0.nextFloat();
      float var6 = param0.nextFloat();
      float var7 = param0.nextFloat();
      return Optional.of(new ChaseClient.TeleportTarget(var2, new Vec3(var3, var4, var5), new Vec2(var7, var6)));
   }

   private void executeCommand(String param0) {
      this.server
         .execute(
            () -> {
               List<ServerPlayer> l_var2 = this.server.getPlayerList().getPlayers();
               if (!l_var2.isEmpty()) {
                  ServerPlayer l_var3 = l_var2.get(0);
                  ServerLevel l_var4 = this.server.overworld();
                  CommandSourceStack l_var5 = new CommandSourceStack(
                     l_var3, Vec3.atLowerCornerOf(l_var4.getSharedSpawnPos()), Vec2.ZERO, l_var4, 4, "", CommonComponents.EMPTY, this.server, l_var3
                  );
                  Commands l_var6 = this.server.getCommands();
                  l_var6.performPrefixedCommand(l_var5, param0);
               }
            }
         );
   }

   record TeleportTarget(ResourceKey<Level> level, Vec3 pos, Vec2 rot) {
   }
}
