package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class RememberIfHoglinWasKilled {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.ATTACK_TARGET), l_param0.registered(MemoryModuleType.HUNTED_RECENTLY))
            .apply(l_param0, (l2_param1, l2_param2) -> (l3_param3, l3_param4, l3_param5) -> {
               LivingEntity l3_var7 = l_param0.get(l2_param1);
               if (l3_var7.getType() == EntityType.HOGLIN && l3_var7.isDeadOrDying()) {
                  l2_param2.setWithExpiry(true, PiglinAi.TIME_BETWEEN_HUNTS.sample(l3_param4.level.random));
               }

               return true;
            })
      );
   }
}
