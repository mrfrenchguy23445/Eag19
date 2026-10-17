package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith {
   public static <T extends LivingEntity> BehaviorControl<LivingEntity> of(
      EntityType<? extends T> param0, int param1, MemoryModuleType<T> param2, float param3, int param4
   ) {
      return of(param0, param1, l_param0 -> true, l_param0 -> true, param2, param3, param4);
   }

   public static <E extends LivingEntity, T extends LivingEntity> BehaviorControl<E> of(
      EntityType<? extends T> param0, int param1, Predicate<E> param2, Predicate<T> param3, MemoryModuleType<T> param4, float param5, int param6
   ) {
      int var7 = param1 * param1;
      Predicate<LivingEntity> var8 = l_param2 -> param0.equals(l_param2.getType()) && param3.test((T)l_param2);
      return BehaviorBuilder.create(
         l_param6 -> l_param6.group(
               l_param6.registered(param4),
               l_param6.registered(MemoryModuleType.LOOK_TARGET),
               l_param6.absent(MemoryModuleType.WALK_TARGET),
               l_param6.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(l_param6, (l2_param6, l2_param7, l2_param8, l2_param9) -> (l3_param10, l3_param11, l3_param12) -> {
               NearestVisibleLivingEntities l3_var14 = l_param6.get(l2_param9);
               if (param2.test(l3_param11) && l3_var14.contains(var8)) {
                  Optional<LivingEntity> l3_var15 = l3_var14.findClosest(l4_param3 -> l4_param3.distanceToSqr(l3_param11) <= var7 && var8.test(l4_param3));
                  l3_var15.ifPresent(l4_param5 -> {
                     ((net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor)l2_param6).set(l4_param5);
                     l2_param7.set(new EntityTracker(l4_param5, true));
                     l2_param8.set(new WalkTarget(new EntityTracker(l4_param5, false), param5, param6));
                  });
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
