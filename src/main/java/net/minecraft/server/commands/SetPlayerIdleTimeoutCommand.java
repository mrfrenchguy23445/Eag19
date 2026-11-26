package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SetPlayerIdleTimeoutCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("setidletimeout")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("minutes", IntegerArgumentType.integer(0))
                  .executes(l_param0 -> setIdleTimeout(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "minutes")))
            )
      );
   }

   private static int setIdleTimeout(CommandSourceStack param0, int param1) {
      param0.getServer().setPlayerIdleTimeout(param1);
      param0.sendSuccess(Component.translatable("commands.setidletimeout.success", param1), true);
      return param1;
   }
}
