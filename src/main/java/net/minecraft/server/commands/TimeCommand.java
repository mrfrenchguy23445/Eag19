package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class TimeCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("time")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("set")
                  .then(Commands.literal("day").executes(l_param0 -> setTime(l_param0.getSource(), 1000)))
                  .then(Commands.literal("noon").executes(l_param0 -> setTime(l_param0.getSource(), 6000)))
                  .then(Commands.literal("night").executes(l_param0 -> setTime(l_param0.getSource(), 13000)))
                  .then(Commands.literal("midnight").executes(l_param0 -> setTime(l_param0.getSource(), 18000)))
                  .then(
                     Commands.argument("time", TimeArgument.time())
                        .executes(l_param0 -> setTime(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "time")))
                  )
            )
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("time", TimeArgument.time())
                        .executes(l_param0 -> addTime(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "time")))
                  )
            )
            .then(
               Commands.literal("query")
                  .then(Commands.literal("daytime").executes(l_param0 -> queryTime(l_param0.getSource(), getDayTime(l_param0.getSource().getLevel()))))
                  .then(
                     Commands.literal("gametime")
                        .executes(l_param0 -> queryTime(l_param0.getSource(), (int)(l_param0.getSource().getLevel().getGameTime() % 2147483647L)))
                  )
                  .then(
                     Commands.literal("day")
                        .executes(l_param0 -> queryTime(l_param0.getSource(), (int)(l_param0.getSource().getLevel().getDayTime() / 24000L % 2147483647L)))
                  )
            )
      );
   }

   private static int getDayTime(ServerLevel param0) {
      return (int)(param0.getDayTime() % 24000L);
   }

   private static int queryTime(CommandSourceStack param0, int param1) {
      param0.sendSuccess(Component.translatable("commands.time.query", param1), false);
      return param1;
   }

   public static int setTime(CommandSourceStack param0, int param1) {
      for (ServerLevel var3 : param0.getServer().getAllLevels()) {
         var3.setDayTime(param1);
      }

      param0.sendSuccess(Component.translatable("commands.time.set", param1), true);
      return getDayTime(param0.getLevel());
   }

   public static int addTime(CommandSourceStack param0, int param1) {
      for (ServerLevel var3 : param0.getServer().getAllLevels()) {
         var3.setDayTime(var3.getDayTime() + param1);
      }

      int var2 = getDayTime(param0.getLevel());
      param0.sendSuccess(Component.translatable("commands.time.set", var2), true);
      return var2;
   }
}
