package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
   public static OneShot<AgeableMob> create(UniformInt param0, float param1) {
      return create(param0, l_param1 -> param1);
   }

   public static OneShot<AgeableMob> create(UniformInt param0, Function<LivingEntity, Float> param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.present(MemoryModuleType.NEAREST_VISIBLE_ADULT),
               l_param2.registered(MemoryModuleType.LOOK_TARGET),
               l_param2.absent(MemoryModuleType.WALK_TARGET)
            )
            .apply(l_param2, (l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
               if (!l3_param7.isBaby()) {
                  return false;
               } else {
                  AgeableMob l3_var10 = l_param2.get(l2_param3);
                  if (l3_param7.closerThan(l3_var10, param0.getMaxValue() + 1) && !l3_param7.closerThan(l3_var10, param0.getMinValue())) {
                     WalkTarget l3_var11 = new WalkTarget(new EntityTracker(l3_var10, false), param1.apply(l3_param7), param0.getMinValue() - 1);
                     l2_param4.set(new EntityTracker(l3_var10, true));
                     l2_param5.set(l3_var11);
                     return true;
                  } else {
                     return false;
                  }
               }
            })
      );
   }
}
