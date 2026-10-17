package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class GetServerDetailsTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RealmsServer server;
   private final Screen lastScreen;
   private final RealmsMainScreen mainScreen;
   private final ReentrantLock connectLock;

   public GetServerDetailsTask(RealmsMainScreen param0, Screen param1, RealmsServer param2, ReentrantLock param3) {
      this.lastScreen = param1;
      this.mainScreen = param0;
      this.server = param2;
      this.connectLock = param3;
   }

   @Override
   public void run() {
      this.setTitle(Component.translatable("mco.connect.connecting"));

      RealmsServerAddress var1;
      try {
         var1 = this.fetchServerAddress();
      } catch (CancellationException var2) {
         LOGGER.info("User aborted connecting to realms");
         return;
      } catch (RealmsServiceException var2) {
         switch (var2.realmsErrorCodeOrDefault(-1)) {
            case 6002:
               setScreen(new RealmsTermsScreen(this.lastScreen, this.mainScreen, this.server));
               return;
            case 6006:
               boolean var3 = this.server.ownerUUID.equals(Minecraft.getInstance().getUser().getUuid());
               setScreen(
                  var3
                     ? new RealmsBrokenWorldScreen(this.lastScreen, this.mainScreen, this.server.id, this.server.worldType == RealmsServer.WorldType.MINIGAME)
                     : new RealmsGenericErrorScreen(
                        Component.translatable("mco.brokenworld.nonowner.title"), Component.translatable("mco.brokenworld.nonowner.error"), this.lastScreen
                     )
               );
               return;
            default:
               this.error(var2.toString());
               LOGGER.error("Couldn't connect to world", var2);
               return;
         }
      } catch (TimeoutException var2) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
         return;
      } catch (Exception var2) {
         LOGGER.error("Couldn't connect to world", var2);
         this.error(var2.getLocalizedMessage());
         return;
      }

      boolean var2 = var1.resourcePackUrl != null && var1.resourcePackHash != null;
      Screen var3 = var2 ? this.resourcePackDownloadConfirmationScreen(var1, this::connectScreen) : this.connectScreen(var1);
      setScreen(var3);
   }

   private RealmsServerAddress fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
      RealmsClient var1 = RealmsClient.create();

      for (int var2 = 0; var2 < 40; var2++) {
         if (this.aborted()) {
            throw new CancellationException();
         }

         try {
            return var1.join(this.server.id);
         } catch (RetryCallException var3) {
            pause(var3.delaySeconds);
         }
      }

      throw new TimeoutException();
   }

   public RealmsLongRunningMcoTaskScreen connectScreen(RealmsServerAddress param0) {
      return new RealmsLongRunningMcoTaskScreen(this.lastScreen, new ConnectTask(this.lastScreen, this.server, param0));
   }

   private RealmsLongConfirmationScreen resourcePackDownloadConfirmationScreen(RealmsServerAddress param0, Function<RealmsServerAddress, Screen> param1) {
      BooleanConsumer var3 = l_param2 -> {
         try {
            if (l_param2) {
               this.scheduleResourcePackDownload(param0).thenRun(() -> setScreen(param1.apply(param0))).exceptionally(l2_param1 -> {
                  Minecraft.getInstance().getDownloadedPackSource().clearServerPack();
                  LOGGER.error("Failed to download resource pack from {}", param0, l2_param1);
                  setScreen(new RealmsGenericErrorScreen(Component.literal("Failed to download resource pack!"), this.lastScreen));
                  return null;
               });
               return;
            }

            setScreen(this.lastScreen);
         } finally {
            if (this.connectLock.isHeldByCurrentThread()) {
               this.connectLock.unlock();
            }
         }
      };
      return new RealmsLongConfirmationScreen(
         var3,
         RealmsLongConfirmationScreen.Type.Info,
         Component.translatable("mco.configure.world.resourcepack.question.line1"),
         Component.translatable("mco.configure.world.resourcepack.question.line2"),
         true
      );
   }

   private CompletableFuture<?> scheduleResourcePackDownload(RealmsServerAddress param0) {
      try {
         return Minecraft.getInstance()
            .getDownloadedPackSource()
            .downloadAndSelectResourcePack(new URL(param0.resourcePackUrl), param0.resourcePackHash, false);
      } catch (Exception var2) {
         CompletableFuture<Void> var3 = new CompletableFuture<>();
         var3.completeExceptionally(var2);
         return var3;
      }
   }
}
