package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SummonCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed"));
   private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.uuid"));
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.summon.invalidPosition"));

   public static void register(CommandDispatcher<CommandSourceStack> param0, CommandBuildContext param1) {
      param0.register(
         Commands.literal("summon")
            .requires(l_param0 -> l_param0.hasPermission(2))
            .then(
               Commands.argument("entity", ResourceArgument.resource(param1, Registries.ENTITY_TYPE))
                  .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                  .executes(
                     l_param0 -> spawnEntity(
                        l_param0.getSource(),
                        ResourceArgument.getSummonableEntityType(l_param0, "entity"),
                        l_param0.getSource().getPosition(),
                        new CompoundTag(),
                        true
                     )
                  )
                  .then(
                     Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           l_param0 -> spawnEntity(
                              l_param0.getSource(),
                              ResourceArgument.getSummonableEntityType(l_param0, "entity"),
                              Vec3Argument.getVec3(l_param0, "pos"),
                              new CompoundTag(),
                              true
                           )
                        )
                        .then(
                           Commands.argument("nbt", CompoundTagArgument.compoundTag())
                              .executes(
                                 l_param0 -> spawnEntity(
                                    l_param0.getSource(),
                                    ResourceArgument.getSummonableEntityType(l_param0, "entity"),
                                    Vec3Argument.getVec3(l_param0, "pos"),
                                    CompoundTagArgument.getCompoundTag(l_param0, "nbt"),
                                    false
                                 )
                              )
                        )
                  )
            )
      );
   }

   public static Entity createEntity(CommandSourceStack param0, Holder.Reference<EntityType<?>> param1, Vec3 param2, CompoundTag param3, boolean param4) throws CommandSyntaxException {
      BlockPos var5 = BlockPos.containing(param2);
      if (!Level.isInSpawnableBounds(var5)) {
         throw INVALID_POSITION.create();
      }

      CompoundTag var6 = param3.copy();
      var6.putString("id", param1.key().location().toString());
      ServerLevel var7 = param0.getLevel();
      Entity var8 = EntityType.loadEntityRecursive(var6, var7, l_param1 -> {
         l_param1.moveTo(param2.x, param2.y, param2.z, l_param1.getYRot(), l_param1.getXRot());
         return l_param1;
      });
      if (var8 == null) {
         throw ERROR_FAILED.create();
      }

      if (param4 && var8 instanceof Mob) {
         ((Mob)var8).finalizeSpawn(param0.getLevel(), param0.getLevel().getCurrentDifficultyAt(var8.blockPosition()), MobSpawnType.COMMAND, null, null);
      }

      if (!var7.tryAddFreshEntityWithPassengers(var8)) {
         throw ERROR_DUPLICATE_UUID.create();
      } else {
         return var8;
      }
   }

   private static int spawnEntity(CommandSourceStack param0, Holder.Reference<EntityType<?>> param1, Vec3 param2, CompoundTag param3, boolean param4) throws CommandSyntaxException {
      Entity var5 = createEntity(param0, param1, param2, param3, param4);
      param0.sendSuccess(Component.translatable("commands.summon.success", var5.getDisplayName()), true);
      return 1;
   }
}
