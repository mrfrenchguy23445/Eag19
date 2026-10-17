package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectCommands {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.give.failed"));
   private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.effect.clear.everything.failed")
   );
   private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.effect.clear.specific.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("effect")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.literal("clear")
                  .executes(l_param0 -> clearEffects(l_param0.getSource(), ImmutableList.of(l_param0.getSource().getEntityOrException())))
                  .then(
                     Commands.argument("targets", EntityArgument.entities())
                        .executes(l_param0 -> clearEffects(l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets")))
                        .then(
                           Commands.argument("effect", ResourceArgument.resource(param1, Registries.MOB_EFFECT))
                              .executes(
                                 l_param0 -> clearEffect(
                                    l_param0.getSource(), EntityArgument.getEntities(l_param0, "targets"), ResourceArgument.getMobEffect(l_param0, "effect")
                                 )
                              )
                        )
                  )
            )
            .then(
               Commands.literal("give")
                  .then(
                     Commands.argument("targets", EntityArgument.entities())
                        .then(
                           Commands.argument("effect", ResourceArgument.resource(param1, Registries.MOB_EFFECT))
                              .executes(
                                 l_param0 -> giveEffect(
                                    l_param0.getSource(),
                                    EntityArgument.getEntities(l_param0, "targets"),
                                    ResourceArgument.getMobEffect(l_param0, "effect"),
                                    null,
                                    0,
                                    true
                                 )
                              )
                              .then(
                                 Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                    .executes(
                                       l_param0 -> giveEffect(
                                          l_param0.getSource(),
                                          EntityArgument.getEntities(l_param0, "targets"),
                                          ResourceArgument.getMobEffect(l_param0, "effect"),
                                          IntegerArgumentType.getInteger(l_param0, "seconds"),
                                          0,
                                          true
                                       )
                                    )
                                    .then(
                                       Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                          .executes(
                                             l_param0 -> giveEffect(
                                                l_param0.getSource(),
                                                EntityArgument.getEntities(l_param0, "targets"),
                                                ResourceArgument.getMobEffect(l_param0, "effect"),
                                                IntegerArgumentType.getInteger(l_param0, "seconds"),
                                                IntegerArgumentType.getInteger(l_param0, "amplifier"),
                                                true
                                             )
                                          )
                                          .then(
                                             Commands.argument("hideParticles", BoolArgumentType.bool())
                                                .executes(
                                                   l_param0 -> giveEffect(
                                                      l_param0.getSource(),
                                                      EntityArgument.getEntities(l_param0, "targets"),
                                                      ResourceArgument.getMobEffect(l_param0, "effect"),
                                                      IntegerArgumentType.getInteger(l_param0, "seconds"),
                                                      IntegerArgumentType.getInteger(l_param0, "amplifier"),
                                                      !BoolArgumentType.getBool(l_param0, "hideParticles")
                                                   )
                                                )
                                          )
                                    )
                              )
                              .then(
                                 Commands.literal("infinite")
                                    .executes(
                                       l_param0 -> giveEffect(
                                          l_param0.getSource(),
                                          EntityArgument.getEntities(l_param0, "targets"),
                                          ResourceArgument.getMobEffect(l_param0, "effect"),
                                          -1,
                                          0,
                                          true
                                       )
                                    )
                                    .then(
                                       Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                          .executes(
                                             l_param0 -> giveEffect(
                                                l_param0.getSource(),
                                                EntityArgument.getEntities(l_param0, "targets"),
                                                ResourceArgument.getMobEffect(l_param0, "effect"),
                                                -1,
                                                IntegerArgumentType.getInteger(l_param0, "amplifier"),
                                                true
                                             )
                                          )
                                          .then(
                                             Commands.argument("hideParticles", BoolArgumentType.bool())
                                                .executes(
                                                   l_param0 -> giveEffect(
                                                      l_param0.getSource(),
                                                      EntityArgument.getEntities(l_param0, "targets"),
                                                      ResourceArgument.getMobEffect(l_param0, "effect"),
                                                      -1,
                                                      IntegerArgumentType.getInteger(l_param0, "amplifier"),
                                                      !BoolArgumentType.getBool(l_param0, "hideParticles")
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

   private static int giveEffect(
      CommandSourceStack param0, Collection<? extends Entity> param1, Holder<MobEffect> param2, @Nullable Integer param3, int param4, boolean param5
   ) throws CommandSyntaxException {
      MobEffect var6 = param2.value();
      int var7 = 0;
      int var8;
      if (param3 != null) {
         if (var6.isInstantenous()) {
            var8 = param3;
         } else if (param3 == -1) {
            var8 = -1;
         } else {
            var8 = param3 * 20;
         }
      } else if (var6.isInstantenous()) {
         var8 = 1;
      } else {
         var8 = 600;
      }

      for (Entity var10 : param1) {
         if (var10 instanceof LivingEntity) {
            MobEffectInstance var11 = new MobEffectInstance(var6, var8, param4, false, param5);
            if (((LivingEntity)var10).addEffect(var11, param0.getEntity())) {
               var7++;
            }
         }
      }

      if (var7 == 0) {
         throw ERROR_GIVE_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.effect.give.success.single", var6.getDisplayName(), param1.iterator().next().getDisplayName(), var8 / 20), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.effect.give.success.multiple", var6.getDisplayName(), param1.size(), var8 / 20), true);
      }

      return var7;
   }

   private static int clearEffects(CommandSourceStack param0, Collection<? extends Entity> param1) throws CommandSyntaxException {
      int var2 = 0;

      for (Entity var4 : param1) {
         if (var4 instanceof LivingEntity && ((LivingEntity)var4).removeAllEffects()) {
            var2++;
         }
      }

      if (var2 == 0) {
         throw ERROR_CLEAR_EVERYTHING_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(Component.translatable("commands.effect.clear.everything.success.single", param1.iterator().next().getDisplayName()), true);
      } else {
         param0.sendSuccess(Component.translatable("commands.effect.clear.everything.success.multiple", param1.size()), true);
      }

      return var2;
   }

   private static int clearEffect(CommandSourceStack param0, Collection<? extends Entity> param1, Holder<MobEffect> param2) throws CommandSyntaxException {
      MobEffect var3 = param2.value();
      int var4 = 0;

      for (Entity var6 : param1) {
         if (var6 instanceof LivingEntity && ((LivingEntity)var6).removeEffect(var3)) {
            var4++;
         }
      }

      if (var4 == 0) {
         throw ERROR_CLEAR_SPECIFIC_FAILED.create();
      }

      if (param1.size() == 1) {
         param0.sendSuccess(
            Component.translatable("commands.effect.clear.specific.success.single", var3.getDisplayName(), param1.iterator().next().getDisplayName()), true
         );
      } else {
         param0.sendSuccess(Component.translatable("commands.effect.clear.specific.success.multiple", var3.getDisplayName(), param1.size()), true);
      }

      return var4;
   }
}
