package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class StartAdmiringItemIfSeen {
   public static BehaviorControl<LivingEntity> create(int param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
               l_param1.absent(MemoryModuleType.ADMIRING_ITEM),
               l_param1.absent(MemoryModuleType.ADMIRING_DISABLED),
               l_param1.absent(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
            )
            .apply(l_param1, (l2_param2, l2_param3, l2_param4, l2_param5) -> (l3_param4, l3_param5, l3_param6) -> {
               ItemEntity l3_var8 = l_param1.get(l2_param2);
               if (!PiglinAi.isLovedItem(l3_var8.getItem())) {
                  return false;
               }

               l2_param3.setWithExpiry(true, param0);
               return true;
            })
      );
   }
}
