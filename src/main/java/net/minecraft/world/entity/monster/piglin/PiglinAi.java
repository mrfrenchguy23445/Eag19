package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
   public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
   public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
   public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
   private static final int PLAYER_ANGER_RANGE = 16;
   private static final int ANGER_DURATION = 600;
   private static final int ADMIRE_DURATION = 120;
   private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
   private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
   private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
   private static final int CELEBRATION_TIME = 300;
   protected static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
   private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
   private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
   private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
   private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
   private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
   private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
   private static final int MELEE_ATTACK_COOLDOWN = 20;
   private static final int EAT_COOLDOWN = 200;
   private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
   private static final int MAX_LOOK_DIST = 8;
   private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
   private static final int INTERACTION_RANGE = 8;
   private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
   private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
   private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
   private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
   private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
   private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
   private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
   private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

   protected static Brain<?> makeBrain(Piglin param0, Brain<Piglin> param1) {
      initCoreActivity(param1);
      initIdleActivity(param1);
      initAdmireItemActivity(param1);
      initFightActivity(param0, param1);
      initCelebrateActivity(param1);
      initRetreatActivity(param1);
      initRideHoglinActivity(param1);
      param1.setCoreActivities(ImmutableSet.of(Activity.CORE));
      param1.setDefaultActivity(Activity.IDLE);
      param1.useDefaultActivity();
      return param1;
   }

   protected static void initMemories(Piglin param0, RandomSource param1) {
      int var2 = TIME_BETWEEN_HUNTS.sample(param1);
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, var2);
   }

   private static void initCoreActivity(Brain<Piglin> param0) {
      param0.addActivity(
         Activity.CORE,
         0,
         ImmutableList.of(
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            InteractWithDoor.create(),
            babyAvoidNemesis(),
            avoidZombified(),
            StopHoldingItemIfNoLongerAdmiring.create(),
            StartAdmiringItemIfSeen.create(120),
            StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance),
            StopBeingAngryIfTargetDead.create()
         )
      );
   }

   private static void initIdleActivity(Brain<Piglin> param0) {
      param0.addActivity(
         Activity.IDLE,
         10,
         ImmutableList.of(
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
            StartAttacking.<Piglin>create(AbstractPiglin::isAdult, PiglinAi::findNearestValidAttackTarget),
            BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create()),
            avoidRepellent(),
            babySometimesRideBabyHoglin(),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            SetLookAndInteract.create(EntityType.PLAYER, 4)
         )
      );
   }

   private static void initFightActivity(Piglin param0, Brain<Piglin> param1) {
      param1.addActivityAndRemoveMemoryWhenStopped(
         Activity.FIGHT,
         10,
         ImmutableList.<BehaviorControl<? super Piglin>>of(
            StopAttackingIfTargetInvalid.create(l_param1 -> !isNearestValidAttackTarget(param0, l_param1)),
            BehaviorBuilder.triggerIf(PiglinAi::hasCrossbow, BackUpIfTooClose.create(5, 0.75F)),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(20),
            new CrossbowAttack(),
            RememberIfHoglinWasKilled.create(),
            EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static void initCelebrateActivity(Brain<Piglin> param0) {
      param0.addActivityAndRemoveMemoryWhenStopped(
         Activity.CELEBRATE,
         10,
         ImmutableList.of(
            avoidRepellent(),
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
            StartAttacking.<Piglin>create(AbstractPiglin::isAdult, PiglinAi::findNearestValidAttackTarget),
            BehaviorBuilder.triggerIf((Piglin l_param0) -> !l_param0.isDancing(), GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 2, 1.0F)),
            BehaviorBuilder.triggerIf(Piglin::isDancing, GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 4, 0.6F)),
            new RunOne<Piglin>(
               ImmutableList.of(
                  Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
                  Pair.of(RandomStroll.stroll(0.6F, 2, 1), 1),
                  Pair.of(new DoNothing(10, 20), 1)
               )
            )
         ),
         MemoryModuleType.CELEBRATE_LOCATION
      );
   }

   private static void initAdmireItemActivity(Brain<Piglin> param0) {
      param0.addActivityAndRemoveMemoryWhenStopped(
         Activity.ADMIRE_ITEM,
         10,
         ImmutableList.of(
            GoToWantedItem.create(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0F, true, 9),
            StopAdmiringIfItemTooFarAway.create(9),
            StopAdmiringIfTiredOfTryingToReachItem.create(200, 200)
         ),
         MemoryModuleType.ADMIRING_ITEM
      );
   }

   private static void initRetreatActivity(Brain<Piglin> param0) {
      param0.addActivityAndRemoveMemoryWhenStopped(
         Activity.AVOID,
         10,
         ImmutableList.of(
            SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            EraseMemoryIf.<Piglin>create(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
         ),
         MemoryModuleType.AVOID_TARGET
      );
   }

   private static void initRideHoglinActivity(Brain<Piglin> param0) {
      param0.addActivityAndRemoveMemoryWhenStopped(
         Activity.RIDE,
         10,
         ImmutableList.of(
            Mount.create(0.8F),
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 8.0F),
            BehaviorBuilder.sequence(
               BehaviorBuilder.triggerIf(Entity::isPassenger),
               TriggerGate.triggerOneShuffled(
                  ImmutableList.<Pair<? extends Trigger<? super LivingEntity>, Integer>>builder()
                     .addAll(createLookBehaviors())
                     .add(Pair.of(BehaviorBuilder.triggerIf(l_param0 -> true), 1))
                     .build()
               )
            ),
            DismountOrSkipMounting.<Piglin>create(8, PiglinAi::wantsToStopRiding)
         ),
         MemoryModuleType.RIDE_TARGET
      );
   }

   private static ImmutableList<Pair<OneShot<LivingEntity>, Integer>> createLookBehaviors() {
      return ImmutableList.of(
         Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 1),
         Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
         Pair.of(SetEntityLookTarget.create(8.0F), 1)
      );
   }

   private static RunOne<LivingEntity> createIdleLookBehaviors() {
      return new RunOne<>(
         ImmutableList.<Pair<? extends BehaviorControl<? super LivingEntity>, Integer>>builder()
            .addAll(createLookBehaviors())
            .add(Pair.of(new DoNothing(30, 60), 1))
            .build()
      );
   }

   private static RunOne<Piglin> createIdleMovementBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(RandomStroll.stroll(0.6F), 2),
            Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(BehaviorBuilder.triggerIf(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, SetWalkTargetFromLookTarget.create(0.6F, 3)), 2),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   private static BehaviorControl<PathfinderMob> avoidRepellent() {
      return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
   }

   private static BehaviorControl<Piglin> babyAvoidNemesis() {
      return CopyMemoryWithExpiry.create(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
   }

   private static BehaviorControl<Piglin> avoidZombified() {
      return CopyMemoryWithExpiry.create(
         PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION
      );
   }

   protected static void updateActivity(Piglin param0) {
      Brain<Piglin> var1 = param0.getBrain();
      Activity var2 = var1.getActiveNonCoreActivity().orElse(null);
      var1.setActiveActivityToFirstValid(
         ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE)
      );
      Activity var3 = var1.getActiveNonCoreActivity().orElse(null);
      if (var2 != var3) {
         getSoundForCurrentActivity(param0).ifPresent(param0::playSoundEvent);
      }

      param0.setAggressive(var1.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
      if (!var1.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(param0)) {
         param0.stopRiding();
      }

      if (!var1.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
         var1.eraseMemory(MemoryModuleType.DANCING);
      }

      param0.setDancing(var1.hasMemoryValue(MemoryModuleType.DANCING));
   }

   private static boolean isBabyRidingBaby(Piglin param0) {
      if (!param0.isBaby()) {
         return false;
      }

      Entity var1 = param0.getVehicle();
      return var1 instanceof Piglin && ((Piglin)var1).isBaby() || var1 instanceof Hoglin && ((Hoglin)var1).isBaby();
   }

   protected static void pickUpItem(Piglin param0, ItemEntity param1) {
      stopWalking(param0);
      ItemStack var2;
      if (param1.getItem().is(Items.GOLD_NUGGET)) {
         param0.take(param1, param1.getItem().getCount());
         var2 = param1.getItem();
         param1.discard();
      } else {
         param0.take(param1, 1);
         var2 = removeOneItemFromItemEntity(param1);
      }

      if (isLovedItem(var2)) {
         param0.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
         holdInOffhand(param0, var2);
         admireGoldItem(param0);
      } else if (isFood(var2) && !hasEatenRecently(param0)) {
         eat(param0);
      } else {
         boolean var3 = !param0.equipItemIfPossible(var2).equals(ItemStack.EMPTY);
         if (!var3) {
            putInInventory(param0, var2);
         }
      }
   }

   private static void holdInOffhand(Piglin param0, ItemStack param1) {
      if (isHoldingItemInOffHand(param0)) {
         param0.spawnAtLocation(param0.getItemInHand(InteractionHand.OFF_HAND));
      }

      param0.holdInOffHand(param1);
   }

   private static ItemStack removeOneItemFromItemEntity(ItemEntity param0) {
      ItemStack var1 = param0.getItem();
      ItemStack var2 = var1.split(1);
      if (var1.isEmpty()) {
         param0.discard();
      } else {
         param0.setItem(var1);
      }

      return var2;
   }

   protected static void stopHoldingOffHandItem(Piglin param0, boolean param1) {
      ItemStack var2 = param0.getItemInHand(InteractionHand.OFF_HAND);
      param0.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      if (param0.isAdult()) {
         boolean var3 = isBarterCurrency(var2);
         if (param1 && var3) {
            throwItems(param0, getBarterResponseItems(param0));
         } else if (!var3) {
            boolean var4 = !param0.equipItemIfPossible(var2).isEmpty();
            if (!var4) {
               putInInventory(param0, var2);
            }
         }
      } else {
         boolean var3 = !param0.equipItemIfPossible(var2).isEmpty();
         if (!var3) {
            ItemStack var4 = param0.getMainHandItem();
            if (isLovedItem(var4)) {
               putInInventory(param0, var4);
            } else {
               throwItems(param0, Collections.singletonList(var4));
            }

            param0.holdInMainHand(var2);
         }
      }
   }

   protected static void cancelAdmiring(Piglin param0) {
      if (isAdmiringItem(param0) && !param0.getOffhandItem().isEmpty()) {
         param0.spawnAtLocation(param0.getOffhandItem());
         param0.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      }
   }

   private static void putInInventory(Piglin param0, ItemStack param1) {
      ItemStack var2 = param0.addToInventory(param1);
      throwItemsTowardRandomPos(param0, Collections.singletonList(var2));
   }

   private static void throwItems(Piglin param0, List<ItemStack> param1) {
      Optional<Player> var2 = param0.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
      if (var2.isPresent()) {
         throwItemsTowardPlayer(param0, var2.get(), param1);
      } else {
         throwItemsTowardRandomPos(param0, param1);
      }
   }

   private static void throwItemsTowardRandomPos(Piglin param0, List<ItemStack> param1) {
      throwItemsTowardPos(param0, param1, getRandomNearbyPos(param0));
   }

   private static void throwItemsTowardPlayer(Piglin param0, Player param1, List<ItemStack> param2) {
      throwItemsTowardPos(param0, param2, param1.position());
   }

   private static void throwItemsTowardPos(Piglin param0, List<ItemStack> param1, Vec3 param2) {
      if (!param1.isEmpty()) {
         param0.swing(InteractionHand.OFF_HAND);

         for (ItemStack var4 : param1) {
            BehaviorUtils.throwItem(param0, var4, param2.add(0.0, 1.0, 0.0));
         }
      }
   }

   private static List<ItemStack> getBarterResponseItems(Piglin param0) {
      LootTable var1 = param0.level.getServer().getLootTables().get(BuiltInLootTables.PIGLIN_BARTERING);
      return var1.getRandomItems(
         new LootContext.Builder((ServerLevel)param0.level)
            .withParameter(LootContextParams.THIS_ENTITY, param0)
            .withRandom(param0.level.random)
            .create(LootContextParamSets.PIGLIN_BARTER)
      );
   }

   private static boolean wantsToDance(LivingEntity param0, LivingEntity param1) {
      return param1.getType() != EntityType.HOGLIN ? false : RandomSource.create(param0.level.getGameTime()).nextFloat() < 0.1F;
   }

   protected static boolean wantsToPickup(Piglin param0, ItemStack param1) {
      if (param0.isBaby() && param1.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
         return false;
      } else if (param1.is(ItemTags.PIGLIN_REPELLENTS)) {
         return false;
      } else if (isAdmiringDisabled(param0) && param0.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
         return false;
      } else if (isBarterCurrency(param1)) {
         return isNotHoldingLovedItemInOffHand(param0);
      } else {
         boolean var2 = param0.canAddToInventory(param1);
         if (param1.is(Items.GOLD_NUGGET)) {
            return var2;
         } else if (isFood(param1)) {
            return !hasEatenRecently(param0) && var2;
         } else {
            return !isLovedItem(param1) ? param0.canReplaceCurrentItem(param1) : isNotHoldingLovedItemInOffHand(param0) && var2;
         }
      }
   }

   protected static boolean isLovedItem(ItemStack param0) {
      return param0.is(ItemTags.PIGLIN_LOVED);
   }

   private static boolean wantsToStopRiding(Piglin param0, Entity param1) {
      return !(param1 instanceof Mob var2)
         ? false
         : !var2.isBaby() || !var2.isAlive() || wasHurtRecently(param0) || wasHurtRecently(var2) || var2 instanceof Piglin && var2.getVehicle() == null;
   }

   private static boolean isNearestValidAttackTarget(Piglin param0, LivingEntity param1) {
      return findNearestValidAttackTarget(param0).filter(l_param1 -> l_param1 == param1).isPresent();
   }

   private static boolean isNearZombified(Piglin param0) {
      Brain<Piglin> var1 = param0.getBrain();
      if (var1.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
         LivingEntity var2 = var1.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
         return param0.closerThan(var2, 6.0);
      } else {
         return false;
      }
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(Piglin param0) {
      Brain<Piglin> var1 = param0.getBrain();
      if (isNearZombified(param0)) {
         return Optional.empty();
      }

      Optional<LivingEntity> var2 = BehaviorUtils.getLivingEntityFromUUIDMemory(param0, MemoryModuleType.ANGRY_AT);
      if (var2.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(param0, var2.get())) {
         return var2;
      }

      if (var1.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER)) {
         Optional<Player> var3 = var1.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
         if (var3.isPresent()) {
            return var3;
         }
      }

      Optional<Mob> var3 = var1.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
      if (var3.isPresent()) {
         return var3;
      }

      Optional<Player> var4 = var1.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
      return var4.isPresent() && Sensor.isEntityAttackable(param0, var4.get()) ? var4 : Optional.empty();
   }

   public static void angerNearbyPiglins(Player param0, boolean param1) {
      List<Piglin> var2 = param0.level.getEntitiesOfClass(Piglin.class, param0.getBoundingBox().inflate(16.0));
      var2.stream().filter(PiglinAi::isIdle).filter(l_param2 -> !param1 || BehaviorUtils.canSee(l_param2, param0)).forEach(l_param1 -> {
         if (l_param1.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
            setAngerTargetToNearestTargetablePlayerIfFound(l_param1, param0);
         } else {
            setAngerTarget(l_param1, param0);
         }
      });
   }

   public static InteractionResult mobInteract(Piglin param0, Player param1, InteractionHand param2) {
      ItemStack var3 = param1.getItemInHand(param2);
      if (canAdmire(param0, var3)) {
         ItemStack var4 = var3.split(1);
         holdInOffhand(param0, var4);
         admireGoldItem(param0);
         stopWalking(param0);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected static boolean canAdmire(Piglin param0, ItemStack param1) {
      return !isAdmiringDisabled(param0) && !isAdmiringItem(param0) && param0.isAdult() && isBarterCurrency(param1);
   }

   protected static void wasHurtBy(Piglin param0, LivingEntity param1) {
      if (!(param1 instanceof Piglin)) {
         if (isHoldingItemInOffHand(param0)) {
            stopHoldingOffHandItem(param0, false);
         }

         Brain<Piglin> var2 = param0.getBrain();
         var2.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
         var2.eraseMemory(MemoryModuleType.DANCING);
         var2.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
         if (param1 instanceof Player) {
            var2.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
         }

         getAvoidTarget(param0).ifPresent(l_param2 -> {
            if (l_param2.getType() != param1.getType()) {
               var2.eraseMemory(MemoryModuleType.AVOID_TARGET);
            }
         });
         if (param0.isBaby()) {
            var2.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, param1, 100L);
            if (Sensor.isEntityAttackableIgnoringLineOfSight(param0, param1)) {
               broadcastAngerTarget(param0, param1);
            }
         } else if (param1.getType() == EntityType.HOGLIN && hoglinsOutnumberPiglins(param0)) {
            setAvoidTargetAndDontHuntForAWhile(param0, param1);
            broadcastRetreat(param0, param1);
         } else {
            maybeRetaliate(param0, param1);
         }
      }
   }

   protected static void maybeRetaliate(AbstractPiglin param0, LivingEntity param1) {
      if (!param0.getBrain().isActive(Activity.AVOID)) {
         if (Sensor.isEntityAttackableIgnoringLineOfSight(param0, param1)) {
            if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(param0, param1, 4.0)) {
               if (param1.getType() == EntityType.PLAYER && param0.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                  setAngerTargetToNearestTargetablePlayerIfFound(param0, param1);
                  broadcastUniversalAnger(param0);
               } else {
                  setAngerTarget(param0, param1);
                  broadcastAngerTarget(param0, param1);
               }
            }
         }
      }
   }

   public static Optional<SoundEvent> getSoundForCurrentActivity(Piglin param0) {
      return param0.getBrain().getActiveNonCoreActivity().map(l_param1 -> getSoundForActivity(param0, l_param1));
   }

   private static SoundEvent getSoundForActivity(Piglin param0, Activity param1) {
      if (param1 == Activity.FIGHT) {
         return SoundEvents.PIGLIN_ANGRY;
      } else if (param0.isConverting()) {
         return SoundEvents.PIGLIN_RETREAT;
      } else if (param1 == Activity.AVOID && isNearAvoidTarget(param0)) {
         return SoundEvents.PIGLIN_RETREAT;
      } else if (param1 == Activity.ADMIRE_ITEM) {
         return SoundEvents.PIGLIN_ADMIRING_ITEM;
      } else if (param1 == Activity.CELEBRATE) {
         return SoundEvents.PIGLIN_CELEBRATE;
      } else if (seesPlayerHoldingLovedItem(param0)) {
         return SoundEvents.PIGLIN_JEALOUS;
      } else {
         return isNearRepellent(param0) ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT;
      }
   }

   private static boolean isNearAvoidTarget(Piglin param0) {
      Brain<Piglin> var1 = param0.getBrain();
      return !var1.hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? false : var1.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(param0, 12.0);
   }

   protected static List<AbstractPiglin> getVisibleAdultPiglins(Piglin param0) {
      return param0.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
   }

   private static List<AbstractPiglin> getAdultPiglins(AbstractPiglin param0) {
      return param0.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
   }

   public static boolean isWearingGold(LivingEntity param0) {
      for (ItemStack var3 : param0.getArmorSlots()) {
         Item var4 = var3.getItem();
         if (var4 instanceof ArmorItem && ((ArmorItem)var4).getMaterial() == ArmorMaterials.GOLD) {
            return true;
         }
      }

      return false;
   }

   private static void stopWalking(Piglin param0) {
      param0.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      param0.getNavigation().stop();
   }

   private static BehaviorControl<LivingEntity> babySometimesRideBabyHoglin() {
      SetEntityLookTargetSometimes.Ticker var0 = new SetEntityLookTargetSometimes.Ticker(RIDE_START_INTERVAL);
      return CopyMemoryWithExpiry.create(
         l_param1 -> l_param1.isBaby() && var0.tickDownAndCheck(l_param1.level.random),
         MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
         MemoryModuleType.RIDE_TARGET,
         RIDE_DURATION
      );
   }

   protected static void broadcastAngerTarget(AbstractPiglin param0, LivingEntity param1) {
      getAdultPiglins(param0).forEach(l_param1 -> {
         if (param1.getType() != EntityType.HOGLIN || l_param1.canHunt() && ((Hoglin)param1).canBeHunted()) {
            setAngerTargetIfCloserThanCurrent(l_param1, param1);
         }
      });
   }

   protected static void broadcastUniversalAnger(AbstractPiglin param0) {
      getAdultPiglins(param0).forEach(l_param0 -> getNearestVisibleTargetablePlayer(l_param0).ifPresent(l2_param1 -> setAngerTarget(l_param0, l2_param1)));
   }

   protected static void setAngerTarget(AbstractPiglin param0, LivingEntity param1) {
      if (Sensor.isEntityAttackableIgnoringLineOfSight(param0, param1)) {
         param0.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         param0.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, param1.getUUID(), 600L);
         if (param1.getType() == EntityType.HOGLIN && param0.canHunt()) {
            dontKillAnyMoreHoglinsForAWhile(param0);
         }

         if (param1.getType() == EntityType.PLAYER && param0.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
            param0.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
         }
      }
   }

   private static void setAngerTargetToNearestTargetablePlayerIfFound(AbstractPiglin param0, LivingEntity param1) {
      Optional<Player> var2 = getNearestVisibleTargetablePlayer(param0);
      if (var2.isPresent()) {
         setAngerTarget(param0, var2.get());
      } else {
         setAngerTarget(param0, param1);
      }
   }

   private static void setAngerTargetIfCloserThanCurrent(AbstractPiglin param0, LivingEntity param1) {
      Optional<LivingEntity> var2 = getAngerTarget(param0);
      LivingEntity var3 = BehaviorUtils.getNearestTarget(param0, var2, param1);
      if (!var2.isPresent() || var2.get() != var3) {
         setAngerTarget(param0, var3);
      }
   }

   private static Optional<LivingEntity> getAngerTarget(AbstractPiglin param0) {
      return BehaviorUtils.getLivingEntityFromUUIDMemory(param0, MemoryModuleType.ANGRY_AT);
   }

   public static Optional<LivingEntity> getAvoidTarget(Piglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? param0.getBrain().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
   }

   public static Optional<Player> getNearestVisibleTargetablePlayer(AbstractPiglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
         ? param0.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
         : Optional.empty();
   }

   private static void broadcastRetreat(Piglin param0, LivingEntity param1) {
      getVisibleAdultPiglins(param0)
         .stream()
         .filter(l_param0 -> l_param0 instanceof Piglin)
         .forEach(l_param1 -> retreatFromNearestTarget((Piglin)l_param1, param1));
   }

   private static void retreatFromNearestTarget(Piglin param0, LivingEntity param1) {
      Brain<Piglin> var2 = param0.getBrain();
      LivingEntity var3 = param1;
      var3 = BehaviorUtils.getNearestTarget(param0, var2.getMemory(MemoryModuleType.AVOID_TARGET), var3);
      var3 = BehaviorUtils.getNearestTarget(param0, var2.getMemory(MemoryModuleType.ATTACK_TARGET), var3);
      setAvoidTargetAndDontHuntForAWhile(param0, var3);
   }

   private static boolean wantsToStopFleeing(Piglin param0) {
      Brain<Piglin> var1 = param0.getBrain();
      if (!var1.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
         return true;
      } else {
         LivingEntity var2 = var1.getMemory(MemoryModuleType.AVOID_TARGET).get();
         EntityType<?> var3 = var2.getType();
         if (var3 == EntityType.HOGLIN) {
            return piglinsEqualOrOutnumberHoglins(param0);
         } else {
            return isZombified(var3) ? !var1.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, var2) : false;
         }
      }
   }

   private static boolean piglinsEqualOrOutnumberHoglins(Piglin param0) {
      return !hoglinsOutnumberPiglins(param0);
   }

   private static boolean hoglinsOutnumberPiglins(Piglin param0) {
      int var1 = param0.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
      int var2 = param0.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
      return var2 > var1;
   }

   private static void setAvoidTargetAndDontHuntForAWhile(Piglin param0, LivingEntity param1) {
      param0.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
      param0.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      param0.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, param1, RETREAT_DURATION.sample(param0.level.random));
      dontKillAnyMoreHoglinsForAWhile(param0);
   }

   protected static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin param0) {
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, TIME_BETWEEN_HUNTS.sample(param0.level.random));
   }

   private static void eat(Piglin param0) {
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
   }

   private static Vec3 getRandomNearbyPos(Piglin param0) {
      Vec3 var1 = LandRandomPos.getPos(param0, 4, 2);
      return var1 == null ? param0.position() : var1;
   }

   private static boolean hasEatenRecently(Piglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
   }

   protected static boolean isIdle(AbstractPiglin param0) {
      return param0.getBrain().isActive(Activity.IDLE);
   }

   private static boolean hasCrossbow(LivingEntity param0) {
      return param0.isHolding(Items.CROSSBOW);
   }

   private static void admireGoldItem(LivingEntity param0) {
      param0.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 120L);
   }

   private static boolean isAdmiringItem(Piglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
   }

   private static boolean isBarterCurrency(ItemStack param0) {
      return param0.is(BARTERING_ITEM);
   }

   private static boolean isFood(ItemStack param0) {
      return param0.is(ItemTags.PIGLIN_FOOD);
   }

   private static boolean isNearRepellent(Piglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
   }

   private static boolean seesPlayerHoldingLovedItem(LivingEntity param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
   }

   private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity param0) {
      return !seesPlayerHoldingLovedItem(param0);
   }

   public static boolean isPlayerHoldingLovedItem(LivingEntity param0) {
      return param0.getType() == EntityType.PLAYER && param0.isHolding(PiglinAi::isLovedItem);
   }

   private static boolean isAdmiringDisabled(Piglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
   }

   private static boolean wasHurtRecently(LivingEntity param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
   }

   private static boolean isHoldingItemInOffHand(Piglin param0) {
      return !param0.getOffhandItem().isEmpty();
   }

   private static boolean isNotHoldingLovedItemInOffHand(Piglin param0) {
      return param0.getOffhandItem().isEmpty() || !isLovedItem(param0.getOffhandItem());
   }

   public static boolean isZombified(EntityType<?> param0) {
      return param0 == EntityType.ZOMBIFIED_PIGLIN || param0 == EntityType.ZOGLIN;
   }
}
