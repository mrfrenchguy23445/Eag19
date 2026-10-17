package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MoveToTargetSink extends Behavior<Mob> {
   private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
   private int remainingCooldown;
   @Nullable
   private Path path;
   @Nullable
   private BlockPos lastTargetPos;
   private float speedModifier;

   public MoveToTargetSink() {
      this(150, 250);
   }

   public MoveToTargetSink(int param0, int param1) {
      super(
         ImmutableMap.of(
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryStatus.REGISTERED,
            MemoryModuleType.PATH,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_PRESENT
         ),
         param0,
         param1
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Mob param1) {
      if (this.remainingCooldown > 0) {
         this.remainingCooldown--;
         return false;
      }

      Brain<?> var3 = param1.getBrain();
      WalkTarget var4 = var3.getMemory(MemoryModuleType.WALK_TARGET).get();
      boolean var5 = this.reachedTarget(param1, var4);
      if (!var5 && this.tryComputePath(param1, var4, param0.getGameTime())) {
         this.lastTargetPos = var4.getTarget().currentBlockPosition();
         return true;
      }

      var3.eraseMemory(MemoryModuleType.WALK_TARGET);
      if (var5) {
         var3.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      }

      return false;
   }

   protected boolean canStillUse(ServerLevel param0, Mob param1, long param2) {
      if (this.path != null && this.lastTargetPos != null) {
         Optional<WalkTarget> var5 = param1.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
         PathNavigation var6 = param1.getNavigation();
         return !var6.isDone() && var5.isPresent() && !this.reachedTarget(param1, var5.get());
      } else {
         return false;
      }
   }

   protected void stop(ServerLevel param0, Mob param1, long param2) {
      if (param1.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
         && !this.reachedTarget(param1, param1.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get())
         && param1.getNavigation().isStuck()) {
         this.remainingCooldown = param0.getRandom().nextInt(40);
      }

      param1.getNavigation().stop();
      param1.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      param1.getBrain().eraseMemory(MemoryModuleType.PATH);
      this.path = null;
   }

   protected void start(ServerLevel param0, Mob param1, long param2) {
      param1.getBrain().setMemory(MemoryModuleType.PATH, this.path);
      param1.getNavigation().moveTo(this.path, this.speedModifier);
   }

   protected void tick(ServerLevel param0, Mob param1, long param2) {
      Path var5 = param1.getNavigation().getPath();
      Brain<?> var6 = param1.getBrain();
      if (this.path != var5) {
         this.path = var5;
         var6.setMemory(MemoryModuleType.PATH, var5);
      }

      if (var5 != null && this.lastTargetPos != null) {
         WalkTarget var7 = var6.getMemory(MemoryModuleType.WALK_TARGET).get();
         if (var7.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0 && this.tryComputePath(param1, var7, param0.getGameTime())) {
            this.lastTargetPos = var7.getTarget().currentBlockPosition();
            this.start(param0, param1, param2);
         }
      }
   }

   private boolean tryComputePath(Mob param0, WalkTarget param1, long param2) {
      BlockPos var5 = param1.getTarget().currentBlockPosition();
      this.path = param0.getNavigation().createPath(var5, 0);
      this.speedModifier = param1.getSpeedModifier();
      Brain<?> var6 = param0.getBrain();
      if (this.reachedTarget(param0, param1)) {
         var6.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      } else {
         boolean var7 = this.path != null && this.path.canReach();
         if (var7) {
            var6.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         } else if (!var6.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
            var6.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, param2);
         }

         if (this.path != null) {
            return true;
         }

         Vec3 var8 = DefaultRandomPos.getPosTowards((PathfinderMob)param0, 10, 7, Vec3.atBottomCenterOf(var5), (float) (Math.PI / 2));
         if (var8 != null) {
            this.path = param0.getNavigation().createPath(var8.x, var8.y, var8.z, 0);
            return this.path != null;
         }
      }

      return false;
   }

   private boolean reachedTarget(Mob param0, WalkTarget param1) {
      return param1.getTarget().currentBlockPosition().distManhattan(param0.blockPosition()) <= param1.getCloseEnoughDist();
   }
}
