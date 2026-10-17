package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("particle")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("name", ParticleArgument.particle(param1))
                  .executes(
                     l_param0 -> sendParticles(
                        l_param0.getSource(),
                        ParticleArgument.getParticle(l_param0, "name"),
                        l_param0.getSource().getPosition(),
                        Vec3.ZERO,
                        0.0F,
                        0,
                        false,
                        l_param0.getSource().getServer().getPlayerList().getPlayers()
                     )
                  )
                  .then(
                     Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           l_param0 -> sendParticles(
                              l_param0.getSource(),
                              ParticleArgument.getParticle(l_param0, "name"),
                              Vec3Argument.getVec3(l_param0, "pos"),
                              Vec3.ZERO,
                              0.0F,
                              0,
                              false,
                              l_param0.getSource().getServer().getPlayerList().getPlayers()
                           )
                        )
                        .then(
                           Commands.argument("delta", Vec3Argument.vec3(false))
                              .then(
                                 Commands.argument("speed", FloatArgumentType.floatArg(0.0F))
                                    .then(
                                       Commands.argument("count", IntegerArgumentType.integer(0))
                                          .executes(
                                             l_param0 -> sendParticles(
                                                l_param0.getSource(),
                                                ParticleArgument.getParticle(l_param0, "name"),
                                                Vec3Argument.getVec3(l_param0, "pos"),
                                                Vec3Argument.getVec3(l_param0, "delta"),
                                                FloatArgumentType.getFloat(l_param0, "speed"),
                                                IntegerArgumentType.getInteger(l_param0, "count"),
                                                false,
                                                l_param0.getSource().getServer().getPlayerList().getPlayers()
                                             )
                                          )
                                          .then(
                                             Commands.literal("force")
                                                .executes(
                                                   l_param0 -> sendParticles(
                                                      l_param0.getSource(),
                                                      ParticleArgument.getParticle(l_param0, "name"),
                                                      Vec3Argument.getVec3(l_param0, "pos"),
                                                      Vec3Argument.getVec3(l_param0, "delta"),
                                                      FloatArgumentType.getFloat(l_param0, "speed"),
                                                      IntegerArgumentType.getInteger(l_param0, "count"),
                                                      true,
                                                      l_param0.getSource().getServer().getPlayerList().getPlayers()
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("viewers", EntityArgument.players())
                                                      .executes(
                                                         l_param0 -> sendParticles(
                                                            l_param0.getSource(),
                                                            ParticleArgument.getParticle(l_param0, "name"),
                                                            Vec3Argument.getVec3(l_param0, "pos"),
                                                            Vec3Argument.getVec3(l_param0, "delta"),
                                                            FloatArgumentType.getFloat(l_param0, "speed"),
                                                            IntegerArgumentType.getInteger(l_param0, "count"),
                                                            true,
                                                            EntityArgument.getPlayers(l_param0, "viewers")
                                                         )
                                                      )
                                                )
                                          )
                                          .then(
                                             Commands.literal("normal")
                                                .executes(
                                                   l_param0 -> sendParticles(
                                                      l_param0.getSource(),
                                                      ParticleArgument.getParticle(l_param0, "name"),
                                                      Vec3Argument.getVec3(l_param0, "pos"),
                                                      Vec3Argument.getVec3(l_param0, "delta"),
                                                      FloatArgumentType.getFloat(l_param0, "speed"),
                                                      IntegerArgumentType.getInteger(l_param0, "count"),
                                                      false,
                                                      l_param0.getSource().getServer().getPlayerList().getPlayers()
                                                   )
                                                )
                                                .then(
                                                   Commands.argument("viewers", EntityArgument.players())
                                                      .executes(
                                                         l_param0 -> sendParticles(
                                                            l_param0.getSource(),
                                                            ParticleArgument.getParticle(l_param0, "name"),
                                                            Vec3Argument.getVec3(l_param0, "pos"),
                                                            Vec3Argument.getVec3(l_param0, "delta"),
                                                            FloatArgumentType.getFloat(l_param0, "speed"),
                                                            IntegerArgumentType.getInteger(l_param0, "count"),
                                                            false,
                                                            EntityArgument.getPlayers(l_param0, "viewers")
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

   private static int sendParticles(
      CommandSourceStack param0, ParticleOptions param1, Vec3 param2, Vec3 param3, float param4, int param5, boolean param6, Collection<ServerPlayer> param7
   ) throws CommandSyntaxException {
      int var8 = 0;

      for (ServerPlayer var10 : param7) {
         if (param0.getLevel().sendParticles(var10, param1, param6, param2.x, param2.y, param2.z, param5, param3.x, param3.y, param3.z, param4)) {
            var8++;
         }
      }

      if (var8 == 0) {
         throw ERROR_FAILED.create();
      }

      param0.sendSuccess(Component.translatable("commands.particle.success", BuiltInRegistries.PARTICLE_TYPE.getKey(param1.getType()).toString()), true);
      return var8;
   }
}
