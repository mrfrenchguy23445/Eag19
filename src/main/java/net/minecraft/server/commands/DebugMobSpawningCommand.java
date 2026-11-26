package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class DebugMobSpawningCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralArgumentBuilder<CommandSourceStack> var1 = Commands.literal("debugmobspawning").requires(l_param0 -> l_param0.hasPermission(2));

      for (MobCategory var5 : MobCategory.values()) {
         var1.then(
            Commands.literal(var5.getName())
               .then(
                  Commands.argument("at", BlockPosArgument.blockPos())
                     .executes(l_param1 -> spawnMobs(l_param1.getSource(), var5, BlockPosArgument.getLoadedBlockPos(l_param1, "at")))
               )
         );
      }

      param0.register(var1);
   }

   private static int spawnMobs(CommandSourceStack param0, MobCategory param1, BlockPos param2) {
      NaturalSpawner.spawnCategoryForPosition(param1, param0.getLevel(), param2);
      return 1;
   }
}
