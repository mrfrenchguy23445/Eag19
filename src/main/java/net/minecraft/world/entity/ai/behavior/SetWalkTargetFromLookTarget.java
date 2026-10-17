package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget {
   public static OneShot<LivingEntity> create(float param0, int param1) {
      return create(l_param0 -> true, l_param1 -> param0, param1);
   }

   public static OneShot<LivingEntity> create(Predicate<LivingEntity> param0, Function<LivingEntity, Float> param1, int param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.absent(MemoryModuleType.WALK_TARGET), l_param3.present(MemoryModuleType.LOOK_TARGET))
            .apply(l_param3, (l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
               if (!param0.test(l3_param7)) {
                  return false;
               }

               l2_param4.set(new WalkTarget(l_param3.get(l2_param5), param1.apply(l3_param7), param2));
               return true;
            })
      );
   }
}
