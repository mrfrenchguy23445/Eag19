package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsBackupScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation PLUS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/plus_icon.png");
   static final ResourceLocation RESTORE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/restore_icon.png");
   static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
   static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
   private static final Component TITLE = Component.translatable("mco.configure.world.backup");
   private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
   static int lastScrollPosition = -1;
   private final RealmsConfigureWorldScreen lastScreen;
   List<Backup> backups = Collections.emptyList();
   @Nullable
   Component toolTip;
   RealmsBackupScreen.BackupObjectSelectionList backupObjectSelectionList;
   int selectedBackup = -1;
   private final int slotId;
   private Button downloadButton;
   private Button restoreButton;
   private Button changesButton;
   Boolean noBackups = false;
   final RealmsServer serverData;
   private static final String UPLOADED_KEY = "Uploaded";

   public RealmsBackupScreen(RealmsConfigureWorldScreen param0, RealmsServer param1, int param2) {
      super(Component.translatable("mco.configure.world.backup"));
      this.lastScreen = param0;
      this.serverData = param1;
      this.slotId = param2;
   }

   @Override
   public void init() {
      this.backupObjectSelectionList = new RealmsBackupScreen.BackupObjectSelectionList();
      if (lastScrollPosition != -1) {
         this.backupObjectSelectionList.setScrollAmount(lastScrollPosition);
      }

      (new Thread("Realms-fetch-backups") {
         @Override
         public void run() {
            RealmsClient l_var1 = RealmsClient.create();

            try {
               List<Backup> l_var2 = l_var1.backupsFor(RealmsBackupScreen.this.serverData.id).backups;
               RealmsBackupScreen.this.minecraft.execute(() -> {
                  RealmsBackupScreen.this.backups = l_var2;
                  RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                  RealmsBackupScreen.this.backupObjectSelectionList.clear();

                  for (Backup l2_var3 : RealmsBackupScreen.this.backups) {
                     RealmsBackupScreen.this.backupObjectSelectionList.addEntry(l2_var3);
                  }

                  RealmsBackupScreen.this.generateChangeList();
               });
            } catch (RealmsServiceException l_var2) {
               RealmsBackupScreen.LOGGER.error("Couldn't request backups", l_var2);
            }
         }
      }).start();
      this.downloadButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.backup.button.download"), l_param0 -> this.downloadClicked())
            .bounds(this.width - 135, row(1), 120, 20)
            .build()
      );
      this.restoreButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.backup.button.restore"), l_param0 -> this.restoreClicked(this.selectedBackup))
            .bounds(this.width - 135, row(3), 120, 20)
            .build()
      );
      this.changesButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.backup.changes.tooltip"), l_param0 -> {
         this.minecraft.setScreen(new RealmsBackupInfoScreen(this, this.backups.get(this.selectedBackup)));
         this.selectedBackup = -1;
      }).bounds(this.width - 135, row(5), 120, 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width - 100, this.height - 35, 85, 20)
            .build()
      );
      this.addWidget(this.backupObjectSelectionList);
      this.magicalSpecialHackyFocus(this.backupObjectSelectionList);
      this.updateButtonStates();
   }

   void generateChangeList() {
      if (this.backups.size() > 1) {
         for (int var1 = 0; var1 < this.backups.size() - 1; var1++) {
            Backup var2 = this.backups.get(var1);
            Backup var3 = this.backups.get(var1 + 1);
            if (!var2.metadata.isEmpty() && !var3.metadata.isEmpty()) {
               for (String var5 : var2.metadata.keySet()) {
                  if (!var5.contains("Uploaded") && var3.metadata.containsKey(var5)) {
                     if (!var2.metadata.get(var5).equals(var3.metadata.get(var5))) {
                        this.addToChangeList(var2, var5);
                     }
                  } else {
                     this.addToChangeList(var2, var5);
                  }
               }
            }
         }
      }
   }

   private void addToChangeList(Backup param0, String param1) {
      if (param1.contains("Uploaded")) {
         String var3 = DateFormat.getDateTimeInstance(3, 3).format(param0.lastModifiedDate);
         param0.changeList.put(param1, var3);
         param0.setUploadedVersion(true);
      } else {
         param0.changeList.put(param1, param0.metadata.get(param1));
      }
   }

   void updateButtonStates() {
      this.restoreButton.visible = this.shouldRestoreButtonBeVisible();
      this.changesButton.visible = this.shouldChangesButtonBeVisible();
   }

   private boolean shouldChangesButtonBeVisible() {
      return this.selectedBackup == -1 ? false : !this.backups.get(this.selectedBackup).changeList.isEmpty();
   }

   private boolean shouldRestoreButtonBeVisible() {
      return this.selectedBackup == -1 ? false : !this.serverData.expired;
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   void restoreClicked(int param0) {
      if (param0 >= 0 && param0 < this.backups.size() && !this.serverData.expired) {
         this.selectedBackup = param0;
         Date var2 = this.backups.get(param0).lastModifiedDate;
         String var3 = DateFormat.getDateTimeInstance(3, 3).format(var2);
         String var4 = RealmsUtil.convertToAgePresentationFromInstant(var2);
         Component var5 = Component.translatable("mco.configure.world.restore.question.line1", var3, var4);
         Component var6 = Component.translatable("mco.configure.world.restore.question.line2");
         this.minecraft.setScreen(new RealmsLongConfirmationScreen(l_param0 -> {
            if (l_param0) {
               this.restore();
            } else {
               this.selectedBackup = -1;
               this.minecraft.setScreen(this);
            }
         }, RealmsLongConfirmationScreen.Type.Warning, var5, var6, true));
      }
   }

   private void downloadClicked() {
      Component var1 = Component.translatable("mco.configure.world.restore.download.question.line1");
      Component var2 = Component.translatable("mco.configure.world.restore.download.question.line2");
      this.minecraft.setScreen(new RealmsLongConfirmationScreen(l_param0 -> {
         if (l_param0) {
            this.downloadWorldData();
         } else {
            this.minecraft.setScreen(this);
         }
      }, RealmsLongConfirmationScreen.Type.Info, var1, var2, true));
   }

   private void downloadWorldData() {
      this.minecraft
         .setScreen(
            new RealmsLongRunningMcoTaskScreen(
               this.lastScreen.getNewScreen(),
               new DownloadTask(
                  this.serverData.id,
                  this.slotId,
                  this.serverData.name + " (" + this.serverData.slots.get(this.serverData.activeSlot).getSlotName(this.serverData.activeSlot) + ")",
                  this
               )
            )
         );
   }

   private void restore() {
      Backup var1 = this.backups.get(this.selectedBackup);
      this.selectedBackup = -1;
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen.getNewScreen(), new RestoreTask(var1, this.serverData.id, this.lastScreen)));
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.toolTip = null;
      this.renderBackground(param0);
      this.backupObjectSelectionList.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 12, 16777215);
      this.font.draw(param0, TITLE, (this.width - 150) / 2 - 90, 20.0F, 10526880);
      if (this.noBackups) {
         this.font.draw(param0, NO_BACKUPS_LABEL, 20.0F, this.height / 2 - 10, 16777215);
      }

      this.downloadButton.active = !this.noBackups;
      super.render(param0, param1, param2, param3);
      if (this.toolTip != null) {
         this.renderMousehoverTooltip(param0, this.toolTip, param1, param2);
      }
   }

   protected void renderMousehoverTooltip(PoseStack param0, @Nullable Component param1, int param2, int param3) {
      if (param1 != null) {
         int var5 = param2 + 12;
         int var6 = param3 - 12;
         int var7 = this.font.width(param1);
         fillGradient(param0, var5 - 3, var6 - 3, var5 + var7 + 3, var6 + 8 + 3, -1073741824, -1073741824);
         this.font.drawShadow(param0, param1, var5, var6, 16777215);
      }
   }

   class BackupObjectSelectionList extends RealmsObjectSelectionList<RealmsBackupScreen.Entry> {
      public BackupObjectSelectionList() {
         super(RealmsBackupScreen.this.width - 150, RealmsBackupScreen.this.height, 32, RealmsBackupScreen.this.height - 15, 36);
      }

      public void addEntry(Backup param0) {
         this.addEntry(RealmsBackupScreen.this.new Entry(param0));
      }

      @Override
      public int getRowWidth() {
         return (int)(this.width * 0.93);
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      @Override
      public void renderBackground(PoseStack param0) {
         RealmsBackupScreen.this.renderBackground(param0);
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         if (param4 != 0) {
            return false;
         }

         if (param0 < this.getScrollbarPosition() && param2 >= this.y0 && param2 <= this.y1) {
            int var6 = this.width / 2 - 92;
            int var7 = this.width;
            int var8 = (int)Math.floor(param2 - this.y0) - this.headerHeight + (int)this.getScrollAmount();
            int var9 = var8 / this.itemHeight;
            if (param0 >= var6 && param0 <= var7 && var9 >= 0 && var8 >= 0 && var9 < this.getItemCount()) {
               this.selectItem(var9);
               this.itemClicked(var8, var9, param0, param2, this.width, param4);
            }

            return true;
         } else {
            return false;
         }
      }

      @Override
      public int getScrollbarPosition() {
         return this.width - 5;
      }

      @Override
      public void itemClicked(int param0, int param1, double param2, double param4, int param6, int param7) {
         int var9 = this.width - 35;
         int var10 = param1 * this.itemHeight + 36 - (int)this.getScrollAmount();
         int var11 = var9 + 10;
         int var12 = var10 - 3;
         if (param2 >= var9 && param2 <= var9 + 9 && param4 >= var10 && param4 <= var10 + 9) {
            if (!RealmsBackupScreen.this.backups.get(param1).changeList.isEmpty()) {
               RealmsBackupScreen.this.selectedBackup = -1;
               RealmsBackupScreen.lastScrollPosition = (int)this.getScrollAmount();
               this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, RealmsBackupScreen.this.backups.get(param1)));
            }
         } else if (param2 >= var11 && param2 < var11 + 13 && param4 >= var12 && param4 < var12 + 15) {
            RealmsBackupScreen.lastScrollPosition = (int)this.getScrollAmount();
            RealmsBackupScreen.this.restoreClicked(param1);
         }
      }

      @Override
      public void selectItem(int param0) {
         super.selectItem(param0);
         this.selectInviteListItem(param0);
      }

      public void selectInviteListItem(int param0) {
         RealmsBackupScreen.this.selectedBackup = param0;
         RealmsBackupScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsBackupScreen.Entry param0) {
         super.setSelected(param0);
         RealmsBackupScreen.this.selectedBackup = this.children().indexOf(param0);
         RealmsBackupScreen.this.updateButtonStates();
      }
   }

   class Entry extends ObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
      private final Backup backup;

      public Entry(Backup param1) {
         this.backup = param1;
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderBackupItem(param0, this.backup, param3 - 40, param2, param6, param7);
      }

      private void renderBackupItem(PoseStack param0, Backup param1, int param2, int param3, int param4, int param5) {
         int var7 = param1.isUploadedVersion() ? -8388737 : 16777215;
         RealmsBackupScreen.this.font
            .draw(param0, "Backup (" + RealmsUtil.convertToAgePresentationFromInstant(param1.lastModifiedDate) + ")", param2 + 40, param3 + 1, var7);
         RealmsBackupScreen.this.font.draw(param0, this.getMediumDatePresentation(param1.lastModifiedDate), param2 + 40, param3 + 12, 5000268);
         int var8 = RealmsBackupScreen.this.width - 175;
         int var9 = -3;
         int var10 = var8 - 10;
         int var11 = 0;
         if (!RealmsBackupScreen.this.serverData.expired) {
            this.drawRestore(param0, var8, param3 + -3, param4, param5);
         }

         if (!param1.changeList.isEmpty()) {
            this.drawInfo(param0, var10, param3 + 0, param4, param5);
         }
      }

      private String getMediumDatePresentation(Date param0) {
         return DateFormat.getDateTimeInstance(3, 3).format(param0);
      }

      private void drawRestore(PoseStack param0, int param1, int param2, int param3, int param4) {
         boolean var6 = param3 >= param1
            && param3 <= param1 + 12
            && param4 >= param2
            && param4 <= param2 + 14
            && param4 < RealmsBackupScreen.this.height - 15
            && param4 > 32;
         RenderSystem.setShaderTexture(0, RealmsBackupScreen.RESTORE_ICON_LOCATION);
         param0.pushPose();
         param0.scale(0.5F, 0.5F, 0.5F);
         float var7 = var6 ? 28.0F : 0.0F;
         GuiComponent.blit(param0, param1 * 2, param2 * 2, 0.0F, var7, 23, 28, 23, 56);
         param0.popPose();
         if (var6) {
            RealmsBackupScreen.this.toolTip = RealmsBackupScreen.RESTORE_TOOLTIP;
         }
      }

      private void drawInfo(PoseStack param0, int param1, int param2, int param3, int param4) {
         boolean var6 = param3 >= param1
            && param3 <= param1 + 8
            && param4 >= param2
            && param4 <= param2 + 8
            && param4 < RealmsBackupScreen.this.height - 15
            && param4 > 32;
         RenderSystem.setShaderTexture(0, RealmsBackupScreen.PLUS_ICON_LOCATION);
         param0.pushPose();
         param0.scale(0.5F, 0.5F, 0.5F);
         float var7 = var6 ? 15.0F : 0.0F;
         GuiComponent.blit(param0, param1 * 2, param2 * 2, 0.0F, var7, 15, 15, 15, 30);
         param0.popPose();
         if (var6) {
            RealmsBackupScreen.this.toolTip = RealmsBackupScreen.HAS_CHANGES_TOOLTIP;
         }
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.backup.lastModifiedDate.toString());
      }
   }
}
