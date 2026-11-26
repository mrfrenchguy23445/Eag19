package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SetWorldSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("setworldspawn")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(l_param0 -> setSpawn(l_param0.getSource(), BlockPos.containing(l_param0.getSource().getPosition()), 0.0F))
            .then(
               Commands.argument("pos", BlockPosArgument.blockPos())
                  .executes(l_param0 -> setSpawn(l_param0.getSource(), BlockPosArgument.getSpawnablePos(l_param0, "pos"), 0.0F))
                  .then(
                     Commands.argument("angle", AngleArgument.angle())
                        .executes(
                           l_param0 -> setSpawn(
                              l_param0.getSource(), BlockPosArgument.getSpawnablePos(l_param0, "pos"), AngleArgument.getAngle(l_param0, "angle")
                           )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack param0, BlockPos param1, float param2) {
      param0.getLevel().setDefaultSpawnPos(param1, param2);
      param0.sendSuccess(Component.translatable("commands.setworldspawn.success", param1.getX(), param1.getY(), param1.getZ(), param2), true);
      return 1;
   }
}
