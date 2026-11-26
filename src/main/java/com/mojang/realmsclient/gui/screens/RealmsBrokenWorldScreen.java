package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class RealmsBrokenWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private final Screen lastScreen;
   private final RealmsMainScreen mainScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private final Component[] message = new Component[]{
      Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")
   };
   private int leftX;
   private int rightX;
   private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
   private int animTick;

   public RealmsBrokenWorldScreen(Screen param0, RealmsMainScreen param1, long param2, boolean param4) {
      super(param4 ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
      this.lastScreen = param0;
      this.mainScreen = param1;
      this.serverId = param2;
   }

   @Override
   public void init() {
      this.leftX = this.width / 2 - 150;
      this.rightX = this.width / 2 + 190;
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.backButtonClicked()).bounds(this.rightX - 80 + 8, row(13) - 5, 70, 20).build()
      );
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      } else {
         this.addButtons();
      }
   }

   @Override
   public Component getNarrationMessage() {
      return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
   }

   private void addButtons() {
      for (Entry<Integer, RealmsWorldOptions> var2 : this.serverData.slots.entrySet()) {
         int var3 = var2.getKey();
         boolean var4 = var3 != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
         Button var5;
         if (var4) {
            var5 = Button.builder(
                  Component.translatable("mco.brokenworld.play"),
                  l_param1 -> {
                     if (this.serverData.slots.get(var3).empty) {
                        RealmsResetWorldScreen l_var3 = new RealmsResetWorldScreen(
                           this,
                           this.serverData,
                           Component.translatable("mco.configure.world.switch.slot"),
                           Component.translatable("mco.configure.world.switch.slot.subtitle"),
                           10526880,
                           CommonComponents.GUI_CANCEL,
                           this::doSwitchOrReset,
                           () -> {
                              this.minecraft.setScreen(this);
                              this.doSwitchOrReset();
                           }
                        );
                        l_var3.setSlot(var3);
                        l_var3.setResetTitle(Component.translatable("mco.create.world.reset.title"));
                        this.minecraft.setScreen(l_var3);
                     } else {
                        this.minecraft
                           .setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, var3, this::doSwitchOrReset)));
                     }
                  }
               )
               .bounds(this.getFramePositionX(var3), row(8), 80, 20)
               .build();
         } else {
            var5 = Button.builder(Component.translatable("mco.brokenworld.download"), l_param1 -> {
               Component l_var3 = Component.translatable("mco.configure.world.restore.download.question.line1");
               Component l_var4 = Component.translatable("mco.configure.world.restore.download.question.line2");
               this.minecraft.setScreen(new RealmsLongConfirmationScreen(l2_param1 -> {
                  if (l2_param1) {
                     this.downloadWorld(var3);
                  } else {
                     this.minecraft.setScreen(this);
                  }
               }, RealmsLongConfirmationScreen.Type.Info, l_var3, l_var4, true));
            }).bounds(this.getFramePositionX(var3), row(8), 80, 20).build();
         }

         if (this.slotsThatHasBeenDownloaded.contains(var3)) {
            var5.active = false;
            var5.setMessage(Component.translatable("mco.brokenworld.downloaded"));
         }

         this.addRenderableWidget(var5);
         this.addRenderableWidget(Button.builder(Component.translatable("mco.brokenworld.reset"), l_param1 -> {
            RealmsResetWorldScreen l_var3 = new RealmsResetWorldScreen(this, this.serverData, this::doSwitchOrReset, () -> {
               this.minecraft.setScreen(this);
               this.doSwitchOrReset();
            });
            if (var3 != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
               l_var3.setSlot(var3);
            }

            this.minecraft.setScreen(l_var3);
         }).bounds(this.getFramePositionX(var3), row(10), 80, 20).build());
      }
   }

   @Override
   public void tick() {
      this.animTick++;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      super.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 17, 16777215);

      for (int var5 = 0; var5 < this.message.length; var5++) {
         drawCenteredString(param0, this.font, this.message[var5], this.width / 2, row(-1) + 3 + var5 * 12, 10526880);
      }

      if (this.serverData != null) {
         for (Entry<Integer, RealmsWorldOptions> var6 : this.serverData.slots.entrySet()) {
            if (var6.getValue().templateImage != null && var6.getValue().templateId != -1L) {
               this.drawSlotFrame(
                  param0,
                  this.getFramePositionX(var6.getKey()),
                  row(1) + 5,
                  param1,
                  param2,
                  this.serverData.activeSlot == var6.getKey() && !this.isMinigame(),
                  var6.getValue().getSlotName(var6.getKey()),
                  var6.getKey(),
                  var6.getValue().templateId,
                  var6.getValue().templateImage,
                  var6.getValue().empty
               );
            } else {
               this.drawSlotFrame(
                  param0,
                  this.getFramePositionX(var6.getKey()),
                  row(1) + 5,
                  param1,
                  param2,
                  this.serverData.activeSlot == var6.getKey() && !this.isMinigame(),
                  var6.getValue().getSlotName(var6.getKey()),
                  var6.getKey(),
                  -1L,
                  null,
                  var6.getValue().empty
               );
            }
         }
      }
   }

   private int getFramePositionX(int param0) {
      return this.leftX + (param0 - 1) * 110;
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
      this.minecraft.setScreen(this.lastScreen);
   }

   private void fetchServerData(long param0) {
      new Thread(() -> {
         RealmsClient l_var3 = RealmsClient.create();

         try {
            this.serverData = l_var3.getOwnWorld(param0);
            this.addButtons();
         } catch (RealmsServiceException l_var4) {
            LOGGER.error("Couldn't get own world");
            this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.nullToEmpty(l_var4.getMessage()), this.lastScreen));
         }
      }).start();
   }

   public void doSwitchOrReset() {
      new Thread(
            () -> {
               RealmsClient l_var1 = RealmsClient.create();
               if (this.serverData.state == RealmsServer.State.CLOSED) {
                  this.minecraft
                     .execute(
                        () -> this.minecraft
                           .setScreen(
                              new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, this.mainScreen, true, this.minecraft))
                           )
                     );
               } else {
                  try {
                     RealmsServer l_var2 = l_var1.getOwnWorld(this.serverId);
                     this.minecraft.execute(() -> this.mainScreen.newScreen().play(l_var2, this));
                  } catch (RealmsServiceException l_var2) {
                     LOGGER.error("Couldn't get own world");
                     this.minecraft.execute(() -> this.minecraft.setScreen(this.lastScreen));
                  }
               }
            }
         )
         .start();
   }

   private void downloadWorld(int param0) {
      RealmsClient var2 = RealmsClient.create();

      try {
         WorldDownload var3 = var2.requestDownloadInfo(this.serverData.id, param0);
         RealmsDownloadLatestWorldScreen var4 = new RealmsDownloadLatestWorldScreen(this, var3, this.serverData.getWorldName(param0), l_param1 -> {
            if (l_param1) {
               this.slotsThatHasBeenDownloaded.add(param0);
               this.clearWidgets();
               this.addButtons();
            } else {
               this.minecraft.setScreen(this);
            }
         });
         this.minecraft.setScreen(var4);
      } catch (RealmsServiceException var3) {
         LOGGER.error("Couldn't download world data");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var3, this));
      }
   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void drawSlotFrame(
      PoseStack param0,
      int param1,
      int param2,
      int param3,
      int param4,
      boolean param5,
      String param6,
      int param7,
      long param8,
      @Nullable String param10,
      boolean param11
   ) {
      if (param11) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.EMPTY_SLOT_LOCATION);
      } else if (param10 != null && param8 != -1L) {
         RenderSystem.setShaderTexture(0, RealmsTextureManager.worldTemplate(String.valueOf(param8), param10));
      } else if (param7 == 1) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1);
      } else if (param7 == 2) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2);
      } else if (param7 == 3) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3);
      } else {
         RenderSystem.setShaderTexture(0, RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage));
      }

      if (!param5) {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      } else if (param5) {
         float var13 = 0.9F + 0.1F * Mth.cos(this.animTick * 0.2F);
         RenderSystem.setShaderColor(var13, var13, var13, 1.0F);
      }

      GuiComponent.blit(param0, param1 + 3, param2 + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.SLOT_FRAME_LOCATION);
      if (param5) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 80, 80, 80, 80);
      drawCenteredString(param0, this.font, param6, param1 + 40, param2 + 66, 16777215);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
