package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SocializeAtBell {
   private static final float SPEED_MODIFIER = 0.3F;

   public static OneShot<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.registered(MemoryModuleType.WALK_TARGET),
               l_param0.registered(MemoryModuleType.LOOK_TARGET),
               l_param0.present(MemoryModuleType.MEETING_POINT),
               l_param0.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
               l_param0.absent(MemoryModuleType.INTERACTION_TARGET)
            )
            .apply(
               l_param0,
               (l2_param1, l2_param2, l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  GlobalPos l3_var10 = l_param0.get(l2_param3);
                  NearestVisibleLivingEntities l3_var11 = l_param0.get(l2_param4);
                  if (l3_param6.getRandom().nextInt(100) == 0
                     && l3_param6.dimension() == l3_var10.dimension()
                     && l3_var10.pos().closerToCenterThan(l3_param7.position(), 4.0)
                     && l3_var11.contains(l4_param0 -> EntityType.VILLAGER.equals(l4_param0.getType()))) {
                     l3_var11.findClosest(l4_param1 -> EntityType.VILLAGER.equals(l4_param1.getType()) && l4_param1.distanceToSqr(l3_param7) <= 32.0)
                        .ifPresent(l4_param3 -> {
                           l2_param5.set(l4_param3);
                           l2_param2.set(new EntityTracker(l4_param3, true));
                           l2_param1.set(new WalkTarget(new EntityTracker(l4_param3, false), 0.3F, 1));
                        });
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}
