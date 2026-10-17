package net.minecraft.world.phys;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class AABB {
   private static final double EPSILON = 1.0E-7;
   public final double minX;
   public final double minY;
   public final double minZ;
   public final double maxX;
   public final double maxY;
   public final double maxZ;

   public AABB(double param0, double param2, double param4, double param6, double param8, double param10) {
      this.minX = Math.min(param0, param6);
      this.minY = Math.min(param2, param8);
      this.minZ = Math.min(param4, param10);
      this.maxX = Math.max(param0, param6);
      this.maxY = Math.max(param2, param8);
      this.maxZ = Math.max(param4, param10);
   }

   public AABB(BlockPos param0) {
      this(param0.getX(), param0.getY(), param0.getZ(), param0.getX() + 1, param0.getY() + 1, param0.getZ() + 1);
   }

   public AABB(BlockPos param0, BlockPos param1) {
      this(param0.getX(), param0.getY(), param0.getZ(), param1.getX(), param1.getY(), param1.getZ());
   }

   public AABB(Vec3 param0, Vec3 param1) {
      this(param0.x, param0.y, param0.z, param1.x, param1.y, param1.z);
   }

   public static AABB of(BoundingBox param0) {
      return new AABB(param0.minX(), param0.minY(), param0.minZ(), param0.maxX() + 1, param0.maxY() + 1, param0.maxZ() + 1);
   }

   public static AABB unitCubeFromLowerCorner(Vec3 param0) {
      return new AABB(param0.x, param0.y, param0.z, param0.x + 1.0, param0.y + 1.0, param0.z + 1.0);
   }

   public AABB setMinX(double param0) {
      return new AABB(param0, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinY(double param0) {
      return new AABB(this.minX, param0, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinZ(double param0) {
      return new AABB(this.minX, this.minY, param0, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMaxX(double param0) {
      return new AABB(this.minX, this.minY, this.minZ, param0, this.maxY, this.maxZ);
   }

   public AABB setMaxY(double param0) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, param0, this.maxZ);
   }

   public AABB setMaxZ(double param0) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, param0);
   }

   public double min(Direction.Axis param0) {
      return param0.choose(this.minX, this.minY, this.minZ);
   }

   public double max(Direction.Axis param0) {
      return param0.choose(this.maxX, this.maxY, this.maxZ);
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (!(param0 instanceof AABB var2)) {
         return false;
      } else if (Double.compare(var2.minX, this.minX) != 0) {
         return false;
      } else if (Double.compare(var2.minY, this.minY) != 0) {
         return false;
      } else if (Double.compare(var2.minZ, this.minZ) != 0) {
         return false;
      } else if (Double.compare(var2.maxX, this.maxX) != 0) {
         return false;
      } else {
         return Double.compare(var2.maxY, this.maxY) != 0 ? false : Double.compare(var2.maxZ, this.maxZ) == 0;
      }
   }

   @Override
   public int hashCode() {
      long var1 = Double.doubleToLongBits(this.minX);
      int var3 = (int)(var1 ^ var1 >>> 32);
      var1 = Double.doubleToLongBits(this.minY);
      var3 = 31 * var3 + (int)(var1 ^ var1 >>> 32);
      var1 = Double.doubleToLongBits(this.minZ);
      var3 = 31 * var3 + (int)(var1 ^ var1 >>> 32);
      var1 = Double.doubleToLongBits(this.maxX);
      var3 = 31 * var3 + (int)(var1 ^ var1 >>> 32);
      var1 = Double.doubleToLongBits(this.maxY);
      var3 = 31 * var3 + (int)(var1 ^ var1 >>> 32);
      var1 = Double.doubleToLongBits(this.maxZ);
      return 31 * var3 + (int)(var1 ^ var1 >>> 32);
   }

   public AABB contract(double param0, double param2, double param4) {
      double var7 = this.minX;
      double var9 = this.minY;
      double var11 = this.minZ;
      double var13 = this.maxX;
      double var15 = this.maxY;
      double var17 = this.maxZ;
      if (param0 < 0.0) {
         var7 -= param0;
      } else if (param0 > 0.0) {
         var13 -= param0;
      }

      if (param2 < 0.0) {
         var9 -= param2;
      } else if (param2 > 0.0) {
         var15 -= param2;
      }

      if (param4 < 0.0) {
         var11 -= param4;
      } else if (param4 > 0.0) {
         var17 -= param4;
      }

      return new AABB(var7, var9, var11, var13, var15, var17);
   }

   public AABB expandTowards(Vec3 param0) {
      return this.expandTowards(param0.x, param0.y, param0.z);
   }

   public AABB expandTowards(double param0, double param2, double param4) {
      double var7 = this.minX;
      double var9 = this.minY;
      double var11 = this.minZ;
      double var13 = this.maxX;
      double var15 = this.maxY;
      double var17 = this.maxZ;
      if (param0 < 0.0) {
         var7 += param0;
      } else if (param0 > 0.0) {
         var13 += param0;
      }

      if (param2 < 0.0) {
         var9 += param2;
      } else if (param2 > 0.0) {
         var15 += param2;
      }

      if (param4 < 0.0) {
         var11 += param4;
      } else if (param4 > 0.0) {
         var17 += param4;
      }

      return new AABB(var7, var9, var11, var13, var15, var17);
   }

   public AABB inflate(double param0, double param2, double param4) {
      double var7 = this.minX - param0;
      double var9 = this.minY - param2;
      double var11 = this.minZ - param4;
      double var13 = this.maxX + param0;
      double var15 = this.maxY + param2;
      double var17 = this.maxZ + param4;
      return new AABB(var7, var9, var11, var13, var15, var17);
   }

   public AABB inflate(double param0) {
      return this.inflate(param0, param0, param0);
   }

   public AABB intersect(AABB param0) {
      double var2 = Math.max(this.minX, param0.minX);
      double var4 = Math.max(this.minY, param0.minY);
      double var6 = Math.max(this.minZ, param0.minZ);
      double var8 = Math.min(this.maxX, param0.maxX);
      double var10 = Math.min(this.maxY, param0.maxY);
      double var12 = Math.min(this.maxZ, param0.maxZ);
      return new AABB(var2, var4, var6, var8, var10, var12);
   }

   public AABB minmax(AABB param0) {
      double var2 = Math.min(this.minX, param0.minX);
      double var4 = Math.min(this.minY, param0.minY);
      double var6 = Math.min(this.minZ, param0.minZ);
      double var8 = Math.max(this.maxX, param0.maxX);
      double var10 = Math.max(this.maxY, param0.maxY);
      double var12 = Math.max(this.maxZ, param0.maxZ);
      return new AABB(var2, var4, var6, var8, var10, var12);
   }

   public AABB move(double param0, double param2, double param4) {
      return new AABB(this.minX + param0, this.minY + param2, this.minZ + param4, this.maxX + param0, this.maxY + param2, this.maxZ + param4);
   }

   public AABB move(BlockPos param0) {
      return new AABB(
         this.minX + param0.getX(),
         this.minY + param0.getY(),
         this.minZ + param0.getZ(),
         this.maxX + param0.getX(),
         this.maxY + param0.getY(),
         this.maxZ + param0.getZ()
      );
   }

   public AABB move(Vec3 param0) {
      return this.move(param0.x, param0.y, param0.z);
   }

   public boolean intersects(AABB param0) {
      return this.intersects(param0.minX, param0.minY, param0.minZ, param0.maxX, param0.maxY, param0.maxZ);
   }

   public boolean intersects(double param0, double param2, double param4, double param6, double param8, double param10) {
      return this.minX < param6 && this.maxX > param0 && this.minY < param8 && this.maxY > param2 && this.minZ < param10 && this.maxZ > param4;
   }

   public boolean intersects(Vec3 param0, Vec3 param1) {
      return this.intersects(
         Math.min(param0.x, param1.x),
         Math.min(param0.y, param1.y),
         Math.min(param0.z, param1.z),
         Math.max(param0.x, param1.x),
         Math.max(param0.y, param1.y),
         Math.max(param0.z, param1.z)
      );
   }

   public boolean contains(Vec3 param0) {
      return this.contains(param0.x, param0.y, param0.z);
   }

   public boolean contains(double param0, double param2, double param4) {
      return param0 >= this.minX && param0 < this.maxX && param2 >= this.minY && param2 < this.maxY && param4 >= this.minZ && param4 < this.maxZ;
   }

   public double getSize() {
      double var1 = this.getXsize();
      double var3 = this.getYsize();
      double var5 = this.getZsize();
      return (var1 + var3 + var5) / 3.0;
   }

   public double getXsize() {
      return this.maxX - this.minX;
   }

   public double getYsize() {
      return this.maxY - this.minY;
   }

   public double getZsize() {
      return this.maxZ - this.minZ;
   }

   public AABB deflate(double param0, double param2, double param4) {
      return this.inflate(-param0, -param2, -param4);
   }

   public AABB deflate(double param0) {
      return this.inflate(-param0);
   }

   public Optional<Vec3> clip(Vec3 param0, Vec3 param1) {
      double[] var3 = new double[]{1.0};
      double var4 = param1.x - param0.x;
      double var6 = param1.y - param0.y;
      double var8 = param1.z - param0.z;
      Direction var10 = getDirection(this, param0, var3, null, var4, var6, var8);
      if (var10 == null) {
         return Optional.empty();
      }

      double var11 = var3[0];
      return Optional.of(param0.add(var11 * var4, var11 * var6, var11 * var8));
   }

   @Nullable
   public static BlockHitResult clip(Iterable<AABB> param0, Vec3 param1, Vec3 param2, BlockPos param3) {
      double[] var4 = new double[]{1.0};
      Direction var5 = null;
      double var6 = param2.x - param1.x;
      double var8 = param2.y - param1.y;
      double var10 = param2.z - param1.z;

      for (AABB var13 : param0) {
         var5 = getDirection(var13.move(param3), param1, var4, var5, var6, var8, var10);
      }

      if (var5 == null) {
         return null;
      }

      double var12 = var4[0];
      return new BlockHitResult(param1.add(var12 * var6, var12 * var8, var12 * var10), var5, param3, false);
   }

   @Nullable
   private static Direction getDirection(AABB param0, Vec3 param1, double[] param2, @Nullable Direction param3, double param4, double param6, double param8) {
      if (param4 > 1.0E-7) {
         param3 = clipPoint(
            param2,
            param3,
            param4,
            param6,
            param8,
            param0.minX,
            param0.minY,
            param0.maxY,
            param0.minZ,
            param0.maxZ,
            Direction.WEST,
            param1.x,
            param1.y,
            param1.z
         );
      } else if (param4 < -1.0E-7) {
         param3 = clipPoint(
            param2,
            param3,
            param4,
            param6,
            param8,
            param0.maxX,
            param0.minY,
            param0.maxY,
            param0.minZ,
            param0.maxZ,
            Direction.EAST,
            param1.x,
            param1.y,
            param1.z
         );
      }

      if (param6 > 1.0E-7) {
         param3 = clipPoint(
            param2,
            param3,
            param6,
            param8,
            param4,
            param0.minY,
            param0.minZ,
            param0.maxZ,
            param0.minX,
            param0.maxX,
            Direction.DOWN,
            param1.y,
            param1.z,
            param1.x
         );
      } else if (param6 < -1.0E-7) {
         param3 = clipPoint(
            param2, param3, param6, param8, param4, param0.maxY, param0.minZ, param0.maxZ, param0.minX, param0.maxX, Direction.UP, param1.y, param1.z, param1.x
         );
      }

      if (param8 > 1.0E-7) {
         param3 = clipPoint(
            param2,
            param3,
            param8,
            param4,
            param6,
            param0.minZ,
            param0.minX,
            param0.maxX,
            param0.minY,
            param0.maxY,
            Direction.NORTH,
            param1.z,
            param1.x,
            param1.y
         );
      } else if (param8 < -1.0E-7) {
         param3 = clipPoint(
            param2,
            param3,
            param8,
            param4,
            param6,
            param0.maxZ,
            param0.minX,
            param0.maxX,
            param0.minY,
            param0.maxY,
            Direction.SOUTH,
            param1.z,
            param1.x,
            param1.y
         );
      }

      return param3;
   }

   @Nullable
   private static Direction clipPoint(
      double[] param0,
      @Nullable Direction param1,
      double param2,
      double param4,
      double param6,
      double param8,
      double param10,
      double param12,
      double param14,
      double param16,
      Direction param18,
      double param19,
      double param21,
      double param23
   ) {
      double var25 = (param8 - param19) / param2;
      double var27 = param21 + var25 * param4;
      double var29 = param23 + var25 * param6;
      if (0.0 < var25 && var25 < param0[0] && param10 - 1.0E-7 < var27 && var27 < param12 + 1.0E-7 && param14 - 1.0E-7 < var29 && var29 < param16 + 1.0E-7) {
         param0[0] = var25;
         return param18;
      } else {
         return param1;
      }
   }

   public double distanceToSqr(Vec3 param0) {
      double var2 = Math.max(Math.max(this.minX - param0.x, param0.x - this.maxX), 0.0);
      double var4 = Math.max(Math.max(this.minY - param0.y, param0.y - this.maxY), 0.0);
      double var6 = Math.max(Math.max(this.minZ - param0.z, param0.z - this.maxZ), 0.0);
      return Mth.lengthSquared(var2, var4, var6);
   }

   @Override
   public String toString() {
      return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
   }

   public boolean hasNaN() {
      return Double.isNaN(this.minX)
         || Double.isNaN(this.minY)
         || Double.isNaN(this.minZ)
         || Double.isNaN(this.maxX)
         || Double.isNaN(this.maxY)
         || Double.isNaN(this.maxZ);
   }

   public Vec3 getCenter() {
      return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), Mth.lerp(0.5, this.minY, this.maxY), Mth.lerp(0.5, this.minZ, this.maxZ));
   }

   public static AABB ofSize(Vec3 param0, double param1, double param3, double param5) {
      return new AABB(
         param0.x - param1 / 2.0, param0.y - param3 / 2.0, param0.z - param5 / 2.0, param0.x + param1 / 2.0, param0.y + param3 / 2.0, param0.z + param5 / 2.0
      );
   }
}
