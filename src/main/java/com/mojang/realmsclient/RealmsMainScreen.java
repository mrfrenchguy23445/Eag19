package com.mojang.realmsclient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerList;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsNewsManager;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class RealmsMainScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation ON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/on_icon.png");
   private static final ResourceLocation OFF_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/off_icon.png");
   private static final ResourceLocation EXPIRED_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expired_icon.png");
   private static final ResourceLocation EXPIRES_SOON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expires_soon_icon.png");
   private static final ResourceLocation INVITATION_ICONS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invitation_icons.png");
   private static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
   static final ResourceLocation WORLDICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/world_icon.png");
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("realms", "textures/gui/title/realms.png");
   private static final ResourceLocation NEWS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_icon.png");
   private static final ResourceLocation POPUP_LOCATION = new ResourceLocation("realms", "textures/gui/realms/popup.png");
   private static final ResourceLocation DARKEN_LOCATION = new ResourceLocation("realms", "textures/gui/realms/darken.png");
   static final ResourceLocation CROSS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/cross_icon.png");
   private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
   static final ResourceLocation INFO_ICON_LOCATION = new ResourceLocation("minecraft", "textures/gui/info_icon.png");
   static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
   static final Component PENDING_INVITES_TEXT = Component.translatable("mco.invites.pending");
   static final List<Component> TRIAL_MESSAGE_LINES = ImmutableList.of(
      Component.translatable("mco.trial.message.line1"), Component.translatable("mco.trial.message.line2")
   );
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   static final Component SELECT_MINIGAME_PREFIX = Component.translatable("mco.selectServer.minigame").append(CommonComponents.SPACE);
   private static final Component POPUP_TEXT = Component.translatable("mco.selectServer.popup");
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   private static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   private static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   private static final Component NEWS_TOOLTIP = Component.translatable("mco.news");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   static final Component TRIAL_TEXT = CommonComponents.joinLines(TRIAL_MESSAGE_LINES);
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_TOP_ROW_WIDTH = 308;
   private static final int BUTTON_BOTTOM_ROW_WIDTH = 204;
   private static final int FOOTER_HEIGHT = 64;
   private static List<ResourceLocation> teaserImages = ImmutableList.of();
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private RealmsServerList serverList;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean overrideConfigure;
   private static int lastScrollYPosition = -1;
   static volatile boolean hasParentalConsent;
   static volatile boolean checkedParentalConsent;
   static volatile boolean checkedClientCompatability;
   @Nullable
   static Screen realmsGenericErrorScreen;
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private boolean dontSetConnectedToRealms;
   final Screen lastScreen;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   private boolean realmsSelectionListAdded;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   private List<RealmsServer> realmsServers = ImmutableList.of();
   volatile int numberOfPendingInvites;
   int animTick;
   private boolean hasFetchedServers;
   boolean popupOpenedByUser;
   private boolean justClosedPopup;
   private volatile boolean trialsAvailable;
   private volatile boolean createdTrial;
   private volatile boolean showingPopup;
   volatile boolean hasUnreadNews;
   @Nullable
   volatile String newsLink;
   private int carouselIndex;
   private int carouselTick;
   private boolean hasSwitchedCarouselImage;
   private List<KeyCombo> keyCombos;
   long lastClickTime;
   private ReentrantLock connectLock = new ReentrantLock();
   private MultiLineLabel formattedPopup = MultiLineLabel.EMPTY;
   private final List<RealmsNotification> notifications = new ArrayList<>();
   private Button showPopupButton;
   private RealmsMainScreen.PendingInvitesButton pendingInvitesButton;
   private Button newsButton;
   private Button createTrialButton;
   private Button buyARealmButton;
   private Button closeButton;

   public RealmsMainScreen(Screen param0) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = param0;
      this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
   }

   private boolean shouldShowMessageInList() {
      if (hasParentalConsent() && this.hasFetchedServers) {
         if (this.trialsAvailable && !this.createdTrial) {
            return true;
         }

         for (RealmsServer var2 : this.realmsServers) {
            if (var2.ownerUUID.equals(this.minecraft.getUser().getUuid())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean shouldShowPopup() {
      if (!hasParentalConsent() || !this.hasFetchedServers) {
         return false;
      } else {
         return this.popupOpenedByUser ? true : this.realmsServers.isEmpty();
      }
   }

   @Override
   public void init() {
      this.keyCombos = Lists.newArrayList(
         new KeyCombo(new char[]{'3', '2', '1', '4', '5', '6'}, () -> overrideConfigure = !overrideConfigure),
         new KeyCombo(new char[]{'9', '8', '7', '1', '2', '3'}, () -> {
            if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
               this.switchToProd();
            } else {
               this.switchToStage();
            }
         }),
         new KeyCombo(new char[]{'9', '8', '7', '4', '5', '6'}, () -> {
            if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
               this.switchToProd();
            } else {
               this.switchToLocal();
            }
         })
      );
      if (realmsGenericErrorScreen != null) {
         this.minecraft.setScreen(realmsGenericErrorScreen);
      } else {
         this.connectLock = new ReentrantLock();
         if (checkedClientCompatability && !hasParentalConsent()) {
            this.checkParentalConsent();
         }

         this.checkClientCompatability();
         if (!this.dontSetConnectedToRealms) {
            this.minecraft.setConnectedToRealms(false);
         }

         this.showingPopup = false;
         this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
         if (lastScrollYPosition != -1) {
            this.realmSelectionList.setScrollAmount(lastScrollYPosition);
         }

         this.addWidget(this.realmSelectionList);
         this.realmsSelectionListAdded = true;
         this.setInitialFocus(this.realmSelectionList);
         this.addMiddleButtons();
         this.addFooterButtons();
         this.addTopButtons();
         this.updateButtonStates(null);
         this.formattedPopup = MultiLineLabel.create(this.font, POPUP_TEXT, 100);
         RealmsNewsManager var1 = this.minecraft.realmsDataFetcher().newsManager;
         this.hasUnreadNews = var1.hasUnreadNews();
         this.newsLink = var1.newsLink();
         if (this.serverList == null) {
            this.serverList = new RealmsServerList(this.minecraft);
         }

         if (this.dataSubscription != null) {
            this.dataSubscription.forceUpdate();
         }
      }
   }

   private static boolean hasParentalConsent() {
      return checkedParentalConsent && hasParentalConsent;
   }

   public void addTopButtons() {
      this.pendingInvitesButton = this.addRenderableWidget(new RealmsMainScreen.PendingInvitesButton());
      this.newsButton = this.addRenderableWidget(new RealmsMainScreen.NewsButton());
      this.showPopupButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.selectServer.purchase"), l_param0 -> this.popupOpenedByUser = !this.popupOpenedByUser)
            .bounds(this.width - 90, 6, 80, 20)
            .build()
      );
   }

   public void addMiddleButtons() {
      this.createTrialButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.selectServer.trial"), l_param0 -> {
         if (this.trialsAvailable && !this.createdTrial) {
            Util.getPlatform().openUri("https://aka.ms/startjavarealmstrial");
            this.minecraft.setScreen(this.lastScreen);
         }
      }).bounds(this.width / 2 + 52, this.popupY0() + 137 - 20, 98, 20).build());
      this.buyARealmButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.selectServer.buy"), l_param0 -> Util.getPlatform().openUri("https://aka.ms/BuyJavaRealms"))
            .bounds(this.width / 2 + 52, this.popupY0() + 160 - 20, 98, 20)
            .build()
      );
      this.closeButton = this.addRenderableWidget(new RealmsMainScreen.CloseButton());
   }

   public void addFooterButtons() {
      this.playButton = Button.builder(PLAY_TEXT, l_param0 -> this.play(this.getSelectedServer(), this)).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, l_param0 -> this.configureClicked(this.getSelectedServer())).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, l_param0 -> this.onRenew(this.getSelectedServer())).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, l_param0 -> this.leaveClicked(this.getSelectedServer())).width(100).build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, l_param0 -> {
         if (!this.justClosedPopup) {
            this.minecraft.setScreen(this.lastScreen);
         }
      }).width(100).build();
      GridLayout var1 = new GridLayout();
      GridLayout.RowHelper var2 = var1.createRowHelper(1);
      LinearLayout var3 = var2.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL), var2.newCellSettings().paddingBottom(4));
      var3.addChild(this.playButton);
      var3.addChild(this.configureButton);
      var3.addChild(this.renewButton);
      LinearLayout var4 = var2.addChild(new LinearLayout(204, 20, LinearLayout.Orientation.HORIZONTAL), var2.newCellSettings().alignHorizontallyCenter());
      var4.addChild(this.leaveButton);
      var4.addChild(this.backButton);
      var1.visitWidgets(l_param1 -> {
         AbstractWidget var10000 = this.addRenderableWidget(l_param1);
      });
      var1.arrangeElements();
      FrameLayout.centerInRectangle(var1, 0, this.height - 64, this.width, 64);
   }

   void updateButtonStates(@Nullable RealmsServer param0) {
      this.backButton.active = true;
      if (hasParentalConsent() && this.hasFetchedServers) {
         boolean var2 = this.shouldShowPopup() && this.trialsAvailable && !this.createdTrial;
         this.createTrialButton.visible = var2;
         this.createTrialButton.active = var2;
         this.buyARealmButton.visible = this.shouldShowPopup();
         this.closeButton.visible = this.shouldShowPopup() && this.popupOpenedByUser;
         this.newsButton.active = true;
         this.newsButton.visible = this.newsLink != null;
         this.pendingInvitesButton.active = true;
         this.pendingInvitesButton.visible = true;
         this.showPopupButton.active = !this.shouldShowPopup();
         this.playButton.visible = !this.shouldShowPopup();
         this.renewButton.visible = !this.shouldShowPopup();
         this.leaveButton.visible = !this.shouldShowPopup();
         this.configureButton.visible = !this.shouldShowPopup();
         this.backButton.visible = !this.shouldShowPopup();
         this.playButton.active = this.shouldPlayButtonBeActive(param0);
         this.renewButton.active = this.shouldRenewButtonBeActive(param0);
         this.leaveButton.active = this.shouldLeaveButtonBeActive(param0);
         this.configureButton.active = this.shouldConfigureButtonBeActive(param0);
      } else {
         hideWidgets(
            this.playButton,
            this.renewButton,
            this.configureButton,
            this.createTrialButton,
            this.buyARealmButton,
            this.closeButton,
            this.newsButton,
            this.pendingInvitesButton,
            this.showPopupButton,
            this.leaveButton
         );
      }
   }

   private boolean shouldShowPopupButton() {
      return (!this.shouldShowPopup() || this.popupOpenedByUser) && hasParentalConsent() && this.hasFetchedServers;
   }

   boolean shouldPlayButtonBeActive(@Nullable RealmsServer param0) {
      return param0 != null && !param0.expired && param0.state == RealmsServer.State.OPEN;
   }

   private boolean shouldRenewButtonBeActive(@Nullable RealmsServer param0) {
      return param0 != null && param0.expired && this.isSelfOwnedServer(param0);
   }

   private boolean shouldConfigureButtonBeActive(@Nullable RealmsServer param0) {
      return param0 != null && this.isSelfOwnedServer(param0);
   }

   private boolean shouldLeaveButtonBeActive(@Nullable RealmsServer param0) {
      return param0 != null && !this.isSelfOwnedServer(param0);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.pendingInvitesButton != null) {
         this.pendingInvitesButton.tick();
      }

      this.justClosedPopup = false;
      this.animTick++;
      boolean var1 = hasParentalConsent();
      if (this.dataSubscription == null && var1) {
         this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
      } else if (this.dataSubscription != null && !var1) {
         this.dataSubscription = null;
      }

      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }

      if (this.shouldShowPopup()) {
         this.carouselTick++;
      }

      if (this.showPopupButton != null) {
         this.showPopupButton.visible = this.shouldShowPopupButton();
         this.showPopupButton.active = this.showPopupButton.visible;
      }
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher param0) {
      DataFetcher.Subscription var2 = param0.dataFetcher.createSubscription();
      var2.subscribe(param0.serverListUpdateTask, l_param0 -> {
         List<RealmsServer> l_var2 = this.serverList.updateServersList(l_param0);
         boolean l_var3 = false;

         for (RealmsServer l_var5 : l_var2) {
            if (this.isSelfOwnedNonExpiredServer(l_var5)) {
               l_var3 = true;
            }
         }

         this.realmsServers = l_var2;
         this.hasFetchedServers = true;
         this.refreshRealmsSelectionList();
         if (!regionsPinged && l_var3) {
            regionsPinged = true;
            this.pingRegions();
         }
      });
      callRealmsClient(RealmsClient::getNotifications, l_param0 -> {
         this.notifications.clear();
         this.notifications.addAll(l_param0);
         this.refreshRealmsSelectionList();
      });
      var2.subscribe(param0.pendingInvitesTask, l_param0 -> {
         this.numberOfPendingInvites = l_param0;
         if (this.numberOfPendingInvites > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", this.numberOfPendingInvites));
         }
      });
      var2.subscribe(param0.trialAvailabilityTask, l_param0 -> {
         if (!this.createdTrial) {
            if (l_param0 != this.trialsAvailable && this.shouldShowPopup()) {
               this.trialsAvailable = l_param0;
               this.showingPopup = false;
            } else {
               this.trialsAvailable = l_param0;
            }
         }
      });
      var2.subscribe(param0.liveStatsTask, l_param0 -> {
         for (RealmsServerPlayerList l_var3 : l_param0.servers) {
            for (RealmsServer l_var5 : this.realmsServers) {
               if (l_var5.id == l_var3.serverId) {
                  l_var5.updateServerPing(l_var3);
                  break;
               }
            }
         }
      });
      var2.subscribe(param0.newsTask, l_param1 -> {
         param0.newsManager.updateUnreadNews(l_param1);
         this.hasUnreadNews = param0.newsManager.hasUnreadNews();
         this.newsLink = param0.newsManager.newsLink();
         this.updateButtonStates(null);
      });
      return var2;
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> param0, Consumer<T> param1) {
      Minecraft var2 = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return param0.request(RealmsClient.create(var2));
         } catch (RealmsServiceException l_var2) {
            throw new RuntimeException(l_var2);
         }
      }).thenAcceptAsync(param1, var2).exceptionally(l_param0 -> {
         LOGGER.error("Failed to execute call to Realms Service", l_param0);
         return null;
      });
   }

   private void refreshRealmsSelectionList() {
      boolean var1 = !this.hasFetchedServers;
      this.realmSelectionList.clear();
      List<UUID> var2 = new ArrayList<>();

      for (RealmsNotification var4 : this.notifications) {
         this.addEntriesForNotification(this.realmSelectionList, var4);
         if (!var4.seen() && !this.handledSeenNotifications.contains(var4.uuid())) {
            var2.add(var4.uuid());
         }
      }

      if (!var2.isEmpty()) {
         callRealmsClient(l_param1 -> {
            l_param1.notificationsSeen(var2);
            return null;
         }, l_param1 -> this.handledSeenNotifications.addAll(var2));
      }

      if (this.shouldShowMessageInList()) {
         this.realmSelectionList.addEntry(new RealmsMainScreen.TrialEntry());
      }

      RealmsMainScreen.Entry var3 = null;
      RealmsServer var4 = this.getSelectedServer();

      for (RealmsServer var6 : this.realmsServers) {
         RealmsMainScreen.ServerEntry var7 = new RealmsMainScreen.ServerEntry(var6);
         this.realmSelectionList.addEntry(var7);
         if (var4 != null && var4.id == var6.id) {
            var3 = var7;
         }
      }

      if (var1) {
         this.updateButtonStates(null);
      } else {
         this.realmSelectionList.setSelected(var3);
      }
   }

   private void addEntriesForNotification(RealmsMainScreen.RealmSelectionList param0, RealmsNotification param1) {
      if (param1 instanceof RealmsNotification.VisitUrl var3) {
         param0.addEntry(new RealmsMainScreen.NotificationMessageEntry(var3.getMessage(), var3));
         param0.addEntry(new RealmsMainScreen.ButtonEntry(var3.buildOpenLinkButton(this)));
      }
   }

   void refreshFetcher() {
      if (this.dataSubscription != null) {
         this.dataSubscription.reset();
      }
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> l_var1 = Ping.pingAllRegions();
         RealmsClient l_var2 = RealmsClient.create();
         PingResult l_var3 = new PingResult();
         l_var3.pingResults = l_var1;
         l_var3.worldIds = this.getOwnedNonExpiredWorldIds();

         try {
            l_var2.sendPingResults(l_var3);
         } catch (Throwable l_var4) {
            LOGGER.warn("Could not send ping result to Realms: ", l_var4);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredWorldIds() {
      List<Long> var1 = Lists.newArrayList();

      for (RealmsServer var3 : this.realmsServers) {
         if (this.isSelfOwnedNonExpiredServer(var3)) {
            var1.add(var3.id);
         }
      }

      return var1;
   }

   public void setCreatedTrial(boolean param0) {
      this.createdTrial = param0;
   }

   private void onRenew(@Nullable RealmsServer param0) {
      if (param0 != null) {
         String var2 = CommonLinks.extendRealms(param0.remoteSubscriptionId, this.minecraft.getUser().getUuid(), param0.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard(var2);
         Util.getPlatform().openUri(var2);
      }
   }

   private void checkClientCompatability() {
      if (!checkedClientCompatability) {
         checkedClientCompatability = true;
         (new Thread("MCO Compatability Checker #1") {
               @Override
               public void run() {
                  RealmsClient l_var1 = RealmsClient.create();

                  try {
                     RealmsClient.CompatibleVersionResponse l_var2 = l_var1.clientCompatible();
                     if (l_var2 != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                        RealmsMainScreen.realmsGenericErrorScreen = new RealmsClientOutdatedScreen(RealmsMainScreen.this.lastScreen);
                        RealmsMainScreen.this.minecraft.execute(() -> RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen));
                        return;
                     }

                     RealmsMainScreen.this.checkParentalConsent();
                  } catch (RealmsServiceException l_var2) {
                     RealmsMainScreen.checkedClientCompatability = false;
                     RealmsMainScreen.LOGGER.error("Couldn't connect to realms", l_var2);
                     if (l_var2.httpResultCode == 401) {
                        RealmsMainScreen.realmsGenericErrorScreen = new RealmsGenericErrorScreen(
                           Component.translatable("mco.error.invalid.session.title"),
                           Component.translatable("mco.error.invalid.session.message"),
                           RealmsMainScreen.this.lastScreen
                        );
                        RealmsMainScreen.this.minecraft.execute(() -> RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen));
                     } else {
                        RealmsMainScreen.this.minecraft
                           .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(l_var2, RealmsMainScreen.this.lastScreen)));
                     }
                  }
               }
            })
            .start();
      }
   }

   void checkParentalConsent() {
      (new Thread("MCO Compatability Checker #1") {
            @Override
            public void run() {
               RealmsClient l_var1 = RealmsClient.create();

               try {
                  Boolean l_var2 = l_var1.mcoEnabled();
                  if (l_var2) {
                     RealmsMainScreen.LOGGER.info("Realms is available for this user");
                     RealmsMainScreen.hasParentalConsent = true;
                  } else {
                     RealmsMainScreen.LOGGER.info("Realms is not available for this user");
                     RealmsMainScreen.hasParentalConsent = false;
                     RealmsMainScreen.this.minecraft
                        .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsParentalConsentScreen(RealmsMainScreen.this.lastScreen)));
                  }

                  RealmsMainScreen.checkedParentalConsent = true;
               } catch (RealmsServiceException l_var2) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to realms", l_var2);
                  RealmsMainScreen.this.minecraft
                     .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(l_var2, RealmsMainScreen.this.lastScreen)));
               }
            }
         })
         .start();
   }

   private void switchToStage() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.STAGE) {
         (new Thread("MCO Stage Availability Checker #1") {
            @Override
            public void run() {
               RealmsClient l_var1 = RealmsClient.create();

               try {
                  Boolean l_var2 = l_var1.stageAvailable();
                  if (l_var2) {
                     RealmsClient.switchToStage();
                     RealmsMainScreen.LOGGER.info("Switched to stage");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException l_var2) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", l_var2.toString());
               }
            }
         }).start();
      }
   }

   private void switchToLocal() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.LOCAL) {
         (new Thread("MCO Local Availability Checker #1") {
            @Override
            public void run() {
               RealmsClient l_var1 = RealmsClient.create();

               try {
                  Boolean l_var2 = l_var1.stageAvailable();
                  if (l_var2) {
                     RealmsClient.switchToLocal();
                     RealmsMainScreen.LOGGER.info("Switched to local");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException l_var2) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", l_var2.toString());
               }
            }
         }).start();
      }
   }

   private void switchToProd() {
      RealmsClient.switchToProd();
      this.refreshFetcher();
   }

   private void configureClicked(@Nullable RealmsServer param0) {
      if (param0 != null && (this.minecraft.getUser().getUuid().equals(param0.ownerUUID) || overrideConfigure)) {
         this.saveListScrollPosition();
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, param0.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer param0) {
      if (param0 != null && !this.minecraft.getUser().getUuid().equals(param0.ownerUUID)) {
         this.saveListScrollPosition();
         Component var2 = Component.translatable("mco.configure.world.leave.question.line1");
         Component var3 = Component.translatable("mco.configure.world.leave.question.line2");
         this.minecraft
            .setScreen(
               new RealmsLongConfirmationScreen(l_param1 -> this.leaveServer(l_param1, param0), RealmsLongConfirmationScreen.Type.Info, var2, var3, true)
            );
      }
   }

   private void saveListScrollPosition() {
      lastScrollYPosition = (int)this.realmSelectionList.getScrollAmount();
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      if (this.realmSelectionList == null) {
         return null;
      }

      RealmsMainScreen.Entry var1 = this.realmSelectionList.getSelected();
      return var1 != null ? var1.getServer() : null;
   }

   private void leaveServer(boolean param0, final RealmsServer param1) {
      if (param0) {
         (new Thread("Realms-leave-server") {
               @Override
               public void run() {
                  try {
                     RealmsClient l_var1 = RealmsClient.create();
                     l_var1.uninviteMyselfFrom(param1.id);
                     RealmsMainScreen.this.minecraft.execute(() -> RealmsMainScreen.this.removeServer(param1));
                  } catch (RealmsServiceException l_var1) {
                     RealmsMainScreen.LOGGER.error("Couldn't configure world");
                     RealmsMainScreen.this.minecraft
                        .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(l_var1, RealmsMainScreen.this)));
                  }
               }
            })
            .start();
      }

      this.minecraft.setScreen(this);
   }

   void removeServer(RealmsServer param0) {
      this.realmsServers = this.serverList.removeItem(param0);
      this.realmSelectionList.children().removeIf(l_param1 -> {
         RealmsServer l_var2 = l_param1.getServer();
         return l_var2 != null && l_var2.id == param0.id;
      });
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      this.updateButtonStates(null);
      this.playButton.active = false;
   }

   void dismissNotification(UUID param0) {
      callRealmsClient(l_param1 -> {
         l_param1.notificationsDismiss(List.of(param0));
         return null;
      }, l_param1 -> {
         this.notifications.removeIf(l2_param1 -> l2_param1.dismissable() && param0.equals(l2_param1.uuid()));
         this.refreshRealmsSelectionList();
      });
   }

   public void resetScreen() {
      if (this.realmSelectionList != null) {
         this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256) {
         this.keyCombos.forEach(KeyCombo::reset);
         this.onClosePopup();
         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   void onClosePopup() {
      if (this.shouldShowPopup() && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      this.keyCombos.forEach(l_param1 -> l_param1.keyPressed(param0));
      return true;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.realmSelectionList.render(param0, param1, param2, param3);
      this.drawRealmsLogo(param0, this.width / 2 - 50, 7);
      if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
         this.renderStage(param0);
      }

      if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
         this.renderLocal(param0);
      }

      if (this.shouldShowPopup()) {
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 100.0F);
         this.drawPopup(param0);
         param0.popPose();
      } else {
         if (this.showingPopup) {
            this.updateButtonStates(null);
            if (!this.realmsSelectionListAdded) {
               this.addWidget(this.realmSelectionList);
               this.realmsSelectionListAdded = true;
            }

            this.playButton.active = this.shouldPlayButtonBeActive(this.getSelectedServer());
         }

         this.showingPopup = false;
      }

      super.render(param0, param1, param2, param3);
      if (this.trialsAvailable && !this.createdTrial && this.shouldShowPopup()) {
         RenderSystem.setShaderTexture(0, TRIAL_ICON_LOCATION);
         int var5 = 8;
         int var6 = 8;
         int var7 = 0;
         if ((Util.getMillis() / 800L & 1L) == 1L) {
            var7 = 8;
         }

         GuiComponent.blit(
            param0,
            this.createTrialButton.getX() + this.createTrialButton.getWidth() - 8 - 4,
            this.createTrialButton.getY() + this.createTrialButton.getHeight() / 2 - 4,
            0.0F,
            var7,
            8,
            8,
            8,
            16
         );
      }
   }

   private void drawRealmsLogo(PoseStack param0, int param1, int param2) {
      RenderSystem.setShaderTexture(0, LOGO_LOCATION);
      param0.pushPose();
      param0.scale(0.5F, 0.5F, 0.5F);
      GuiComponent.blit(param0, param1 * 2, param2 * 2 - 5, 0.0F, 0.0F, 200, 50, 200, 50);
      param0.popPose();
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.isOutsidePopup(param0, param2) && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
         this.justClosedPopup = true;
         return true;
      } else {
         return super.mouseClicked(param0, param2, param4);
      }
   }

   private boolean isOutsidePopup(double param0, double param2) {
      int var5 = this.popupX0();
      int var6 = this.popupY0();
      return param0 < var5 - 5 || param0 > var5 + 315 || param2 < var6 - 5 || param2 > var6 + 171;
   }

   private void drawPopup(PoseStack param0) {
      int var2 = this.popupX0();
      int var3 = this.popupY0();
      if (!this.showingPopup) {
         this.carouselIndex = 0;
         this.carouselTick = 0;
         this.hasSwitchedCarouselImage = true;
         this.updateButtonStates(null);
         if (this.realmsSelectionListAdded) {
            this.removeWidget(this.realmSelectionList);
            this.realmsSelectionListAdded = false;
         }

         this.minecraft.getNarrator().sayNow(POPUP_TEXT);
      }

      if (this.hasFetchedServers) {
         this.showingPopup = true;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);
      RenderSystem.enableBlend();
      RenderSystem.setShaderTexture(0, DARKEN_LOCATION);
      int var4 = 0;
      int var5 = 32;
      GuiComponent.blit(param0, 0, 32, 0.0F, 0.0F, this.width, this.height - 40 - 32, 310, 166);
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, POPUP_LOCATION);
      GuiComponent.blit(param0, var2, var3, 0.0F, 0.0F, 310, 166, 310, 166);
      if (!teaserImages.isEmpty()) {
         RenderSystem.setShaderTexture(0, teaserImages.get(this.carouselIndex));
         GuiComponent.blit(param0, var2 + 7, var3 + 7, 0.0F, 0.0F, 195, 152, 195, 152);
         if (this.carouselTick % 95 < 5) {
            if (!this.hasSwitchedCarouselImage) {
               this.carouselIndex = (this.carouselIndex + 1) % teaserImages.size();
               this.hasSwitchedCarouselImage = true;
            }
         } else {
            this.hasSwitchedCarouselImage = false;
         }
      }

      this.formattedPopup.renderLeftAlignedNoShadow(param0, this.width / 2 + 52, var3 + 7, 10, 16777215);
   }

   int popupX0() {
      return (this.width - 310) / 2;
   }

   int popupY0() {
      return this.height / 2 - 80;
   }

   void drawInvitationPendingIcon(PoseStack param0, int param1, int param2, int param3, int param4, boolean param5, boolean param6) {
      int var8 = this.numberOfPendingInvites;
      boolean var9 = this.inPendingInvitationArea(param1, param2);
      boolean var10 = param6 && param5;
      if (var10) {
         float var11 = 0.25F + (1.0F + Mth.sin(this.animTick * 0.5F)) * 0.25F;
         int var12 = 0xFF000000 | (int)(var11 * 64.0F) << 16 | (int)(var11 * 64.0F) << 8 | (int)(var11 * 64.0F) << 0;
         int var13 = param3 - 2;
         int var14 = param3 + 16;
         int var15 = param4 + 1;
         int var16 = param4 + 16;
         fillGradient(param0, var13, var15, var14, var16, var12, var12);
         var12 = 0xFF000000 | (int)(var11 * 255.0F) << 16 | (int)(var11 * 255.0F) << 8 | (int)(var11 * 255.0F) << 0;
         fillGradient(param0, var13, param4, var14, param4 + 1, var12, var12);
         fillGradient(param0, var13 - 1, param4, var13, var16 + 1, var12, var12);
         fillGradient(param0, var14, param4, var14 + 1, var16, var12, var12);
         fillGradient(param0, var13, var16, var14 + 1, var16 + 1, var12, var12);
      }

      RenderSystem.setShaderTexture(0, INVITE_ICON_LOCATION);
      boolean var11 = param6 && param5;
      float var12 = var11 ? 16.0F : 0.0F;
      GuiComponent.blit(param0, param3, param4 - 6, var12, 0.0F, 15, 25, 31, 25);
      boolean var13 = param6 && var8 != 0;
      if (var13) {
         int var14 = (Math.min(var8, 6) - 1) * 8;
         int var15 = (int)(Math.max(0.0F, Math.max(Mth.sin((10 + this.animTick) * 0.57F), Mth.cos(this.animTick * 0.35F))) * -6.0F);
         RenderSystem.setShaderTexture(0, INVITATION_ICONS_LOCATION);
         float var16 = var9 ? 8.0F : 0.0F;
         GuiComponent.blit(param0, param3 + 4, param4 + 4 + var15, var14, var16, 8, 8, 48, 16);
      }

      int var14 = param1 + 12;
      int var15 = param2;
      boolean var16 = param6 && var9;
      if (var16) {
         Component var17 = var8 == 0 ? NO_PENDING_INVITES_TEXT : PENDING_INVITES_TEXT;
         int var18 = this.font.width(var17);
         fillGradient(param0, var14 - 3, var15 - 3, var14 + var18 + 3, var15 + 8 + 3, -1073741824, -1073741824);
         this.font.drawShadow(param0, var17, var14, var15, -1);
      }
   }

   private boolean inPendingInvitationArea(double param0, double param2) {
      int var5 = this.width / 2 + 50;
      int var6 = this.width / 2 + 66;
      int var7 = 11;
      int var8 = 23;
      if (this.numberOfPendingInvites != 0) {
         var5 -= 3;
         var6 += 3;
         var7 -= 5;
         var8 += 5;
      }

      return var5 <= param0 && param0 <= var6 && var7 <= param2 && param2 <= var8;
   }

   public void play(@Nullable RealmsServer param0, Screen param1) {
      if (param0 != null) {
         try {
            if (!this.connectLock.tryLock(1L, TimeUnit.SECONDS)) {
               return;
            }

            if (this.connectLock.getHoldCount() > 1) {
               return;
            }
         } catch (InterruptedException var3) {
            return;
         }

         this.dontSetConnectedToRealms = true;
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(param1, new GetServerDetailsTask(this, param1, param0, this.connectLock)));
      }
   }

   boolean isSelfOwnedServer(RealmsServer param0) {
      return param0.ownerUUID != null && param0.ownerUUID.equals(this.minecraft.getUser().getUuid());
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer param0) {
      return this.isSelfOwnedServer(param0) && !param0.expired;
   }

   void drawExpired(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, EXPIRED_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1
         && param3 <= param1 + 9
         && param4 >= param2
         && param4 <= param2 + 27
         && param4 < this.height - 40
         && param4 > 32
         && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_EXPIRED_TOOLTIP);
      }
   }

   void drawExpiring(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      RenderSystem.setShaderTexture(0, EXPIRES_SOON_ICON_LOCATION);
      if (this.animTick % 20 < 10) {
         GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         GuiComponent.blit(param0, param1, param2, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (param3 >= param1
         && param3 <= param1 + 9
         && param4 >= param2
         && param4 <= param2 + 27
         && param4 < this.height - 40
         && param4 > 32
         && !this.shouldShowPopup()) {
         if (param5 <= 0) {
            this.setTooltipForNextRenderPass(SERVER_EXPIRES_SOON_TOOLTIP);
         } else if (param5 == 1) {
            this.setTooltipForNextRenderPass(SERVER_EXPIRES_IN_DAY_TOOLTIP);
         } else {
            this.setTooltipForNextRenderPass(Component.translatable("mco.selectServer.expires.days", param5));
         }
      }
   }

   void drawOpen(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, ON_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1
         && param3 <= param1 + 9
         && param4 >= param2
         && param4 <= param2 + 27
         && param4 < this.height - 40
         && param4 > 32
         && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_OPEN_TOOLTIP);
      }
   }

   void drawClose(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, OFF_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1
         && param3 <= param1 + 9
         && param4 >= param2
         && param4 <= param2 + 27
         && param4 < this.height - 40
         && param4 > 32
         && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_CLOSED_TOOLTIP);
      }
   }

   void renderNews(PoseStack param0, int param1, int param2, boolean param3, int param4, int param5, boolean param6, boolean param7) {
      boolean var9 = false;
      if (param1 >= param4 && param1 <= param4 + 20 && param2 >= param5 && param2 <= param5 + 20) {
         var9 = true;
      }

      RenderSystem.setShaderTexture(0, NEWS_LOCATION);
      if (!param7) {
         RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
      }

      boolean var10 = param7 && param6;
      float var11 = var10 ? 20.0F : 0.0F;
      GuiComponent.blit(param0, param4, param5, var11, 0.0F, 20, 20, 40, 20);
      if (var9 && param7) {
         this.setTooltipForNextRenderPass(NEWS_TOOLTIP);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (param3 && param7) {
         int var12 = var9 ? 0 : (int)(Math.max(0.0F, Math.max(Mth.sin((10 + this.animTick) * 0.57F), Mth.cos(this.animTick * 0.35F))) * -6.0F);
         RenderSystem.setShaderTexture(0, INVITATION_ICONS_LOCATION);
         GuiComponent.blit(param0, param4 + 10, param5 + 2 + var12, 40.0F, 0.0F, 8, 8, 48, 16);
      }
   }

   private void renderLocal(PoseStack param0) {
      String var2 = "LOCAL!";
      param0.pushPose();
      param0.translate(this.width / 2 - 25, 20.0F, 0.0F);
      param0.mulPose(Axis.ZP.rotationDegrees(-20.0F));
      param0.scale(1.5F, 1.5F, 1.5F);
      this.font.draw(param0, "LOCAL!", 0.0F, 0.0F, 8388479);
      param0.popPose();
   }

   private void renderStage(PoseStack param0) {
      String var2 = "STAGE!";
      param0.pushPose();
      param0.translate(this.width / 2 - 25, 20.0F, 0.0F);
      param0.mulPose(Axis.ZP.rotationDegrees(-20.0F));
      param0.scale(1.5F, 1.5F, 1.5F);
      this.font.draw(param0, "STAGE!", 0.0F, 0.0F, -256);
      param0.popPose();
   }

   public RealmsMainScreen newScreen() {
      RealmsMainScreen var1 = new RealmsMainScreen(this.lastScreen);
      var1.init(this.minecraft, this.width, this.height);
      return var1;
   }

   public static void updateTeaserImages(ResourceManager param0) {
      Collection<ResourceLocation> var1 = param0.listResources("textures/gui/images", l_param0 -> l_param0.getPath().endsWith(".png")).keySet();
      teaserImages = var1.stream().filter(l_param0 -> l_param0.getNamespace().equals("realms")).toList();
   }

   private void pendingButtonPress(Button param0) {
      this.minecraft.setScreen(new RealmsPendingInvitesScreen(this.lastScreen));
   }

   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;
      private final int xPos = RealmsMainScreen.this.width / 2 - 75;

      public ButtonEntry(Button param1) {
         this.button = param1;
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         return this.button.isMouseOver(param0, param2) ? this.button.mouseClicked(param0, param2, param4) : false;
      }

      @Override
      public boolean keyPressed(int param0, int param1, int param2) {
         return this.button.keyPressed(param0, param1, param2) ? true : super.keyPressed(param0, param1, param2);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.button.setPosition(this.xPos, param2 + 4);
         this.button.render(param0, param6, param7, param9);
      }

      @Override
      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   class CloseButton extends RealmsMainScreen.CrossButton {
      public CloseButton() {
         super(
            RealmsMainScreen.this.popupX0() + 4,
            RealmsMainScreen.this.popupY0() + 4,
            l_param1 -> RealmsMainScreen.this.onClosePopup(),
            Component.translatable("mco.selectServer.close")
         );
      }
   }

   static class CrossButton extends Button {
      protected CrossButton(Button.OnPress param0, Component param1) {
         this(0, 0, param0, param1);
      }

      protected CrossButton(int param0, int param1, Button.OnPress param2, Component param3) {
         super(param0, param1, 14, 14, param3, param2, DEFAULT_NARRATION);
         this.setTooltip(Tooltip.create(param3));
      }

      @Override
      public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
         RenderSystem.setShaderTexture(0, RealmsMainScreen.CROSS_ICON_LOCATION);
         float var5 = this.isHoveredOrFocused() ? 14.0F : 0.0F;
         blit(param0, this.getX(), this.getY(), 0.0F, var5, 14, 14, 14, 28);
      }
   }

   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      @Nullable
      public RealmsServer getServer() {
         return null;
      }
   }

   class NewsButton extends Button {
      public NewsButton() {
         super(RealmsMainScreen.this.width - 115, 6, 20, 20, Component.translatable("mco.news"), l_param1 -> {
            if (RealmsMainScreen.this.newsLink != null) {
               ConfirmLinkScreen.confirmLinkNow(RealmsMainScreen.this.newsLink, RealmsMainScreen.this, true);
               if (RealmsMainScreen.this.hasUnreadNews) {
                  RealmsPersistence.RealmsPersistenceData l_var2 = RealmsPersistence.readFile();
                  l_var2.hasUnreadNews = false;
                  RealmsMainScreen.this.hasUnreadNews = false;
                  RealmsPersistence.writeFile(l_var2);
               }
            }
         }, DEFAULT_NARRATION);
      }

      @Override
      public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
         RealmsMainScreen.this.renderNews(
            param0, param1, param2, RealmsMainScreen.this.hasUnreadNews, this.getX(), this.getY(), this.isHoveredOrFocused(), this.active
         );
      }
   }

   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int ITEM_HEIGHT = 36;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(Component param1, RealmsNotification param2) {
         this.text = param1;
         this.gridLayout = new GridLayout();
         int var4 = 7;
         this.gridLayout.addChild(new ImageWidget(20, 20, RealmsMainScreen.INFO_ICON_LOCATION), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(param1, RealmsMainScreen.this.font).setCentered(true).setMaxRows(3),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (param2.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     l_param1 -> RealmsMainScreen.this.dismissNotification(param2.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(int param0, int param1, int param2) {
         return this.dismissButton != null && this.dismissButton.keyPressed(param0, param1, param2) ? true : super.keyPressed(param0, param1, param2);
      }

      private void updateEntryWidth(int param0) {
         if (this.lastEntryWidth != param0) {
            this.refreshLayout(param0);
            this.lastEntryWidth = param0;
         }
      }

      private void refreshLayout(int param0) {
         int var2 = param0 - 80;
         this.textFrame.setMinWidth(var2);
         this.textWidget.setMaxWidth(var2);
         this.gridLayout.arrangeElements();
      }

      @Override
      public void renderBack(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         super.renderBack(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9);
         GuiComponent.renderOutline(param0, param3 - 2, param2 - 2, param4, 70, -12303292);
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.gridLayout.setPosition(param3, param2);
         this.updateEntryWidth(param4 - 4);
         this.children.forEach(l_param4 -> l_param4.render(param0, param6, param7, param9));
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         return this.dismissButton != null && this.dismissButton.mouseClicked(param0, param2, param4) ? true : super.mouseClicked(param0, param2, param4);
      }

      @Override
      public Component getNarration() {
         return this.text;
      }
   }

   class PendingInvitesButton extends Button {
      public PendingInvitesButton() {
         super(RealmsMainScreen.this.width / 2 + 50, 6, 22, 22, CommonComponents.EMPTY, RealmsMainScreen.this::pendingButtonPress, DEFAULT_NARRATION);
      }

      public void tick() {
         this.setMessage(RealmsMainScreen.this.numberOfPendingInvites == 0 ? RealmsMainScreen.NO_PENDING_INVITES_TEXT : RealmsMainScreen.PENDING_INVITES_TEXT);
      }

      @Override
      public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
         RealmsMainScreen.this.drawInvitationPendingIcon(param0, param1, param2, this.getX(), this.getY(), this.isHoveredOrFocused(), this.active);
      }
   }

   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 32, RealmsMainScreen.this.height - 64, 36);
      }

      @Override
      public boolean keyPressed(int param0, int param1, int param2) {
         if (param0 == 257 || param0 == 32 || param0 == 335) {
            RealmsMainScreen.Entry var4 = this.getSelected();
            if (var4 == null) {
               return super.keyPressed(param0, param1, param2);
            }

            var4.keyPressed(param0, param1, param2);
         }

         return super.keyPressed(param0, param1, param2);
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         if (param4 == 0 && param0 < this.getScrollbarPosition() && param2 >= this.y0 && param2 <= this.y1) {
            int var6 = RealmsMainScreen.this.realmSelectionList.getRowLeft();
            int var7 = this.getScrollbarPosition();
            int var8 = (int)Math.floor(param2 - this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
            int var9 = var8 / this.itemHeight;
            if (param0 >= var6 && param0 <= var7 && var9 >= 0 && var8 >= 0 && var9 < this.getItemCount()) {
               this.itemClicked(var8, var9, param0, param2, this.width, param4);
               this.selectItem(var9);
            }

            return true;
         } else {
            return super.mouseClicked(param0, param2, param4);
         }
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry param0) {
         super.setSelected(param0);
         if (param0 != null) {
            RealmsMainScreen.this.updateButtonStates(param0.getServer());
         } else {
            RealmsMainScreen.this.updateButtonStates(null);
         }
      }

      @Override
      public void itemClicked(int param0, int param1, double param2, double param4, int param6, int param7) {
         RealmsMainScreen.Entry var9 = this.getEntry(param1);
         if (!var9.mouseClicked(param2, param4, param7)) {
            if (var9 instanceof RealmsMainScreen.TrialEntry) {
               RealmsMainScreen.this.popupOpenedByUser = true;
            } else {
               RealmsServer var10 = var9.getServer();
               if (var10 != null) {
                  if (var10.state == RealmsServer.State.UNINITIALIZED) {
                     Minecraft.getInstance().setScreen(new RealmsCreateRealmScreen(var10, RealmsMainScreen.this));
                  } else {
                     if (RealmsMainScreen.this.shouldPlayButtonBeActive(var10)) {
                        if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isSelectedItem(param1)) {
                           RealmsMainScreen.this.play(var10, RealmsMainScreen.this);
                        }

                        RealmsMainScreen.this.lastClickTime = Util.getMillis();
                     }
                  }
               }
            }
         }
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   interface RealmsCall<T> {
      T request(RealmsClient var1) throws RealmsServiceException;
   }

   class ServerEntry extends RealmsMainScreen.Entry {
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;

      public ServerEntry(RealmsServer param1) {
         this.serverData = param1;
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderMcoServerItem(this.serverData, param0, param3, param2, param6, param7);
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(this.serverData, RealmsMainScreen.this));
         }

         return true;
      }

      private void renderMcoServerItem(RealmsServer param0, PoseStack param1, int param2, int param3, int param4, int param5) {
         this.renderLegacy(param0, param1, param2 + 36, param3, param4, param5);
      }

      private void renderLegacy(RealmsServer param0, PoseStack param1, int param2, int param3, int param4, int param5) {
         if (param0.state == RealmsServer.State.UNINITIALIZED) {
            RenderSystem.setShaderTexture(0, RealmsMainScreen.WORLDICON_LOCATION);
            GuiComponent.blit(param1, param2 + 10, param3 + 6, 0.0F, 0.0F, 40, 20, 40, 20);
            float var7 = 0.5F + (1.0F + Mth.sin(RealmsMainScreen.this.animTick * 0.25F)) * 0.25F;
            int var8 = 0xFF000000 | (int)(127.0F * var7) << 16 | (int)(255.0F * var7) << 8 | (int)(127.0F * var7);
            GuiComponent.drawCenteredString(
               param1, RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, param2 + 10 + 40 + 75, param3 + 12, var8
            );
         } else {
            int var7 = 225;
            int var8 = 2;
            this.renderStatusLights(param0, param1, param2, param3, param4, param5, 225, 2);
            if (!"0".equals(param0.serverPing.nrOfPlayers)) {
               String var9 = ChatFormatting.GRAY + param0.serverPing.nrOfPlayers;
               RealmsMainScreen.this.font.draw(param1, var9, param2 + 207 - RealmsMainScreen.this.font.width(var9), param3 + 3, 8421504);
               if (param4 >= param2 + 207 - RealmsMainScreen.this.font.width(var9)
                  && param4 <= param2 + 207
                  && param5 >= param3 + 1
                  && param5 <= param3 + 10
                  && param5 < RealmsMainScreen.this.height - 40
                  && param5 > 32
                  && !RealmsMainScreen.this.shouldShowPopup()) {
                  RealmsMainScreen.this.setTooltipForNextRenderPass(Component.literal(param0.serverPing.playerList));
               }
            }

            if (RealmsMainScreen.this.isSelfOwnedServer(param0) && param0.expired) {
               Component var9 = param0.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
               int var10 = param3 + 11 + 5;
               RealmsMainScreen.this.font.draw(param1, var9, param2 + 2, var10 + 1, 15553363);
            } else {
               if (param0.worldType == RealmsServer.WorldType.MINIGAME) {
                  int var9 = 13413468;
                  int var10 = RealmsMainScreen.this.font.width(RealmsMainScreen.SELECT_MINIGAME_PREFIX);
                  RealmsMainScreen.this.font.draw(param1, RealmsMainScreen.SELECT_MINIGAME_PREFIX, param2 + 2, param3 + 12, 13413468);
                  RealmsMainScreen.this.font.draw(param1, param0.getMinigameName(), param2 + 2 + var10, param3 + 12, 7105644);
               } else {
                  RealmsMainScreen.this.font.draw(param1, param0.getDescription(), param2 + 2, param3 + 12, 7105644);
               }

               if (!RealmsMainScreen.this.isSelfOwnedServer(param0)) {
                  RealmsMainScreen.this.font.draw(param1, param0.owner, param2 + 2, param3 + 12 + 11, 5000268);
               }
            }

            RealmsMainScreen.this.font.draw(param1, param0.getName(), param2 + 2, param3 + 1, 16777215);
            RealmsUtil.renderPlayerFace(param1, param2 - 36, param3, 32, param0.ownerUUID);
         }
      }

      private void renderStatusLights(RealmsServer param0, PoseStack param1, int param2, int param3, int param4, int param5, int param6, int param7) {
         int var9 = param2 + param6 + 22;
         if (param0.expired) {
            RealmsMainScreen.this.drawExpired(param1, var9, param3 + param7, param4, param5);
         } else if (param0.state == RealmsServer.State.CLOSED) {
            RealmsMainScreen.this.drawClose(param1, var9, param3 + param7, param4, param5);
         } else if (RealmsMainScreen.this.isSelfOwnedServer(param0) && param0.daysLeft < 7) {
            RealmsMainScreen.this.drawExpiring(param1, var9, param3 + param7, param4, param5, param0.daysLeft);
         } else if (param0.state == RealmsServer.State.OPEN) {
            RealmsMainScreen.this.drawOpen(param1, var9, param3 + param7, param4, param5);
         }
      }

      @Override
      public Component getNarration() {
         return this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", this.serverData.name);
      }

      @Nullable
      @Override
      public RealmsServer getServer() {
         return this.serverData;
      }
   }

   class TrialEntry extends RealmsMainScreen.Entry {
      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderTrialItem(param0, param1, param3, param2, param6, param7);
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         RealmsMainScreen.this.popupOpenedByUser = true;
         return true;
      }

      private void renderTrialItem(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
         int var7 = param3 + 8;
         int var8 = 0;
         boolean var9 = false;
         if (param2 <= param4 && param4 <= (int)RealmsMainScreen.this.realmSelectionList.getScrollAmount() && param3 <= param5 && param5 <= param3 + 32) {
            var9 = true;
         }

         int var10 = 8388479;
         if (var9 && !RealmsMainScreen.this.shouldShowPopup()) {
            var10 = 6077788;
         }

         for (Component var12 : RealmsMainScreen.TRIAL_MESSAGE_LINES) {
            GuiComponent.drawCenteredString(param0, RealmsMainScreen.this.font, var12, RealmsMainScreen.this.width / 2, var7 + var8, var10);
            var8 += 10;
         }
      }

      @Override
      public Component getNarration() {
         return RealmsMainScreen.TRIAL_TEXT;
      }
   }
}
