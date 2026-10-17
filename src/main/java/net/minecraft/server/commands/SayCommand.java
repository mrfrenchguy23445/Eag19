package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class SayCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("say")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(Commands.argument("message", MessageArgument.message()).executes(l_param0 -> {
               MessageArgument.resolveChatMessage(l_param0, "message", l2_param1 -> {
                  CommandSourceStack l2_var2 = l_param0.getSource();
                  PlayerList l2_var3 = l2_var2.getServer().getPlayerList();
                  l2_var3.broadcastChatMessage(l2_param1, l2_var2, ChatType.bind(ChatType.SAY_COMMAND, l2_var2));
               });
               return 1;
            }))
      );
   }
}
