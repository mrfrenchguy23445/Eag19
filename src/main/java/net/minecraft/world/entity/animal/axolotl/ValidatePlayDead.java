package net.minecraft.world.entity.animal.axolotl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class ValidatePlayDead {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.PLAY_DEAD_TICKS), l_param0.registered(MemoryModuleType.HURT_BY_ENTITY))
            .apply(l_param0, (l2_param1, l2_param2) -> (l3_param3, l3_param4, l3_param5) -> {
               int l3_var7 = l_param0.get(l2_param1);
               if (l3_var7 <= 0) {
                  l2_param1.erase();
                  l2_param2.erase();
                  l3_param4.getBrain().useDefaultActivity();
               } else {
                  l2_param1.set(l3_var7 - 1);
               }

               return true;
            })
      );
   }
}
