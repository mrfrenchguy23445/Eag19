package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.HEARD_BELL_TIME)).apply(l_param0, l2_param0 -> (l3_param0, l3_param1, l3_param2) -> {
            Raid l3_var4 = l3_param0.getRaidAt(l3_param1.blockPosition());
            if (l3_var4 == null) {
               l3_param1.getBrain().setActiveActivityIfPossible(Activity.HIDE);
            }

            return true;
         })
      );
   }
}
