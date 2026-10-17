package net.minecraft.server.dedicated;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class ServerWatchdog implements Runnable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final long MAX_SHUTDOWN_TIME = 10000L;
   private static final int SHUTDOWN_STATUS = 1;
   private final DedicatedServer server;
   private final long maxTickTime;

   public ServerWatchdog(DedicatedServer param0) {
      this.server = param0;
      this.maxTickTime = param0.getMaxTickLength();
   }

   @Override
   public void run() {
      while (this.server.isRunning()) {
         long var1 = this.server.getNextTickTime();
         long var3 = Util.getMillis();
         long var5 = var3 - var1;
         if (var5 > this.maxTickTime) {
            LOGGER.error(
               LogUtils.FATAL_MARKER,
               "A single server tick took {} seconds (should be max {})",
               String.format(Locale.ROOT, "%.2f", (float)var5 / 1000.0F),
               String.format(Locale.ROOT, "%.2f", 0.05F)
            );
            LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed, server will forcibly shutdown.");
            ThreadMXBean var7 = ManagementFactory.getThreadMXBean();
            ThreadInfo[] var8 = var7.dumpAllThreads(true, true);
            StringBuilder var9 = new StringBuilder();
            Error var10 = new Error("Watchdog");

            for (ThreadInfo var14 : var8) {
               if (var14.getThreadId() == this.server.getRunningThread().getId()) {
                  var10.setStackTrace(var14.getStackTrace());
               }

               var9.append(var14);
               var9.append("\n");
            }

            CrashReport var11 = new CrashReport("Watching Server", var10);
            this.server.fillSystemReport(var11.getSystemReport());
            CrashReportCategory var12 = var11.addCategory("Thread Dump");
            var12.setDetail("Threads", var9);
            CrashReportCategory var13 = var11.addCategory("Performance stats");
            var13.setDetail("Random tick rate", () -> this.server.getWorldData().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).toString());
            var13.setDetail(
               "Level stats",
               () -> Streams.stream(this.server.getAllLevels())
                  .map(l2_param0 -> l2_param0.dimension() + ": " + l2_param0.getWatchdogStats())
                  .collect(Collectors.joining(",\n"))
            );
            Bootstrap.realStdoutPrintln("Crash report:\n" + var11.getFriendlyReport());
            File var14 = new File(new File(this.server.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            if (var11.saveToFile(var14)) {
               LOGGER.error("This crash report has been saved to: {}", var14.getAbsolutePath());
            } else {
               LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.exit();
         }

         try {
            Thread.sleep(var1 + this.maxTickTime - var3);
         } catch (InterruptedException var15) {
         }
      }
   }

   private void exit() {
      try {
         Timer var1 = new Timer();
         var1.schedule(new TimerTask() {
            @Override
            public void run() {
               Runtime.getRuntime().halt(1);
            }
         }, 10000L);
         System.exit(1);
      } catch (Throwable var1) {
         Runtime.getRuntime().halt(1);
      }
   }
}
