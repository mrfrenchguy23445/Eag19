package net.minecraft.world.entity.ai.behavior.warden;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public class SetRoarTarget {
   public static <E extends Warden> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.absent(MemoryModuleType.ROAR_TARGET),
               l_param1.absent(MemoryModuleType.ATTACK_TARGET),
               l_param1.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
            )
            .apply(l_param1, (l2_param1, l2_param2, l2_param3) -> (l3_param3, l3_param4, l3_param5) -> {
               Optional<? extends LivingEntity> l3_var7 = param0.apply(l3_param4);
               if (l3_var7.filter(l3_param4::canTargetEntity).isEmpty()) {
                  return false;
               }

               l2_param1.set(l3_var7.get());
               l2_param3.erase();
               return true;
            })
      );
   }
}
