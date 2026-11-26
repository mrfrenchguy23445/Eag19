package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class DamageCommand {
   private static final SimpleCommandExceptionType ERROR_INVULNERABLE = new SimpleCommandExceptionType(Component.translatable("commands.damage.invulnerable"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("damage")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
                        .executes(
                           l_param0 -> damage(
                              l_param0.getSource(),
                              EntityArgument.getEntity(l_param0, "target"),
                              FloatArgumentType.getFloat(l_param0, "amount"),
                              l_param0.getSource().getLevel().damageSources().generic()
                           )
                        )
                        .then(
                           Commands.argument("damageType", ResourceArgument.resource(param1, Registries.DAMAGE_TYPE))
                              .executes(
                                 l_param0 -> damage(
                                    l_param0.getSource(),
                                    EntityArgument.getEntity(l_param0, "target"),
                                    FloatArgumentType.getFloat(l_param0, "amount"),
                                    new DamageSource(ResourceArgument.getResource(l_param0, "damageType", Registries.DAMAGE_TYPE))
                                 )
                              )
                              .then(
                                 Commands.literal("at")
                                    .then(
                                       Commands.argument("location", Vec3Argument.vec3())
                                          .executes(
                                             l_param0 -> damage(
                                                l_param0.getSource(),
                                                EntityArgument.getEntity(l_param0, "target"),
                                                FloatArgumentType.getFloat(l_param0, "amount"),
                                                new DamageSource(
                                                   ResourceArgument.getResource(l_param0, "damageType", Registries.DAMAGE_TYPE),
                                                   Vec3Argument.getVec3(l_param0, "location")
                                                )
                                             )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("by")
                                    .then(
                                       Commands.argument("entity", EntityArgument.entity())
                                          .executes(
                                             l_param0 -> damage(
                                                l_param0.getSource(),
                                                EntityArgument.getEntity(l_param0, "target"),
                                                FloatArgumentType.getFloat(l_param0, "amount"),
                                                new DamageSource(
                                                   ResourceArgument.getResource(l_param0, "damageType", Registries.DAMAGE_TYPE),
                                                   EntityArgument.getEntity(l_param0, "entity")
                                                )
                                             )
                                          )
                                          .then(
                                             Commands.literal("from")
                                                .then(
                                                   Commands.argument("cause", EntityArgument.entity())
                                                      .executes(
                                                         l_param0 -> damage(
                                                            l_param0.getSource(),
                                                            EntityArgument.getEntity(l_param0, "target"),
                                                            FloatArgumentType.getFloat(l_param0, "amount"),
                                                            new DamageSource(
                                                               ResourceArgument.getResource(l_param0, "damageType", Registries.DAMAGE_TYPE),
                                                               EntityArgument.getEntity(l_param0, "entity"),
                                                               EntityArgument.getEntity(l_param0, "cause")
                                                            )
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

   private static int damage(CommandSourceStack param0, Entity param1, float param2, DamageSource param3) throws CommandSyntaxException {
      if (param1.hurt(param3, param2)) {
         param0.sendSuccess(Component.translatable("commands.damage.success", param2, param1.getDisplayName()), true);
         return 1;
      } else {
         throw ERROR_INVULNERABLE.create();
      }
   }
}
