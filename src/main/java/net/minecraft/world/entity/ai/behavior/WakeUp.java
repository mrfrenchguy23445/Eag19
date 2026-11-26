package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.schedule.Activity;

public class WakeUp {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(l_param0 -> l_param0.point((l2_param0, l2_param1, l2_param2) -> {
         if (!l2_param1.getBrain().isActive(Activity.REST) && l2_param1.isSleeping()) {
            l2_param1.stopSleeping();
            return true;
         } else {
            return false;
         }
      }));
   }
}
