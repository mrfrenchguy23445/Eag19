package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class OpenServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RealmsServer serverData;
   private final Screen returnScreen;
   private final boolean join;
   private final RealmsMainScreen mainScreen;
   private final Minecraft minecraft;

   public OpenServerTask(RealmsServer param0, Screen param1, RealmsMainScreen param2, boolean param3, Minecraft param4) {
      this.serverData = param0;
      this.returnScreen = param1;
      this.join = param3;
      this.mainScreen = param2;
      this.minecraft = param4;
   }

   @Override
   public void run() {
      this.setTitle(Component.translatable("mco.configure.world.opening"));
      RealmsClient var1 = RealmsClient.create();

      for (int var2 = 0; var2 < 25; var2++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean var3 = var1.open(this.serverData.id);
            if (var3) {
               this.minecraft.execute(() -> {
                  if (this.returnScreen instanceof RealmsConfigureWorldScreen) {
                     ((RealmsConfigureWorldScreen)this.returnScreen).stateChanged();
                  }

                  this.serverData.state = RealmsServer.State.OPEN;
                  if (this.join) {
                     this.mainScreen.play(this.serverData, this.returnScreen);
                  } else {
                     this.minecraft.setScreen(this.returnScreen);
                  }
               });
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

            LOGGER.error("Failed to open server", var3);
            this.error("Failed to open the server");
         }
      }
   }
}
