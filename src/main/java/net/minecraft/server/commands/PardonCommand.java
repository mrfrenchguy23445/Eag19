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
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.players.UserBanList;

public class PardonCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardon.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("pardon")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (l_param0, l_param1) -> SharedSuggestionProvider.suggest(
                        l_param0.getSource().getServer().getPlayerList().getBans().getUserList(), l_param1
                     )
                  )
                  .executes(l_param0 -> pardonPlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets")))
            )
      );
   }

   private static int pardonPlayers(CommandSourceStack param0, Collection<GameProfile> param1) throws CommandSyntaxException {
      UserBanList var2 = param0.getServer().getPlayerList().getBans();
      int var3 = 0;

      for (GameProfile var5 : param1) {
         if (var2.isBanned(var5)) {
            var2.remove(var5);
            var3++;
            param0.sendSuccess(Component.translatable("commands.pardon.success", ComponentUtils.getDisplayName(var5)), true);
         }
      }

      if (var3 == 0) {
         throw ERROR_NOT_BANNED.create();
      } else {
         return var3;
      }
   }
}
