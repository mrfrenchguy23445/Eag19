package net.minecraft.world.level.block.piston;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class PistonMath {
   public static AABB getMovementArea(AABB param0, Direction param1, double param2) {
      double var4 = param2 * param1.getAxisDirection().getStep();
      double var6 = Math.min(var4, 0.0);
      double var8 = Math.max(var4, 0.0);
      switch (param1) {
         case WEST:
            return new AABB(param0.minX + var6, param0.minY, param0.minZ, param0.minX + var8, param0.maxY, param0.maxZ);
         case EAST:
            return new AABB(param0.maxX + var6, param0.minY, param0.minZ, param0.maxX + var8, param0.maxY, param0.maxZ);
         case DOWN:
            return new AABB(param0.minX, param0.minY + var6, param0.minZ, param0.maxX, param0.minY + var8, param0.maxZ);
         case UP:
         default:
            return new AABB(param0.minX, param0.maxY + var6, param0.minZ, param0.maxX, param0.maxY + var8, param0.maxZ);
         case NORTH:
            return new AABB(param0.minX, param0.minY, param0.minZ + var6, param0.maxX, param0.maxY, param0.minZ + var8);
         case SOUTH:
            return new AABB(param0.minX, param0.minY, param0.maxZ + var6, param0.maxX, param0.maxY, param0.maxZ + var8);
      }
   }
}
