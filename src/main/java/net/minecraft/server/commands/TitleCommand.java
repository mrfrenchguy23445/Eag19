package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("title")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(Commands.literal("clear").executes(l_param0 -> clearTitle(l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"))))
                  .then(Commands.literal("reset").executes(l_param0 -> resetTitle(l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"))))
                  .then(
                     Commands.literal("title")
                        .then(
                           Commands.argument("title", ComponentArgument.textComponent())
                              .executes(
                                 l_param0 -> showTitle(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    ComponentArgument.getComponent(l_param0, "title"),
                                    "title",
                                    ClientboundSetTitleTextPacket::new
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("subtitle")
                        .then(
                           Commands.argument("title", ComponentArgument.textComponent())
                              .executes(
                                 l_param0 -> showTitle(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    ComponentArgument.getComponent(l_param0, "title"),
                                    "subtitle",
                                    ClientboundSetSubtitleTextPacket::new
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("actionbar")
                        .then(
                           Commands.argument("title", ComponentArgument.textComponent())
                              .executes(
                                 l_param0 -> showTitle(
                                    l_param0.getSource(),
                                    EntityArgument.getPlayers(l_param0, "targets"),
                                    ComponentArgument.getComponent(l_param0, "title"),
                                    "actionbar",
                                    ClientboundSetActionBarTextPacket::new
                                 )
                              )
                        )
                  )
                  .then(
                     Commands.literal("times")
                        .then(
                           Commands.argument("fadeIn", TimeArgument.time())
                              .then(
                                 Commands.argument("stay", TimeArgument.time())
                                    .then(
                                       Commands.argument("fadeOut", TimeArgument.time())
                                          .executes(
                                             l_param0 -> setTimes(
                                                l_param0.getSource(),
                                                EntityArgument.getPlayers(l_param0, "targets"),
                                                IntegerArgumentType.getInteger(l_param0, "fadeIn"),
                                                IntegerArgumentType.getInteger(l_param0, "stay"),
                                                IntegerArgumentType.getInteger(l_param0, "fadeOut")
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int clearTitle(CommandSourceStack param0, Collection<ServerPlayer> param1) {
      ClientboundClearTitlesPacket var2 = new ClientboundClearTitlesPacket(false);

      for (ServerPlayer var4 : param1) {
         var4.connection.send(var2);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.title.cleared.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.title.cleared.multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int resetTitle(CommandSourceStack param0, Collection<ServerPlayer> param1) {
      ClientboundClearTitlesPacket var2 = new ClientboundClearTitlesPacket(true);

      for (ServerPlayer var4 : param1) {
         var4.connection.send(var2);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.title.reset.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.title.reset.multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int showTitle(
      CommandSourceStack param0, Collection<ServerPlayer> param1, Component param2, String param3, Function<Component, Packet<?>> param4
   ) throws CommandSyntaxException {
      for (ServerPlayer var6 : param1) {
         var6.connection.send(param4.apply(ComponentUtils.updateForEntity(param0, param2, var6, 0)));
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.title.show." + param3 + ".single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.title.show." + param3 + ".multiple", param1.size()), true);
      }

      return param1.size();
   }

   private static int setTimes(CommandSourceStack param0, Collection<ServerPlayer> param1, int param2, int param3, int param4) {
      ClientboundSetTitlesAnimationPacket var5 = new ClientboundSetTitlesAnimationPacket(param2, param3, param4);

      for (ServerPlayer var7 : param1) {
         var7.connection.send(var5);
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.title.times.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.title.times.multiple", param1.size()), true);
      }

      return param1.size();
   }
}
