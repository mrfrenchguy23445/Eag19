package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class WorldCreationTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String name;
   private final String motd;
   private final long worldId;
   private final Screen lastScreen;

   public WorldCreationTask(long param0, String param2, String param3, Screen param4) {
      this.worldId = param0;
      this.name = param2;
      this.motd = param3;
      this.lastScreen = param4;
   }

   @Override
   public void run() {
      this.setTitle(Component.translatable("mco.create.world.wait"));
      RealmsClient var1 = RealmsClient.create();

      try {
         var1.initializeWorld(this.worldId, this.name, this.motd);
         setScreen(this.lastScreen);
      } catch (RealmsServiceException var2) {
         LOGGER.error("Couldn't create world");
         this.error(var2.toString());
      } catch (Exception var2) {
         LOGGER.error("Could not create world");
         this.error(var2.getLocalizedMessage());
      }
   }
}
