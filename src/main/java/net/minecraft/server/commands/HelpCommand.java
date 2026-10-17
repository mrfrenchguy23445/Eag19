package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class HelpCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.help.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("help")
            .executes(l_param1 -> {
               Map<CommandNode<CommandSourceStack>, String> l_var2 = param0.getSmartUsage(param0.getRoot(), l_param1.getSource());

               for (String l_var4 : l_var2.values()) {
                  l_param1.getSource().sendSuccess(Component.literal("/" + l_var4), false);
               }

               return l_var2.size();
            })
            .then(
               Commands.argument("command", StringArgumentType.greedyString())
                  .executes(
                     l_param1 -> {
                        ParseResults<CommandSourceStack> l_var2 = param0.parse(StringArgumentType.getString(l_param1, "command"), l_param1.getSource());
                        if (l_var2.getContext().getNodes().isEmpty()) {
                           throw ERROR_FAILED.create();
                        }

                        Map<CommandNode<CommandSourceStack>, String> l_var3 = param0.getSmartUsage(
                           Iterables.getLast(l_var2.getContext().getNodes()).getNode(), l_param1.getSource()
                        );

                        for (String l_var5 : l_var3.values()) {
                           l_param1.getSource().sendSuccess(Component.literal("/" + l_var2.getReader().getString() + " " + l_var5), false);
                        }

                        return l_var3.size();
                     }
                  )
            )
      );
   }
}
