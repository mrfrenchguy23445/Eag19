package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.packs.PackResources;
import org.slf4j.Logger;

public class ResourceLoadStateTracker {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private ResourceLoadStateTracker.ReloadState reloadState;
   private int reloadCount;

   public void startReload(ResourceLoadStateTracker.ReloadReason param0, List<PackResources> param1) {
      this.reloadCount++;
      if (this.reloadState != null && !this.reloadState.finished) {
         LOGGER.warn("Reload already ongoing, replacing");
      }

      this.reloadState = new ResourceLoadStateTracker.ReloadState(param0, param1.stream().map(PackResources::packId).collect(ImmutableList.toImmutableList()));
   }

   public void startRecovery(Throwable param0) {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to signal reload recovery, but nothing was started");
         this.reloadState = new ResourceLoadStateTracker.ReloadState(ResourceLoadStateTracker.ReloadReason.UNKNOWN, ImmutableList.of());
      }

      this.reloadState.recoveryReloadInfo = new ResourceLoadStateTracker.RecoveryInfo(param0);
   }

   public void finishReload() {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to finish reload, but nothing was started");
      } else {
         this.reloadState.finished = true;
      }
   }

   public void fillCrashReport(CrashReport param0) {
      CrashReportCategory var2 = param0.addCategory("Last reload");
      var2.setDetail("Reload number", this.reloadCount);
      if (this.reloadState != null) {
         this.reloadState.fillCrashInfo(var2);
      }
   }

   static class RecoveryInfo {
      private final Throwable error;

      RecoveryInfo(Throwable param0) {
         this.error = param0;
      }

      public void fillCrashInfo(CrashReportCategory param0) {
         param0.setDetail("Recovery", "Yes");
         param0.setDetail("Recovery reason", () -> {
            StringWriter l_var1 = new StringWriter();
            this.error.printStackTrace(new PrintWriter(l_var1));
            return l_var1.toString();
         });
      }
   }

   public enum ReloadReason {
      INITIAL("initial"),
      MANUAL("manual"),
      UNKNOWN("unknown");

      final String name;

      ReloadReason(String param2) {
         this.name = param2;
      }
   }

   static class ReloadState {
      private final ResourceLoadStateTracker.ReloadReason reloadReason;
      private final List<String> packs;
      @Nullable
      ResourceLoadStateTracker.RecoveryInfo recoveryReloadInfo;
      boolean finished;

      ReloadState(ResourceLoadStateTracker.ReloadReason param0, List<String> param1) {
         this.reloadReason = param0;
         this.packs = param1;
      }

      public void fillCrashInfo(CrashReportCategory param0) {
         param0.setDetail("Reload reason", this.reloadReason.name);
         param0.setDetail("Finished", this.finished ? "Yes" : "No");
         param0.setDetail("Packs", () -> String.join(", ", this.packs));
         if (this.recoveryReloadInfo != null) {
            this.recoveryReloadInfo.fillCrashInfo(param0);
         }
      }
   }
}
