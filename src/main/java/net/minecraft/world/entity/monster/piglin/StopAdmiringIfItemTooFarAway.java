package net.minecraft.world.entity.monster.piglin;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class StopAdmiringIfItemTooFarAway<E extends Piglin> {
   public static BehaviorControl<LivingEntity> create(int param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(l_param1.present(MemoryModuleType.ADMIRING_ITEM), l_param1.registered(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM))
            .apply(l_param1, (l2_param2, l2_param3) -> (l3_param4, l3_param5, l3_param6) -> {
               if (!l3_param5.getOffhandItem().isEmpty()) {
                  return false;
               }

               Optional<ItemEntity> l3_var8 = l_param1.tryGet(l2_param3);
               if (l3_var8.isPresent() && l3_var8.get().closerThan(l3_param5, param0)) {
                  return false;
               }

               l2_param2.erase();
               return true;
            })
      );
   }
}
