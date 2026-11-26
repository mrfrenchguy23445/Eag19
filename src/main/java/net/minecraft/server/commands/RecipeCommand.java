package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCommand {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.give.failed"));
   private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.take.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("recipe")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("give")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .then(
                           Commands.argument("recipe", ResourceLocationArgument.id())
                              .suggests(SuggestionProviders.ALL_RECIPES)
                              .executes(
                                 l_param0 -> giveRecipes(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    Collections.singleton(ResourceLocationArgument.getRecipe(l_param0, "recipe"))
                                 )
                              )
                        )
                        .then(
                           Commands.literal("*")
                              .executes(
                                 l_param0 -> giveRecipes(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    l_param0.getSource().getServer().getRecipeManager().getRecipes()
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("take")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .then(
                           Commands.argument("recipe", ResourceLocationArgument.id())
                              .suggests(SuggestionProviders.ALL_RECIPES)
                              .executes(
                                 l_param0 -> takeRecipes(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    Collections.singleton(ResourceLocationArgument.getRecipe(l_param0, "recipe"))
                                 )
                              )
                        )
                        .then(
                           Commands.literal("*")
                              .executes(
                                 l_param0 -> takeRecipes(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    l_param0.getSource().getServer().getRecipeManager().getRecipes()
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int giveRecipes(CommandSourceStack param0, Collection<ServerPlayer> param1, Collection<Recipe<?>> param2) throws CommandSyntaxException {
      int var3 = 0;

      for (ServerPlayer var5 : param1) {
         var3 += var5.awardRecipes(param2);
      }

      if (var3 == 0) {
         throw ERROR_GIVE_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.recipe.give.success.single", param2.size(), param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.recipe.give.success.multiple", param2.size(), param1.size()), true);
      }

      return var3;
   }

   private static int takeRecipes(CommandSourceStack param0, Collection<ServerPlayer> param1, Collection<Recipe<?>> param2) throws CommandSyntaxException {
      int var3 = 0;

      for (ServerPlayer var5 : param1) {
         var3 += var5.resetRecipes(param2);
      }

      if (var3 == 0) {
         throw ERROR_TAKE_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.recipe.take.success.single", param2.size(), param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.recipe.take.success.multiple", param2.size(), param1.size()), true);
      }

      return var3;
   }
}
