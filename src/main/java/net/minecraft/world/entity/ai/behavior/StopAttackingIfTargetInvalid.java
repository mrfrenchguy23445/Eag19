package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAttackingIfTargetInvalid {
   private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

   public static <E extends Mob> BehaviorControl<E> create(BiConsumer<E, LivingEntity> param0) {
      return create(l_param0 -> false, param0, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(Predicate<LivingEntity> param0) {
      return create(param0, (l_param0, l_param1) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create() {
      return create(l_param0 -> false, (l_param0, l_param1) -> {}, true);
   }

   public static <E extends Mob> BehaviorControl<E> create(Predicate<LivingEntity> param0, BiConsumer<E, LivingEntity> param1, boolean param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.present(MemoryModuleType.ATTACK_TARGET), l_param3.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
            .apply(
               l_param3,
               (l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  LivingEntity l3_var10 = l_param3.get(l2_param4);
                  if (l3_param7.canAttack(l3_var10)
                     && (!param2 || !isTiredOfTryingToReachTarget(l3_param7, l_param3.tryGet(l2_param5)))
                     && l3_var10.isAlive()
                     && l3_var10.level == l3_param7.level
                     && !param0.test(l3_var10)) {
                     return true;
                  }

                  param1.accept(l3_param7, l3_var10);
                  l2_param4.erase();
                  return true;
               }
            )
      );
   }

   private static boolean isTiredOfTryingToReachTarget(LivingEntity param0, Optional<Long> param1) {
      return param1.isPresent() && param0.level.getGameTime() - param1.get() > 200L;
   }
}
