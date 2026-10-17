package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TriggerCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(Component.translatable("commands.trigger.failed.unprimed"));
   private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(
      Component.translatable("commands.trigger.failed.invalid")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("trigger")
            .then(
               Commands.argument("objective", ObjectiveArgument.objective())
                  .suggests((l_param0, l_param1) -> suggestObjectives(l_param0.getSource(), l_param1))
                  .executes(
                     l_param0 -> simpleTrigger(
                        l_param0.getSource(), getScore(l_param0.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(l_param0, "objective"))
                     )
                  )
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("value", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> addValue(
                                    l_param0.getSource(),
                                    getScore(l_param0.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(l_param0, "objective")),
                                    IntegerArgumentType.getInteger(l_param0, "value")
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("set")
                        .then(
                           Commands.argument("value", IntegerArgumentType.integer())
                              .executes(
                                 l_param0 -> setValue(
                                    l_param0.getSource(),
                                    getScore(l_param0.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(l_param0, "objective")),
                                    IntegerArgumentType.getInteger(l_param0, "value")
                                 )
                              )
                        )
                  )
            )
      );
   }

   public static CompletableFuture<Suggestions> suggestObjectives(CommandSourceStack param0, SuggestionsBuilder param1) {
      Entity var2 = param0.getEntity();
      List<String> var3 = Lists.newArrayList();
      if (var2 != null) {
         Scoreboard var4 = param0.getServer().getScoreboard();
         String var5 = var2.getScoreboardName();

         for (Objective var7 : var4.getObjectives()) {
            if (var7.getCriteria() == ObjectiveCriteria.TRIGGER && var4.hasPlayerScore(var5, var7)) {
               Score var8 = var4.getOrCreatePlayerScore(var5, var7);
               if (!var8.isLocked()) {
                  var3.add(var7.getName());
               }
            }
         }
      }

      return SharedSuggestionProvider.suggest(var3, param1);
   }

   private static int addValue(CommandSourceStack param0, Score param1, int param2) {
      param1.add(param2);
      param0.sendSuccess(Component.translatable("commands.trigger.add.success", param1.getObjective().getFormattedDisplayName(), param2), true);
      return param1.getScore();
   }

   private static int setValue(CommandSourceStack param0, Score param1, int param2) {
      param1.setScore(param2);
      param0.sendSuccess(Component.translatable("commands.trigger.set.success", param1.getObjective().getFormattedDisplayName(), param2), true);
      return param2;
   }

   private static int simpleTrigger(CommandSourceStack param0, Score param1) {
      param1.add(1);
      param0.sendSuccess(Component.translatable("commands.trigger.simple.success", param1.getObjective().getFormattedDisplayName()), true);
      return param1.getScore();
   }

   private static Score getScore(ServerPlayer param0, Objective param1) throws CommandSyntaxException {
      if (param1.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_INVALID_OBJECTIVE.create();
      }

      Scoreboard var2 = param0.getScoreboard();
      String var3 = param0.getScoreboardName();
      if (!var2.hasPlayerScore(var3, param1)) {
         throw ERROR_NOT_PRIMED.create();
      }

      Score var4 = var2.getOrCreatePlayerScore(var3, param1);
      if (var4.isLocked()) {
         throw ERROR_NOT_PRIMED.create();
      }

      var4.setLocked(true);
      return var4;
   }
}
