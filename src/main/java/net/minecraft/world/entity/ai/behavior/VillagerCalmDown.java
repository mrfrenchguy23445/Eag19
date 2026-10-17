package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class VillagerCalmDown {
   private static final int SAFE_DISTANCE_FROM_DANGER = 36;

   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.registered(MemoryModuleType.HURT_BY),
               l_param0.registered(MemoryModuleType.HURT_BY_ENTITY),
               l_param0.registered(MemoryModuleType.NEAREST_HOSTILE)
            )
            .apply(
               l_param0,
               (l2_param1, l2_param2, l2_param3) -> (l3_param4, l3_param5, l3_param6) -> {
                  boolean l3_var8 = l_param0.tryGet(l2_param1).isPresent()
                     || l_param0.tryGet(l2_param3).isPresent()
                     || l_param0.<LivingEntity>tryGet(l2_param2).filter(l4_param1 -> l4_param1.distanceToSqr(l3_param5) <= 36.0).isPresent();
                  if (!l3_var8) {
                     l2_param1.erase();
                     l2_param2.erase();
                     l3_param5.getBrain().updateActivityFromSchedule(l3_param4.getDayTime(), l3_param4.getGameTime());
                  }

                  return true;
               }
            )
      );
   }
}
