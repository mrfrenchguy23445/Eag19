package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.GameRules;

public class StartCelebratingIfTargetDead {
   public static BehaviorControl<LivingEntity> create(int param0, BiPredicate<LivingEntity, LivingEntity> param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.present(MemoryModuleType.ATTACK_TARGET),
               l_param2.registered(MemoryModuleType.ANGRY_AT),
               l_param2.absent(MemoryModuleType.CELEBRATE_LOCATION),
               l_param2.registered(MemoryModuleType.DANCING)
            )
            .apply(l_param2, (l2_param3, l2_param4, l2_param5, l2_param6) -> (l3_param7, l3_param8, l3_param9) -> {
               LivingEntity l3_var11 = l_param2.get(l2_param3);
               if (!l3_var11.isDeadOrDying()) {
                  return false;
               }

               if (param1.test(l3_param8, l3_var11)) {
                  l2_param6.setWithExpiry(true, param0);
               }

               l2_param5.setWithExpiry(l3_var11.blockPosition(), param0);
               if (l3_var11.getType() != EntityType.PLAYER || l3_param7.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
                  l2_param3.erase();
                  l2_param4.erase();
               }

               return true;
            })
      );
   }
}
