package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;

public class RealmsSelectFileToUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Component WORLD_TEXT = Component.translatable("selectWorld.world");
   static final Component HARDCORE_TEXT = Component.translatable("mco.upload.hardcore").withStyle(param0 -> param0.withColor(-65536));
   static final Component CHEATS_TEXT = Component.translatable("selectWorld.cheats");
   private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
   private final RealmsResetWorldScreen lastScreen;
   private final long worldId;
   private final int slotId;
   Button uploadButton;
   List<LevelSummary> levelList = Lists.newArrayList();
   int selectedWorld = -1;
   RealmsSelectFileToUploadScreen.WorldSelectionList worldSelectionList;
   private final Runnable callback;

   public RealmsSelectFileToUploadScreen(long param0, int param2, RealmsResetWorldScreen param3, Runnable param4) {
      super(Component.translatable("mco.upload.select.world.title"));
      this.lastScreen = param3;
      this.worldId = param0;
      this.slotId = param2;
      this.callback = param4;
   }

   private void loadLevelList() throws Exception {
      LevelStorageSource.LevelCandidates var1 = this.minecraft.getLevelSource().findLevelCandidates();
      this.levelList = this.minecraft
         .getLevelSource()
         .loadLevelSummaries(var1)
         .join()
         .stream()
         .filter(l_param0 -> !l_param0.requiresManualConversion() && !l_param0.isLocked())
         .collect(Collectors.toList());

      for (LevelSummary var3 : this.levelList) {
         this.worldSelectionList.addEntry(var3);
      }
   }

   @Override
   public void init() {
      this.worldSelectionList = new RealmsSelectFileToUploadScreen.WorldSelectionList();

      try {
         this.loadLevelList();
      } catch (Exception var1) {
         LOGGER.error("Couldn't load level list", var1);
         this.minecraft
            .setScreen(new RealmsGenericErrorScreen(Component.literal("Unable to load worlds"), Component.nullToEmpty(var1.getMessage()), this.lastScreen));
         return;
      }

      this.addWidget(this.worldSelectionList);
      this.uploadButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.upload.button.name"), l_param0 -> this.upload())
            .bounds(this.width / 2 - 154, this.height - 32, 153, 20)
            .build()
      );
      this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 6, this.height - 32, 153, 20)
            .build()
      );
      this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.subtitle"), this.width / 2, row(-1), 10526880));
      if (this.levelList.isEmpty()) {
         this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, 16777215));
      }
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   private void upload() {
      if (this.selectedWorld != -1 && !this.levelList.get(this.selectedWorld).isHardcore()) {
         LevelSummary var1 = this.levelList.get(this.selectedWorld);
         this.minecraft.setScreen(new RealmsUploadScreen(this.worldId, this.slotId, this.lastScreen, var1, this.callback));
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.worldSelectionList.render(param0, param1, param2, param3);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 13, 16777215);
      super.render(param0, param1, param2, param3);
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

   static Component gameModeName(LevelSummary param0) {
      return param0.getGameMode().getLongDisplayName();
   }

   static String formatLastPlayed(LevelSummary param0) {
      return DATE_FORMAT.format(new Date(param0.getLastPlayed()));
   }

   class Entry extends ObjectSelectionList.Entry<RealmsSelectFileToUploadScreen.Entry> {
      private final LevelSummary levelSummary;
      private final String name;
      private final String id;
      private final Component info;

      public Entry(LevelSummary param1) {
         this.levelSummary = param1;
         this.name = param1.getLevelName();
         this.id = param1.getLevelId() + " (" + RealmsSelectFileToUploadScreen.formatLastPlayed(param1) + ")";
         Component var3;
         if (param1.isHardcore()) {
            var3 = RealmsSelectFileToUploadScreen.HARDCORE_TEXT;
         } else {
            var3 = RealmsSelectFileToUploadScreen.gameModeName(param1);
         }

         if (param1.hasCheats()) {
            var3 = var3.copy().append(", ").append(RealmsSelectFileToUploadScreen.CHEATS_TEXT);
         }

         this.info = var3;
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderItem(param0, param1, param3, param2);
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         RealmsSelectFileToUploadScreen.this.worldSelectionList.selectItem(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.levelSummary));
         return true;
      }

      protected void renderItem(PoseStack param0, int param1, int param2, int param3) {
         String var5;
         if (this.name.isEmpty()) {
            var5 = RealmsSelectFileToUploadScreen.WORLD_TEXT + " " + (param1 + 1);
         } else {
            var5 = this.name;
         }

         RealmsSelectFileToUploadScreen.this.font.draw(param0, var5, param2 + 2, param3 + 1, 16777215);
         RealmsSelectFileToUploadScreen.this.font.draw(param0, this.id, param2 + 2, param3 + 12, 8421504);
         RealmsSelectFileToUploadScreen.this.font.draw(param0, this.info, param2 + 2, param3 + 12 + 10, 8421504);
      }

      @Override
      public Component getNarration() {
         Component var1 = CommonComponents.joinLines(
            Component.literal(this.levelSummary.getLevelName()),
            Component.literal(RealmsSelectFileToUploadScreen.formatLastPlayed(this.levelSummary)),
            RealmsSelectFileToUploadScreen.gameModeName(this.levelSummary)
         );
         return Component.translatable("narrator.select", var1);
      }
   }

   class WorldSelectionList extends RealmsObjectSelectionList<RealmsSelectFileToUploadScreen.Entry> {
      public WorldSelectionList() {
         super(
            RealmsSelectFileToUploadScreen.this.width,
            RealmsSelectFileToUploadScreen.this.height,
            RealmsSelectFileToUploadScreen.row(0),
            RealmsSelectFileToUploadScreen.this.height - 40,
            36
         );
      }

      public void addEntry(LevelSummary param0) {
         this.addEntry(RealmsSelectFileToUploadScreen.this.new Entry(param0));
      }

      @Override
      public int getMaxPosition() {
         return RealmsSelectFileToUploadScreen.this.levelList.size() * 36;
      }

      @Override
      public void renderBackground(PoseStack param0) {
         RealmsSelectFileToUploadScreen.this.renderBackground(param0);
      }

      public void setSelected(@Nullable RealmsSelectFileToUploadScreen.Entry param0) {
         super.setSelected(param0);
         RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(param0);
         RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0
            && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getItemCount()
            && !RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld).isHardcore();
      }
   }
}
