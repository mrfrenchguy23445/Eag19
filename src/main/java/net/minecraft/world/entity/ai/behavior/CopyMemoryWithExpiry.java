package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class CopyMemoryWithExpiry {
   public static <E extends LivingEntity, T> BehaviorControl<E> create(
      Predicate<E> param0, MemoryModuleType<? extends T> param1, MemoryModuleType<T> param2, UniformInt param3
   ) {
      return BehaviorBuilder.create(
         l_param4 -> l_param4.group(l_param4.present(param1), l_param4.absent(param2))
            .apply(l_param4, (l2_param3, l2_param4) -> (l3_param5, l3_param6, l3_param7) -> {
               if (!param0.test(l3_param6)) {
                  return false;
               }

               l2_param4.setWithExpiry(l_param4.get(l2_param3), param3.sample(l3_param5.random));
               return true;
            })
      );
   }
}
