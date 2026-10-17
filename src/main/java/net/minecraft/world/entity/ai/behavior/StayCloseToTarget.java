package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StayCloseToTarget {
   public static BehaviorControl<LivingEntity> create(
      Function<LivingEntity, Optional<PositionTracker>> param0, Predicate<LivingEntity> param1, int param2, int param3, float param4
   ) {
      return BehaviorBuilder.create(
         l_param5 -> l_param5.group(l_param5.registered(MemoryModuleType.LOOK_TARGET), l_param5.registered(MemoryModuleType.WALK_TARGET))
            .apply(l_param5, (l2_param5, l2_param6) -> (l3_param7, l3_param8, l3_param9) -> {
               Optional<PositionTracker> l3_var11 = param0.apply(l3_param8);
               if (!l3_var11.isEmpty() && param1.test(l3_param8)) {
                  PositionTracker l3_var12 = l3_var11.get();
                  if (l3_param8.position().closerThan(l3_var12.currentPosition(), param3)) {
                     return false;
                  }

                  PositionTracker l3_var13 = l3_var11.get();
                  l2_param5.set(l3_var13);
                  l2_param6.set(new WalkTarget(l3_var13, param4, param2));
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
