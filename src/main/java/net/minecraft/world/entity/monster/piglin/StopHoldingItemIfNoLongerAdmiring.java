package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Items;

public class StopHoldingItemIfNoLongerAdmiring {
   public static BehaviorControl<Piglin> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.absent(MemoryModuleType.ADMIRING_ITEM)).apply(l_param0, l2_param0 -> (l3_param0, l3_param1, l3_param2) -> {
            if (!l3_param1.getOffhandItem().isEmpty() && !l3_param1.getOffhandItem().is(Items.SHIELD)) {
               PiglinAi.stopHoldingOffHandItem(l3_param1, true);
               return true;
            } else {
               return false;
            }
         })
      );
   }
}
