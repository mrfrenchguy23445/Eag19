package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class EraseMemoryIf {
   public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> param0, MemoryModuleType<?> param1) {
      return BehaviorBuilder.create(l_param2 -> l_param2.group(l_param2.present(param1)).apply(l_param2, l2_param1 -> (l3_param2, l3_param3, l3_param4) -> {
         if (param0.test(l3_param3)) {
            l2_param1.erase();
            return true;
         } else {
            return false;
         }
      }));
   }
}
