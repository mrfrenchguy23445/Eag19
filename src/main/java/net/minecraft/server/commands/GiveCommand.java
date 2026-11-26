package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class GiveCommand {
   public static final int MAX_ALLOWED_ITEMSTACKS = 100;

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("give")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     Commands.argument("item", ItemArgument.item(param1))
                        .executes(
                           l_param0 -> giveItem(l_param0.getSource(), ItemArgument.getItem(l_param0, "item"), EntityArgument.getPlayers(l_param0, "targets"), 1)
                        )
                        .then(
                           Commands.argument("count", IntegerArgumentType.integer(1))
                              .executes(
                                 l_param0 -> giveItem(
                                    l_param0.getSource(),
                                    ItemArgument.getItem(l_param0, "item"),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    IntegerArgumentType.getInteger(l_param0, "count")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int giveItem(CommandSourceStack param0, ItemInput param1, Collection<ServerPlayer> param2, int param3) throws CommandSyntaxException {
      int var4 = param1.getItem().getMaxStackSize();
      int var5 = var4 * 100;
      if (param3 > var5) {
         param0.sendFailure(Component.translatable("commands.give.failed.toomanyitems", var5, param1.createItemStack(param3, false).getDisplayName()));
         return 0;
      }

      for (ServerPlayer var7 : param2) {
         int var8 = param3;

         while (var8 > 0) {
            int var9 = Math.min(var4, var8);
            var8 -= var9;
            ItemStack var10 = param1.createItemStack(var9, false);
            boolean var11 = var7.getInventory().add(var10);
            if (var11 && var10.isEmpty()) {
               var10.setCount(1);
               ItemEntity var12 = var7.drop(var10, false);
               if (var12 != null) {
                  var12.makeFakeItem();
               }

               var7.level
                  .playSound(
                     null,
                     var7.getX(),
                     var7.getY(),
                     var7.getZ(),
                     SoundEvents.ITEM_PICKUP,
                     SoundSource.PLAYERS,
                     0.2F,
                     ((var7.getRandom().nextFloat() - var7.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                  );
               var7.containerMenu.broadcastChanges();
            } else {
               ItemEntity var12 = var7.drop(var10, false);
               if (var12 != null) {
                  var12.setNoPickUpDelay();
                  var12.setTarget(var7.getUUID());
               }
            }
         }
      }

      if (param2.size() == 1) {
         param0.sendSuccess(
            Component.translatable(
               "commands.give.success.single", param3, param1.createItemStack(param3, false).getDisplayName(), param2.iterator().next().getDisplayName()
            ),
            true
         );
      } else {
         param0.sendSuccess(
            Component.translatable("commands.give.success.single", param3, param1.createItemStack(param3, false).getDisplayName(), param2.size()), true
         );
      }

      return param2.size();
   }
}
