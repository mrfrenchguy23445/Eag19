package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

public class SpectateCommand {
   private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(Component.translatable("commands.spectate.self"));
   private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.spectate.not_spectator", param0)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("spectate")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(l_param0 -> spectate(l_param0.getSource(), null, l_param0.getSource().getPlayerOrException()))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .executes(
                     l_param0 -> spectate(l_param0.getSource(), EntityArgument.getEntity(l_param0, "target"), l_param0.getSource().getPlayerOrException())
                  )
                  .then(
                     Commands.argument("player", EntityArgument.player())
                        .executes(
                           l_param0 -> spectate(
                              l_param0.getSource(), EntityArgument.getEntity(l_param0, "target"), EntityArgument.getPlayer(l_param0, "player")
                           )
                        )
                  )
            )
      );
   }

   private static int spectate(CommandSourceStack param0, @Nullable Entity param1, ServerPlayer param2) throws CommandSyntaxException {
      if (param2 == param1) {
         throw ERROR_SELF.create();
      }

      if (param2.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
         throw ERROR_NOT_SPECTATOR.create(param2.getDisplayName());
      }

      param2.setCamera(param1);
      if (param1 != null) {
         param0.sendSuccess(Component.translatable("commands.spectate.success.started", param1.getDisplayName()), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.spectate.success.stopped"), false);
      }

      return 1;
   }
}
