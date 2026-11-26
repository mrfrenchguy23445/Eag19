package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ComponentArgument implements ArgumentType<Component> {
   private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
   public static final DynamicCommandExceptionType ERROR_INVALID_JSON = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.component.invalid", param0)
   );

   private ComponentArgument() {
   }

   public static Component getComponent(CommandContext<CommandSourceStack> param0, String param1) {
      return param0.getArgument(param1, Component.class);
   }

   public static ComponentArgument textComponent() {
      return new ComponentArgument();
   }

   public Component parse(StringReader param0) throws CommandSyntaxException {
      try {
         Component var2 = Component.Serializer.fromJson(param0);
         if (var2 == null) {
            throw ERROR_INVALID_JSON.createWithContext(param0, "empty");
         } else {
            return var2;
         }
      } catch (Exception var2) {
         String var3 = var2.getCause() != null ? var2.getCause().getMessage() : var2.getMessage();
         throw ERROR_INVALID_JSON.createWithContext(param0, var3);
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
