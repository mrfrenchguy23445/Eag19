package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KickCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("kick")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .executes(
                     l_param0 -> kickPlayers(
                        l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), Component.translatable("multiplayer.disconnect.kicked")
                     )
                  )
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           l_param0 -> kickPlayers(
                              l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), MessageArgument.getMessage(l_param0, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int kickPlayers(CommandSourceStack param0, Collection<ServerPlayer> param1, Component param2) {
      for (ServerPlayer var4 : param1) {
         var4.connection.disconnect(param2);
         param0.sendSuccess(Component.translatable("commands.kick.success", var4.getDisplayName(), param2), true);
      }

      return param1.size();
   }
}
