package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.apache.commons.lang3.mutable.MutableInt;

public class SetHiddenState {
   private static final int HIDE_TIMEOUT = 300;

   public static BehaviorControl<LivingEntity> create(int param0, int param1) {
      int var2 = param0 * 20;
      MutableInt var3 = new MutableInt(0);
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.present(MemoryModuleType.HIDING_PLACE), l_param3.present(MemoryModuleType.HEARD_BELL_TIME))
            .apply(l_param3, (l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
               long l3_var10 = l_param3.<Long>get(l2_param5);
               boolean l3_var12 = l3_var10 + 300L <= l3_param8;
               if (var3.getValue() <= var2 && !l3_var12) {
                  BlockPos l3_var13 = l_param3.get(l2_param4).pos();
                  if (l3_var13.closerThan(l3_param7.blockPosition(), param1)) {
                     var3.increment();
                  }

                  return true;
               } else {
                  l2_param5.erase();
                  l2_param4.erase();
                  l3_param7.getBrain().updateActivityFromSchedule(l3_param6.getDayTime(), l3_param6.getGameTime());
                  var3.setValue(0);
                  return true;
               }
            })
      );
   }
}
