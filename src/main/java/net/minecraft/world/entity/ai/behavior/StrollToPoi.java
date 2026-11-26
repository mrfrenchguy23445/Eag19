package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoi {
   public static BehaviorControl<PathfinderMob> create(MemoryModuleType<GlobalPos> param0, float param1, int param2, int param3) {
      MutableLong var4 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param5 -> l_param5.group(l_param5.registered(MemoryModuleType.WALK_TARGET), l_param5.present(param0))
            .apply(l_param5, (l2_param5, l2_param6) -> (l3_param7, l3_param8, l3_param9) -> {
               GlobalPos l3_var11 = l_param5.get(l2_param6);
               if (l3_param7.dimension() != l3_var11.dimension() || !l3_var11.pos().closerToCenterThan(l3_param8.position(), param3)) {
                  return false;
               }

               if (l3_param9 <= var4.getValue()) {
                  return true;
               }

               l2_param5.set(new WalkTarget(l3_var11.pos(), param1, param2));
               var4.setValue(l3_param9 + 80L);
               return true;
            })
      );
   }
}
