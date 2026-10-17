package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class KillCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("kill")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(l_param0 -> kill(l_param0.getSource(), ImmutableList.of(l_param0.getSource().getEntityOrException())))
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .executes(l_param0 -> kill(l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets")))
            )
      );
   }

   private static int kill(CommandSourceStack param0, Collection<? extends Entity> param1) {
      for (Entity var3 : param1) {
         var3.kill();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.kill.success.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.kill.success.multiple", param1.size()), true);
      }

      return param1.size();
   }
}
