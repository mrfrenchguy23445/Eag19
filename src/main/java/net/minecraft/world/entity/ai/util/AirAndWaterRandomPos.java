package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirAndWaterRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob param0, int param1, int param2, int param3, double param4, double param6, double param8) {
      boolean var10 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(param0, () -> generateRandomPos(param0, param1, param2, param3, param4, param6, param8, var10));
   }

   @Nullable
   public static BlockPos generateRandomPos(
      PathfinderMob param0, int param1, int param2, int param3, double param4, double param6, double param8, boolean param10
   ) {
      BlockPos var11 = RandomPos.generateRandomDirectionWithinRadians(param0.getRandom(), param1, param2, param3, param4, param6, param8);
      if (var11 == null) {
         return null;
      } else {
         BlockPos var12 = RandomPos.generateRandomPosTowardDirection(param0, param1, param0.getRandom(), var11);
         if (!GoalUtils.isOutsideLimits(var12, param0) && !GoalUtils.isRestricted(param10, param0, var12)) {
            var12 = RandomPos.moveUpOutOfSolid(var12, param0.level.getMaxBuildHeight(), l_param1 -> GoalUtils.isSolid(param0, l_param1));
            return GoalUtils.hasMalus(param0, var12) ? null : var12;
         } else {
            return null;
         }
      }
   }
}
