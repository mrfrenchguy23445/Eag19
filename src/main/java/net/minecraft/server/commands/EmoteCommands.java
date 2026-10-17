package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class EmoteCommands {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(Commands.literal("me").then(Commands.argument("action", MessageArgument.message()).executes(l_param0 -> {
         MessageArgument.resolveChatMessage(l_param0, "action", l2_param1 -> {
            CommandSourceStack l2_var2 = l_param0.getSource();
            PlayerList l2_var3 = l2_var2.getServer().getPlayerList();
            l2_var3.broadcastChatMessage(l2_param1, l2_var2, ChatType.bind(ChatType.EMOTE_COMMAND, l2_var2));
         });
         return 1;
      })));
   }
}
