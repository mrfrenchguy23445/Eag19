package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StartAttacking {
   public static <E extends Mob> BehaviorControl<E> create(Function<E, Optional<? extends LivingEntity>> param0) {
      return create(l_param0 -> true, param0);
   }

   public static <E extends Mob> BehaviorControl<E> create(Predicate<E> param0, Function<E, Optional<? extends LivingEntity>> param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(l_param2.absent(MemoryModuleType.ATTACK_TARGET), l_param2.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(l_param2, (l2_param2, l2_param3) -> (l3_param4, l3_param5, l3_param6) -> {
               if (!param0.test(l3_param5)) {
                  return false;
               }

               Optional<? extends LivingEntity> l3_var8 = param1.apply(l3_param5);
               if (l3_var8.isEmpty()) {
                  return false;
               }

               LivingEntity l3_var9 = l3_var8.get();
               if (!l3_param5.canAttack(l3_var9)) {
                  return false;
               }

               l2_param2.set(l3_var9);
               l2_param3.erase();
               return true;
            })
      );
   }
}
