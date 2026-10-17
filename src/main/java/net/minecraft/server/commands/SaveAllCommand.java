package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class SaveAllCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.save.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("save-all")
            .requires(l_param0 -> l_param0.hasPermission(4))
            .executes(l_param0 -> saveAll(l_param0.getSource(), false))
            .then(Commands.literal("flush").executes(l_param0 -> saveAll(l_param0.getSource(), true)))
      );
   }

   private static int saveAll(CommandSourceStack param0, boolean param1) throws CommandSyntaxException {
      param0.sendSuccess(Component.translatable("commands.save.saving"), false);
      MinecraftServer var2 = param0.getServer();
      boolean var3 = var2.saveEverything(true, param1, true);
      if (!var3) {
         throw ERROR_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.save.success"), true);
      return 1;
   }
}
