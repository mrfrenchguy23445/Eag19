package net.minecraft.client.gui.screens.worldselection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class EditWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
   private Button renameButton;
   private final BooleanConsumer callback;
   private EditBox nameEdit;
   private final LevelStorageSource.LevelStorageAccess levelAccess;

   public EditWorldScreen(BooleanConsumer param0, LevelStorageSource.LevelStorageAccess param1) {
      super(Component.translatable("selectWorld.edit.title"));
      this.callback = param0;
      this.levelAccess = param1;
   }

   @Override
   public void tick() {
      this.nameEdit.tick();
   }

   @Override
   protected void init() {
      this.renameButton = Button.builder(Component.translatable("selectWorld.edit.save"), l_param0 -> this.onRename())
         .bounds(this.width / 2 - 100, this.height / 4 + 144 + 5, 98, 20)
         .build();
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 38, 200, 20, Component.translatable("selectWorld.enterName"));
      LevelSummary var1 = this.levelAccess.getSummary();
      String var2 = var1 == null ? "" : var1.getLevelName();
      this.nameEdit.setValue(var2);
      this.nameEdit.setResponder(l_param0 -> this.renameButton.active = !l_param0.trim().isEmpty());
      this.addWidget(this.nameEdit);
      Button var3 = this.addRenderableWidget(Button.builder(Component.translatable("selectWorld.edit.resetIcon"), l_param0 -> {
         this.levelAccess.getIconFile().ifPresent(l2_param0 -> FileUtils.deleteQuietly(l2_param0.toFile()));
         l_param0.active = false;
      }).bounds(this.width / 2 - 100, this.height / 4 + 0 + 5, 200, 20).build());
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("selectWorld.edit.openFolder"),
               l_param0 -> Util.getPlatform().openFile(this.levelAccess.getLevelPath(LevelResource.ROOT).toFile())
            )
            .bounds(this.width / 2 - 100, this.height / 4 + 24 + 5, 200, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("selectWorld.edit.backup"), l_param0 -> {
         boolean l_var2 = makeBackupAndShowToast(this.levelAccess);
         this.callback.accept(!l_var2);
      }).bounds(this.width / 2 - 100, this.height / 4 + 48 + 5, 200, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("selectWorld.edit.backupFolder"), l_param0 -> {
         LevelStorageSource l_var2 = this.minecraft.getLevelSource();
         Path l_var3 = l_var2.getBackupPath();

         try {
            FileUtil.createDirectoriesSafe(l_var3);
         } catch (IOException l_var4) {
            throw new RuntimeException(l_var4);
         }

         Util.getPlatform().openFile(l_var3.toFile());
      }).bounds(this.width / 2 - 100, this.height / 4 + 72 + 5, 200, 20).build());
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("selectWorld.edit.optimize"),
               l_param0 -> this.minecraft.setScreen(new BackupConfirmScreen(this, (l2_param0, l2_param1) -> {
                  if (l2_param0) {
                     makeBackupAndShowToast(this.levelAccess);
                  }

                  this.minecraft
                     .setScreen(OptimizeWorldScreen.create(this.minecraft, this.callback, this.minecraft.getFixerUpper(), this.levelAccess, l2_param1));
               }, Component.translatable("optimizeWorld.confirm.title"), Component.translatable("optimizeWorld.confirm.description"), true))
            )
            .bounds(this.width / 2 - 100, this.height / 4 + 96 + 5, 200, 20)
            .build()
      );
      this.addRenderableWidget(this.renameButton);
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.callback.accept(false))
            .bounds(this.width / 2 + 2, this.height / 4 + 144 + 5, 98, 20)
            .build()
      );
      var3.active = this.levelAccess.getIconFile().filter(l_param0 -> Files.isRegularFile(l_param0)).isPresent();
      this.setInitialFocus(this.nameEdit);
   }

   @Override
   public void resize(Minecraft param0, int param1, int param2) {
      String var4 = this.nameEdit.getValue();
      this.init(param0, param1, param2);
      this.nameEdit.setValue(var4);
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   private void onRename() {
      try {
         this.levelAccess.renameLevel(this.nameEdit.getValue().trim());
         this.callback.accept(true);
      } catch (IOException var1) {
         LOGGER.error("Failed to access world '{}'", this.levelAccess.getLevelId(), var1);
         SystemToast.onWorldAccessFailure(this.minecraft, this.levelAccess.getLevelId());
         this.callback.accept(true);
      }
   }

   public static void makeBackupAndShowToast(LevelStorageSource param0, String param1) {
      boolean var2 = false;

      try (LevelStorageSource.LevelStorageAccess var3 = param0.createAccess(param1)) {
         var2 = true;
         makeBackupAndShowToast(var3);
      } catch (IOException var3) {
         if (!var2) {
            SystemToast.onWorldAccessFailure(Minecraft.getInstance(), param1);
         }

         LOGGER.warn("Failed to create backup of level {}", param1, var3);
      }
   }

   public static boolean makeBackupAndShowToast(LevelStorageSource.LevelStorageAccess param0) {
      long var1 = 0L;
      IOException var3 = null;

      try {
         var1 = param0.makeWorldBackup();
      } catch (IOException var4) {
         var3 = var4;
      }

      if (var3 != null) {
         Component var4 = Component.translatable("selectWorld.edit.backupFailed");
         Component var5 = Component.literal(var3.getMessage());
         Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, var4, var5));
         return false;
      } else {
         Component var4 = Component.translatable("selectWorld.edit.backupCreated", param0.getLevelId());
         Component var5 = Component.translatable("selectWorld.edit.backupSize", Mth.ceil(var1 / 1048576.0));
         Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, var4, var5));
         return true;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 15, 16777215);
      drawString(param0, this.font, NAME_LABEL, this.width / 2 - 100, 24, 10526880);
      this.nameEdit.render(param0, param1, param2, param3);
      super.render(param0, param1, param2, param3);
   }
}
