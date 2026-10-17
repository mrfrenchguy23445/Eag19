package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;

public class RealmsNotificationsScreen extends RealmsScreen {
   private static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
   private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
   private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");
   private static final ResourceLocation UNSEEN_NOTIFICATION_ICON_LOCATION = new ResourceLocation("minecraft", "textures/gui/unseen_notification.png");
   @Nullable
   private DataFetcher.Subscription realmsDataSubscription;
   @Nullable
   private RealmsNotificationsScreen.DataFetcherConfiguration currentConfiguration;
   private volatile int numberOfPendingInvites;
   static boolean checkedMcoAvailability;
   private static boolean trialAvailable;
   static boolean validClient;
   private static boolean hasUnreadNews;
   private static boolean hasUnseenNotifications;
   private final RealmsNotificationsScreen.DataFetcherConfiguration showAll = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher param0) {
         DataFetcher.Subscription var2 = param0.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNewsAndInvitesSubscriptions(param0, var2);
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(param0, var2);
         return var2;
      }

      @Override
      public boolean showOldNotifications() {
         return true;
      }
   };
   private final RealmsNotificationsScreen.DataFetcherConfiguration onlyNotifications = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher param0) {
         DataFetcher.Subscription var2 = param0.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(param0, var2);
         return var2;
      }

      @Override
      public boolean showOldNotifications() {
         return false;
      }
   };

   public RealmsNotificationsScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public void init() {
      this.checkIfMcoEnabled();
      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.forceUpdate();
      }
   }

   @Override
   public void added() {
      super.added();
      this.minecraft.realmsDataFetcher().notificationsTask.reset();
   }

   @Nullable
   private RealmsNotificationsScreen.DataFetcherConfiguration getConfiguration() {
      boolean var1 = this.inTitleScreen() && validClient;
      if (!var1) {
         return null;
      } else {
         return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
      }
   }

   @Override
   public void tick() {
      RealmsNotificationsScreen.DataFetcherConfiguration var1 = this.getConfiguration();
      if (!Objects.equals(this.currentConfiguration, var1)) {
         this.currentConfiguration = var1;
         if (this.currentConfiguration != null) {
            this.realmsDataSubscription = this.currentConfiguration.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.realmsDataSubscription = null;
         }
      }

      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.tick();
      }
   }

   private boolean getRealmsNotificationsEnabled() {
      return this.minecraft.options.realmsNotifications().get();
   }

   private boolean inTitleScreen() {
      return this.minecraft.screen instanceof TitleScreen;
   }

   private void checkIfMcoEnabled() {
      // Upstream spawns a background thread that asks api.mojang.com whether Realms ("MCO") is
      // usable, and shows notification icons if it is. That thread cannot be started here:
      // Thread.start routes through the org.teavm.platform.Platform support this target does not
      // provide, and the WASM-GC heap is single-threaded so there is nothing to start it on anyway.
      //
      // The probe is therefore not run at all rather than being run inline. Running it inline would
      // be worse than dropping it: it is a blocking HTTPS request issued from Screen.init(), so a
      // slow or unreachable Mojang endpoint would stall the client with no way to cancel it.
      //
      // checkedMcoAvailability is still set, so this is genuinely "checked, and the answer is no"
      // and not "re-check every frame". validClient stays false, so drawIcons renders nothing --
      // which is the correct outcome regardless, since Eaglercraft has no Realms backend and the
      // probe could never have returned COMPATIBLE. The fields and the render path are kept so the
      // screen still compiles and lays out as upstream.
      checkedMcoAvailability = true;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (validClient) {
         this.drawIcons(param0, param1, param2);
      }

      super.render(param0, param1, param2, param3);
   }

   private void drawIcons(PoseStack param0, int param1, int param2) {
      int var4 = this.numberOfPendingInvites;
      int var5 = 24;
      int var6 = this.height / 4 + 48;
      int var7 = this.width / 2 + 80;
      int var8 = var6 + 48 + 2;
      int var9 = 0;
      if (hasUnseenNotifications) {
         RenderSystem.setShaderTexture(0, UNSEEN_NOTIFICATION_ICON_LOCATION);
         GuiComponent.blit(param0, var7 - var9 + 5, var8 + 3, 0.0F, 0.0F, 10, 10, 10, 10);
         var9 += 14;
      }

      if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
         if (hasUnreadNews) {
            RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
            param0.pushPose();
            param0.scale(0.4F, 0.4F, 0.4F);
            GuiComponent.blit(param0, (int)((var7 + 2 - var9) * 2.5), (int)(var8 * 2.5), 0.0F, 0.0F, 40, 40, 40, 40);
            param0.popPose();
            var9 += 14;
         }

         if (var4 != 0) {
            RenderSystem.setShaderTexture(0, INVITE_ICON_LOCATION);
            GuiComponent.blit(param0, var7 - var9, var8 - 6, 0.0F, 0.0F, 15, 25, 31, 25);
            var9 += 16;
         }

         if (trialAvailable) {
            RenderSystem.setShaderTexture(0, TRIAL_ICON_LOCATION);
            int var10 = 0;
            if ((Util.getMillis() / 800L & 1L) == 1L) {
               var10 = 8;
            }

            GuiComponent.blit(param0, var7 + 4 - var9, var8 + 4, 0.0F, var10, 8, 8, 8, 16);
         }
      }
   }

   void addNewsAndInvitesSubscriptions(RealmsDataFetcher param0, DataFetcher.Subscription param1) {
      param1.subscribe(param0.pendingInvitesTask, l_param0 -> this.numberOfPendingInvites = l_param0);
      param1.subscribe(param0.trialAvailabilityTask, l_param0 -> trialAvailable = l_param0);
      param1.subscribe(param0.newsTask, l_param1 -> {
         param0.newsManager.updateUnreadNews(l_param1);
         hasUnreadNews = param0.newsManager.hasUnreadNews();
      });
   }

   void addNotificationsSubscriptions(RealmsDataFetcher param0, DataFetcher.Subscription param1) {
      param1.subscribe(param0.notificationsTask, l_param0 -> {
         hasUnseenNotifications = false;

         for (RealmsNotification l_var2 : l_param0) {
            if (!l_var2.seen()) {
               hasUnseenNotifications = true;
               break;
            }
         }
      });
   }

   interface DataFetcherConfiguration {
      DataFetcher.Subscription initDataFetcher(RealmsDataFetcher var1);

      boolean showOldNotifications();
   }
}
