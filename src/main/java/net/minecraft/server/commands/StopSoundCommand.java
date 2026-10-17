package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class StopSoundCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> var1 = Commands.argument("targets", EntityArgument.players())
         .executes(l_param0 -> stopSound(l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), null, null))
         .then(
            Commands.literal("*")
               .then(
                  Commands.argument("sound", ResourceLocationArgument.id())
                     .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                     .executes(
                        l_param0 -> stopSound(
                           l_param0.getSource(), EntityArgument.getPlayers(l_param0, "targets"), null, ResourceLocationArgument.getId(l_param0, "sound")
                        )
                     )
               )
         );

      for (SoundSource var5 : SoundSource.values()) {
         var1.then(
            Commands.literal(var5.getName())
               .executes(l_param1 -> stopSound(l_param1.getSource(), EntityArgument.getPlayers(l_param1, "targets"), var5, null))
               .then(
                  Commands.argument("sound", ResourceLocationArgument.id())
                     .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                     .executes(
                        l_param1 -> stopSound(
                           l_param1.getSource(), EntityArgument.getPlayers(l_param1, "targets"), var5, ResourceLocationArgument.getId(l_param1, "sound")
                        )
                     )
               )
         );
      }

      param0.register(Commands.literal("stopsound").requires(l_param0 -> l_param0.hasPermission(2)).then(var1));
   }

   private static int stopSound(CommandSourceStack param0, Collection<ServerPlayer> param1, @Nullable SoundSource param2, @Nullable ResourceLocation param3) {
      ClientboundStopSoundPacket var4 = new ClientboundStopSoundPacket(param3, param2);

      for (ServerPlayer var6 : param1) {
         var6.connection.send(var4);
      }

      if (param2 != null) {
         if (param3 != null) {
            param0.sendSuccess(Component.translatable("commands.stopsound.success.source.sound", param3, param2.getName()), true);
         } else {
            param0.sendSuccess(Component.translatable("commands.stopsound.success.source.any", param2.getName()), true);
         }
      } else if (param3 != null) {
         param0.sendSuccess(Component.translatable("commands.stopsound.success.sourceless.sound", param3), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.stopsound.success.sourceless.any"), true);
      }

      return param1.size();
   }
}
