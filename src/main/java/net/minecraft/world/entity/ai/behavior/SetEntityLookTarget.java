package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetEntityLookTarget {
   public static BehaviorControl<LivingEntity> create(MobCategory param0, float param1) {
      return create(l_param1 -> param0.equals(l_param1.getType().getCategory()), param1);
   }

   public static OneShot<LivingEntity> create(EntityType<?> param0, float param1) {
      return create(l_param1 -> param0.equals(l_param1.getType()), param1);
   }

   public static OneShot<LivingEntity> create(float param0) {
      return create(l_param0 -> true, param0);
   }

   public static OneShot<LivingEntity> create(Predicate<LivingEntity> param0, float param1) {
      float var2 = param1 * param1;
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(l_param2.absent(MemoryModuleType.LOOK_TARGET), l_param2.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
            .apply(
               l_param2,
               (l2_param3, l2_param4) -> (l3_param5, l3_param6, l3_param7) -> {
                  Optional<LivingEntity> l3_var9 = l_param2.<NearestVisibleLivingEntities>get(l2_param4)
                     .findClosest(param0.and(l4_param2 -> l4_param2.distanceToSqr(l3_param6) <= var2 && !l3_param6.hasPassenger(l4_param2)));
                  if (l3_var9.isEmpty()) {
                     return false;
                  }

                  l2_param3.set(new EntityTracker(l3_var9.get(), true));
                  return true;
               }
            )
      );
   }
}
