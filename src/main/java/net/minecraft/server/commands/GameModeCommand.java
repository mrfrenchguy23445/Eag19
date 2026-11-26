package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public class GameModeCommand {
   public static final int PERMISSION_LEVEL = 2;

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("gamemode")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("gamemode", GameModeArgument.gameMode())
                  .executes(
                     l_param0 -> setMode(
                        l_param0, Collections.singleton(l_param0.getSource().getPlayerOrException()), GameModeArgument.getGameMode(l_param0, "gamemode")
                     )
                  )
                  .then(
                     Commands.argument("target", EntityArgument.players())
                        .executes(
                           l_param0 -> setMode(l_param0, EntityArgument.getPlayers(l_param0, "target"), GameModeArgument.getGameMode(l_param0, "gamemode"))
                        )
                  )
            )
      );
   }

   private static void logGamemodeChange(CommandSourceStack param0, ServerPlayer param1, GameType param2) {
      Component var3 = Component.translatable("gameMode." + param2.getName());
      if (param0.getEntity() == param1) {
         param0.sendSuccess(Component.translatable("commands.gamemode.success.self", var3), true);
      } else {
         if (param0.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            param1.sendSystemMessage(Component.translatable("gameMode.changed", var3));
         }

         param0.sendSuccess(Component.translatable("commands.gamemode.success.other", param1.getDisplayName(), var3), true);
      }
   }

   private static int setMode(CommandContext<CommandSourceStack> param0, Collection<ServerPlayer> param1, GameType param2) {
      int var3 = 0;

      for (ServerPlayer var5 : param1) {
         if (var5.setGameMode(param2)) {
            logGamemodeChange(param0.getSource(), var5, param2);
            var3++;
         }
      }

      return var3;
   }
}
