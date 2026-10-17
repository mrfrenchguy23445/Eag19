package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RestoreTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Backup backup;
   private final long worldId;
   private final RealmsConfigureWorldScreen lastScreen;

   public RestoreTask(Backup param0, long param1, RealmsConfigureWorldScreen param3) {
      this.backup = param0;
      this.worldId = param1;
      this.lastScreen = param3;
   }

   @Override
   public void run() {
      this.setTitle(Component.translatable("mco.backup.restoring"));
      RealmsClient var1 = RealmsClient.create();
      int var2 = 0;

      while (var2 < 25) {
         try {
            if (this.aborted()) {
               return;
            }

            var1.restoreWorld(this.worldId, this.backup.backupId);
            pause(1L);
            if (this.aborted()) {
               return;
            }

            setScreen(this.lastScreen.getNewScreen());
            return;
         } catch (RetryCallException var3) {
            if (this.aborted()) {
               return;
            }

            pause(var3.delaySeconds);
            var2++;
         } catch (RealmsServiceException var3) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't restore backup", var3);
            setScreen(new RealmsGenericErrorScreen(var3, this.lastScreen));
            return;
         } catch (Exception var3) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't restore backup", var3);
            this.error(var3.getLocalizedMessage());
            return;
         }
      }
   }
}
