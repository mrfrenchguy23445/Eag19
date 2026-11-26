package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.GameRules;

public class StopBeingAngryIfTargetDead {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.ANGRY_AT))
            .apply(
               l_param0,
               l2_param1 -> (l3_param2, l3_param3, l3_param4) -> {
                  Optional.ofNullable(l3_param2.getEntity(l_param0.get(l2_param1)))
                     .map(l4_param0 -> l4_param0 instanceof LivingEntity l4_var1 ? l4_var1 : null)
                     .filter(LivingEntity::isDeadOrDying)
                     .filter(l4_param1 -> l4_param1.getType() != EntityType.PLAYER || l3_param2.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS))
                     .ifPresent(l4_param1 -> l2_param1.erase());
                  return true;
               }
            )
      );
   }
}
