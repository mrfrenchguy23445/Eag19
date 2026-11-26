package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("defaultgamemode")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("gamemode", GameModeArgument.gameMode())
                  .executes(l_param0 -> setMode(l_param0.getSource(), GameModeArgument.getGameMode(l_param0, "gamemode")))
            )
      );
   }

   private static int setMode(CommandSourceStack param0, GameType param1) {
      int var2 = 0;
      MinecraftServer var3 = param0.getServer();
      var3.setDefaultGameType(param1);
      GameType var4 = var3.getForcedGameType();
      if (var4 != null) {
         for (ServerPlayer var6 : var3.getPlayerList().getPlayers()) {
            if (var6.setGameMode(var4)) {
               var2++;
            }
         }
      }

      param0.sendSuccess(Component.translatable("commands.defaultgamemode.success", param1.getLongDisplayName()), true);
      return var2;
   }
}
