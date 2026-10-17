package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;

public class RaidCommand {
   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(
         Commands.literal("raid")
            .requires(l_param0 -> l_param0.hasPermission(3))
            .then(
               Commands.literal("start")
                  .then(
                     Commands.argument("omenlvl", IntegerArgumentType.integer(0))
                        .executes(l_param0 -> start(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "omenlvl")))
                  )
            )
            .then(Commands.literal("stop").executes(l_param0 -> stop(l_param0.getSource())))
            .then(Commands.literal("check").executes(l_param0 -> check(l_param0.getSource())))
            .then(
               Commands.literal("sound")
                  .then(
                     Commands.argument("type", ComponentArgument.textComponent())
                        .executes(l_param0 -> playSound(l_param0.getSource(), ComponentArgument.getComponent(l_param0, "type")))
                  )
            )
            .then(Commands.literal("spawnleader").executes(l_param0 -> spawnLeader(l_param0.getSource())))
            .then(
               Commands.literal("setomen")
                  .then(
                     Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(l_param0 -> setBadOmenLevel(l_param0.getSource(), IntegerArgumentType.getInteger(l_param0, "level")))
                  )
            )
            .then(Commands.literal("glow").executes(l_param0 -> glow(l_param0.getSource())))
      );
   }

   private static int glow(CommandSourceStack param0) throws CommandSyntaxException {
      Raid var1 = getRaid(param0.getPlayerOrException());
      if (var1 != null) {
         for (Raider var4 : var1.getAllRaiders()) {
            var4.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
         }
      }

      return 1;
   }

   private static int setBadOmenLevel(CommandSourceStack param0, int param1) throws CommandSyntaxException {
      Raid var2 = getRaid(param0.getPlayerOrException());
      if (var2 != null) {
         int var3 = var2.getMaxBadOmenLevel();
         if (param1 > var3) {
            param0.sendFailure(Component.literal("Sorry, the max bad omen level you can set is " + var3));
         } else {
            int var4 = var2.getBadOmenLevel();
            var2.setBadOmenLevel(param1);
            param0.sendSuccess(Component.literal("Changed village's bad omen level from " + var4 + " to " + param1), false);
         }
      } else {
         param0.sendFailure(Component.literal("No raid found here"));
      }

      return 1;
   }

   private static int spawnLeader(CommandSourceStack param0) {
      param0.sendSuccess(Component.literal("Spawned a raid captain"), false);
      Raider var1 = EntityType.PILLAGER.create(param0.getLevel());
      if (var1 == null) {
         param0.sendFailure(Component.literal("Pillager failed to spawn"));
         return 0;
      } else {
         var1.setPatrolLeader(true);
         var1.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
         var1.setPos(param0.getPosition().x, param0.getPosition().y, param0.getPosition().z);
         var1.finalizeSpawn(
            param0.getLevel(), param0.getLevel().getCurrentDifficultyAt(BlockPos.containing(param0.getPosition())), MobSpawnType.COMMAND, null, null
         );
         param0.getLevel().addFreshEntityWithPassengers(var1);
         return 1;
      }
   }

   private static int playSound(CommandSourceStack param0, @Nullable Component param1) {
      if (param1 != null && param1.getString().equals("local")) {
         ServerLevel var2 = param0.getLevel();
         Vec3 var3 = param0.getPosition().add(5.0, 0.0, 0.0);
         var2.playSeededSound(null, var3.x, var3.y, var3.z, SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F, var2.random.nextLong());
      }

      return 1;
   }

   private static int start(CommandSourceStack param0, int param1) throws CommandSyntaxException {
      ServerPlayer var2 = param0.getPlayerOrException();
      BlockPos var3 = var2.blockPosition();
      if (var2.getLevel().isRaided(var3)) {
         param0.sendFailure(Component.literal("Raid already started close by"));
         return -1;
      }

      Raids var4 = var2.getLevel().getRaids();
      Raid var5 = var4.createOrExtendRaid(var2);
      if (var5 != null) {
         var5.setBadOmenLevel(param1);
         var4.setDirty();
         param0.sendSuccess(Component.literal("Created a raid in your local village"), false);
      } else {
         param0.sendFailure(Component.literal("Failed to create a raid in your local village"));
      }

      return 1;
   }

   private static int stop(CommandSourceStack param0) throws CommandSyntaxException {
      ServerPlayer var1 = param0.getPlayerOrException();
      BlockPos var2 = var1.blockPosition();
      Raid var3 = var1.getLevel().getRaidAt(var2);
      if (var3 != null) {
         var3.stop();
         param0.sendSuccess(Component.literal("Stopped raid"), false);
         return 1;
      } else {
         param0.sendFailure(Component.literal("No raid here"));
         return -1;
      }
   }

   private static int check(CommandSourceStack param0) throws CommandSyntaxException {
      Raid var1 = getRaid(param0.getPlayerOrException());
      if (var1 != null) {
         StringBuilder var2 = new StringBuilder();
         var2.append("Found a started raid! ");
         param0.sendSuccess(Component.literal(var2.toString()), false);
         var2 = new StringBuilder();
         var2.append("Num groups spawned: ");
         var2.append(var1.getGroupsSpawned());
         var2.append(" Bad omen level: ");
         var2.append(var1.getBadOmenLevel());
         var2.append(" Num mobs: ");
         var2.append(var1.getTotalRaidersAlive());
         var2.append(" Raid health: ");
         var2.append(var1.getHealthOfLivingRaiders());
         var2.append(" / ");
         var2.append(var1.getTotalHealth());
         param0.sendSuccess(Component.literal(var2.toString()), false);
         return 1;
      } else {
         param0.sendFailure(Component.literal("Found no started raids"));
         return 0;
      }
   }

   @Nullable
   private static Raid getRaid(ServerPlayer param0) {
      return param0.getLevel().getRaidAt(param0.blockPosition());
   }
}
