package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

public class TellRawCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("tellraw")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     Commands.argument("message", ComponentArgument.textComponent())
                        .executes(
                           l_param0 -> {
                              int l_var1 = 0;

                              for (ServerPlayer l_var3 : EntityArgument.getPlayers(l_param0, "targets")) {
                                 l_var3.sendSystemMessage(
                                    ComponentUtils.updateForEntity(l_param0.getSource(), ComponentArgument.getComponent(l_param0, "message"), l_var3, 0), false
                                 );
                                 l_var1++;
                              }

                              return l_var1;
                           }
                        )
                  )
            )
      );
   }
}
