package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerFunctionManager;

public class FunctionCommand {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (param0, param1) -> {
      ServerFunctionManager var2 = param0.getSource().getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(var2.getTagNames(), param1, "#");
      return SharedSuggestionProvider.suggestResource(var2.getFunctionNames(), param1);
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("function")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("name", FunctionArgument.functions())
                  .suggests(SUGGEST_FUNCTION)
                  .executes(l_param0 -> runFunction(l_param0.getSource(), FunctionArgument.getFunctions(l_param0, "name")))
            )
      );
   }

   private static int runFunction(CommandSourceStack param0, Collection<CommandFunction> param1) {
      int var2 = 0;

      for (CommandFunction var4 : param1) {
         var2 += param0.getServer().getFunctions().execute(var4, param0.withSuppressedOutput().withMaximumPermission(2));
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.function.success.single", var2, param1.iterator().next().getId()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.function.success.multiple", var2, param1.size()), true);
      }

      return var2;
   }
}
