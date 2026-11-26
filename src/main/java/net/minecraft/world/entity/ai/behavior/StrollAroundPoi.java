package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollAroundPoi {
   private static final int MIN_TIME_BETWEEN_STROLLS = 180;
   private static final int STROLL_MAX_XZ_DIST = 8;
   private static final int STROLL_MAX_Y_DIST = 6;

   public static OneShot<PathfinderMob> create(MemoryModuleType<GlobalPos> param0, float param1, int param2) {
      MutableLong var3 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param4 -> l_param4.group(l_param4.registered(MemoryModuleType.WALK_TARGET), l_param4.present(param0))
            .apply(l_param4, (l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
               GlobalPos l3_var10 = l_param4.get(l2_param5);
               if (l3_param6.dimension() != l3_var10.dimension() || !l3_var10.pos().closerToCenterThan(l3_param7.position(), param2)) {
                  return false;
               }

               if (l3_param8 <= var3.getValue()) {
                  return true;
               }

               Optional<Vec3> l3_var11 = Optional.ofNullable(LandRandomPos.getPos(l3_param7, 8, 6));
               l2_param4.setOrErase(l3_var11.map(l4_param1 -> new WalkTarget(l4_param1, param1, 1)));
               var3.setValue(l3_param8 + 180L);
               return true;
            })
      );
   }
}
