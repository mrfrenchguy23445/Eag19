package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.ErrorCallback;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public abstract class LongRunningTask implements ErrorCallback, Runnable {
   protected static final int NUMBER_OF_RETRIES = 25;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected RealmsLongRunningMcoTaskScreen longRunningMcoTaskScreen;

   protected static void pause(long param0) {
      try {
         Thread.sleep(param0 * 1000L);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
         LOGGER.error("", var2);
      }
   }

   public static void setScreen(Screen param0) {
      Minecraft var1 = Minecraft.getInstance();
      var1.execute(() -> var1.setScreen(param0));
   }

   public void setScreen(RealmsLongRunningMcoTaskScreen param0) {
      this.longRunningMcoTaskScreen = param0;
   }

   @Override
   public void error(Component param0) {
      this.longRunningMcoTaskScreen.error(param0);
   }

   public void setTitle(Component param0) {
      this.longRunningMcoTaskScreen.setTitle(param0);
   }

   public boolean aborted() {
      return this.longRunningMcoTaskScreen.aborted();
   }

   public void tick() {
   }

   public void init() {
   }

   public void abortTask() {
   }
}
