package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

public class BanPlayerCommands {
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.ban.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("ban")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("targets", GameProfileArgument.gameProfile())
                  .executes(l_param0 -> banPlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets"), null))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           l_param0 -> banPlayers(
                              l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets"), MessageArgument.getMessage(l_param0, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banPlayers(CommandSourceStack param0, Collection<GameProfile> param1, @Nullable Component param2) throws CommandSyntaxException {
      UserBanList var3 = param0.getServer().getPlayerList().getBans();
      int var4 = 0;

      for (GameProfile var6 : param1) {
         if (!var3.isBanned(var6)) {
            UserBanListEntry var7 = new UserBanListEntry(var6, null, param0.getTextName(), null, param2 == null ? null : param2.getString());
            var3.add(var7);
            var4++;
            param0.sendSuccess(Component.translatable("commands.ban.success", ComponentUtils.getDisplayName(var6), var7.getReason()), true);
            ServerPlayer var8 = param0.getServer().getPlayerList().getPlayer(var6.getId());
            if (var8 != null) {
               var8.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      if (var4 == 0) {
         throw ERROR_ALREADY_BANNED.create();
      } else {
         return var4;
      }
   }
}
