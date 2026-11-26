package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class PrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E> {
   public static final int TIME_OUT_DURATION = 160;
   private final ToIntFunction<E> getCooldownOnFail;
   private final int minRamDistance;
   private final int maxRamDistance;
   private final float walkSpeed;
   private final TargetingConditions ramTargeting;
   private final int ramPrepareTime;
   private final Function<E, SoundEvent> getPrepareRamSound;
   private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
   private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

   public PrepareRamNearestTarget(
      ToIntFunction<E> param0, int param1, int param2, float param3, TargetingConditions param4, int param5, Function<E, SoundEvent> param6
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.RAM_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.RAM_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         160
      );
      this.getCooldownOnFail = param0;
      this.minRamDistance = param1;
      this.maxRamDistance = param2;
      this.walkSpeed = param3;
      this.ramTargeting = param4;
      this.ramPrepareTime = param5;
      this.getPrepareRamSound = param6;
   }

   protected void start(ServerLevel param0, PathfinderMob param1, long param2) {
      Brain<?> var5 = param1.getBrain();
      var5.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .flatMap(l_param1 -> l_param1.findClosest(l2_param1 -> this.ramTargeting.test(param1, l2_param1)))
         .ifPresent(l_param1 -> this.chooseRamPosition(param1, l_param1));
   }

   protected void stop(ServerLevel param0, E param1, long param2) {
      Brain<?> var5 = param1.getBrain();
      if (!var5.hasMemoryValue(MemoryModuleType.RAM_TARGET)) {
         param0.broadcastEntityEvent(param1, (byte)59);
         var5.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(param1));
      }
   }

   protected boolean canStillUse(ServerLevel param0, PathfinderMob param1, long param2) {
      return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
   }

   protected void tick(ServerLevel param0, E param1, long param2) {
      if (!this.ramCandidate.isEmpty()) {
         param1.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
         param1.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
         boolean var5 = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
         if (var5) {
            param0.broadcastEntityEvent(param1, (byte)59);
            param1.getNavigation().stop();
            this.chooseRamPosition(param1, this.ramCandidate.get().target);
         } else {
            BlockPos var6 = param1.blockPosition();
            if (var6.equals(this.ramCandidate.get().getStartPosition())) {
               param0.broadcastEntityEvent(param1, (byte)58);
               if (this.reachedRamPositionTimestamp.isEmpty()) {
                  this.reachedRamPositionTimestamp = Optional.of(param2);
               }

               if (param2 - this.reachedRamPositionTimestamp.get() >= this.ramPrepareTime) {
                  param1.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(var6, this.ramCandidate.get().getTargetPosition()));
                  param0.playSound(null, param1, this.getPrepareRamSound.apply(param1), SoundSource.NEUTRAL, 1.0F, param1.getVoicePitch());
                  this.ramCandidate = Optional.empty();
               }
            }
         }
      }
   }

   private Vec3 getEdgeOfBlock(BlockPos param0, BlockPos param1) {
      double var3 = 0.5;
      double var5 = 0.5 * Mth.sign(param1.getX() - param0.getX());
      double var7 = 0.5 * Mth.sign(param1.getZ() - param0.getZ());
      return Vec3.atBottomCenterOf(param1).add(var5, 0.0, var7);
   }

   private Optional<BlockPos> calculateRammingStartPosition(PathfinderMob param0, LivingEntity param1) {
      BlockPos var3 = param1.blockPosition();
      if (!this.isWalkableBlock(param0, var3)) {
         return Optional.empty();
      }

      List<BlockPos> var4 = Lists.newArrayList();
      BlockPos.MutableBlockPos var5 = var3.mutable();

      for (Direction var7 : Direction.Plane.HORIZONTAL) {
         var5.set(var3);

         for (int var8 = 0; var8 < this.maxRamDistance; var8++) {
            if (!this.isWalkableBlock(param0, var5.move(var7))) {
               var5.move(var7.getOpposite());
               break;
            }
         }

         if (var5.distManhattan(var3) >= this.minRamDistance) {
            var4.add(var5.immutable());
         }
      }

      PathNavigation var6 = param0.getNavigation();
      return var4.stream().sorted(Comparator.comparingDouble(param0.blockPosition()::distSqr)).filter(l_param1 -> {
         Path l_var2 = var6.createPath(l_param1, 0);
         return l_var2 != null && l_var2.canReach();
      }).findFirst();
   }

   private boolean isWalkableBlock(PathfinderMob param0, BlockPos param1) {
      return param0.getNavigation().isStableDestination(param1)
         && param0.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(param0.level, param1.mutable())) == 0.0F;
   }

   private void chooseRamPosition(PathfinderMob param0, LivingEntity param1) {
      this.reachedRamPositionTimestamp = Optional.empty();
      this.ramCandidate = this.calculateRammingStartPosition(param0, param1)
         .map(l_param1 -> new PrepareRamNearestTarget.RamCandidate(l_param1, param1.blockPosition(), param1));
   }

   public static class RamCandidate {
      private final BlockPos startPosition;
      private final BlockPos targetPosition;
      final LivingEntity target;

      public RamCandidate(BlockPos param0, BlockPos param1, LivingEntity param2) {
         this.startPosition = param0;
         this.targetPosition = param1;
         this.target = param2;
      }

      public BlockPos getStartPosition() {
         return this.startPosition;
      }

      public BlockPos getTargetPosition() {
         return this.targetPosition;
      }

      public LivingEntity getTarget() {
         return this.target;
      }
   }
}
