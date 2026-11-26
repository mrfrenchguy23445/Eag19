package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
   public static OneShot<Villager> create(MemoryModuleType<GlobalPos> param0, float param1, int param2, int param3, int param4) {
      return BehaviorBuilder.create(
         l_param5 -> l_param5.group(
               l_param5.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE), l_param5.absent(MemoryModuleType.WALK_TARGET), l_param5.present(param0)
            )
            .apply(l_param5, (l2_param6, l2_param7, l2_param8) -> (l3_param9, l3_param10, l3_param11) -> {
               GlobalPos l3_var13 = l_param5.get(l2_param8);
               Optional<Long> l3_var14 = l_param5.tryGet(l2_param6);
               if (l3_var13.dimension() == l3_param9.dimension() && (!l3_var14.isPresent() || l3_param9.getGameTime() - l3_var14.get() <= param4)) {
                  if (l3_var13.pos().distManhattan(l3_param10.blockPosition()) > param3) {
                     Vec3 l3_var15 = null;
                     int l3_var16 = 0;
                     int l3_var17 = 1000;

                     while (l3_var15 == null || BlockPos.containing(l3_var15).distManhattan(l3_param10.blockPosition()) > param3) {
                        l3_var15 = DefaultRandomPos.getPosTowards(l3_param10, 15, 7, Vec3.atBottomCenterOf(l3_var13.pos()), (float) (Math.PI / 2));
                        if (++l3_var16 == 1000) {
                           l3_param10.releasePoi(param0);
                           l2_param8.erase();
                           l2_param6.set(l3_param11);
                           return true;
                        }
                     }

                     l2_param7.set(new WalkTarget(l3_var15, param1, param2));
                  } else if (l3_var13.pos().distManhattan(l3_param10.blockPosition()) > param2) {
                     l2_param7.set(new WalkTarget(l3_var13.pos(), param1, param2));
                  }
               } else {
                  l3_param10.releasePoi(param0);
                  l2_param8.erase();
                  l2_param6.set(l3_param11);
               }

               return true;
            })
      );
   }
}
