package net.minecraft.world.entity.ai.behavior;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk {
   public static BehaviorControl<PathfinderMob> create(float param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(l_param1.absent(MemoryModuleType.WALK_TARGET))
            .apply(
               l_param1,
               l2_param1 -> (l3_param2, l3_param3, l3_param4) -> {
                  if (l3_param2.canSeeSky(l3_param3.blockPosition())) {
                     return false;
                  }

                  BlockPos l3_var6 = l3_param3.blockPosition();
                  List<BlockPos> l3_var7 = BlockPos.betweenClosedStream(l3_var6.offset(-1, -1, -1), l3_var6.offset(1, 1, 1))
                     .map(BlockPos::immutable)
                     .collect(Collectors.toList());
                  Collections.shuffle(l3_var7);
                  l3_var7.stream()
                     .filter(l4_param1 -> !l3_param2.canSeeSky(l4_param1))
                     .filter(l4_param2 -> l3_param2.loadedAndEntityCanStandOn(l4_param2, l3_param3))
                     .filter(l4_param2 -> l3_param2.noCollision(l3_param3))
                     .findFirst()
                     .ifPresent(l4_param2 -> l2_param1.set(new WalkTarget(l4_param2, param0, 0)));
                  return true;
               }
            )
      );
   }
}
