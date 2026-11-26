package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
   private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("clear.failed.single", param0)
   );
   private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(
      param0 -> Component.translatable("clear.failed.multiple", param0)
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("clear")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .executes(
               l_param0 -> clearInventory(l_param0.getSource(), Collections.singleton(l_param0.getSource().getPlayerOrException()), l2_param0 -> true, -1)
            )
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .executes(l_param0 -> clearInventory(l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), l2_param0 -> true, -1))
                  .then(
                     Commands.argument("item", ItemPredicateArgument.itemPredicate(param1))
                        .executes(
                           l_param0 -> clearInventory(
                              l_param0.getSource(),
                              EntityArgument.getPlayers(l_param0, "targets"),
                              ItemPredicateArgument.getItemPredicate(l_param0, "item"),
                              -1
                           )
                        )
                        .then(
                           Commands.argument("maxCount", IntegerArgumentType.integer(0))
                              .executes(
                                 l_param0 -> clearInventory(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    ItemPredicateArgument.getItemPredicate(l_param0, "item"),
                                    IntegerArgumentType.getInteger(l_param0, "maxCount")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int clearInventory(CommandSourceStack param0, Collection<ServerPlayer> param1, Predicate<ItemStack> param2, int param3) throws CommandSyntaxException {
      int var4 = 0;

      for (ServerPlayer var6 : param1) {
         var4 += var6.getInventory().clearOrCountMatchingItems(param2, param3, var6.inventoryMenu.getCraftSlots());
         var6.containerMenu.broadcastChanges();
         var6.inventoryMenu.slotsChanged(var6.getInventory());
      }

      if (var4 == 0) {
         if (param1.size() == 1) {
            throw ERROR_SINGLE.create(param1.iterator().next().getName());
         } else {
            throw ERROR_MULTIPLE.create(param1.size());
         }
      } else {
         if (param3 == 0) {
            if (param1.size() == 1) {
               param0.sendSuccess(Component.translatable("commands.clear.test.single", var4, param1.iterator().next().getDisplayName()), true);
            } else {
               param0.sendSuccess(Component.translatable("commands.clear.test.multiple", var4, param1.size()), true);
            }
         } else if (param1.size() == 1) {
            param0.sendSuccess(Component.translatable("commands.clear.success.single", var4, param1.iterator().next().getDisplayName()), true);
         } else {
            param0.sendSuccess(Component.translatable("commands.clear.success.multiple", var4, param1.size()), true);
         }

         return var4;
      }
   }
}
