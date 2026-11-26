package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;

public class WardenSpawnTrackerCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("warden_spawn_tracker")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("clear")
                  .executes(l_param0 -> resetTracker(l_param0.getSource(), ImmutableList.of(l_param0.getSource().getPlayerOrException())))
            )
            .then(
               Commands.literal("set")
                  .then(
                     Commands.argument("warning_level", IntegerArgumentType.integer(0, 4))
                        .executes(
                           l_param0 -> setWarningLevel(
                              l_param0.getSource(),
                              ImmutableList.of(l_param0.getSource().getPlayerOrException()),
                              IntegerArgumentType.getInteger(l_param0, "warning_level")
                           )
                        )
                  )
            )
      );
   }

   private static int setWarningLevel(CommandSourceStack param0, Collection<? extends Player> param1, int param2) {
      for (Player var4 : param1) {
         var4.getWardenSpawnTracker().ifPresent(l_param1 -> l_param1.setWarningLevel(param2));
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.warden_spawn_tracker.set.success.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.warden_spawn_tracker.set.success.multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int resetTracker(CommandSourceStack param0, Collection<? extends Player> param1) {
      for (Player var3 : param1) {
         var3.getWardenSpawnTracker().ifPresent(WardenSpawnTracker::reset);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.warden_spawn_tracker.clear.success.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.warden_spawn_tracker.clear.success.multiple", param1.size()), true);
      }

      return param1.size();
   }
}
