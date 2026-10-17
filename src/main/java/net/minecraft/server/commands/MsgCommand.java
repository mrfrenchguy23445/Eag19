package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class MsgCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralCommandNode<CommandSourceStack> var1 = param0.register(
         Commands.literal("msg")
            .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes(l_param0 -> {
               Collection<ServerPlayer> l_var1 = EntityArgument.getPlayers(l_param0, "targets");
               if (!l_var1.isEmpty()) {
                  MessageArgument.resolveChatMessage(l_param0, "message", l2_param2 -> sendMessage(l_param0.getSource(), l_var1, l2_param2));
               }

               return l_var1.size();
            })))
      );
      param0.register(Commands.literal("tell").redirect(var1));
      param0.register(Commands.literal("w").redirect(var1));
   }

   private static void sendMessage(CommandSourceStack param0, Collection<ServerPlayer> param1, PlayerChatMessage param2) {
      ChatType.Bound var3 = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, param0);
      OutgoingChatMessage var4 = OutgoingChatMessage.create(param2);
      boolean var5 = false;

      for (ServerPlayer var7 : param1) {
         ChatType.Bound var8 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, param0).withTargetName(var7.getDisplayName());
         param0.sendChatMessage(var4, false, var8);
         boolean var9 = param0.shouldFilterMessageTo(var7);
         var7.sendChatMessage(var4, var9, var3);
         var5 |= var9 && param2.isFullyFiltered();
      }

      if (var5) {
         param0.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
