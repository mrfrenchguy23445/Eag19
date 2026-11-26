package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoiList {
   public static BehaviorControl<Villager> create(
      MemoryModuleType<List<GlobalPos>> param0, float param1, int param2, int param3, MemoryModuleType<GlobalPos> param4
   ) {
      MutableLong var5 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param6 -> l_param6.group(l_param6.registered(MemoryModuleType.WALK_TARGET), l_param6.present(param0), l_param6.present(param4))
            .apply(l_param6, (l2_param5, l2_param6, l2_param7) -> (l3_param8, l3_param9, l3_param10) -> {
               List<GlobalPos> l3_var12 = l_param6.get(l2_param6);
               GlobalPos l3_var13 = l_param6.get(l2_param7);
               if (l3_var12.isEmpty()) {
                  return false;
               }

               GlobalPos l3_var14 = l3_var12.get(l3_param8.getRandom().nextInt(l3_var12.size()));
               if (l3_var14 != null && l3_param8.dimension() == l3_var14.dimension() && l3_var13.pos().closerToCenterThan(l3_param9.position(), param3)) {
                  if (l3_param10 > var5.getValue()) {
                     l2_param5.set(new WalkTarget(l3_var14.pos(), param1, param2));
                     var5.setValue(l3_param10 + 100L);
                  }

                  return true;
               } else {
                  return false;
               }
            })
      );
   }
}
