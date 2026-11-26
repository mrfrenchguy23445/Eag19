package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromAttackTargetIfTargetOutOfReach {
   private static final int PROJECTILE_ATTACK_RANGE_BUFFER = 1;

   public static BehaviorControl<Mob> create(float param0) {
      return create(l_param1 -> param0);
   }

   public static BehaviorControl<Mob> create(Function<LivingEntity, Float> param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.registered(MemoryModuleType.WALK_TARGET),
               l_param1.registered(MemoryModuleType.LOOK_TARGET),
               l_param1.present(MemoryModuleType.ATTACK_TARGET),
               l_param1.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(l_param1, (l2_param2, l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
               LivingEntity l3_var10 = l_param1.get(l2_param4);
               Optional<NearestVisibleLivingEntities> l3_var11 = l_param1.tryGet(l2_param5);
               if (l3_var11.isPresent() && l3_var11.get().contains(l3_var10) && BehaviorUtils.isWithinAttackRange(l3_param7, l3_var10, 1)) {
                  l2_param2.erase();
               } else {
                  l2_param3.set(new EntityTracker(l3_var10, true));
                  l2_param2.set(new WalkTarget(new EntityTracker(l3_var10, false), param0.apply(l3_param7), 0));
               }

               return true;
            })
      );
   }
}
