package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(l_param0 -> l_param0.point((l2_param0, l2_param1, l2_param2) -> {
         l2_param1.getBrain().updateActivityFromSchedule(l2_param0.getDayTime(), l2_param0.getGameTime());
         return true;
      }));
   }
}
