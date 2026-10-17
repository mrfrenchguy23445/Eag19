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
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;

public class WhitelistCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOn")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOff")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.add.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.remove.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("whitelist")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(Commands.literal("on").executes(l_param0 -> enableWhitelist(l_param0.getSource())))
            .then(Commands.literal("off").executes(l_param0 -> disableWhitelist(l_param0.getSource())))
            .then(Commands.literal("list").executes(l_param0 -> showList(l_param0.getSource())))
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests(
                           (l_param0, l_param1) -> {
                              PlayerList l_var2 = l_param0.getSource().getServer().getPlayerList();
                              return SharedSuggestionProvider.suggest(
                                 l_var2.getPlayers()
                                    .stream()
                                    .filter(l2_param1 -> !l_var2.getWhiteList().isWhiteListed(l2_param1.getGameProfile()))
                                    .map(l2_param0 -> l2_param0.getGameProfile().getName()),
                                 l_param1
                              );
                           }
                        )
                        .executes(l_param0 -> addPlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets")))
                  )
            )
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests(
                           (l_param0, l_param1) -> SharedSuggestionProvider.suggest(
                              l_param0.getSource().getServer().getPlayerList().getWhiteListNames(), l_param1
                           )
                        )
                        .executes(l_param0 -> removePlayers(l_param0.getSource(), GameProfileArgument.getGameProfiles(l_param0, "targets")))
                  )
            )
            .then(Commands.literal("reload").executes(l_param0 -> reload(l_param0.getSource())))
      );
   }

   private static int reload(CommandSourceStack param0) {
      param0.getServer().getPlayerList().reloadWhiteList();
      param0.sendSuccess(Component.translatable("commands.whitelist.reloaded"), true);
      param0.getServer().kickUnlistedPlayers(param0);
      return 1;
   }

   private static int addPlayers(CommandSourceStack param0, Collection<GameProfile> param1) throws CommandSyntaxException {
      UserWhiteList var2 = param0.getServer().getPlayerList().getWhiteList();
      int var3 = 0;

      for (GameProfile var5 : param1) {
         if (!var2.isWhiteListed(var5)) {
            UserWhiteListEntry var6 = new UserWhiteListEntry(var5);
            var2.add(var6);
            param0.sendSuccess(Component.translatable("commands.whitelist.add.success", ComponentUtils.getDisplayName(var5)), true);
            var3++;
         }
      }

      if (var3 == 0) {
         throw ERROR_ALREADY_WHITELISTED.create();
      } else {
         return var3;
      }
   }

   private static int removePlayers(CommandSourceStack param0, Collection<GameProfile> param1) throws CommandSyntaxException {
      UserWhiteList var2 = param0.getServer().getPlayerList().getWhiteList();
      int var3 = 0;

      for (GameProfile var5 : param1) {
         if (var2.isWhiteListed(var5)) {
            UserWhiteListEntry var6 = new UserWhiteListEntry(var5);
            var2.remove(var6);
            param0.sendSuccess(Component.translatable("commands.whitelist.remove.success", ComponentUtils.getDisplayName(var5)), true);
            var3++;
         }
      }

      if (var3 == 0) {
         throw ERROR_NOT_WHITELISTED.create();
      }

      param0.getServer().kickUnlistedPlayers(param0);
      return var3;
   }

   private static int enableWhitelist(CommandSourceStack param0) throws CommandSyntaxException {
      PlayerList var1 = param0.getServer().getPlayerList();
      if (var1.isUsingWhitelist()) {
         throw ERROR_ALREADY_ENABLED.create();
      }

      var1.setUsingWhiteList(true);
      param0.sendSuccess(Component.translatable("commands.whitelist.enabled"), true);
      param0.getServer().kickUnlistedPlayers(param0);
      return 1;
   }

   private static int disableWhitelist(CommandSourceStack param0) throws CommandSyntaxException {
      PlayerList var1 = param0.getServer().getPlayerList();
      if (!var1.isUsingWhitelist()) {
         throw ERROR_ALREADY_DISABLED.create();
      }

      var1.setUsingWhiteList(false);
      param0.sendSuccess(Component.translatable("commands.whitelist.disabled"), true);
      return 1;
   }

   private static int showList(CommandSourceStack param0) {
      String[] var1 = param0.getServer().getPlayerList().getWhiteListNames();
      if (var1.length == 0) {
         param0.sendSuccess(Component.translatable("commands.whitelist.none"), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.whitelist.list", var1.length, String.join(", ", var1)), false);
      }

      return var1.length;
   }
}
