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

public class OpCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("op")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .suggests(
                     (l_param0, l_param1) -> {
                        PlayerList l_var2 = l_param0.getSource().getServer().getPlayerList();
                        return SharedSuggestionProvider.suggest(
                           l_var2.getPlayers()
                              .stream()
                              .filter(l2_param1 -> !l_var2.isOp(l2_param1.getGameProfile()))
                              .map(l2_param0 -> l2_param0.getGameProfile().getName()),
                           l_param1
                        );
                     }
                  )
                  .executes(l_param0 -> opPlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets")))
            )
      );
   }

   private static int opPlayers(CommandSourceStack param0, Collection<GameProfile> param1) throws CommandSyntaxException {
      PlayerList var2 = param0.getServer().getPlayerList();
      int var3 = 0;

      for (GameProfile var5 : param1) {
         if (!var2.isOp(var5)) {
            var2.op(var5);
            var3++;
            param0.sendSuccess(Component.translatable("commands.op.success", param1.iterator().next().getName()), true);
         }
      }

      if (var3 == 0) {
         throw ERROR_ALREADY_OP.create();
      } else {
         return var3;
      }
   }
}
