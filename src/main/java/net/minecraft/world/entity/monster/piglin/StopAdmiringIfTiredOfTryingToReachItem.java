package net.minecraft.world.entity.monster.piglin;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAdmiringIfTiredOfTryingToReachItem {
   public static BehaviorControl<LivingEntity> create(int param0, int param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(
               l_param2.present(MemoryModuleType.ADMIRING_ITEM),
               l_param2.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               l_param2.registered(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM),
               l_param2.registered(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(l_param2, (l2_param3, l2_param4, l2_param5, l2_param6) -> (l3_param6, l3_param7, l3_param8) -> {
               if (!l3_param7.getOffhandItem().isEmpty()) {
                  return false;
               }

               Optional<Integer> l3_var10 = l_param2.tryGet(l2_param5);
               if (l3_var10.isEmpty()) {
                  l2_param5.set(0);
               } else {
                  int l3_var11 = l3_var10.get();
                  if (l3_var11 > param0) {
                     l2_param3.erase();
                     l2_param5.erase();
                     l2_param6.setWithExpiry(true, param1);
                  } else {
                     l2_param5.set(l3_var11 + 1);
                  }
               }

               return true;
            })
      );
   }
}
