package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsConfigureWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation ON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/on_icon.png");
   private static final ResourceLocation OFF_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/off_icon.png");
   private static final ResourceLocation EXPIRED_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expired_icon.png");
   private static final ResourceLocation EXPIRES_SOON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expires_soon_icon.png");
   private static final Component WORLD_LIST_TITLE = Component.translatable("mco.configure.worlds.title");
   private static final Component TITLE = Component.translatable("mco.configure.world.title");
   private static final Component MINIGAME_PREFIX = Component.translatable("mco.configure.current.minigame").append(": ");
   private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   private static final Component SERVER_EXPIRING_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   private static final Component SERVER_EXPIRING_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private static final int DEFAULT_BUTTON_OFFSET = 5;
   @Nullable
   private Component toolTip;
   private final RealmsMainScreen lastScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private int leftX;
   private int rightX;
   private Button playersButton;
   private Button settingsButton;
   private Button subscriptionButton;
   private Button optionsButton;
   private Button backupButton;
   private Button resetWorldButton;
   private Button switchMinigameButton;
   private boolean stateChanged;
   private int animTick;
   private int clicks;
   private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

   public RealmsConfigureWorldScreen(RealmsMainScreen param0, long param1) {
      super(TITLE);
      this.lastScreen = param0;
      this.serverId = param1;
   }

   @Override
   public void init() {
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      }

      this.leftX = this.width / 2 - 187;
      this.rightX = this.width / 2 + 190;
      this.playersButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.players"),
               l_param0 -> this.minecraft.setScreen(new RealmsPlayerScreen(this, this.serverData))
            )
            .bounds(this.centerButton(0, 3), row(0), 100, 20)
            .build()
      );
      this.settingsButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.settings"),
               l_param0 -> this.minecraft.setScreen(new RealmsSettingsScreen(this, this.serverData.clone()))
            )
            .bounds(this.centerButton(1, 3), row(0), 100, 20)
            .build()
      );
      this.subscriptionButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.subscription"),
               l_param0 -> this.minecraft.setScreen(new RealmsSubscriptionInfoScreen(this, this.serverData.clone(), this.lastScreen))
            )
            .bounds(this.centerButton(2, 3), row(0), 100, 20)
            .build()
      );
      this.slotButtonList.clear();

      for (int var1 = 1; var1 < 5; var1++) {
         this.slotButtonList.add(this.addSlotButton(var1));
      }

      this.switchMinigameButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.switchminigame"),
               l_param0 -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(
                        Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
                     )
                  )
            )
            .bounds(this.leftButton(0), row(13) - 5, 100, 20)
            .build()
      );
      this.optionsButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.options"),
               l_param0 -> this.minecraft
                  .setScreen(
                     new RealmsSlotOptionsScreen(
                        this, this.serverData.slots.get(this.serverData.activeSlot).clone(), this.serverData.worldType, this.serverData.activeSlot
                     )
                  )
            )
            .bounds(this.leftButton(0), row(13) - 5, 90, 20)
            .build()
      );
      this.backupButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.backup"),
               l_param0 -> this.minecraft.setScreen(new RealmsBackupScreen(this, this.serverData.clone(), this.serverData.activeSlot))
            )
            .bounds(this.leftButton(1), row(13) - 5, 90, 20)
            .build()
      );
      this.resetWorldButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.resetworld"),
               l_param0 -> this.minecraft
                  .setScreen(
                     new RealmsResetWorldScreen(
                        this,
                        this.serverData.clone(),
                        () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())),
                        () -> this.minecraft.setScreen(this.getNewScreen())
                     )
                  )
            )
            .bounds(this.leftButton(2), row(13) - 5, 90, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.backButtonClicked()).bounds(this.rightX - 80 + 8, row(13) - 5, 70, 20).build()
      );
      this.backupButton.active = true;
      if (this.serverData == null) {
         this.hideMinigameButtons();
         this.hideRegularButtons();
         this.playersButton.active = false;
         this.settingsButton.active = false;
         this.subscriptionButton.active = false;
      } else {
         this.disableButtons();
         if (this.isMinigame()) {
            this.hideRegularButtons();
         } else {
            this.hideMinigameButtons();
         }
      }
   }

   private RealmsWorldSlotButton addSlotButton(int param0) {
      int var2 = this.frame(param0);
      int var3 = row(5) + 5;
      RealmsWorldSlotButton var4 = new RealmsWorldSlotButton(
         var2, var3, 80, 80, () -> this.serverData, l_param0 -> this.toolTip = l_param0, param0, l_param1 -> {
            RealmsWorldSlotButton.State l_var3 = ((RealmsWorldSlotButton)l_param1).getState();
            if (l_var3 != null) {
               switch (l_var3.action) {
                  case NOTHING:
                     break;
                  case JOIN:
                     this.joinRealm(this.serverData);
                     break;
                  case SWITCH_SLOT:
                     if (l_var3.minigame) {
                        this.switchToMinigame();
                     } else if (l_var3.empty) {
                        this.switchToEmptySlot(param0, this.serverData);
                     } else {
                        this.switchToFullSlot(param0, this.serverData);
                     }
                     break;
                  default:
                     throw new IllegalStateException("Unknown action " + l_var3.action);
               }
            }
         }
      );
      return this.addRenderableWidget(var4);
   }

   private int leftButton(int param0) {
      return this.leftX + param0 * 95;
   }

   private int centerButton(int param0, int param1) {
      return this.width / 2 - (param1 * 105 - 5) / 2 + param0 * 105;
   }

   @Override
   public void tick() {
      super.tick();
      this.animTick++;
      this.clicks--;
      if (this.clicks < 0) {
         this.clicks = 0;
      }

      this.slotButtonList.forEach(RealmsWorldSlotButton::tick);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.toolTip = null;
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, WORLD_LIST_TITLE, this.width / 2, row(4), 16777215);
      super.render(param0, param1, param2, param3);
      if (this.serverData == null) {
         drawCenteredString(param0, this.font, this.title, this.width / 2, 17, 16777215);
      } else {
         String var5 = this.serverData.getName();
         int var6 = this.font.width(var5);
         int var7 = this.serverData.state == RealmsServer.State.CLOSED ? 10526880 : 8388479;
         int var8 = this.font.width(this.title);
         drawCenteredString(param0, this.font, this.title, this.width / 2, 12, 16777215);
         drawCenteredString(param0, this.font, var5, this.width / 2, 24, var7);
         int var9 = Math.min(this.centerButton(2, 3) + 80 - 11, this.width / 2 + var6 / 2 + var8 / 2 + 10);
         this.drawServerStatus(param0, var9, 7, param1, param2);
         if (this.isMinigame()) {
            this.font.draw(param0, MINIGAME_PREFIX.copy().append(this.serverData.getMinigameName()), this.leftX + 80 + 20 + 10, row(13), 16777215);
         }

         if (this.toolTip != null) {
            this.renderMousehoverTooltip(param0, this.toolTip, param1, param2);
         }
      }
   }

   private int frame(int param0) {
      return this.leftX + (param0 - 1) * 98;
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.lastScreen.resetScreen();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   private void fetchServerData(long param0) {
      new Thread(() -> {
         RealmsClient l_var3 = RealmsClient.create();

         try {
            RealmsServer l_var4 = l_var3.getOwnWorld(param0);
            this.minecraft.execute(() -> {
               this.serverData = l_var4;
               this.disableButtons();
               if (this.isMinigame()) {
                  this.show(this.switchMinigameButton);
               } else {
                  this.show(this.optionsButton);
                  this.show(this.backupButton);
                  this.show(this.resetWorldButton);
               }
            });
         } catch (RealmsServiceException l_var4) {
            LOGGER.error("Couldn't get own world");
            this.minecraft.execute(() -> this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.nullToEmpty(l_var4.getMessage()), this.lastScreen)));
         }
      }).start();
   }

   private void disableButtons() {
      this.playersButton.active = !this.serverData.expired;
      this.settingsButton.active = !this.serverData.expired;
      this.subscriptionButton.active = true;
      this.switchMinigameButton.active = !this.serverData.expired;
      this.optionsButton.active = !this.serverData.expired;
      this.resetWorldButton.active = !this.serverData.expired;
   }

   private void joinRealm(RealmsServer param0) {
      if (this.serverData.state == RealmsServer.State.OPEN) {
         this.lastScreen.play(param0, new RealmsConfigureWorldScreen(this.lastScreen.newScreen(), this.serverId));
      } else {
         this.openTheWorld(true, new RealmsConfigureWorldScreen(this.lastScreen.newScreen(), this.serverId));
      }
   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen var1 = new RealmsSelectWorldTemplateScreen(
         Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
      );
      var1.setWarning(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"));
      this.minecraft.setScreen(var1);
   }

   private void switchToFullSlot(int param0, RealmsServer param1) {
      Component var3 = Component.translatable("mco.configure.world.slot.switch.question.line1");
      Component var4 = Component.translatable("mco.configure.world.slot.switch.question.line2");
      this.minecraft
         .setScreen(
            new RealmsLongConfirmationScreen(
               l_param2 -> {
                  if (l_param2) {
                     this.minecraft
                        .setScreen(
                           new RealmsLongRunningMcoTaskScreen(
                              this.lastScreen,
                              new SwitchSlotTask(param1.id, param0, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())))
                           )
                        );
                  } else {
                     this.minecraft.setScreen(this);
                  }
               },
               RealmsLongConfirmationScreen.Type.Info,
               var3,
               var4,
               true
            )
         );
   }

   private void switchToEmptySlot(int param0, RealmsServer param1) {
      Component var3 = Component.translatable("mco.configure.world.slot.switch.question.line1");
      Component var4 = Component.translatable("mco.configure.world.slot.switch.question.line2");
      this.minecraft
         .setScreen(
            new RealmsLongConfirmationScreen(
               l_param2 -> {
                  if (l_param2) {
                     RealmsResetWorldScreen l_var4 = new RealmsResetWorldScreen(
                        this,
                        param1,
                        Component.translatable("mco.configure.world.switch.slot"),
                        Component.translatable("mco.configure.world.switch.slot.subtitle"),
                        10526880,
                        CommonComponents.GUI_CANCEL,
                        () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())),
                        () -> this.minecraft.setScreen(this.getNewScreen())
                     );
                     l_var4.setSlot(param0);
                     l_var4.setResetTitle(Component.translatable("mco.create.world.reset.title"));
                     this.minecraft.setScreen(l_var4);
                  } else {
                     this.minecraft.setScreen(this);
                  }
               },
               RealmsLongConfirmationScreen.Type.Info,
               var3,
               var4,
               true
            )
         );
   }

   protected void renderMousehoverTooltip(PoseStack param0, @Nullable Component param1, int param2, int param3) {
      int var5 = param2 + 12;
      int var6 = param3 - 12;
      int var7 = this.font.width(param1);
      if (var5 + var7 + 3 > this.rightX) {
         var5 = var5 - var7 - 20;
      }

      fillGradient(param0, var5 - 3, var6 - 3, var5 + var7 + 3, var6 + 8 + 3, -1073741824, -1073741824);
      this.font.drawShadow(param0, param1, var5, var6, 16777215);
   }

   private void drawServerStatus(PoseStack param0, int param1, int param2, int param3, int param4) {
      if (this.serverData.expired) {
         this.drawExpired(param0, param1, param2, param3, param4);
      } else if (this.serverData.state == RealmsServer.State.CLOSED) {
         this.drawClose(param0, param1, param2, param3, param4);
      } else if (this.serverData.state == RealmsServer.State.OPEN) {
         if (this.serverData.daysLeft < 7) {
            this.drawExpiring(param0, param1, param2, param3, param4, this.serverData.daysLeft);
         } else {
            this.drawOpen(param0, param1, param2, param3, param4);
         }
      }
   }

   private void drawExpired(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, EXPIRED_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1 && param3 <= param1 + 9 && param4 >= param2 && param4 <= param2 + 27) {
         this.toolTip = SERVER_EXPIRED_TOOLTIP;
      }
   }

   private void drawExpiring(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      RenderSystem.setShaderTexture(0, EXPIRES_SOON_ICON_LOCATION);
      if (this.animTick % 20 < 10) {
         GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         GuiComponent.blit(param0, param1, param2, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (param3 >= param1 && param3 <= param1 + 9 && param4 >= param2 && param4 <= param2 + 27) {
         if (param5 <= 0) {
            this.toolTip = SERVER_EXPIRING_SOON_TOOLTIP;
         } else if (param5 == 1) {
            this.toolTip = SERVER_EXPIRING_IN_DAY_TOOLTIP;
         } else {
            this.toolTip = Component.translatable("mco.selectServer.expires.days", param5);
         }
      }
   }

   private void drawOpen(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, ON_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1 && param3 <= param1 + 9 && param4 >= param2 && param4 <= param2 + 27) {
         this.toolTip = SERVER_OPEN_TOOLTIP;
      }
   }

   private void drawClose(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.setShaderTexture(0, OFF_ICON_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 10, 28, 10, 28);
      if (param3 >= param1 && param3 <= param1 + 9 && param4 >= param2 && param4 <= param2 + 27) {
         this.toolTip = SERVER_CLOSED_TOOLTIP;
      }
   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void hideRegularButtons() {
      this.hide(this.optionsButton);
      this.hide(this.backupButton);
      this.hide(this.resetWorldButton);
   }

   private void hide(Button param0) {
      param0.visible = false;
      this.removeWidget(param0);
   }

   private void show(Button param0) {
      param0.visible = true;
      this.addRenderableWidget(param0);
   }

   private void hideMinigameButtons() {
      this.hide(this.switchMinigameButton);
   }

   public void saveSlotSettings(RealmsWorldOptions param0) {
      RealmsWorldOptions var2 = this.serverData.slots.get(this.serverData.activeSlot);
      param0.templateId = var2.templateId;
      param0.templateImage = var2.templateImage;
      RealmsClient var3 = RealmsClient.create();

      try {
         var3.updateSlot(this.serverData.id, this.serverData.activeSlot, param0);
         this.serverData.slots.put(this.serverData.activeSlot, param0);
      } catch (RealmsServiceException var4) {
         LOGGER.error("Couldn't save slot settings");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var4, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void saveSettings(String param0, String param1) {
      String var3 = param1.trim().isEmpty() ? null : param1;
      RealmsClient var4 = RealmsClient.create();

      try {
         var4.update(this.serverData.id, param0, var3);
         this.serverData.setName(param0);
         this.serverData.setDescription(var3);
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't save settings");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void openTheWorld(boolean param0, Screen param1) {
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(param1, new OpenServerTask(this.serverData, this, this.lastScreen, param0, this.minecraft)));
   }

   public void closeTheWorld(Screen param0) {
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(param0, new CloseServerTask(this.serverData, this)));
   }

   public void stateChanged() {
      this.stateChanged = true;
   }

   private void templateSelectionCallback(@Nullable WorldTemplate param0) {
      if (param0 != null && WorldTemplate.WorldTemplateType.MINIGAME == param0.type) {
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchMinigameTask(this.serverData.id, param0, this.getNewScreen())));
      } else {
         this.minecraft.setScreen(this);
      }
   }

   public RealmsConfigureWorldScreen getNewScreen() {
      return new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
   }
}
