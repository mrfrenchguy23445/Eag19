package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class LandRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob param0, int param1, int param2) {
      return getPos(param0, param1, param2, param0::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 getPos(PathfinderMob param0, int param1, int param2, ToDoubleFunction<BlockPos> param3) {
      boolean var4 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(() -> {
         BlockPos l_var4 = RandomPos.generateRandomDirection(param0.getRandom(), param1, param2);
         BlockPos l_var5 = generateRandomPosTowardDirection(param0, param1, var4, l_var4);
         return l_var5 == null ? null : movePosUpOutOfSolid(param0, l_var5);
      }, param3);
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob param0, int param1, int param2, Vec3 param3) {
      Vec3 var4 = param3.subtract(param0.getX(), param0.getY(), param0.getZ());
      boolean var5 = GoalUtils.mobRestricted(param0, param1);
      return getPosInDirection(param0, param1, param2, var4, var5);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob param0, int param1, int param2, Vec3 param3) {
      Vec3 var4 = param0.position().subtract(param3);
      boolean var5 = GoalUtils.mobRestricted(param0, param1);
      return getPosInDirection(param0, param1, param2, var4, var5);
   }

   @Nullable
   private static Vec3 getPosInDirection(PathfinderMob param0, int param1, int param2, Vec3 param3, boolean param4) {
      return RandomPos.generateRandomPos(param0, () -> {
         BlockPos l_var5 = RandomPos.generateRandomDirectionWithinRadians(param0.getRandom(), param1, param2, 0, param3.x, param3.z, (float) (Math.PI / 2));
         if (l_var5 == null) {
            return null;
         }

         BlockPos l_var6 = generateRandomPosTowardDirection(param0, param1, param4, l_var5);
         return l_var6 == null ? null : movePosUpOutOfSolid(param0, l_var6);
      });
   }

   @Nullable
   public static BlockPos movePosUpOutOfSolid(PathfinderMob param0, BlockPos param1) {
      param1 = RandomPos.moveUpOutOfSolid(param1, param0.level.getMaxBuildHeight(), l_param1 -> GoalUtils.isSolid(param0, l_param1));
      return !GoalUtils.isWater(param0, param1) && !GoalUtils.hasMalus(param0, param1) ? param1 : null;
   }

   @Nullable
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob param0, int param1, boolean param2, BlockPos param3) {
      BlockPos var4 = RandomPos.generateRandomPosTowardDirection(param0, param1, param0.getRandom(), param3);
      return !GoalUtils.isOutsideLimits(var4, param0) && !GoalUtils.isRestricted(param2, param0, var4) && !GoalUtils.isNotStable(param0.getNavigation(), var4)
         ? var4
         : null;
   }
}
