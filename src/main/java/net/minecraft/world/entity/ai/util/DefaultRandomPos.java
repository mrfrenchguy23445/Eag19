package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class DefaultRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob param0, int param1, int param2) {
      boolean var3 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(param0, () -> {
         BlockPos l_var4 = RandomPos.generateRandomDirection(param0.getRandom(), param1, param2);
         return generateRandomPosTowardDirection(param0, param1, var3, l_var4);
      });
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob param0, int param1, int param2, Vec3 param3, double param4) {
      Vec3 var6 = param3.subtract(param0.getX(), param0.getY(), param0.getZ());
      boolean var7 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(param0, () -> {
         BlockPos l_var7 = RandomPos.generateRandomDirectionWithinRadians(param0.getRandom(), param1, param2, 0, var6.x, var6.z, param4);
         return l_var7 == null ? null : generateRandomPosTowardDirection(param0, param1, var7, l_var7);
      });
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob param0, int param1, int param2, Vec3 param3) {
      Vec3 var4 = param0.position().subtract(param3);
      boolean var5 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(param0, () -> {
         BlockPos l_var5 = RandomPos.generateRandomDirectionWithinRadians(param0.getRandom(), param1, param2, 0, var4.x, var4.z, (float) (Math.PI / 2));
         return l_var5 == null ? null : generateRandomPosTowardDirection(param0, param1, var5, l_var5);
      });
   }

   @Nullable
   private static BlockPos generateRandomPosTowardDirection(PathfinderMob param0, int param1, boolean param2, BlockPos param3) {
      BlockPos var4 = RandomPos.generateRandomPosTowardDirection(param0, param1, param0.getRandom(), param3);
      return !GoalUtils.isOutsideLimits(var4, param0)
            && !GoalUtils.isRestricted(param2, param0, var4)
            && !GoalUtils.isNotStable(param0.getNavigation(), var4)
            && !GoalUtils.hasMalus(param0, var4)
         ? var4
         : null;
   }
}
