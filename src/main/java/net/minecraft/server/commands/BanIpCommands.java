package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

public class BanIpCommands {
   private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(Component.translatable("commands.banip.invalid"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.banip.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("ban-ip")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.argument("target", StringArgumentType.word())
                  .executes(l_param0 -> banIpOrName(l_param0.getSource(), StringArgumentType.getString(l_param0, "target"), null))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           l_param0 -> banIpOrName(
                              l_param0.getSource(), StringArgumentType.getString(l_param0, "target"), MessageArgument.getMessage(l_param0, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banIpOrName(CommandSourceStack param0, String param1, @Nullable Component param2) throws CommandSyntaxException {
      if (InetAddresses.isInetAddress(param1)) {
         return banIp(param0, param1, param2);
      } else {
         ServerPlayer var3 = param0.getServer().getPlayerList().getPlayerByName(param1);
         if (var3 != null) {
            return banIp(param0, var3.getIpAddress(), param2);
         } else {
            throw ERROR_INVALID_IP.create();
         }
      }
   }

   private static int banIp(CommandSourceStack param0, String param1, @Nullable Component param2) throws CommandSyntaxException {
      IpBanList var3 = param0.getServer().getPlayerList().getIpBans();
      if (var3.isBanned(param1)) {
         throw ERROR_ALREADY_BANNED.create();
      }

      List<ServerPlayer> var4 = param0.getServer().getPlayerList().getPlayersWithAddress(param1);
      IpBanListEntry var5 = new IpBanListEntry(param1, null, param0.getTextName(), null, param2 == null ? null : param2.getString());
      var3.add(var5);
      param0.sendSuccess(Component.translatable("commands.banip.success", param1, var5.getReason()), true);
      if (!var4.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.banip.info", var4.size(), EntitySelector.joinNames(var4)), true);
      }

      for (ServerPlayer var7 : var4) {
         var7.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
      }

      return var4.size();
   }
}
