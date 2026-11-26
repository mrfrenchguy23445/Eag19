package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class HoverRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob param0, int param1, int param2, double param3, double param5, float param7, int param8, int param9) {
      boolean var10 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(
         param0,
         () -> {
            BlockPos l_var11 = RandomPos.generateRandomDirectionWithinRadians(param0.getRandom(), param1, param2, 0, param3, param5, param7);
            if (l_var11 == null) {
               return null;
            }

            BlockPos l_var12 = LandRandomPos.generateRandomPosTowardDirection(param0, param1, var10, l_var11);
            if (l_var12 == null) {
               return null;
            }

            l_var12 = RandomPos.moveUpToAboveSolid(
               l_var12,
               param0.getRandom().nextInt(param8 - param9 + 1) + param9,
               param0.level.getMaxBuildHeight(),
               l2_param1 -> GoalUtils.isSolid(param0, l2_param1)
            );
            return !GoalUtils.isWater(param0, l_var12) && !GoalUtils.hasMalus(param0, l_var12) ? l_var12 : null;
         }
      );
   }
}
