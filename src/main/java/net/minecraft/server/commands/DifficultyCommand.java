package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.difficulty.failure", param0)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      LiteralArgumentBuilder<CommandSourceStack> var1 = Commands.literal("difficulty");

      for (Difficulty var5 : Difficulty.values()) {
         var1.then(Commands.literal(var5.getKey()).executes(l_param1 -> setDifficulty(l_param1.getSource(), var5)));
      }

      param0.register(var1.requires(l_param0 -> l_param0.hasPermission(2)).executes(l_param0 -> {
         Difficulty l_var1 = l_param0.getSource().getLevel().getDifficulty();
         l_param0.getSource().sendSuccess(Component.translatable("commands.difficulty.query", l_var1.getDisplayName()), false);
         return l_var1.getId();
      }));
   }

   public static int setDifficulty(CommandSourceStack param0, Difficulty param1) throws CommandSyntaxException {
      MinecraftServer var2 = param0.getServer();
      if (var2.getWorldData().getDifficulty() == param1) {
         throw ERROR_ALREADY_DIFFICULT.create(param1.getKey());
      }

      var2.setDifficulty(param1, true);
      param0.sendSuccess(Component.translatable("commands.difficulty.success", param1.getDisplayName()), true);
      return 0;
   }
}
