package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LookAtTargetSink extends Behavior<Mob> {
   public LookAtTargetSink(int param0, int param1) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT), param0, param1);
   }

   protected boolean canStillUse(ServerLevel param0, Mob param1, long param2) {
      return param1.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).filter(l_param1 -> l_param1.isVisibleBy(param1)).isPresent();
   }

   protected void stop(ServerLevel param0, Mob param1, long param2) {
      param1.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(ServerLevel param0, Mob param1, long param2) {
      param1.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(l_param1 -> param1.getLookControl().setLookAt(l_param1.currentPosition()));
   }
}
