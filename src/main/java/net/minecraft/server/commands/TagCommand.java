package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.Entity;

public class TagCommand {
   private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.add.failed"));
   private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.tag.remove.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("tag")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("targets", EntityArgument.entities())
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("name", StringArgumentType.word())
                              .executes(
                                 l_param0 -> addTag(
                                    l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets"), StringArgumentType.getString(l_param0, "name")
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("remove")
                        .then(
                           Commands.argument("name", StringArgumentType.word())
                              .suggests(
                                 (l_param0, l_param1) -> SharedSuggestionProvider.suggest(getTags(EntityArgument.getEntities(l_param0, "targets")), l_param1)
                              )
                              .executes(
                                 l_param0 -> removeTag(
                                    l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets"), StringArgumentType.getString(l_param0, "name")
                                 )
                              )
                        )
                  )
                  .then(Commands.literal("list").executes(l_param0 -> listTags(l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets"))))
            )
      );
   }

   private static Collection<String> getTags(Collection<? extends Entity> param0) {
      Set<String> var1 = Sets.newHashSet();

      for (Entity var3 : param0) {
         var1.addAll(var3.getTags());
      }

      return var1;
   }

   private static int addTag(CommandSourceStack param0, Collection<? extends Entity> param1, String param2) throws CommandSyntaxException {
      int var3 = 0;

      for (Entity var5 : param1) {
         if (var5.addTag(param2)) {
            var3++;
         }
      }

      if (var3 == 0) {
         throw ERROR_ADD_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.tag.add.success.single", param2, param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.tag.add.success.multiple", param2, param1.size()), true);
      }

      return var3;
   }

   private static int removeTag(CommandSourceStack param0, Collection<? extends Entity> param1, String param2) throws CommandSyntaxException {
      int var3 = 0;

      for (Entity var5 : param1) {
         if (var5.removeTag(param2)) {
            var3++;
         }
      }

      if (var3 == 0) {
         throw ERROR_REMOVE_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.tag.remove.success.single", param2, param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.tag.remove.success.multiple", param2, param1.size()), true);
      }

      return var3;
   }

   private static int listTags(CommandSourceStack param0, Collection<? extends Entity> param1) {
      Set<String> var2 = Sets.newHashSet();

      for (Entity var4 : param1) {
         var2.addAll(var4.getTags());
      }

      if (param1.size() == 1) {
         Entity var3 = param1.iterator().next();
         if (var2.isEmpty()) {
            param0.sendSuccess(Component.translatable("commands.tag.list.single.empty", var3.getDisplayName()), false);
         } else {
            param0.sendSuccess(
               Component.translatable("commands.tag.list.single.success", var3.getDisplayName(), var2.size(), ComponentUtils.formatList(var2)), false
            );
         }
      } else if (var2.isEmpty()) {
         param0.sendSuccess(Component.translatable("commands.tag.list.multiple.empty", param1.size()), false);
      } else {
         param0.sendSuccess(Component.translatable("commands.tag.list.multiple.success", param1.size(), var2.size(), ComponentUtils.formatList(var2)), false);
      }

      return var2.size();
   }
}
