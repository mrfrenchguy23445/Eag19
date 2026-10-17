package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_ADVANCEMENTS = (param0, param1) -> {
      Collection<Advancement> var2 = param0.getSource().getServer().getAdvancements().getAllAdvancements();
      return SharedSuggestionProvider.suggestResource(var2.stream().map(Advancement::getId), param1);
   };

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("advancement")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("grant")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .then(
                           Commands.literal("only")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.GRANT,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.ONLY)
                                       )
                                    )
                                    .then(
                                       Commands.argument("criterion", StringArgumentType.greedyString())
                                          .suggests(
                                             (l_param0, l_param1) -> SharedSuggestionProvider.suggest(
                                                ResourceLocationArgument.getAdvancement(l_param0, "advancement").getCriteria().keySet(), l_param1
                                             )
                                          )
                                          .executes(
                                             l_param0 -> performCriterion(
                                                l_param0.getSource(),
                                                EntityArgument.getPlayers(l_param0, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                ResourceLocationArgument.getAdvancement(l_param0, "advancement"),
                                                StringArgumentType.getString(l_param0, "criterion")
                                             )
                                          )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("from")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.GRANT,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.FROM)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("until")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.GRANT,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.UNTIL)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("through")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.GRANT,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.THROUGH)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("everything")
                              .executes(
                                 l_param0 -> perform(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    AdvancementCommands.Action.GRANT,
                                    l_param0.getSource().getServer().getAdvancements().getAllAdvancements()
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("revoke")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .then(
                           Commands.literal("only")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.REVOKE,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.ONLY)
                                       )
                                    )
                                    .then(
                                       Commands.argument("criterion", StringArgumentType.greedyString())
                                          .suggests(
                                             (l_param0, l_param1) -> SharedSuggestionProvider.suggest(
                                                ResourceLocationArgument.getAdvancement(l_param0, "advancement").getCriteria().keySet(), l_param1
                                             )
                                          )
                                          .executes(
                                             l_param0 -> performCriterion(
                                                l_param0.getSource(),
                                                EntityArgument.getPlayers(l_param0, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                ResourceLocationArgument.getAdvancement(l_param0, "advancement"),
                                                StringArgumentType.getString(l_param0, "criterion")
                                             )
                                          )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("from")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.REVOKE,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.FROM)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("until")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.REVOKE,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.UNTIL)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("through")
                              .then(
                                 Commands.argument("advancement", ResourceLocationArgument.id())
                                    .suggests(SUGGEST_ADVANCEMENTS)
                                    .executes(
                                       l_param0 -> perform(
                                          l_param0.getSource(),
                                          EntityArgument.getPlayers(l_param0, "targets"),
                                          AdvancementCommands.Action.REVOKE,
                                          getAdvancements(ResourceLocationArgument.getAdvancement(l_param0, "advancement"), AdvancementCommands.Mode.THROUGH)
                                       )
                                    )
                              )
                        )
                        .then(
                           Commands.literal("everything")
                              .executes(
                                 l_param0 -> perform(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    AdvancementCommands.Action.REVOKE,
                                    l_param0.getSource().getServer().getAdvancements().getAllAdvancements()
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int perform(CommandSourceStack param0, Collection<ServerPlayer> param1, AdvancementCommands.Action param2, Collection<Advancement> param3) {
      int var4 = 0;

      for (ServerPlayer var6 : param1) {
         var4 += param2.perform(var6, param3);
      }

      if (var4 == 0) {
         if (param3.size() == 1) {
            if (param1.size() == 1) {
               throw new CommandRuntimeException(
                  Component.translatable(
                     param2.getKey() + ".one.to.one.failure", param3.iterator().next().getChatComponent(), param1.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw new CommandRuntimeException(
                  Component.translatable(param2.getKey() + ".one.to.many.failure", param3.iterator().next().getChatComponent(), param1.size())
               );
            }
         } else if (param1.size() == 1) {
            throw new CommandRuntimeException(
               Component.translatable(param2.getKey() + ".many.to.one.failure", param3.size(), param1.iterator().next().getDisplayName())
            );
         } else {
            throw new CommandRuntimeException(Component.translatable(param2.getKey() + ".many.to.many.failure", param3.size(), param1.size()));
         }
      } else {
         if (param3.size() == 1) {
            if (param1.size() == 1) {
               param0.sendSuccess(
                  Component.translatable(
                     param2.getKey() + ".one.to.one.success", param3.iterator().next().getChatComponent(), param1.iterator().next().getDisplayName()
                  ),
                  true
               );
            } else {
               param0.sendSuccess(
                  Component.translatable(param2.getKey() + ".one.to.many.success", param3.iterator().next().getChatComponent(), param1.size()), true
               );
            }
         } else if (param1.size() == 1) {
            param0.sendSuccess(Component.translatable(param2.getKey() + ".many.to.one.success", param3.size(), param1.iterator().next().getDisplayName()), true);
         } else {
            param0.sendSuccess(Component.translatable(param2.getKey() + ".many.to.many.success", param3.size(), param1.size()), true);
         }

         return var4;
      }
   }

   private static int performCriterion(
      CommandSourceStack param0, Collection<ServerPlayer> param1, AdvancementCommands.Action param2, Advancement param3, String param4
   ) {
      int var5 = 0;
      if (!param3.getCriteria().containsKey(param4)) {
         throw new CommandRuntimeException(Component.translatable("commands.advancement.criterionNotFound", param3.getChatComponent(), param4));
      }

      for (ServerPlayer var7 : param1) {
         if (param2.performCriterion(var7, param3, param4)) {
            var5++;
         }
      }

      if (var5 == 0) {
         if (param1.size() == 1) {
            throw new CommandRuntimeException(
               Component.translatable(
                  param2.getKey() + ".criterion.to.one.failure", param4, param3.getChatComponent(), param1.iterator().next().getDisplayName()
               )
            );
         } else {
            throw new CommandRuntimeException(
               Component.translatable(param2.getKey() + ".criterion.to.many.failure", param4, param3.getChatComponent(), param1.size())
            );
         }
      } else {
         if (param1.size() == 1) {
            param0.sendSuccess(
               Component.translatable(
                  param2.getKey() + ".criterion.to.one.success", param4, param3.getChatComponent(), param1.iterator().next().getDisplayName()
               ),
               true
            );
         } else {
            param0.sendSuccess(Component.translatable(param2.getKey() + ".criterion.to.many.success", param4, param3.getChatComponent(), param1.size()), true);
         }

         return var5;
      }
   }

   private static List<Advancement> getAdvancements(Advancement param0, AdvancementCommands.Mode param1) {
      List<Advancement> var2 = Lists.newArrayList();
      if (param1.parents) {
         for (Advancement var3 = param0.getParent(); var3 != null; var3 = var3.getParent()) {
            var2.add(var3);
         }
      }

      var2.add(param0);
      if (param1.children) {
         addChildren(param0, var2);
      }

      return var2;
   }

   private static void addChildren(Advancement param0, List<Advancement> param1) {
      for (Advancement var3 : param0.getChildren()) {
         param1.add(var3);
         addChildren(var3, param1);
      }
   }

   enum Action {
      GRANT("grant") {
         @Override
         protected boolean perform(ServerPlayer param0, Advancement param1) {
            AdvancementProgress var3 = param0.getAdvancements().getOrStartProgress(param1);
            if (var3.isDone()) {
               return false;
            }

            for (String var5 : var3.getRemainingCriteria()) {
               param0.getAdvancements().award(param1, var5);
            }

            return true;
         }

         @Override
         protected boolean performCriterion(ServerPlayer param0, Advancement param1, String param2) {
            return param0.getAdvancements().award(param1, param2);
         }
      },
      REVOKE("revoke") {
         @Override
         protected boolean perform(ServerPlayer param0, Advancement param1) {
            AdvancementProgress var3 = param0.getAdvancements().getOrStartProgress(param1);
            if (!var3.hasProgress()) {
               return false;
            }

            for (String var5 : var3.getCompletedCriteria()) {
               param0.getAdvancements().revoke(param1, var5);
            }

            return true;
         }

         @Override
         protected boolean performCriterion(ServerPlayer param0, Advancement param1, String param2) {
            return param0.getAdvancements().revoke(param1, param2);
         }
      };

      private final String key;

      Action(String param2) {
         this.key = "commands.advancement." + param2;
      }

      public int perform(ServerPlayer param0, Iterable<Advancement> param1) {
         int var3 = 0;

         for (Advancement var5 : param1) {
            if (this.perform(param0, var5)) {
               var3++;
            }
         }

         return var3;
      }

      protected abstract boolean perform(ServerPlayer var1, Advancement var2);

      protected abstract boolean performCriterion(ServerPlayer var1, Advancement var2, String var3);

      protected String getKey() {
         return this.key;
      }
   }

   enum Mode {
      ONLY(false, false),
      THROUGH(true, true),
      FROM(false, true),
      UNTIL(true, false),
      EVERYTHING(true, true);

      final boolean parents;
      final boolean children;

      Mode(boolean param2, boolean param3) {
         this.parents = param2;
         this.children = param3;
      }
   }
}
