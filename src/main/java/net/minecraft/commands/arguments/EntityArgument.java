package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class EntityArgument implements ArgumentType<EntitySelector> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType(Component.translatable("argument.entity.toomany"));
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
   public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.player.entities")
   );
   public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.entity"));
   public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.player"));
   public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.selector.not_allowed")
   );
   final boolean single;
   final boolean playersOnly;

   protected EntityArgument(boolean param0, boolean param1) {
      this.single = param0;
      this.playersOnly = param1;
   }

   public static EntityArgument entity() {
      return new EntityArgument(true, false);
   }

   public static Entity getEntity(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, EntitySelector.class).findSingleEntity(param0.getSource());
   }

   public static EntityArgument entities() {
      return new EntityArgument(false, false);
   }

   public static Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      Collection<? extends Entity> var2 = getOptionalEntities(param0, param1);
      if (var2.isEmpty()) {
         throw NO_ENTITIES_FOUND.create();
      } else {
         return var2;
      }
   }

   public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, EntitySelector.class).findEntities(param0.getSource());
   }

   public static Collection<ServerPlayer> getOptionalPlayers(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, EntitySelector.class).findPlayers(param0.getSource());
   }

   public static EntityArgument player() {
      return new EntityArgument(true, true);
   }

   public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      return param0.getArgument(param1, EntitySelector.class).findSinglePlayer(param0.getSource());
   }

   public static EntityArgument players() {
      return new EntityArgument(false, true);
   }

   public static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> param0, String param1) throws CommandSyntaxException {
      List<ServerPlayer> var2 = param0.getArgument(param1, EntitySelector.class).findPlayers(param0.getSource());
      if (var2.isEmpty()) {
         throw NO_PLAYERS_FOUND.create();
      } else {
         return var2;
      }
   }

   public EntitySelector parse(StringReader param0) throws CommandSyntaxException {
      int var2 = 0;
      EntitySelectorParser var3 = new EntitySelectorParser(param0);
      EntitySelector var4 = var3.parse();
      if (var4.getMaxResults() > 1 && this.single) {
         if (this.playersOnly) {
            param0.setCursor(0);
            throw ERROR_NOT_SINGLE_PLAYER.createWithContext(param0);
         } else {
            param0.setCursor(0);
            throw ERROR_NOT_SINGLE_ENTITY.createWithContext(param0);
         }
      } else if (var4.includesEntities() && this.playersOnly && !var4.isSelfSelector()) {
         param0.setCursor(0);
         throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(param0);
      } else {
         return var4;
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> param0, SuggestionsBuilder param1) {
      if (param0.getSource() instanceof SharedSuggestionProvider var3) {
         StringReader var4 = new StringReader(param1.getInput());
         var4.setCursor(param1.getStart());
         EntitySelectorParser var5 = new EntitySelectorParser(var4, var3.hasPermission(2));

         try {
            var5.parse();
         } catch (CommandSyntaxException var7) {
         }

         return var5.fillSuggestions(param1, l_param1 -> {
            Collection<String> l_var3 = var3.getOnlinePlayerNames();
            Iterable<String> l_var4 = this.playersOnly ? l_var3 : Iterables.concat(l_var3, var3.getSelectedEntities());
            SharedSuggestionProvider.suggest(l_var4, l_param1);
         });
      } else {
         return Suggestions.empty();
      }
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<EntityArgument, EntityArgument.Info.Template> {
      private static final byte FLAG_SINGLE = 1;
      private static final byte FLAG_PLAYERS_ONLY = 2;

      public void serializeToNetwork(EntityArgument.Info.Template param0, FriendlyByteBuf param1) {
         int var3 = 0;
         if (param0.single) {
            var3 |= 1;
         }

         if (param0.playersOnly) {
            var3 |= 2;
         }

         param1.writeByte(var3);
      }

      public EntityArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf param0) {
         byte var2 = param0.readByte();
         return new EntityArgument.Info.Template((var2 & 1) != 0, (var2 & 2) != 0);
      }

      public void serializeToJson(EntityArgument.Info.Template param0, JsonObject param1) {
         param1.addProperty("amount", param0.single ? "single" : "multiple");
         param1.addProperty("type", param0.playersOnly ? "players" : "entities");
      }

      public EntityArgument.Info.Template unpack(EntityArgument param0) {
         return new EntityArgument.Info.Template(param0.single, param0.playersOnly);
      }

      public final class Template implements ArgumentTypeInfo.Template<EntityArgument> {
         final boolean single;
         final boolean playersOnly;

         Template(boolean param1, boolean param2) {
            this.single = param1;
            this.playersOnly = param2;
         }

         public EntityArgument instantiate(CommandBuildContext param0) {
            return new EntityArgument(this.single, this.playersOnly);
         }

         @Override
         public ArgumentTypeInfo<EntityArgument, ?> type() {
            return Info.this;
         }
      }
   }
}
