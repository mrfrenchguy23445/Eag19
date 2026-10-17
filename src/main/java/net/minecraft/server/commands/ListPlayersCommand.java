package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("list")
            .executes(l_param0 -> listPlayers(l_param0.getSource()))
            .then(Commands.literal("uuids").executes(l_param0 -> listPlayersWithUuids(l_param0.getSource())))
      );
   }

   private static int listPlayers(CommandSourceStack param0) {
      return format(param0, Player::getDisplayName);
   }

   private static int listPlayersWithUuids(CommandSourceStack param0) {
      return format(param0, l_param0 -> Component.translatable("commands.list.nameAndId", l_param0.getName(), l_param0.getGameProfile().getId()));
   }

   private static int format(CommandSourceStack param0, Function<ServerPlayer, Component> param1) {
      PlayerList var2 = param0.getServer().getPlayerList();
      List<ServerPlayer> var3 = var2.getPlayers();
      Component var4 = ComponentUtils.formatList(var3, param1);
      param0.sendSuccess(Component.translatable("commands.list.players", var3.size(), var2.getMaxPlayers(), var4), false);
      return var3.size();
   }
}
