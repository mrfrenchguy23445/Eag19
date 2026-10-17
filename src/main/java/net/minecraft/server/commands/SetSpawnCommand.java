package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SetSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("spawnpoint")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(
               l_param0 -> setSpawn(
                  l_param0.getSource(),
                  Collections.singleton(l_param0.getSource().getPlayerOrException()),
                  BlockPos.containing(l_param0.getSource().getPosition()),
                  0.0F
               )
            )
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .executes(
                     l_param0 -> setSpawn(
                        l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), BlockPos.containing(l_param0.getSource().getPosition()), 0.0F
                     )
                  )
                  .then(
                     Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(
                           l_param0 -> setSpawn(
                              l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), BlockPosArgument.getSpawnablePos(l_param0, "pos"), 0.0F
                           )
                        )
                        .then(
                           Commands.argument("angle", AngleArgument.angle())
                              .executes(
                                 l_param0 -> setSpawn(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    BlockPosArgument.getSpawnablePos(l_param0, "pos"),
                                    AngleArgument.getAngle(l_param0, "angle")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack param0, Collection<ServerPlayer> param1, BlockPos param2, float param3) {
      ResourceKey<Level> var4 = param0.getLevel().dimension();

      for (ServerPlayer var6 : param1) {
         var6.setRespawnPosition(var4, param2, param3, true, false);
      }

      String var5 = var4.location().toString();
      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable(
               "commands.spawnpoint.success.single", param2.getX(), param2.getY(), param2.getZ(), param3, var5, param1.iterator().next().getDisplayName()
            ),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.spawnpoint.success.multiple", param2.getX(), param2.getY(), param2.getZ(), param3, var5, param1.size()), true
         );
      }

      return param1.size();
   }
}
