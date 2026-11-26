package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
   private static BlockPos getNearbyPos(Mob param0, BlockPos param1) {
      RandomSource var2 = param0.level.random;
      return param1.offset(getRandomOffset(var2), 0, getRandomOffset(var2));
   }

   private static int getRandomOffset(RandomSource param0) {
      return param0.nextInt(3) - 1;
   }

   public static <E extends Mob> OneShot<E> create(MemoryModuleType<BlockPos> param0, int param1, float param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(
               l_param3.present(param0),
               l_param3.absent(MemoryModuleType.ATTACK_TARGET),
               l_param3.absent(MemoryModuleType.WALK_TARGET),
               l_param3.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(l_param3, (l2_param3, l2_param4, l2_param5, l2_param6) -> (l3_param4, l3_param5, l3_param6) -> {
               BlockPos l3_var8 = l_param3.get(l2_param3);
               boolean l3_var9 = l3_var8.closerThan(l3_param5.blockPosition(), param1);
               if (!l3_var9) {
                  BehaviorUtils.setWalkAndLookTargetMemories(l3_param5, getNearbyPos(l3_param5, l3_var8), param2, param1);
               }

               return true;
            })
      );
   }
}
