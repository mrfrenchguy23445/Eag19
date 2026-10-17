package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      param0 -> Component.translatable("arguments.function.tag.unknown", param0)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType(
      param0 -> Component.translatable("arguments.function.unknown", param0)
   );

   public static FunctionArgument functions() {
      return new FunctionArgument();
   }

   public FunctionArgument.Result parse(StringReader param0) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '#') {
         param0.skip();
         final ResourceLocation var2 = ResourceLocation.read(param0);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction> create(CommandContext<CommandSourceStack> l_param0) throws CommandSyntaxException {
               return FunctionArgument.getFunctionTag(l_param0, var2);
            }

            @Override
            public Pair<ResourceLocation, Either<CommandFunction, Collection<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> l_param0) throws CommandSyntaxException {
               return Pair.of(var2, Either.right(FunctionArgument.getFunctionTag(l_param0, var2)));
            }
         };
      } else {
         final ResourceLocation var2 = ResourceLocation.read(param0);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction> create(CommandContext<CommandSourceStack> l_param0) throws CommandSyntaxException {
               return Collections.singleton(FunctionArgument.getFunction(l_param0, var2));
            }

            @Override
            public Pair<ResourceLocation, Either<CommandFunction, Collection<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> l_param0) throws CommandSyntaxException {
               return Pair.of(var2, Either.left(FunctionArgument.getFunction(l_param0, var2)));
            }
         };
      }
   }

   static CommandFunction getFunction(CommandContext<CommandSourceStack> param0, ResourceLocation param1) throws CommandSyntaxException {
      return param0.getSource().getServer().getFunctions().get(param1).orElseThrow(() -> ERROR_UNKNOWN_FUNCTION.create(param1.toString()));
   }

   static Collection<CommandFunction> getFunctionTag(CommandContext<CommandSourceStack> param0, ResourceLocation param1) throws CommandSyntaxException {
      Collection<CommandFunction> var2 = param0.getSource().getServer().getFunctions().getTag(param1);
      if (var2 == null) {
         throw ERROR_UNKNOWN_TAG.create(param1.toString());
      } else {
         return var2;
      }
   }

   public static Collection<CommandFunction> getFunctions(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, FunctionArgument.Result.class).create(param0);
   }

   public static Pair<ResourceLocation, Either<CommandFunction, Collection<CommandFunction>>> getFunctionOrTag(
      CommandContext<CommandSourceStack> param0, String param1
   ) throws CommandSyntaxException {
      return param0.getArgument(param1, FunctionArgument.Result.class).unwrap(param0);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public interface Result {
      Collection<CommandFunction> create(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

      Pair<ResourceLocation, Either<CommandFunction, Collection<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }
}
