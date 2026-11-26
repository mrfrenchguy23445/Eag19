package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirRandomPos {
   @Nullable
   public static Vec3 getPosTowards(PathfinderMob param0, int param1, int param2, int param3, Vec3 param4, double param5) {
      Vec3 var7 = param4.subtract(param0.getX(), param0.getY(), param0.getZ());
      boolean var8 = GoalUtils.mobRestricted(param0, param1);
      return RandomPos.generateRandomPos(param0, () -> {
         BlockPos l_var8 = AirAndWaterRandomPos.generateRandomPos(param0, param1, param2, param3, var7.x, var7.z, param5, var8);
         return l_var8 != null && !GoalUtils.isWater(param0, l_var8) ? l_var8 : null;
      });
   }
}
