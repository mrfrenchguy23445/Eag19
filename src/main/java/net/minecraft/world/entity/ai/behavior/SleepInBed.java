package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SleepInBed extends Behavior<LivingEntity> {
   public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
   private long nextOkStartTime;

   public SleepInBed() {
      super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel param0, LivingEntity param1) {
      if (param1.isPassenger()) {
         return false;
      }

      Brain<?> var3 = param1.getBrain();
      GlobalPos var4 = var3.getMemory(MemoryModuleType.HOME).get();
      if (param0.dimension() != var4.dimension()) {
         return false;
      }

      Optional<Long> var5 = var3.getMemory(MemoryModuleType.LAST_WOKEN);
      if (var5.isPresent()) {
         long var6 = param0.getGameTime() - var5.get();
         if (var6 > 0L && var6 < 100L) {
            return false;
         }
      }

      BlockState var6 = param0.getBlockState(var4.pos());
      return var4.pos().closerToCenterThan(param1.position(), 2.0) && var6.is(BlockTags.BEDS) && !var6.getValue(BedBlock.OCCUPIED);
   }

   @Override
   protected boolean canStillUse(ServerLevel param0, LivingEntity param1, long param2) {
      Optional<GlobalPos> var5 = param1.getBrain().getMemory(MemoryModuleType.HOME);
      if (!var5.isPresent()) {
         return false;
      }

      BlockPos var6 = var5.get().pos();
      return param1.getBrain().isActive(Activity.REST) && param1.getY() > var6.getY() + 0.4 && var6.closerToCenterThan(param1.position(), 1.14);
   }

   @Override
   protected void start(ServerLevel param0, LivingEntity param1, long param2) {
      if (param2 > this.nextOkStartTime) {
         Brain<?> var5 = param1.getBrain();
         if (var5.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Set<GlobalPos> var6 = var5.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
            Optional<List<LivingEntity>> var7;
            if (var5.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)) {
               var7 = var5.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
            } else {
               var7 = Optional.empty();
            }

            InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(param0, param1, null, null, var6, var7);
         }

         param1.startSleeping(param1.getBrain().getMemory(MemoryModuleType.HOME).get().pos());
      }
   }

   @Override
   protected boolean timedOut(long param0) {
      return false;
   }

   @Override
   protected void stop(ServerLevel param0, LivingEntity param1, long param2) {
      if (param1.isSleeping()) {
         param1.stopSleeping();
         this.nextOkStartTime = param2 + 40L;
      }
   }
}
