package net.minecraft.world.entity.ai.behavior;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BackUpIfTooClose {
   public static OneShot<Mob> create(int param0, float param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.absent(MemoryModuleType.WALK_TARGET),
               l_param2.registered(MemoryModuleType.LOOK_TARGET),
               l_param2.present(MemoryModuleType.ATTACK_TARGET),
               l_param2.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(l_param2, (l2_param3, l2_param4, l2_param5, l2_param6) -> (l3_param6, l3_param7, l3_param8) -> {
               LivingEntity l3_var10 = l_param2.get(l2_param5);
               if (l3_var10.closerThan(l3_param7, param0) && l_param2.<NearestVisibleLivingEntities>get(l2_param6).contains(l3_var10)) {
                  l2_param4.set(new EntityTracker(l3_var10, true));
                  l3_param7.getMoveControl().strafe(-param1, 0.0F);
                  l3_param7.setYRot(Mth.rotateIfNecessary(l3_param7.getYRot(), l3_param7.yHeadRot, 0.0F));
                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
