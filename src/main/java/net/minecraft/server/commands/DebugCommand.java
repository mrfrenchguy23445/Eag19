package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ProfileResults;
import org.slf4j.Logger;

public class DebugCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.debug.notRunning"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
      Component.translatable("commands.debug.alreadyRunning")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("debug")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(Commands.literal("start").executes(l_param0 -> start(l_param0.getSource())))
            .then(Commands.literal("stop").executes(l_param0 -> stop(l_param0.getSource())))
            .then(
               Commands.literal("function")
                  .requires(l_param0 -> l_param0.hasPermission(3))
                  .then(
                     Commands.argument("name", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .executes(l_param0 -> traceFunction(l_param0.getSource(), FunctionArgument.getFunctions(l_param0, "name")))
                  )
            )
      );
   }

   private static int start(CommandSourceStack param0) throws CommandSyntaxException {
      MinecraftServer var1 = param0.getServer();
      if (var1.isTimeProfilerRunning()) {
         throw ERROR_ALREADY_RUNNING.create();
      }

      var1.startTimeProfiler();
      param0.sendSuccess(Component.translatable("commands.debug.started"), true);
      return 0;
   }

   private static int stop(CommandSourceStack param0) throws CommandSyntaxException {
      MinecraftServer var1 = param0.getServer();
      if (!var1.isTimeProfilerRunning()) {
         throw ERROR_NOT_RUNNING.create();
      }

      ProfileResults var2 = var1.stopTimeProfiler();
      double var3 = (double)var2.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
      double var5 = var2.getTickDuration() / var3;
      param0.sendSuccess(
         Component.translatable(
            "commands.debug.stopped", String.format(Locale.ROOT, "%.2f", var3), var2.getTickDuration(), String.format(Locale.ROOT, "%.2f", var5)
         ),
         true
      );
      return (int)var5;
   }

   private static int traceFunction(CommandSourceStack param0, Collection<CommandFunction> param1) {
      int var2 = 0;
      MinecraftServer var3 = param0.getServer();
      String var4 = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";

      try {
         Path var5 = var3.getFile("debug").toPath();
         Files.createDirectories(var5);

         try (Writer var6 = Files.newBufferedWriter(var5.resolve(var4), StandardCharsets.UTF_8)) {
            PrintWriter var7 = new PrintWriter(var6);

            for (CommandFunction var9 : param1) {
               var7.println(var9.getId());
               DebugCommand.Tracer var10 = new DebugCommand.Tracer(var7);
               var2 += param0.getServer().getFunctions().execute(var9, param0.withSource(var10).withMaximumPermission(2), var10);
            }
         }
      } catch (UncheckedIOException | IOException var5) {
         LOGGER.warn("Tracing failed", var5);
         param0.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.debug.function.success.single", var2, param1.iterator().next().getId(), var4), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.debug.function.success.multiple", var2, param1.size(), var4), true);
      }

      return var2;
   }

   static class Tracer implements ServerFunctionManager.TraceCallbacks, CommandSource {
      public static final int INDENT_OFFSET = 1;
      private final PrintWriter output;
      private int lastIndent;
      private boolean waitingForResult;

      Tracer(PrintWriter param0) {
         this.output = param0;
      }

      private void indentAndSave(int param0) {
         this.printIndent(param0);
         this.lastIndent = param0;
      }

      private void printIndent(int param0) {
         for (int var2 = 0; var2 < param0 + 1; var2++) {
            this.output.write("    ");
         }
      }

      private void newLine() {
         if (this.waitingForResult) {
            this.output.println();
            this.waitingForResult = false;
         }
      }

      @Override
      public void onCommand(int param0, String param1) {
         this.newLine();
         this.indentAndSave(param0);
         this.output.print("[C] ");
         this.output.print(param1);
         this.waitingForResult = true;
      }

      @Override
      public void onReturn(int param0, String param1, int param2) {
         if (this.waitingForResult) {
            this.output.print(" -> ");
            this.output.println(param2);
            this.waitingForResult = false;
         } else {
            this.indentAndSave(param0);
            this.output.print("[R = ");
            this.output.print(param2);
            this.output.print("] ");
            this.output.println(param1);
         }
      }

      @Override
      public void onCall(int param0, ResourceLocation param1, int param2) {
         this.newLine();
         this.indentAndSave(param0);
         this.output.print("[F] ");
         this.output.print(param1);
         this.output.print(" size=");
         this.output.println(param2);
      }

      @Override
      public void onError(int param0, String param1) {
         this.newLine();
         this.indentAndSave(param0 + 1);
         this.output.print("[E] ");
         this.output.print(param1);
      }

      @Override
      public void sendSystemMessage(Component param0) {
         this.newLine();
         this.printIndent(this.lastIndent + 1);
         this.output.print("[M] ");
         this.output.println(param0.getString());
      }

      @Override
      public boolean acceptsSuccess() {
         return true;
      }

      @Override
      public boolean acceptsFailure() {
         return true;
      }

      @Override
      public boolean shouldInformAdmins() {
         return false;
      }

      @Override
      public boolean alwaysAccepts() {
         return true;
      }
   }
}
