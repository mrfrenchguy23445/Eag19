package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Mount {
   private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;

   public static BehaviorControl<LivingEntity> create(float param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.registered(MemoryModuleType.LOOK_TARGET), l_param1.absent(MemoryModuleType.WALK_TARGET), l_param1.present(MemoryModuleType.RIDE_TARGET)
            )
            .apply(l_param1, (l2_param2, l2_param3, l2_param4) -> (l3_param5, l3_param6, l3_param7) -> {
               if (l3_param6.isPassenger()) {
                  return false;
               }

               Entity l3_var9 = l_param1.get(l2_param4);
               if (l3_var9.closerThan(l3_param6, 1.0)) {
                  l3_param6.startRiding(l3_var9);
               } else {
                  l2_param2.set(new EntityTracker(l3_var9, true));
                  l2_param3.set(new WalkTarget(new EntityTracker(l3_var9, false), param0, 1));
               }

               return true;
            })
      );
   }
}
