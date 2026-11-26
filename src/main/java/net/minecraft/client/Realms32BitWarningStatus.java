package net.minecraft.client;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.Realms32bitWarningScreen;
import org.slf4j.Logger;

public class Realms32BitWarningStatus {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private CompletableFuture<Boolean> subscriptionCheck;
   private boolean warningScreenShown;

   public Realms32BitWarningStatus(Minecraft param0) {
      this.minecraft = param0;
   }

   public void showRealms32BitWarningIfNeeded(Screen param0) {
      if (!this.minecraft.is64Bit() && !this.minecraft.options.skipRealms32bitWarning && !this.warningScreenShown && this.checkForRealmsSubscription()) {
         this.minecraft.setScreen(new Realms32bitWarningScreen(param0));
         this.warningScreenShown = true;
      }
   }

   private Boolean checkForRealmsSubscription() {
      if (this.subscriptionCheck == null) {
         this.subscriptionCheck = CompletableFuture.supplyAsync(this::hasRealmsSubscription, Util.backgroundExecutor());
      }

      try {
         return this.subscriptionCheck.getNow(false);
      } catch (CompletionException var1) {
         LOGGER.warn("Failed to retrieve realms subscriptions", var1);
         this.warningScreenShown = true;
         return false;
      }
   }

   private boolean hasRealmsSubscription() {
      /*
       * Port note: body replaced with a constant false for wasm_gc. The original
       * built a RealmsClient and called listWorlds(), which reaches the network
       * stack through java.net.HttpURLConnection:
       *
       *   Class org.teavm.runtime.Fiber is not supported on current target
       *       at java.net.impl.XHRURLConnection.performRequest
       *       at java.net.impl.XHRURLConnection.getHeaderField(TXHRURLConnection.java:191)
       *       at com.mojang.realmsclient.client.Request.getRetryAfterHeader...
       *       at com.mojang.realmsclient.client.RealmsClient.execute...
       *       at net.minecraft.client.Realms32BitWarningStatus.hasRealmsSubscription...
       *       at net.minecraft.client.telemetry.TelemetryLogManager.open...
       *       at net.minecraft.client.Minecraft.<init>(Minecraft.java:565)
       *
       * HttpURLConnection on this target is XHR under the hood, and XHR's
       * performRequest is @Async, so ANY reachable HTTP call fails the link the
       * same way -- the stack's detour through TelemetryLogManager is a merged
       * supplyAsync frame, the real caller is TitleScreen.init calling
       * showRealms32BitWarningIfNeeded on every boot. The question the method
       * asks is whether the account owns an unexpired Realms world; Eaglercraft
       * has no Realms service to ask, so false is not a workaround for the
       * missing HTTP, it is the correct answer, and it also removes the 32-bit
       * warning screen that would have depended on it. The other Realms entry
       * points (RealmsMainScreen and friends) are not reachable from boot, so
       * RealmsClient needs no further cutting here; if a later link reaches
       * them, the same one-line answer applies.
       */
      return false;
   }
}
