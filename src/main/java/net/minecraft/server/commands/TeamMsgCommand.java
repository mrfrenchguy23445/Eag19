package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
   private static final Style SUGGEST_STYLE = Style.EMPTY
      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.type.team.hover")))
      .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
   private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralCommandNode<CommandSourceStack> var1 = param0.register(
         Commands.literal("teammsg")
            .then(
               Commands.argument("message", MessageArgument.message())
                  .executes(
                     l_param0 -> {
                        CommandSourceStack l_var1 = l_param0.getSource();
                        Entity l_var2 = l_var1.getEntityOrException();
                        PlayerTeam l_var3 = (PlayerTeam)l_var2.getTeam();
                        if (l_var3 == null) {
                           throw ERROR_NOT_ON_TEAM.create();
                        }

                        List<ServerPlayer> l_var4 = l_var1.getServer()
                           .getPlayerList()
                           .getPlayers()
                           .stream()
                           .filter(l2_param2 -> l2_param2 == l_var2 || l2_param2.getTeam() == l_var3)
                           .toList();
                        if (!l_var4.isEmpty()) {
                           MessageArgument.resolveChatMessage(l_param0, "message", l2_param4 -> sendMessage(l_var1, l_var2, l_var3, l_var4, l2_param4));
                        }

                        return l_var4.size();
                     }
                  )
            )
      );
      param0.register(Commands.literal("tm").redirect(var1));
   }

   private static void sendMessage(CommandSourceStack param0, Entity param1, PlayerTeam param2, List<ServerPlayer> param3, PlayerChatMessage param4) {
      Component var5 = param2.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
      ChatType.Bound var6 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, param0).withTargetName(var5);
      ChatType.Bound var7 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, param0).withTargetName(var5);
      OutgoingChatMessage var8 = OutgoingChatMessage.create(param4);
      boolean var9 = false;

      for (ServerPlayer var11 : param3) {
         ChatType.Bound var12 = var11 == param1 ? var7 : var6;
         boolean var13 = param0.shouldFilterMessageTo(var11);
         var11.sendChatMessage(var8, var13, var12);
         var9 |= var13 && param4.isFullyFiltered();
      }

      if (var9) {
         param0.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
