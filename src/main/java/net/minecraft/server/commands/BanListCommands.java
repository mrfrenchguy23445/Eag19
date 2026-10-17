package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.PlayerList;

public class BanListCommands {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("banlist")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .executes(l_param0 -> {
               PlayerList l_var1 = l_param0.getSource().getServer().getPlayerList();
               return showList(l_param0.getSource(), Lists.newArrayList(Iterables.concat(l_var1.getBans().getEntries(), l_var1.getIpBans().getEntries())));
            })
            .then(
               Commands.literal("ips")
                  .executes(l_param0 -> showList(l_param0.getSource(), l_param0.getSource().getServer().getPlayerList().getIpBans().getEntries()))
            )
            .then(
               Commands.literal("players")
                  .executes(l_param0 -> showList(l_param0.getSource(), l_param0.getSource().getServer().getPlayerList().getBans().getEntries()))
            )
      );
   }

   private static int showList(CommandSourceStack param0, Collection<? extends BanListEntry<?>> param1) {
      if (param1.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.banlist.none"), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.banlist.list", param1.size()), false);

         for (BanListEntry<?> var3 : param1) {
            param0.sendSuccess(Component.translatable("commands.banlist.entry", var3.getDisplayName(), var3.getSource(), var3.getReason()), false);
         }
      }

      return param1.size();
   }
}
