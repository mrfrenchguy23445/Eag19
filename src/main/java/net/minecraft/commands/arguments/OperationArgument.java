package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.Score;

public class OperationArgument implements ArgumentType<OperationArgument.Operation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
   private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(
      Component.translatable("arguments.operation.invalid")
   );
   private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

   public static OperationArgument operation() {
      return new OperationArgument();
   }

   public static OperationArgument.Operation getOperation(CommandContext<CommandSourceStack> param0, String param1) {
      return param0.getArgument(param1, OperationArgument.Operation.class);
   }

   public OperationArgument.Operation parse(StringReader param0) throws CommandSyntaxException {
      if (!param0.canRead()) {
         throw ERROR_INVALID_OPERATION.create();
      }

      int var2 = param0.getCursor();

      while (param0.canRead() && param0.peek() != ' ') {
         param0.skip();
      }

      return getOperation(param0.getString().substring(var2, param0.getCursor()));
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> param0, SuggestionsBuilder param1) {
      return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, param1);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static OperationArgument.Operation getOperation(String param0) throws CommandSyntaxException {
      return param0.equals("><") ? (l_param0, l_param1) -> {
         int l_var2 = l_param0.getScore();
         l_param0.setScore(l_param1.getScore());
         l_param1.setScore(l_var2);
      } : getSimpleOperation(param0);
   }

   private static OperationArgument.SimpleOperation getSimpleOperation(String param0) throws CommandSyntaxException {
      switch (param0) {
         case "=":
            return (l_param0, l_param1) -> l_param1;
         case "+=":
            return (l_param0, l_param1) -> l_param0 + l_param1;
         case "-=":
            return (l_param0, l_param1) -> l_param0 - l_param1;
         case "*=":
            return (l_param0, l_param1) -> l_param0 * l_param1;
         case "/=":
            return (l_param0, l_param1) -> {
               if (l_param1 == 0) {
                  throw ERROR_DIVIDE_BY_ZERO.create();
               } else {
                  return Mth.floorDiv(l_param0, l_param1);
               }
            };
         case "%=":
            return (l_param0, l_param1) -> {
               if (l_param1 == 0) {
                  throw ERROR_DIVIDE_BY_ZERO.create();
               } else {
                  return Mth.positiveModulo(l_param0, l_param1);
               }
            };
         case "<":
            return Math::min;
         case ">":
            return Math::max;
         default:
            throw ERROR_INVALID_OPERATION.create();
      }
   }

   @FunctionalInterface
   public interface Operation {
      void apply(Score var1, Score var2) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface SimpleOperation extends OperationArgument.Operation {
      int apply(int var1, int var2) throws CommandSyntaxException;

      @Override
      default void apply(Score param0, Score param1) throws CommandSyntaxException {
         param0.setScore(this.apply(param0.getScore(), param1.getScore()));
      }
   }
}
