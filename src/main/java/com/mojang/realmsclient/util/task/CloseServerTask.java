package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class CloseServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RealmsServer serverData;
   private final RealmsConfigureWorldScreen configureScreen;

   public CloseServerTask(RealmsServer param0, RealmsConfigureWorldScreen param1) {
      this.serverData = param0;
      this.configureScreen = param1;
   }

   @Override
   public void run() {
      this.setTitle(Component.translatable("mco.configure.world.closing"));
      RealmsClient var1 = RealmsClient.create();

      for (int var2 = 0; var2 < 25; var2++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean var3 = var1.close(this.serverData.id);
            if (var3) {
               this.configureScreen.stateChanged();
               this.serverData.state = RealmsServer.State.CLOSED;
               setScreen(this.configureScreen);
               break;
            }
         } catch (RetryCallException var3) {
            if (this.aborted()) {
               return;
            }

            pause(var3.delaySeconds);
         } catch (Exception var3) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Failed to close server", var3);
            this.error("Failed to close the server");
         }
      }
   }
}
