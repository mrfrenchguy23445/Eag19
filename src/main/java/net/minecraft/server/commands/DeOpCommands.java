package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

public class DeOpCommands {
   private static final SimpleCommandExceptionType ERROR_NOT_OP = new SimpleCommandExceptionType(Component.translatable("commands.deop.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("deop")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests((l_param0, l_param1) -> SharedSuggestionProvider.suggest(l_param0.getSource().getServer().getPlayerList().getOpNames(), l_param1))
                  .executes(l_param0 -> deopPlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets")))
            )
      );
   }

   private static int deopPlayers(CommandSourceStack param0, Collection<GameProfile> param1) throws CommandSyntaxException {
      PlayerList var2 = param0.getServer().getPlayerList();
      int var3 = 0;

      for (GameProfile var5 : param1) {
         if (var2.isOp(var5)) {
            var2.deop(var5);
            var3++;
            param0.sendSuccess(Component.translatable("commands.deop.success", param1.iterator().next().getName()), true);
         }
      }

      if (var3 == 0) {
         throw ERROR_NOT_OP.create();
      }

      param0.getServer().kickUnlistedPlayers(param0);
      return var3;
   }
}
