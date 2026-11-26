package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SaveOnCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(Component.translatable("commands.save.alreadyOn"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(Commands.literal("save-on").requires(l_param0 -> l_param0.hasPermission(4)).executes(l_param0 -> {
         CommandSourceStack l_var1 = l_param0.getSource();
         boolean l_var2 = false;

         for (ServerLevel l_var4 : l_var1.getServer().getAllLevels()) {
            if (l_var4 != null && l_var4.noSave) {
               l_var4.noSave = false;
               l_var2 = true;
            }
         }

         if (!l_var2) {
            throw ERROR_ALREADY_ON.create();
         }

         l_var1.sendSuccess(Component.translatable("commands.save.enabled"), true);
         return 1;
      }));
   }
}
