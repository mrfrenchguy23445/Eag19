package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;

public class RealmsUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ReentrantLock UPLOAD_LOCK = new ReentrantLock();
   private static final int BAR_WIDTH = 200;
   private static final int BAR_TOP = 80;
   private static final int BAR_BOTTOM = 95;
   private static final int BAR_BORDER = 1;
   private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
   private static final Component VERIFYING_TEXT = Component.translatable("mco.upload.verifying");
   private final RealmsResetWorldScreen lastScreen;
   private final LevelSummary selectedLevel;
   private final long worldId;
   private final int slotId;
   private final UploadStatus uploadStatus;
   private final RateLimiter narrationRateLimiter;
   @Nullable
   private volatile Component[] errorMessage;
   private volatile Component status = Component.translatable("mco.upload.preparing");
   private volatile String progress;
   private volatile boolean cancelled;
   private volatile boolean uploadFinished;
   private volatile boolean showDots = true;
   private volatile boolean uploadStarted;
   private Button backButton;
   private Button cancelButton;
   private int tickCount;
   @Nullable
   private Long previousWrittenBytes;
   @Nullable
   private Long previousTimeSnapshot;
   private long bytesPersSecond;
   private final Runnable callback;

   public RealmsUploadScreen(long param0, int param2, RealmsResetWorldScreen param3, LevelSummary param4, Runnable param5) {
      super(GameNarrator.NO_TITLE);
      this.worldId = param0;
      this.slotId = param2;
      this.lastScreen = param3;
      this.selectedLevel = param4;
      this.uploadStatus = new UploadStatus();
      this.narrationRateLimiter = RateLimiter.create(0.1F);
      this.callback = param5;
   }

   @Override
   public void init() {
      this.backButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, l_param0 -> this.onBack()).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build()
      );
      this.backButton.visible = false;
      this.cancelButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> this.onCancel()).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build()
      );
      if (!this.uploadStarted) {
         if (this.lastScreen.slot == -1) {
            this.upload();
         } else {
            this.lastScreen.switchSlot(() -> {
               if (!this.uploadStarted) {
                  this.uploadStarted = true;
                  this.minecraft.setScreen(this);
                  this.upload();
               }
            });
         }
      }
   }

   private void onBack() {
      this.callback.run();
   }

   private void onCancel() {
      this.cancelled = true;
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256) {
         if (this.showDots) {
            this.onCancel();
         } else {
            this.onBack();
         }

         return true;
      } else {
         return super.keyPressed(param0, param1, param2);
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      if (!this.uploadFinished && this.uploadStatus.bytesWritten != 0L && this.uploadStatus.bytesWritten == this.uploadStatus.totalBytes) {
         this.status = VERIFYING_TEXT;
         this.cancelButton.active = false;
      }

      drawCenteredString(param0, this.font, this.status, this.width / 2, 50, 16777215);
      if (this.showDots) {
         this.drawDots(param0);
      }

      if (this.uploadStatus.bytesWritten != 0L && !this.cancelled) {
         this.drawProgressBar(param0);
         this.drawUploadSpeed(param0);
      }

      if (this.errorMessage != null) {
         for (int var5 = 0; var5 < this.errorMessage.length; var5++) {
            drawCenteredString(param0, this.font, this.errorMessage[var5], this.width / 2, 110 + 12 * var5, 16711680);
         }
      }

      super.render(param0, param1, param2, param3);
   }

   private void drawDots(PoseStack param0) {
      int var2 = this.font.width(this.status);
      this.font.draw(param0, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + var2 / 2 + 5, 50.0F, 16777215);
   }

   private void drawProgressBar(PoseStack param0) {
      double var2 = Math.min((double)this.uploadStatus.bytesWritten / this.uploadStatus.totalBytes, 1.0);
      this.progress = String.format(Locale.ROOT, "%.1f", var2 * 100.0);
      int var4 = (this.width - 200) / 2;
      int var5 = var4 + (int)Math.round(200.0 * var2);
      fill(param0, var4 - 1, 79, var5 + 1, 175, -2501934);
      fill(param0, var4, 80, var5, 95, -8355712);
      drawCenteredString(param0, this.font, this.progress + " %", this.width / 2, 84, 16777215);
   }

   private void drawUploadSpeed(PoseStack param0) {
      if (this.tickCount % 20 == 0) {
         if (this.previousWrittenBytes != null) {
            long var2 = Util.getMillis() - this.previousTimeSnapshot;
            if (var2 == 0L) {
               var2 = 1L;
            }

            this.bytesPersSecond = 1000L * (this.uploadStatus.bytesWritten - this.previousWrittenBytes) / var2;
            this.drawUploadSpeed0(param0, this.bytesPersSecond);
         }

         this.previousWrittenBytes = this.uploadStatus.bytesWritten;
         this.previousTimeSnapshot = Util.getMillis();
      } else {
         this.drawUploadSpeed0(param0, this.bytesPersSecond);
      }
   }

   private void drawUploadSpeed0(PoseStack param0, long param1) {
      if (param1 > 0L) {
         int var4 = this.font.width(this.progress);
         String var5 = "(" + Unit.humanReadable(param1) + "/s)";
         this.font.draw(param0, var5, this.width / 2 + var4 / 2 + 15, 84.0F, 16777215);
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.tickCount++;
      if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
         Component var1 = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().sayNow(var1);
      }
   }

   private Component createProgressNarrationMessage() {
      List<Component> var1 = Lists.newArrayList();
      var1.add(this.status);
      if (this.progress != null) {
         var1.add(Component.literal(this.progress + "%"));
      }

      if (this.errorMessage != null) {
         var1.addAll(Arrays.asList(this.errorMessage));
      }

      return CommonComponents.joinLines(var1);
   }

   private void upload() {
      // Port note (no Mojang account; no FileChannel): refused.
      //
      // Two independent reasons, either of which is enough on its own.
      //
      // The first is that this uploads to Mojang's Realms service: the first thing the body does
      // is RealmsClient.create(), and the whole rest of it is a signed upload of a world archive
      // to an account. This port has no Mojang account and no Realms service to talk to, so there
      // is nothing to authenticate as. That is the same conclusion Ping.pingAllRegions reached
      // for the same package.
      //
      // The second is narrower and is the one the linker cares about. The body tars the world
      // directory up, and org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
      // needs java.nio.channels.FileChannel for its block layout --
      // FixedLengthBlockOutputStream's constructor calls FileOutputStream.getChannel() to learn
      // how much padding it may skip. The classlib has no FileChannel and no getChannel() on
      // either file stream, and both are classes it owns, so neither can be shimmed. Commons
      // Compress is a jar, so it cannot be patched either. Vendoring TarArchiveOutputStream and
      // its dependencies to keep a code path that has no server to upload to was the wrong trade.
      //
      // Reported through the screen's own failure path -- the same setErrorMessage and the same
      // mco.upload.failed key the IOException and RealmsServiceException handlers use -- rather
      // than by throwing. init() calls this unconditionally when the screen opens, so throwing
      // would fire on every visit, and the screen would be left showing a Cancel button for an
      // upload that is never going to happen.
      this.uploadStarted = true;
      this.setErrorMessage(
         Component.translatable(
            "mco.upload.failed",
            "This build cannot upload to Realms: it has no Mojang account to authenticate with, and its tar writer needs a file channel that this platform does not provide."
         )
      );
      this.uploadFinished = true;
      this.showDots = false;
      this.backButton.visible = true;
      this.cancelButton.visible = false;
   }

   private void setErrorMessage(Component... param0) {
      this.errorMessage = param0;
   }

   private void uploadCancelled() {
      this.status = Component.translatable("mco.upload.cancelled");
      LOGGER.debug("Upload was cancelled");
   }

   private boolean verify(File param0) {
      return param0.length() < 5368709120L;
   }

   private File tarGzipArchive(File param0) throws IOException {
      TarArchiveOutputStream var2 = null;

      try {
         File var3 = File.createTempFile("realms-upload-file", ".tar.gz");
         var2 = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(var3)));
         var2.setLongFileMode(3);
         this.addFileToTarGz(var2, param0.getAbsolutePath(), "world", true);
         var2.finish();
         return var3;
      } finally {
         if (var2 != null) {
            var2.close();
         }
      }
   }

   private void addFileToTarGz(TarArchiveOutputStream param0, String param1, String param2, boolean param3) throws IOException {
      if (!this.cancelled) {
         File var5 = new File(param1);
         String var6 = param3 ? param2 : param2 + var5.getName();
         TarArchiveEntry var7 = new TarArchiveEntry(var5, var6);
         param0.putArchiveEntry(var7);
         if (var5.isFile()) {
            IOUtils.copy(new FileInputStream(var5), param0);
            param0.closeArchiveEntry();
         } else {
            param0.closeArchiveEntry();
            File[] var8 = var5.listFiles();
            if (var8 != null) {
               for (File var12 : var8) {
                  this.addFileToTarGz(param0, var12.getAbsolutePath(), var6 + "/", false);
               }
            }
         }
      }
   }
}
