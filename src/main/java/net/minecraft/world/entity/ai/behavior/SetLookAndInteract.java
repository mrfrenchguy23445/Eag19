package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetLookAndInteract {
   public static BehaviorControl<LivingEntity> create(EntityType<?> param0, int param1) {
      int var2 = param1 * param1;
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.registered(MemoryModuleType.LOOK_TARGET),
               l_param2.absent(MemoryModuleType.INTERACTION_TARGET),
               l_param2.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               l_param2,
               (l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  Optional<LivingEntity> l3_var10 = l_param2.<NearestVisibleLivingEntities>get(l2_param5)
                     .findClosest(l4_param3 -> l4_param3.distanceToSqr(l3_param7) <= var2 && param0.equals(l4_param3.getType()));
                  if (l3_var10.isEmpty()) {
                     return false;
                  }

                  LivingEntity l3_var11 = l3_var10.get();
                  l2_param4.set(l3_var11);
                  l2_param3.set(new EntityTracker(l3_var11, true));
                  return true;
               }
            )
      );
   }
}
