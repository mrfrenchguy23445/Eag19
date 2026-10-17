package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (param0, param1) -> {
      StringReader var2 = new StringReader(param1.getInput());
      var2.setCursor(param1.getStart());
      EntitySelectorParser var3 = new EntitySelectorParser(var2);

      try {
         var3.parse();
      } catch (CommandSyntaxException var5) {
      }

      return var3.fillSuggestions(param1, l_param1 -> SharedSuggestionProvider.suggest(param0.getSource().getOnlinePlayerNames(), l_param1));
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   final boolean multiple;

   public ScoreHolderArgument(boolean param0) {
      this.multiple = param0;
   }

   public static String getName(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return getNames(param0, param1).iterator().next();
   }

   public static Collection<String> getNames(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return getNames(param0, param1, Collections::emptyList);
   }

   public static Collection<String> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return getNames(param0, param1, param0.getSource().getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<String> getNames(CommandContext<CommandSourceStack> param0, String param1, Supplier<Collection<String>> param2) throws CommandSyntaxException {
      Collection<String> var3 = param0.getArgument(param1, ScoreHolderArgument.Result.class).getNames(param0.getSource(), param2);
      if (var3.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return var3;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader param0) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '@') {
         EntitySelectorParser var2 = new EntitySelectorParser(param0);
         EntitySelector var3 = var2.parse();
         if (!this.multiple && var3.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
         } else {
            return new ScoreHolderArgument.SelectorResult(var3);
         }
      } else {
         int var2 = param0.getCursor();

         while (param0.canRead() && param0.peek() != ' ') {
            param0.skip();
         }

         String var3 = param0.getString().substring(var2, param0.getCursor());
         if (var3.equals("*")) {
            return (l_param0, l_param1) -> {
               Collection<String> l_var2 = l_param1.get();
               if (l_var2.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return l_var2;
               }
            };
         }

         Collection<String> var4 = Collections.singleton(var3);
         return (l_param1, l_param2) -> var4;
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;

      public void serializeToNetwork(ScoreHolderArgument.Info.Template param0, FriendlyByteBuf param1) {
         int var3 = 0;
         if (param0.multiple) {
            var3 |= 1;
         }

         param1.writeByte(var3);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf param0) {
         byte var2 = param0.readByte();
         boolean var3 = (var2 & 1) != 0;
         return new ScoreHolderArgument.Info.Template(var3);
      }

      public void serializeToJson(ScoreHolderArgument.Info.Template param0, JsonObject param1) {
         param1.addProperty("amount", param0.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument param0) {
         return new ScoreHolderArgument.Info.Template(param0.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         final boolean multiple;

         Template(boolean param1) {
            this.multiple = param1;
         }

         public ScoreHolderArgument instantiate(CommandBuildContext param0) {
            return new ScoreHolderArgument(this.multiple);
         }

         @Override
         public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
            return Info.this;
         }
      }
   }

   @FunctionalInterface
   public interface Result {
      Collection<String> getNames(CommandSourceStack var1, Supplier<Collection<String>> var2) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector param0) {
         this.selector = param0;
      }

      @Override
      public Collection<String> getNames(CommandSourceStack param0, Supplier<Collection<String>> param1) throws CommandSyntaxException {
         List<? extends Entity> var3 = this.selector.findEntities(param0);
         if (var3.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         }

         List<String> var4 = Lists.newArrayList();

         for (Entity var6 : var3) {
            var4.add(var6.getScoreboardName());
         }

         return var4;
      }
   }
}
