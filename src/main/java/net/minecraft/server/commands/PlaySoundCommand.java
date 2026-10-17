package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PlaySoundCommand {
   private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(Component.translatable("commands.playsound.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> var1 = Commands.argument("sound", ResourceLocationArgument.id())
         .suggests(SuggestionProviders.AVAILABLE_SOUNDS);

      for (SoundSource var5 : SoundSource.values()) {
         var1.then(source(var5));
      }

      param0.register(Commands.literal("playsound").requires(l_param0 -> l_param0.hasPermission(2)).then(var1));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource param0) {
      return Commands.literal(param0.getName())
         .then(
            Commands.argument("targets", EntityArgument.players())
               .executes(
                  l_param1 -> playSound(
                     l_param1.getSource(),
                     EntityArgument.getPlayers(l_param1, "targets"),
                     ResourceLocationArgument.getId(l_param1, "sound"),
                     param0,
                     l_param1.getSource().getPosition(),
                     1.0F,
                     1.0F,
                     0.0F
                  )
               )
               .then(
                  Commands.argument("pos", Vec3Argument.vec3())
                     .executes(
                        l_param1 -> playSound(
                           l_param1.getSource(),
                           EntityArgument.getPlayers(l_param1, "targets"),
                           ResourceLocationArgument.getId(l_param1, "sound"),
                           param0,
                           Vec3Argument.getVec3(l_param1, "pos"),
                           1.0F,
                           1.0F,
                           0.0F
                        )
                     )
                     .then(
                        Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                           .executes(
                              l_param1 -> playSound(
                                 l_param1.getSource(),
                                 EntityArgument.getPlayers(l_param1, "targets"),
                                 ResourceLocationArgument.getId(l_param1, "sound"),
                                 param0,
                                 Vec3Argument.getVec3(l_param1, "pos"),
                                 l_param1.getArgument("volume", Float.class),
                                 1.0F,
                                 0.0F
                              )
                           )
                           .then(
                              Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                 .executes(
                                    l_param1 -> playSound(
                                       l_param1.getSource(),
                                       EntityArgument.getPlayers(l_param1, "targets"),
                                       ResourceLocationArgument.getId(l_param1, "sound"),
                                       param0,
                                       Vec3Argument.getVec3(l_param1, "pos"),
                                       l_param1.getArgument("volume", Float.class),
                                       l_param1.getArgument("pitch", Float.class),
                                       0.0F
                                    )
                                 )
                                 .then(
                                    Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                       .executes(
                                          l_param1 -> playSound(
                                             l_param1.getSource(),
                                             EntityArgument.getPlayers(l_param1, "targets"),
                                             ResourceLocationArgument.getId(l_param1, "sound"),
                                             param0,
                                             Vec3Argument.getVec3(l_param1, "pos"),
                                             l_param1.getArgument("volume", Float.class),
                                             l_param1.getArgument("pitch", Float.class),
                                             l_param1.getArgument("minVolume", Float.class)
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static int playSound(
      CommandSourceStack param0,
      Collection<ServerPlayer> param1,
      ResourceLocation param2,
      SoundSource param3,
      Vec3 param4,
      float param5,
      float param6,
      float param7
   ) throws CommandSyntaxException {
      Holder<SoundEvent> var8 = Holder.direct(SoundEvent.createVariableRangeEvent(param2));
      double var9 = Mth.square(var8.value().getRange(param5));
      int var11 = 0;
      long var12 = param0.getLevel().getRandom().nextLong();

      for (ServerPlayer var15 : param1) {
         double var16 = param4.x - var15.getX();
         double var18 = param4.y - var15.getY();
         double var20 = param4.z - var15.getZ();
         double var22 = var16 * var16 + var18 * var18 + var20 * var20;
         Vec3 var24 = param4;
         float var25 = param5;
         if (var22 > var9) {
            if (param7 <= 0.0F) {
               continue;
            }

            double var26 = Math.sqrt(var22);
            var24 = new Vec3(var15.getX() + var16 / var26 * 2.0, var15.getY() + var18 / var26 * 2.0, var15.getZ() + var20 / var26 * 2.0);
            var25 = param7;
         }

         var15.connection.send(new ClientboundSoundPacket(var8, param3, var24.x(), var24.y(), var24.z(), var25, param6, var12));
         var11++;
      }

      if (var11 == 0) {
         throw ERROR_TOO_FAR.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.playsound.success.single", param2, param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.playsound.success.multiple", param2, param1.size()), true);
      }

      return var11;
   }
}
