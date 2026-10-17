package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BecomePassiveIfMemoryPresent {
   public static BehaviorControl<LivingEntity> create(MemoryModuleType<?> param0, int param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(l_param2.registered(MemoryModuleType.ATTACK_TARGET), l_param2.absent(MemoryModuleType.PACIFIED), l_param2.present(param0))
            .apply(
               l_param2,
               l_param2.point(() -> "[BecomePassive if " + param0 + " present]", (l2_param1, l2_param2, l2_param3) -> (l3_param3, l3_param4, l3_param5) -> {
                  l2_param2.setWithExpiry(true, param1);
                  l2_param1.erase();
                  return true;
               })
            )
      );
   }
}
