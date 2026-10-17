package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;

public class BossBarCommands {
   private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.bossbar.create.failed", param0)
   );
   private static final DynamicCommandExceptionType ERROR_DOESNT_EXIST = new DynamicCommandExceptionType(
      param0 -> Component.translatable("commands.bossbar.unknown", param0)
   );
   private static final SimpleCommandExceptionType ERROR_NO_PLAYER_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.players.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_NAME_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.name.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_COLOR_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.color.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_STYLE_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.style.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_VALUE_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.value.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_MAX_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.max.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_HIDDEN = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.visibility.unchanged.hidden")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_VISIBLE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.visibility.unchanged.visible")
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_BOSS_BAR = (param0, param1) -> SharedSuggestionProvider.suggestResource(
      param0.getSource().getServer().getCustomBossEvents().getIds(), param1
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("bossbar")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("add")
                  .then(
                     Commands.argument("id", ResourceLocationArgument.id())
                        .then(
                           Commands.argument("name", ComponentArgument.textComponent())
                              .executes(
                                 l_param0 -> createBar(
                                    l_param0.getSource(), ResourceLocationArgument.getId(l_param0, "id"), ComponentArgument.getComponent(l_param0, "name")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("remove")
                  .then(
                     Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(SUGGEST_BOSS_BAR)
                        .executes(l_param0 -> removeBar(l_param0.getSource(), getBossBar(l_param0)))
                  )
            )
            .then(Commands.literal("list").executes(l_param0 -> listBars(l_param0.getSource())))
            .then(
               Commands.literal("set")
                  .then(
                     Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(SUGGEST_BOSS_BAR)
                        .then(
                           Commands.literal("name")
                              .then(
                                 Commands.argument("name", ComponentArgument.textComponent())
                                    .executes(l_param0 -> setName(l_param0.getSource(), getBossBar(l_param0), ComponentArgument.getComponent(l_param0, "name")))
                              )
                        )
                        .then(
                           Commands.literal("color")
                              .then(
                                 Commands.literal("pink")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.PINK))
                              )
                              .then(
                                 Commands.literal("blue")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.BLUE))
                              )
                              .then(
                                 Commands.literal("red").executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.RED))
                              )
                              .then(
                                 Commands.literal("green")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.GREEN))
                              )
                              .then(
                                 Commands.literal("yellow")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.YELLOW))
                              )
                              .then(
                                 Commands.literal("purple")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.PURPLE))
                              )
                              .then(
                                 Commands.literal("white")
                                    .executes(l_param0 -> setColor(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarColor.WHITE))
                              )
                        )
                        .then(
                           Commands.literal("style")
                              .then(
                                 Commands.literal("progress")
                                    .executes(l_param0 -> setStyle(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarOverlay.PROGRESS))
                              )
                              .then(
                                 Commands.literal("notched_6")
                                    .executes(l_param0 -> setStyle(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarOverlay.NOTCHED_6))
                              )
                              .then(
                                 Commands.literal("notched_10")
                                    .executes(l_param0 -> setStyle(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarOverlay.NOTCHED_10))
                              )
                              .then(
                                 Commands.literal("notched_12")
                                    .executes(l_param0 -> setStyle(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarOverlay.NOTCHED_12))
                              )
                              .then(
                                 Commands.literal("notched_20")
                                    .executes(l_param0 -> setStyle(l_param0.getSource(), getBossBar(l_param0), BossEvent.BossBarOverlay.NOTCHED_20))
                              )
                        )
                        .then(
                           Commands.literal("value")
                              .then(
                                 Commands.argument("value", IntegerArgumentType.integer(0))
                                    .executes(
                                       l_param0 -> setValue(l_param0.getSource(), getBossBar(l_param0), IntegerArgumentType.getInteger(l_param0, "value"))
                                    )
                              )
                        )
                        .then(
                           Commands.literal("max")
                              .then(
                                 Commands.argument("max", IntegerArgumentType.integer(1))
                                    .executes(l_param0 -> setMax(l_param0.getSource(), getBossBar(l_param0), IntegerArgumentType.getInteger(l_param0, "max")))
                              )
                        )
                        .then(
                           Commands.literal("visible")
                              .then(
                                 Commands.argument("visible", BoolArgumentType.bool())
                                    .executes(l_param0 -> setVisible(l_param0.getSource(), getBossBar(l_param0), BoolArgumentType.getBool(l_param0, "visible")))
                              )
                        )
                        .then(
                           Commands.literal("players")
                              .executes(l_param0 -> setPlayers(l_param0.getSource(), getBossBar(l_param0), Collections.emptyList()))
                              .then(
                                 Commands.argument("targets", EntityArgument.players())
                                    .executes(
                                       l_param0 -> setPlayers(
                                          l_param0.getSource(), getBossBar(l_param0), EntityArgument.getOptionalPlayers(l_param0, "targets")
                                       )
                                    )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("get")
                  .then(
                     Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(SUGGEST_BOSS_BAR)
                        .then(Commands.literal("value").executes(l_param0 -> getValue(l_param0.getSource(), getBossBar(l_param0))))
                        .then(Commands.literal("max").executes(l_param0 -> getMax(l_param0.getSource(), getBossBar(l_param0))))
                        .then(Commands.literal("visible").executes(l_param0 -> getVisible(l_param0.getSource(), getBossBar(l_param0))))
                        .then(Commands.literal("players").executes(l_param0 -> getPlayers(l_param0.getSource(), getBossBar(l_param0))))
                  )
            )
      );
   }

   private static int getValue(CommandSourceStack param0, CustomBossEvent param1) {
      param0.sendSuccess(Component.translatable("commands.bossbar.get.value", param1.getDisplayName(), param1.getValue()), true);
      return param1.getValue();
   }

   private static int getMax(CommandSourceStack param0, CustomBossEvent param1) {
      param0.sendSuccess(Component.translatable("commands.bossbar.get.max", param1.getDisplayName(), param1.getMax()), true);
      return param1.getMax();
   }

   private static int getVisible(CommandSourceStack param0, CustomBossEvent param1) {
      if (param1.isVisible()) {
         param0.sendSuccess(Component.translatable("commands.bossbar.get.visible.visible", param1.getDisplayName()), true);
         return 1;
      } else {
         param0.sendSuccess(Component.translatable("commands.bossbar.get.visible.hidden", param1.getDisplayName()), true);
         return 0;
      }
   }

   private static int getPlayers(CommandSourceStack param0, CustomBossEvent param1) {
      if (param1.getPlayers().isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.bossbar.get.players.none", param1.getDisplayName()), true);
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.bossbar.get.players.some",
               param1.getDisplayName(),
               param1.getPlayers().size(),
               ComponentUtils.formatList(param1.getPlayers(), Player::getDisplayName)
            ),
            true
         );
      }

      return param1.getPlayers().size();
   }

   private static int setVisible(CommandSourceStack param0, CustomBossEvent param1, boolean param2) throws CommandSyntaxException {
      if (param1.isVisible() == param2) {
         if (param2) {
            throw ERROR_ALREADY_VISIBLE.create();
         } else {
            throw ERROR_ALREADY_HIDDEN.create();
         }
      } else {
         param1.setVisible(param2);
         if (param2) {
            param0.sendSuccess(Component.translatable("commands.bossbar.set.visible.success.visible", param1.getDisplayName()), true);
         } else {
            param0.sendSuccess(Component.translatable("commands.bossbar.set.visible.success.hidden", param1.getDisplayName()), true);
         }

         return 0;
      }
   }

   private static int setValue(CommandSourceStack param0, CustomBossEvent param1, int param2) throws CommandSyntaxException {
      if (param1.getValue() == param2) {
         throw ERROR_NO_VALUE_CHANGE.create();
      }

      param1.setValue(param2);
      param0.sendSuccess(Component.translatable("commands.bossbar.set.value.success", param1.getDisplayName(), param2), true);
      return param2;
   }

   private static int setMax(CommandSourceStack param0, CustomBossEvent param1, int param2) throws CommandSyntaxException {
      if (param1.getMax() == param2) {
         throw ERROR_NO_MAX_CHANGE.create();
      }

      param1.setMax(param2);
      param0.sendSuccess(Component.translatable("commands.bossbar.set.max.success", param1.getDisplayName(), param2), true);
      return param2;
   }

   private static int setColor(CommandSourceStack param0, CustomBossEvent param1, BossEvent.BossBarColor param2) throws CommandSyntaxException {
      if (param1.getColor().equals(param2)) {
         throw ERROR_NO_COLOR_CHANGE.create();
      }

      param1.setColor(param2);
      param0.sendSuccess(Component.translatable("commands.bossbar.set.color.success", param1.getDisplayName()), true);
      return 0;
   }

   private static int setStyle(CommandSourceStack param0, CustomBossEvent param1, BossEvent.BossBarOverlay param2) throws CommandSyntaxException {
      if (param1.getOverlay().equals(param2)) {
         throw ERROR_NO_STYLE_CHANGE.create();
      }

      param1.setOverlay(param2);
      param0.sendSuccess(Component.translatable("commands.bossbar.set.style.success", param1.getDisplayName()), true);
      return 0;
   }

   private static int setName(CommandSourceStack param0, CustomBossEvent param1, Component param2) throws CommandSyntaxException {
      Component var3 = ComponentUtils.updateForEntity(param0, param2, null, 0);
      if (param1.getName().equals(var3)) {
         throw ERROR_NO_NAME_CHANGE.create();
      }

      param1.setName(var3);
      param0.sendSuccess(Component.translatable("commands.bossbar.set.name.success", param1.getDisplayName()), true);
      return 0;
   }

   private static int setPlayers(CommandSourceStack param0, CustomBossEvent param1, Collection<ServerPlayer> param2) throws CommandSyntaxException {
      boolean var3 = param1.setPlayers(param2);
      if (!var3) {
         throw ERROR_NO_PLAYER_CHANGE.create();
      }

      if (param1.getPlayers().isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.bossbar.set.players.success.none", param1.getDisplayName()), true);
      } else {
         param0.sendSuccess(
            Component.translatable(
               "commands.bossbar.set.players.success.some", param1.getDisplayName(), param2.size(), ComponentUtils.formatList(param2, Player::getDisplayName)
            ),
            true
         );
      }

      return param1.getPlayers().size();
   }

   private static int listBars(CommandSourceStack param0) {
      Collection<CustomBossEvent> var1 = param0.getServer().getCustomBossEvents().getEvents();
      if (var1.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.bossbar.list.bars.none"), false);
      } else {
         param0.sendSuccess(
            Component.translatable("commands.bossbar.list.bars.some", var1.size(), ComponentUtils.formatList(var1, CustomBossEvent::getDisplayName)), false
         );
      }

      return var1.size();
   }

   private static int createBar(CommandSourceStack param0, ResourceLocation param1, Component param2) throws CommandSyntaxException {
      CustomBossEvents var3 = param0.getServer().getCustomBossEvents();
      if (var3.get(param1) != null) {
         throw ERROR_ALREADY_EXISTS.create(param1.toString());
      }

      CustomBossEvent var4 = var3.create(param1, ComponentUtils.updateForEntity(param0, param2, null, 0));
      param0.sendSuccess(Component.translatable("commands.bossbar.create.success", var4.getDisplayName()), true);
      return var3.getEvents().size();
   }

   private static int removeBar(CommandSourceStack param0, CustomBossEvent param1) {
      CustomBossEvents var2 = param0.getServer().getCustomBossEvents();
      param1.removeAllPlayers();
      var2.remove(param1);
      param0.sendSuccess(Component.translatable("commands.bossbar.remove.success", param1.getDisplayName()), true);
      return var2.getEvents().size();
   }

   public static CustomBossEvent getBossBar(CommandContext<CommandSourceStack> param0) throws CommandSyntaxException {
      ResourceLocation var1 = ResourceLocationArgument.getId(param0, "id");
      CustomBossEvent var2 = param0.getSource().getServer().getCustomBossEvents().get(var1);
      if (var2 == null) {
         throw ERROR_DOESNT_EXIST.create(var1.toString());
      } else {
         return var2;
      }
   }
}
