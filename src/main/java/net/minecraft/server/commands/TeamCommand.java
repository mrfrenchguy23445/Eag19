package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class TeamCommand {
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.team.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.team.empty.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_NAME = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.name.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_COLOR = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.color.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.nametagVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.deathMessageVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_COLLISION_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.collisionRule.unchanged")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("team")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("list")
                  .executes(l_param0 -> listTeams(l_param0.getSource()))
                  .then(
                     Commands.argument("team", TeamArgument.team())
                        .executes(l_param0 -> listMembers(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team")))
                  )
            )
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("team", StringArgumentType.word())
                        .executes(l_param0 -> createTeam(l_param0.getSource(), StringArgumentType.getString(l_param0, "team")))
                        .then(
                           Commands.argument("displayName", ComponentArgument.textComponent())
                              .executes(
                                 l_param0 -> createTeam(
                                    l_param0.getSource(),
                                    StringArgumentType.getString(l_param0, "team"),
                                    ComponentArgument.getComponent(l_param0, "displayName")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("team", TeamArgument.team())
                        .executes(l_param0 -> deleteTeam(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team")))
                  )
            )
            .then(
               Commands.literal("empty")
                  .then(
                     Commands.argument("team", TeamArgument.team())
                        .executes(l_param0 -> emptyTeam(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team")))
                  )
            )
            .then(
               Commands.literal("join")
                  .then(
                     Commands.argument("team", TeamArgument.team())
                        .executes(
                           l_param0 -> joinTeam(
                              l_param0.getSource(),
                              TeamArgument.getTeam(l_param0, "team"),
                              Collections.singleton(l_param0.getSource().getEntityOrException().getScoreboardName())
                           )
                        )
                        .then(
                           Commands.argument("members", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .executes(
                                 l_param0 -> joinTeam(
                                    l_param0.getSource(),
                                    TeamArgument.getTeam(l_param0, "team"),
                                    ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "members")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("leave")
                  .then(
                     Commands.argument("members", ScoreHolderArgument.scoreHolders())
                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                        .executes(l_param0 -> leaveTeam(l_param0.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(l_param0, "members")))
                  )
            )
            .then(
               Commands.literal("modify")
                  .then(
                     Commands.argument("team", TeamArgument.team())
                        .then(
                           Commands.literal("displayName")
                              .then(
                                 Commands.argument("displayName", ComponentArgument.textComponent())
                                    .executes(
                                       l_param0 -> setDisplayName(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), ComponentArgument.getComponent(l_param0, "displayName")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("color")
                              .then(
                                 Commands.argument("value", ColorArgument.color())
                                    .executes(
                                       l_param0 -> setColor(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), ColorArgument.getColor(l_param0, "value")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("friendlyFire")
                              .then(
                                 Commands.argument("allowed", BoolArgumentType.bool())
                                    .executes(
                                       l_param0 -> setFriendlyFire(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), BoolArgumentType.getBool(l_param0, "allowed")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("seeFriendlyInvisibles")
                              .then(
                                 Commands.argument("allowed", BoolArgumentType.bool())
                                    .executes(
                                       l_param0 -> setFriendlySight(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), BoolArgumentType.getBool(l_param0, "allowed")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("nametagVisibility")
                              .then(
                                 Commands.literal("never")
                                    .executes(
                                       l_param0 -> setNametagVisibility(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.NEVER)
                                    )
                              )
                              .then(
                                 Commands.literal("hideForOtherTeams")
                                    .executes(
                                       l_param0 -> setNametagVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("hideForOwnTeam")
                                    .executes(
                                       l_param0 -> setNametagVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.HIDE_FOR_OWN_TEAM
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("always")
                                    .executes(
                                       l_param0 -> setNametagVisibility(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.ALWAYS)
                                    )
                              )
                        )
                        .then(
                           Commands.literal("deathMessageVisibility")
                              .then(
                                 Commands.literal("never")
                                    .executes(
                                       l_param0 -> setDeathMessageVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.NEVER
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("hideForOtherTeams")
                                    .executes(
                                       l_param0 -> setDeathMessageVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("hideForOwnTeam")
                                    .executes(
                                       l_param0 -> setDeathMessageVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.HIDE_FOR_OWN_TEAM
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("always")
                                    .executes(
                                       l_param0 -> setDeathMessageVisibility(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.Visibility.ALWAYS
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("collisionRule")
                              .then(
                                 Commands.literal("never")
                                    .executes(l_param0 -> setCollision(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.CollisionRule.NEVER))
                              )
                              .then(
                                 Commands.literal("pushOwnTeam")
                                    .executes(
                                       l_param0 -> setCollision(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.CollisionRule.PUSH_OWN_TEAM)
                                    )
                              )
                              .then(
                                 Commands.literal("pushOtherTeams")
                                    .executes(
                                       l_param0 -> setCollision(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.CollisionRule.PUSH_OTHER_TEAMS
                                       )
                                    )
                              )
                              .then(
                                 Commands.literal("always")
                                    .executes(l_param0 -> setCollision(l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), Team.CollisionRule.ALWAYS))
                              )
                        )
                        .then(
                           Commands.literal("prefix")
                              .then(
                                 Commands.argument("prefix", ComponentArgument.textComponent())
                                    .executes(
                                       l_param0 -> setPrefix(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), ComponentArgument.getComponent(l_param0, "prefix")
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("suffix")
                              .then(
                                 Commands.argument("suffix", ComponentArgument.textComponent())
                                    .executes(
                                       l_param0 -> setSuffix(
                                          l_param0.getSource(), TeamArgument.getTeam(l_param0, "team"), ComponentArgument.getComponent(l_param0, "suffix")
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int leaveTeam(CommandSourceStack param0, Collection<String> param1) {
      Scoreboard var2 = param0.getServer().getScoreboard();

      for (String var4 : param1) {
         var2.removePlayerFromTeam(var4);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.team.leave.success.single", param1.iterator().next()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.team.leave.success.multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int joinTeam(CommandSourceStack param0, PlayerTeam param1, Collection<String> param2) {
      Scoreboard var3 = param0.getServer().getScoreboard();

      for (String var5 : param2) {
         var3.addPlayerToTeam(var5, param1);
      }

      if (param2.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.team.join.success.single", param2.iterator().next(), param1.getFormattedDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.team.join.success.multiple", param2.size(), param1.getFormattedDisplayName()), true);
      }

      return param2.size();
   }

   private static int setNametagVisibility(CommandSourceStack param0, PlayerTeam param1, Team.Visibility param2) throws CommandSyntaxException {
      if (param1.getNameTagVisibility() == param2) {
         throw ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED.create();
      }

      param1.setNameTagVisibility(param2);
      param0.sendSuccess(
         Component.translatable("commands.team.option.nametagVisibility.success", param1.getFormattedDisplayName(), param2.getDisplayName()), true
      );
      return 0;
   }

   private static int setDeathMessageVisibility(CommandSourceStack param0, PlayerTeam param1, Team.Visibility param2) throws CommandSyntaxException {
      if (param1.getDeathMessageVisibility() == param2) {
         throw ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED.create();
      }

      param1.setDeathMessageVisibility(param2);
      param0.sendSuccess(
         Component.translatable("commands.team.option.deathMessageVisibility.success", param1.getFormattedDisplayName(), param2.getDisplayName()), true
      );
      return 0;
   }

   private static int setCollision(CommandSourceStack param0, PlayerTeam param1, Team.CollisionRule param2) throws CommandSyntaxException {
      if (param1.getCollisionRule() == param2) {
         throw ERROR_TEAM_COLLISION_UNCHANGED.create();
      }

      param1.setCollisionRule(param2);
      param0.sendSuccess(Component.translatable("commands.team.option.collisionRule.success", param1.getFormattedDisplayName(), param2.getDisplayName()), true);
      return 0;
   }

   private static int setFriendlySight(CommandSourceStack param0, PlayerTeam param1, boolean param2) throws CommandSyntaxException {
      if (param1.canSeeFriendlyInvisibles() == param2) {
         if (param2) {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED.create();
         }
      } else {
         param1.setSeeFriendlyInvisibles(param2);
         param0.sendSuccess(
            Component.translatable("commands.team.option.seeFriendlyInvisibles." + (param2 ? "enabled" : "disabled"), param1.getFormattedDisplayName()), true
         );
         return 0;
      }
   }

   private static int setFriendlyFire(CommandSourceStack param0, PlayerTeam param1, boolean param2) throws CommandSyntaxException {
      if (param1.isAllowFriendlyFire() == param2) {
         if (param2) {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED.create();
         }
      } else {
         param1.setAllowFriendlyFire(param2);
         param0.sendSuccess(
            Component.translatable("commands.team.option.friendlyfire." + (param2 ? "enabled" : "disabled"), param1.getFormattedDisplayName()), true
         );
         return 0;
      }
   }

   private static int setDisplayName(CommandSourceStack param0, PlayerTeam param1, Component param2) throws CommandSyntaxException {
      if (param1.getDisplayName().equals(param2)) {
         throw ERROR_TEAM_ALREADY_NAME.create();
      }

      param1.setDisplayName(param2);
      param0.sendSuccess(Component.translatable("commands.team.option.name.success", param1.getFormattedDisplayName()), true);
      return 0;
   }

   private static int setColor(CommandSourceStack param0, PlayerTeam param1, ChatFormatting param2) throws CommandSyntaxException {
      if (param1.getColor() == param2) {
         throw ERROR_TEAM_ALREADY_COLOR.create();
      }

      param1.setColor(param2);
      param0.sendSuccess(Component.translatable("commands.team.option.color.success", param1.getFormattedDisplayName(), param2.getName()), true);
      return 0;
   }

   private static int emptyTeam(CommandSourceStack param0, PlayerTeam param1) throws CommandSyntaxException {
      Scoreboard var2 = param0.getServer().getScoreboard();
      Collection<String> var3 = Lists.newArrayList(param1.getPlayers());
      if (var3.isEmpty()) {
         throw ERROR_TEAM_ALREADY_EMPTY.create();
      }

      for (String var5 : var3) {
         var2.removePlayerFromTeam(var5, param1);
      }

      param0.sendSuccess(Component.translatable("commands.team.empty.success", var3.size(), param1.getFormattedDisplayName()), true);
      return var3.size();
   }

   private static int deleteTeam(CommandSourceStack param0, PlayerTeam param1) {
      Scoreboard var2 = param0.getServer().getScoreboard();
      var2.removePlayerTeam(param1);
      param0.sendSuccess(Component.translatable("commands.team.remove.success", param1.getFormattedDisplayName()), true);
      return var2.getPlayerTeams().size();
   }

   private static int createTeam(CommandSourceStack param0, String param1) throws CommandSyntaxException {
      return createTeam(param0, param1, Component.literal(param1));
   }

   private static int createTeam(CommandSourceStack param0, String param1, Component param2) throws CommandSyntaxException {
      Scoreboard var3 = param0.getServer().getScoreboard();
      if (var3.getPlayerTeam(param1) != null) {
         throw ERROR_TEAM_ALREADY_EXISTS.create();
      }

      PlayerTeam var4 = var3.addPlayerTeam(param1);
      var4.setDisplayName(param2);
      param0.sendSuccess(Component.translatable("commands.team.add.success", var4.getFormattedDisplayName()), true);
      return var3.getPlayerTeams().size();
   }

   private static int listMembers(CommandSourceStack param0, PlayerTeam param1) {
      Collection<String> var2 = param1.getPlayers();
      if (var2.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.team.list.members.empty", param1.getFormattedDisplayName()), false);
      } else {
         param0.sendSuccess(
            Component.translatable("commands.team.list.members.success", param1.getFormattedDisplayName(), var2.size(), ComponentUtils.formatList(var2)), false
         );
      }

      return var2.size();
   }

   private static int listTeams(CommandSourceStack param0) {
      Collection<PlayerTeam> var1 = param0.getServer().getScoreboard().getPlayerTeams();
      if (var1.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.team.list.teams.empty"), false);
      } else {
         param0.sendSuccess(
            Component.translatable("commands.team.list.teams.success", var1.size(), ComponentUtils.formatList(var1, PlayerTeam::getFormattedDisplayName)),
            false
         );
      }

      return var1.size();
   }

   private static int setPrefix(CommandSourceStack param0, PlayerTeam param1, Component param2) {
      param1.setPlayerPrefix(param2);
      param0.sendSuccess(Component.translatable("commands.team.option.prefix.success", param2), false);
      return 1;
   }

   private static int setSuffix(CommandSourceStack param0, PlayerTeam param1, Component param2) {
      param1.setPlayerSuffix(param2);
      param0.sendSuccess(Component.translatable("commands.team.option.suffix.success", param2), false);
      return 1;
   }
}
