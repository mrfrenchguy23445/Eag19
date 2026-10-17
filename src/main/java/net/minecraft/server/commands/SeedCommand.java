package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class SeedCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0, boolean param1) {
      param0.register(Commands.literal("seed").requires(l_param1 -> !param1 || l_param1.hasPermission(2)).executes(l_param0 -> {
         long l_var1 = l_param0.getSource().getLevel().getSeed();
         Component l_var3 = ComponentUtils.copyOnClickText(String.valueOf(l_var1));
         l_param0.getSource().sendSuccess(Component.translatable("commands.seed.success", l_var3), false);
         return (int)l_var1;
      }));
   }
}
