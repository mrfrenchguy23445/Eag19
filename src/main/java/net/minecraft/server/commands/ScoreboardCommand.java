package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardCommand {
   private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadyEmpty")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadySet")
   );
   private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.invalid")
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("commands.scoreboard.players.get.null", param0, param1)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("scoreboard")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("objectives")
                  .then(Commands.literal("list").executes(l_param0 -> listObjectives(l_param0.getSource())))
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("objective", StringArgumentType.word())
                              .then(
                                 Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                    .executes(
                                       l_param0 -> addObjective(
                                          l_param0.getSource(),
                                          StringArgumentType.getString(l_param0, "objective"),
                                          ObjectiveCriteriaArgument.getCriteria(l_param0, "criteria"),
                                          Component.literal(StringArgumentType.getString(l_param0, "objective"))
                                       )
                                    )
                                    .then(
                                       Commands.argument("displayName", ComponentArgument.textComponent())
                                          .executes(
                                             l_param0 -> addObjective(
                                                l_param0.getSource(),
                                                StringArgumentType.getString(l_param0, "objective"),
                                                ObjectiveCriteriaArgument.getCriteria(l_param0, "criteria"),
                                                ComponentArgument.getComponent(l_param0, "displayName")
                                             )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("modify")
                        .then(
                           Commands.argument("objective", ObjectiveArgument.objective())
                              .then(
                                 Commands.literal("displayname")
                                    .then(
                                       Commands.argument("displayName", ComponentArgument.textComponent())
                                          .executes(
                                             l_param0 -> setDisplayName(
                                                l_param0.getSource(),
                                                ObjectiveArgument.getObjective(l_param0, "objective"),
                                                ComponentArgument.getComponent(l_param0, "displayName")
                                             )
                                          )
                                    )
                              )
                              .then(createRenderTypeModify())
                        )
                  )
                  .then(
                     Commands.literal("remove")
                        .then(
                           Commands.argument("objective", ObjectiveArgument.objective())
                              .executes(l_param0 -> removeObjective(l_param0.getSource(), ObjectiveArgument.getObjective(l_param0, "objective")))
                        )
                  )
                  .then(
                     Commands.literal("setdisplay")
                        .then(
                           Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                              .executes(l_param0 -> clearDisplaySlot(l_param0.getSource(), ScoreboardSlotArgument.getDisplaySlot(l_param0, "slot")))
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(
                                       l_param0 -> setDisplaySlot(
                                          l_param0.getSource(),
                                          ScoreboardSlotArgument.getDisplaySlot(l_param0, "slot"),
                                          ObjectiveArgument.getObjective(l_param0, "objective")
                                       )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("players")
                  .then(
                     Commands.literal("list")
                        .executes(l_param0 -> listTrackedPlayers(l_param0.getSource()))
                        .then(
                           Commands.argument("target", ScoreHolderArgument.scoreHolder())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .executes(l_param0 -> listTrackedPlayerScores(l_param0.getSource(), ScoreHolderArgument.getName(l_param0, "target")))
                        )
                  )
                  .then(
                     Commands.literal("set")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("score", IntegerArgumentType.integer())
                                          .executes(
                                             l_param0 -> setScore(
                                                l_param0.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                                ObjectiveArgument.getWritableObjective(l_param0, "objective"),
                                                IntegerArgumentType.getInteger(l_param0, "score")
                                             )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("get")
                        .then(
                           Commands.argument("target", ScoreHolderArgument.scoreHolder())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(
                                       l_param0 -> getScore(
                                          l_param0.getSource(),
                                          ScoreHolderArgument.getName(l_param0, "target"),
                                          ObjectiveArgument.getObjective(l_param0, "objective")
                                       )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("score", IntegerArgumentType.integer(0))
                                          .executes(
                                             l_param0 -> addScore(
                                                l_param0.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                                ObjectiveArgument.getWritableObjective(l_param0, "objective"),
                                                IntegerArgumentType.getInteger(l_param0, "score")
                                             )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("remove")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("score", IntegerArgumentType.integer(0))
                                          .executes(
                                             l_param0 -> removeScore(
                                                l_param0.getSource(),
                                                ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                                ObjectiveArgument.getWritableObjective(l_param0, "objective"),
                                                IntegerArgumentType.getInteger(l_param0, "score")
                                             )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("reset")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .executes(l_param0 -> resetScores(l_param0.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets")))
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(
                                       l_param0 -> resetScore(
                                          l_param0.getSource(),
                                          ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                          ObjectiveArgument.getObjective(l_param0, "objective")
                                       )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("enable")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .suggests(
                                       (l_param0, l_param1) -> suggestTriggers(
                                          l_param0.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"), l_param1
                                       )
                                    )
                                    .executes(
                                       l_param0 -> enableTrigger(
                                          l_param0.getSource(),
                                          ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                          ObjectiveArgument.getObjective(l_param0, "objective")
                                       )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("operation")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("targetObjective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("operation", OperationArgument.operation())
                                          .then(
                                             Commands.argument("source", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                   Commands.argument("sourceObjective", ObjectiveArgument.objective())
                                                      .executes(
                                                         l_param0 -> performOperation(
                                                            l_param0.getSource(),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "targets"),
                                                            ObjectiveArgument.getWritableObjective(l_param0, "targetObjective"),
                                                            OperationArgument.getOperation(l_param0, "operation"),
                                                            ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "source"),
                                                            ObjectiveArgument.getObjective(l_param0, "sourceObjective")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
      LiteralArgumentBuilder<CommandSourceStack> var0 = Commands.literal("rendertype");

      for (ObjectiveCriteria.RenderType var4 : ObjectiveCriteria.RenderType.values()) {
         var0.then(
            Commands.literal(var4.getId())
               .executes(l_param1 -> setRenderType(l_param1.getSource(), ObjectiveArgument.getObjective(l_param1, "objective"), var4))
         );
      }

      return var0;
   }

   private static CompletableFuture<Suggestions> suggestTriggers(CommandSourceStack param0, Collection<String> param1, SuggestionsBuilder param2) {
      List<String> var3 = Lists.newArrayList();
      Scoreboard var4 = param0.getServer().getScoreboard();

      for (Objective var6 : var4.getObjectives()) {
         if (var6.getCriteria() == ObjectiveCriteria.TRIGGER) {
            boolean var7 = false;

            for (String var9 : param1) {
               if (!var4.hasPlayerScore(var9, var6) || var4.getOrCreatePlayerScore(var9, var6).isLocked()) {
                  var7 = true;
                  break;
               }
            }

            if (var7) {
               var3.add(var6.getName());
            }
         }
      }

      return SharedSuggestionProvider.suggest(var3, param2);
   }

   private static int getScore(CommandSourceStack param0, String param1, Objective param2) throws CommandSyntaxException {
      Scoreboard var3 = param0.getServer().getScoreboard();
      if (!var3.hasPlayerScore(param1, param2)) {
         throw ERROR_NO_VALUE.create(param2.getName(), param1);
      }

      Score var4 = var3.getOrCreatePlayerScore(param1, param2);
      param0.sendSuccess(Component.translatable("commands.scoreboard.players.get.success", param1, var4.getScore(), param2.getFormattedDisplayName()), false);
      return var4.getScore();
   }

   private static int performOperation(
      CommandSourceStack param0, Collection<String> param1, Objective param2, OperationArgument.Operation param3, Collection<String> param4, Objective param5
   ) throws CommandSyntaxException {
      Scoreboard var6 = param0.getServer().getScoreboard();
      int var7 = 0;

      for (String var9 : param1) {
         Score var10 = var6.getOrCreatePlayerScore(var9, param2);

         for (String var12 : param4) {
            Score var13 = var6.getOrCreatePlayerScore(var12, param5);
            param3.apply(var10, var13);
         }

         var7 += var10.getScore();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.operation.success.single", param2.getFormattedDisplayName(), param1.iterator().next(), var7),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.operation.success.multiple", param2.getFormattedDisplayName(), param1.size()), true
         );
      }

      return var7;
   }

   private static int enableTrigger(CommandSourceStack param0, Collection<String> param1, Objective param2) throws CommandSyntaxException {
      if (param2.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_NOT_TRIGGER.create();
      }

      Scoreboard var3 = param0.getServer().getScoreboard();
      int var4 = 0;

      for (String var6 : param1) {
         Score var7 = var3.getOrCreatePlayerScore(var6, param2);
         if (var7.isLocked()) {
            var7.setLocked(false);
            var4++;
         }
      }

      if (var4 == 0) {
         throw ERROR_TRIGGER_ALREADY_ENABLED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.enable.success.single", param2.getFormattedDisplayName(), param1.iterator().next()), true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.enable.success.multiple", param2.getFormattedDisplayName(), param1.size()), true
         );
      }

      return var4;
   }

   private static int resetScores(CommandSourceStack param0, Collection<String> param1) {
      Scoreboard var2 = param0.getServer().getScoreboard();

      for (String var4 : param1) {
         var2.resetPlayerScore(var4, null);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.reset.all.single", param1.iterator().next()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.reset.all.multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int resetScore(CommandSourceStack param0, Collection<String> param1, Objective param2) {
      Scoreboard var3 = param0.getServer().getScoreboard();

      for (String var5 : param1) {
         var3.resetPlayerScore(var5, param2);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.reset.specific.single", param2.getFormattedDisplayName(), param1.iterator().next()), true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.reset.specific.multiple", param2.getFormattedDisplayName(), param1.size()), true
         );
      }

      return param1.size();
   }

   private static int setScore(CommandSourceStack param0, Collection<String> param1, Objective param2, int param3) {
      Scoreboard var4 = param0.getServer().getScoreboard();

      for (String var6 : param1) {
         Score var7 = var4.getOrCreatePlayerScore(var6, param2);
         var7.setScore(param3);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.set.success.single", param2.getFormattedDisplayName(), param1.iterator().next(), param3), true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.set.success.multiple", param2.getFormattedDisplayName(), param1.size(), param3), true
         );
      }

      return param3 * param1.size();
   }

   private static int addScore(CommandSourceStack param0, Collection<String> param1, Objective param2, int param3) {
      Scoreboard var4 = param0.getServer().getScoreboard();
      int var5 = 0;

      for (String var7 : param1) {
         Score var8 = var4.getOrCreatePlayerScore(var7, param2);
         var8.setScore(var8.getScore() + param3);
         var5 += var8.getScore();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.add.success.single", param3, param2.getFormattedDisplayName(), param1.iterator().next(), var5),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.add.success.multiple", param3, param2.getFormattedDisplayName(), param1.size()), true
         );
      }

      return var5;
   }

   private static int removeScore(CommandSourceStack param0, Collection<String> param1, Objective param2, int param3) {
      Scoreboard var4 = param0.getServer().getScoreboard();
      int var5 = 0;

      for (String var7 : param1) {
         Score var8 = var4.getOrCreatePlayerScore(var7, param2);
         var8.setScore(var8.getScore() - param3);
         var5 += var8.getScore();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable(
               "commands.scoreboard.players.remove.success.single", param3, param2.getFormattedDisplayName(), param1.iterator().next(), var5
            ),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.players.remove.success.multiple", param3, param2.getFormattedDisplayName(), param1.size()), true
         );
      }

      return var5;
   }

   private static int listTrackedPlayers(CommandSourceStack param0) {
      Collection<String> var1 = param0.getServer().getScoreboard().getTrackedPlayers();
      if (var1.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.list.empty"), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.list.success", var1.size(), ComponentUtils.formatList(var1)), false);
      }

      return var1.size();
   }

   private static int listTrackedPlayerScores(CommandSourceStack param0, String param1) {
      Map<Objective, Score> var2 = param0.getServer().getScoreboard().getPlayerScores(param1);
      if (var2.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.list.entity.empty", param1), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.scoreboard.players.list.entity.success", param1, var2.size()), false);

         for (Entry<Objective, Score> var4 : var2.entrySet()) {
            param0.sendSuccess(
               Component.translatable("commands.scoreboard.players.list.entity.entry", var4.getKey().getFormattedDisplayName(), var4.getValue().getScore()),
               false
            );
         }
      }

      return var2.size();
   }

   private static int clearDisplaySlot(CommandSourceStack param0, int param1) throws CommandSyntaxException {
      Scoreboard var2 = param0.getServer().getScoreboard();
      if (var2.getDisplayObjective(param1) == null) {
         throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
      }

      var2.setDisplayObjective(param1, null);
      param0.sendSuccess(Component.translatable("commands.scoreboard.objectives.display.cleared", Scoreboard.getDisplaySlotNames()[param1]), true);
      return 0;
   }

   private static int setDisplaySlot(CommandSourceStack param0, int param1, Objective param2) throws CommandSyntaxException {
      Scoreboard var3 = param0.getServer().getScoreboard();
      if (var3.getDisplayObjective(param1) == param2) {
         throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
      }

      var3.setDisplayObjective(param1, param2);
      param0.sendSuccess(
         Component.translatable("commands.scoreboard.objectives.display.set", Scoreboard.getDisplaySlotNames()[param1], param2.getDisplayName()), true
      );
      return 0;
   }

   private static int setDisplayName(CommandSourceStack param0, Objective param1, Component param2) {
      if (!param1.getDisplayName().equals(param2)) {
         param1.setDisplayName(param2);
         param0.sendSuccess(
            Component.translatable("commands.scoreboard.objectives.modify.displayname", param1.getName(), param1.getFormattedDisplayName()), true
         );
      }

      return 0;
   }

   private static int setRenderType(CommandSourceStack param0, Objective param1, ObjectiveCriteria.RenderType param2) {
      if (param1.getRenderType() != param2) {
         param1.setRenderType(param2);
         param0.sendSuccess(Component.translatable("commands.scoreboard.objectives.modify.rendertype", param1.getFormattedDisplayName()), true);
      }

      return 0;
   }

   private static int removeObjective(CommandSourceStack param0, Objective param1) {
      Scoreboard var2 = param0.getServer().getScoreboard();
      var2.removeObjective(param1);
      param0.sendSuccess(Component.translatable("commands.scoreboard.objectives.remove.success", param1.getFormattedDisplayName()), true);
      return var2.getObjectives().size();
   }

   private static int addObjective(CommandSourceStack param0, String param1, ObjectiveCriteria param2, Component param3) throws CommandSyntaxException {
      Scoreboard var4 = param0.getServer().getScoreboard();
      if (var4.getObjective(param1) != null) {
         throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
      }

      var4.addObjective(param1, param2, param3, param2.getDefaultRenderType());
      Objective var5 = var4.getObjective(param1);
      param0.sendSuccess(Component.translatable("commands.scoreboard.objectives.add.success", var5.getFormattedDisplayName()), true);
      return var4.getObjectives().size();
   }

   private static int listObjectives(CommandSourceStack param0) {
      Collection<Objective> var1 = param0.getServer().getScoreboard().getObjectives();
      if (var1.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.scoreboard.objectives.list.empty"), false);
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.scoreboard.objectives.list.success", var1.size(), ComponentUtils.formatList(var1, Objective::getFormattedDisplayName)
            ),
            false
         );
      }

      return var1.size();
   }
}
