package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public class AnimalPanic extends Behavior<PathfinderMob> {
   private static final int PANIC_MIN_DURATION = 100;
   private static final int PANIC_MAX_DURATION = 120;
   private static final int PANIC_DISTANCE_HORIZONTAL = 5;
   private static final int PANIC_DISTANCE_VERTICAL = 4;
   private static final Predicate<PathfinderMob> DEFAULT_SHOULD_PANIC_PREDICATE = param0 -> param0.getLastHurtByMob() != null
      || param0.isFreezing()
      || param0.isOnFire();
   private final float speedMultiplier;
   private final Predicate<PathfinderMob> shouldPanic;

   public AnimalPanic(float param0) {
      this(param0, DEFAULT_SHOULD_PANIC_PREDICATE);
   }

   public AnimalPanic(float param0, Predicate<PathfinderMob> param1) {
      super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.REGISTERED, MemoryModuleType.HURT_BY, MemoryStatus.VALUE_PRESENT), 100, 120);
      this.speedMultiplier = param0;
      this.shouldPanic = param1;
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, PathfinderMob param1) {
      return this.shouldPanic.test(param1);
   }

   protected boolean canStillUse(ServerLevel param0, PathfinderMob param1, long param2) {
      return true;
   }

   protected void start(ServerLevel param0, PathfinderMob param1, long param2) {
      param1.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
      param1.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   protected void stop(ServerLevel param0, PathfinderMob param1, long param2) {
      Brain<?> var5 = param1.getBrain();
      var5.eraseMemory(MemoryModuleType.IS_PANICKING);
   }

   protected void tick(ServerLevel param0, PathfinderMob param1, long param2) {
      if (param1.getNavigation().isDone()) {
         Vec3 var5 = this.getPanicPos(param1, param0);
         if (var5 != null) {
            param1.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(var5, this.speedMultiplier, 0));
         }
      }
   }

   @Nullable
   private Vec3 getPanicPos(PathfinderMob param0, ServerLevel param1) {
      if (param0.isOnFire()) {
         Optional<Vec3> var3 = this.lookForWater(param1, param0).map(Vec3::atBottomCenterOf);
         if (var3.isPresent()) {
            return var3.get();
         }
      }

      return LandRandomPos.getPos(param0, 5, 4);
   }

   private Optional<BlockPos> lookForWater(BlockGetter param0, Entity param1) {
      BlockPos var3 = param1.blockPosition();
      return !param0.getBlockState(var3).getCollisionShape(param0, var3).isEmpty()
         ? Optional.empty()
         : BlockPos.findClosestMatch(var3, 5, 1, l_param1 -> param0.getFluidState(l_param1).is(FluidTags.WATER));
   }
}
