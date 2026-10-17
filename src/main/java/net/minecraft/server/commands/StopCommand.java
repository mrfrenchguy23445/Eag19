package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StopCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(Commands.literal("stop").requires(l_param0 -> l_param0.hasPermission(4)).executes(l_param0 -> {
         l_param0.getSource().sendSuccess(Component.translatable("commands.stop.stopping"), true);
         l_param0.getSource().getServer().halt(false);
         return 1;
      }));
   }
}
