package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class DebugPathCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType(Component.literal("Source is not a mob"));
   private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType(Component.literal("Path not found"));
   private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.literal("Target not reached"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("debugpath")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("to", BlockPosArgument.blockPos())
                  .executes(l_param0 -> fillBlocks(l_param0.getSource(), BlockPosArgument.getLoadedBlockPos(l_param0, "to")))
            )
      );
   }

   private static int fillBlocks(CommandSourceStack param0, BlockPos param1) throws CommandSyntaxException {
      if (!(param0.getEntity() instanceof Mob var3)) {
         throw ERROR_NOT_MOB.create();
      } else {
         PathNavigation var4 = new GroundPathNavigation(var3, param0.getLevel());
         Path var5 = var4.createPath(param1, 0);
         DebugPackets.sendPathFindingPacket(param0.getLevel(), var3, var5, var4.getMaxDistanceToWaypoint());
         if (var5 == null) {
            throw ERROR_NO_PATH.create();
         }

         if (!var5.canReach()) {
            throw ERROR_NOT_COMPLETE.create();
         }

         param0.sendSuccess(Component.literal("Made path"), true);
         return 1;
      }
   }
}
