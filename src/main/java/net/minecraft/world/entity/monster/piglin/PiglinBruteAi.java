package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAi {
   private static final int ANGER_DURATION = 600;
   private static final int MELEE_ATTACK_COOLDOWN = 20;
   private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125;
   private static final int MAX_LOOK_DIST = 8;
   private static final int INTERACTION_RANGE = 8;
   private static final double TARGETING_RANGE = 12.0;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;
   private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
   private static final int HOME_TOO_FAR_DISTANCE = 100;
   private static final int HOME_STROLL_AROUND_DISTANCE = 5;

   protected static Brain<?> makeBrain(PiglinBrute param0, Brain<PiglinBrute> param1) {
      initCoreActivity(param0, param1);
      initIdleActivity(param0, param1);
      initFightActivity(param0, param1);
      param1.setCoreActivities(ImmutableSet.of(Activity.CORE));
      param1.setDefaultActivity(Activity.IDLE);
      param1.useDefaultActivity();
      return param1;
   }

   protected static void initMemories(PiglinBrute param0) {
      GlobalPos var1 = GlobalPos.of(param0.level.dimension(), param0.blockPosition());
      param0.getBrain().setMemory(MemoryModuleType.HOME, var1);
   }

   private static void initCoreActivity(PiglinBrute param0, Brain<PiglinBrute> param1) {
      param1.addActivity(
         Activity.CORE,
         0,
         ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(), InteractWithDoor.create(), StopBeingAngryIfTargetDead.create())
      );
   }

   private static void initIdleActivity(PiglinBrute param0, Brain<PiglinBrute> param1) {
      param1.addActivity(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.<PiglinBrute>create(PiglinBruteAi::findNearestValidAttackTarget),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            SetLookAndInteract.create(EntityType.PLAYER, 4)
         )
      );
   }

   private static void initFightActivity(PiglinBrute param0, Brain<PiglinBrute> param1) {
      param1.addActivityAndRemoveMemoryWhenStopped(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            StopAttackingIfTargetInvalid.<Mob>create(l_param1 -> !isNearestValidAttackTarget(param0, l_param1)),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(20)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static RunOne<PiglinBrute> createIdleLookBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN_BRUTE, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(8.0F), 1),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   private static RunOne<PiglinBrute> createIdleMovementBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(RandomStroll.stroll(0.6F), 2),
            Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(InteractWith.of(EntityType.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(StrollToPoi.create(MemoryModuleType.HOME, 0.6F, 2, 100), 2),
            Pair.of(StrollAroundPoi.create(MemoryModuleType.HOME, 0.6F, 5), 2),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   protected static void updateActivity(PiglinBrute param0) {
      Brain<PiglinBrute> var1 = param0.getBrain();
      Activity var2 = var1.getActiveNonCoreActivity().orElse(null);
      var1.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      Activity var3 = var1.getActiveNonCoreActivity().orElse(null);
      if (var2 != var3) {
         playActivitySound(param0);
      }

      param0.setAggressive(var1.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
   }

   private static boolean isNearestValidAttackTarget(AbstractPiglin param0, LivingEntity param1) {
      return findNearestValidAttackTarget(param0).filter(l_param1 -> l_param1 == param1).isPresent();
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(AbstractPiglin param0) {
      Optional<LivingEntity> var1 = BehaviorUtils.getLivingEntityFromUUIDMemory(param0, MemoryModuleType.ANGRY_AT);
      if (var1.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(param0, var1.get())) {
         return var1;
      }

      Optional<? extends LivingEntity> var2 = getTargetIfWithinRange(param0, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
      return var2.isPresent() ? var2 : param0.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
   }

   private static Optional<? extends LivingEntity> getTargetIfWithinRange(AbstractPiglin param0, MemoryModuleType<? extends LivingEntity> param1) {
      return param0.getBrain().getMemory(param1).filter(l_param1 -> l_param1.closerThan(param0, 12.0));
   }

   protected static void wasHurtBy(PiglinBrute param0, LivingEntity param1) {
      if (!(param1 instanceof AbstractPiglin)) {
         PiglinAi.maybeRetaliate(param0, param1);
      }
   }

   protected static void setAngerTarget(PiglinBrute param0, LivingEntity param1) {
      param0.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, param1.getUUID(), 600L);
   }

   protected static void maybePlayActivitySound(PiglinBrute param0) {
      if (param0.level.random.nextFloat() < 0.0125) {
         playActivitySound(param0);
      }
   }

   private static void playActivitySound(PiglinBrute param0) {
      param0.getBrain().getActiveNonCoreActivity().ifPresent(l_param1 -> {
         if (l_param1 == Activity.FIGHT) {
            param0.playAngrySound();
         }
      });
   }
}
