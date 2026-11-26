package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;

public class JfrCommand {
   private static final SimpleCommandExceptionType START_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.jfr.start.failed"));
   private static final DynamicCommandExceptionType DUMP_FAILED = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.jfr.dump.failed", param0)
   );

   private JfrCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("jfr")
            .requires(l_param0 -> l_param0.hasPermission(4))
            .then(Commands.literal("start").executes(l_param0 -> startJfr(l_param0.getSource())))
            .then(Commands.literal("stop").executes(l_param0 -> stopJfr(l_param0.getSource())))
      );
   }

   private static int startJfr(CommandSourceStack param0) throws CommandSyntaxException {
      Environment var1 = Environment.from(param0.getServer());
      if (!JvmProfiler.INSTANCE.start(var1)) {
         throw START_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.jfr.started"), false);
      return 1;
   }

   private static int stopJfr(CommandSourceStack param0) throws CommandSyntaxException {
      try {
         Path var1 = Paths.get(".").relativize(JvmProfiler.INSTANCE.stop().normalize());
         Path var2 = param0.getServer().isPublished() && !SharedConstants.IS_RUNNING_IN_IDE ? var1 : var1.toAbsolutePath();
         Component var3 = Component.literal(var1.toString())
            .withStyle(ChatFormatting.UNDERLINE)
            .withStyle(
               l_param1 -> l_param1.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, var2.toString()))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
            );
         param0.sendSuccess(Component.translatable("commands.jfr.stopped", var3), false);
         return 1;
      } catch (Throwable var1) {
         throw DUMP_FAILED.create(var1.getMessage());
      }
   }
}
