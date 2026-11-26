package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanList;

public class PardonIpCommand {
   private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.invalid"));
   private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("pardon-ip")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("target", StringArgumentType.word())
                  .suggests(
                     (l_param0, l_param1) -> SharedSuggestionProvider.suggest(
                        l_param0.getSource().getServer().getPlayerList().getIpBans().getUserList(), l_param1
                     )
                  )
                  .executes(l_param0 -> unban(l_param0.getSource(), StringArgumentType.getString(l_param0, "target")))
            )
      );
   }

   private static int unban(CommandSourceStack param0, String param1) throws CommandSyntaxException {
      if (!InetAddresses.isInetAddress(param1)) {
         throw ERROR_INVALID.create();
      }

      IpBanList var2 = param0.getServer().getPlayerList().getIpBans();
      if (!var2.isBanned(param1)) {
         throw ERROR_NOT_BANNED.create();
      }

      var2.remove(param1);
      param0.sendSuccess(Component.translatable("commands.pardonip.success", param1), true);
      return 1;
   }
}
