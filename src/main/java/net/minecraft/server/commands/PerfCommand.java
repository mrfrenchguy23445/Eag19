package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FileZipper;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.lax1dude.eaglercraft.v1_8.FileUtils;
import org.slf4j.Logger;

public class PerfCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.perf.notRunning"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
      Component.translatable("commands.perf.alreadyRunning")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("perf")
            .requires(l_param0 -> l_param0.hasPermission(4))
            .then(Commands.literal("start").executes(l_param0 -> startProfilingDedicatedServer(l_param0.getSource())))
            .then(Commands.literal("stop").executes(l_param0 -> stopProfilingDedicatedServer(l_param0.getSource())))
      );
   }

   private static int startProfilingDedicatedServer(CommandSourceStack param0) throws CommandSyntaxException {
      MinecraftServer var1 = param0.getServer();
      if (var1.isRecordingMetrics()) {
         throw ERROR_ALREADY_RUNNING.create();
      }

      Consumer<ProfileResults> var2 = l_param1 -> whenStopped(param0, l_param1);
      Consumer<Path> var3 = l_param2 -> saveResults(param0, l_param2, var1);
      var1.startRecordingMetrics(var2, var3);
      param0.sendSuccess(Component.translatable("commands.perf.started"), false);
      return 0;
   }

   private static int stopProfilingDedicatedServer(CommandSourceStack param0) throws CommandSyntaxException {
      MinecraftServer var1 = param0.getServer();
      if (!var1.isRecordingMetrics()) {
         throw ERROR_NOT_RUNNING.create();
      }

      var1.finishRecordingMetrics();
      return 0;
   }

   private static void saveResults(CommandSourceStack param0, Path param1, MinecraftServer param2) {
      String var3 = String.format(
         Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), param2.getWorldData().getLevelName(), SharedConstants.getCurrentVersion().getId()
      );

      String var4;
      try {
         var4 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, var3, ".zip");
      } catch (IOException var5) {
         param0.sendFailure(Component.translatable("commands.perf.reportFailed"));
         LOGGER.error("Failed to create report name", var5);
         return;
      }

      try (FileZipper var5 = new FileZipper(MetricsPersister.PROFILING_RESULTS_DIR.resolve(var4))) {
         var5.add(Paths.get("system.txt"), param2.fillSystemReport(new SystemReport()).toLineSeparatedString());
         var5.add(param1);
      }

      try {
         FileUtils.forceDelete(param1.toFile());
      } catch (IOException var5) {
         LOGGER.warn("Failed to delete temporary profiling file {}", param1, var5);
      }

      param0.sendSuccess(Component.translatable("commands.perf.reportSaved", var4), false);
   }

   private static void whenStopped(CommandSourceStack param0, ProfileResults param1) {
      if (param1 != EmptyProfileResults.EMPTY) {
         int var2 = param1.getTickDuration();
         double var3 = (double)param1.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
         param0.sendSuccess(
            Component.translatable("commands.perf.stopped", String.format(Locale.ROOT, "%.2f", var3), var2, String.format(Locale.ROOT, "%.2f", var2 / var3)),
            false
         );
      }
   }
}
