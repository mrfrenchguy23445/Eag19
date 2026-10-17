package net.minecraft.world.level.levelgen.synth;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SimplexNoise {
   protected static final int[][] GRADIENT = new int[][]{
      {1, 1, 0},
      {-1, 1, 0},
      {1, -1, 0},
      {-1, -1, 0},
      {1, 0, 1},
      {-1, 0, 1},
      {1, 0, -1},
      {-1, 0, -1},
      {0, 1, 1},
      {0, -1, 1},
      {0, 1, -1},
      {0, -1, -1},
      {1, 1, 0},
      {0, -1, 1},
      {-1, 1, 0},
      {0, -1, -1}
   };
   private static final double SQRT_3 = Math.sqrt(3.0);
   private static final double F2 = 0.5 * (SQRT_3 - 1.0);
   private static final double G2 = (3.0 - SQRT_3) / 6.0;
   private final int[] p = new int[512];
   public final double xo;
   public final double yo;
   public final double zo;

   public SimplexNoise(RandomSource param0) {
      this.xo = param0.nextDouble() * 256.0;
      this.yo = param0.nextDouble() * 256.0;
      this.zo = param0.nextDouble() * 256.0;
      int var2 = 0;

      while (var2 < 256) {
         this.p[var2] = var2++;
      }

      for (int var2x = 0; var2x < 256; var2x++) {
         int var3 = param0.nextInt(256 - var2x);
         int var4 = this.p[var2x];
         this.p[var2x] = this.p[var3 + var2x];
         this.p[var3 + var2x] = var4;
      }
   }

   private int p(int param0) {
      return this.p[param0 & 0xFF];
   }

   protected static double dot(int[] param0, double param1, double param3, double param5) {
      return param0[0] * param1 + param0[1] * param3 + param0[2] * param5;
   }

   private double getCornerNoise3D(int param0, double param1, double param3, double param5, double param7) {
      double var12 = param7 - param1 * param1 - param3 * param3 - param5 * param5;
      double var10;
      if (var12 < 0.0) {
         var10 = 0.0;
      } else {
         var12 *= var12;
         var10 = var12 * var12 * dot(GRADIENT[param0], param1, param3, param5);
      }

      return var10;
   }

   public double getValue(double param0, double param2) {
      double var5 = (param0 + param2) * F2;
      int var7 = Mth.floor(param0 + var5);
      int var8 = Mth.floor(param2 + var5);
      double var9 = (var7 + var8) * G2;
      double var11 = var7 - var9;
      double var13 = var8 - var9;
      double var15 = param0 - var11;
      double var17 = param2 - var13;
      int var19;
      int var20;
      if (var15 > var17) {
         var19 = 1;
         var20 = 0;
      } else {
         var19 = 0;
         var20 = 1;
      }

      double var21 = var15 - var19 + G2;
      double var23 = var17 - var20 + G2;
      double var25 = var15 - 1.0 + 2.0 * G2;
      double var27 = var17 - 1.0 + 2.0 * G2;
      int var29 = var7 & 0xFF;
      int var30 = var8 & 0xFF;
      int var31 = this.p(var29 + this.p(var30)) % 12;
      int var32 = this.p(var29 + var19 + this.p(var30 + var20)) % 12;
      int var33 = this.p(var29 + 1 + this.p(var30 + 1)) % 12;
      double var34 = this.getCornerNoise3D(var31, var15, var17, 0.0, 0.5);
      double var36 = this.getCornerNoise3D(var32, var21, var23, 0.0, 0.5);
      double var38 = this.getCornerNoise3D(var33, var25, var27, 0.0, 0.5);
      return 70.0 * (var34 + var36 + var38);
   }

   public double getValue(double param0, double param2, double param4) {
      double var7 = 0.3333333333333333;
      double var9 = (param0 + param2 + param4) * 0.3333333333333333;
      int var11 = Mth.floor(param0 + var9);
      int var12 = Mth.floor(param2 + var9);
      int var13 = Mth.floor(param4 + var9);
      double var14 = 0.16666666666666666;
      double var16 = (var11 + var12 + var13) * 0.16666666666666666;
      double var18 = var11 - var16;
      double var20 = var12 - var16;
      double var22 = var13 - var16;
      double var24 = param0 - var18;
      double var26 = param2 - var20;
      double var28 = param4 - var22;
      int var30;
      int var31;
      int var32;
      int var33;
      int var34;
      int var35;
      if (var24 >= var26) {
         if (var26 >= var28) {
            var30 = 1;
            var31 = 0;
            var32 = 0;
            var33 = 1;
            var34 = 1;
            var35 = 0;
         } else if (var24 >= var28) {
            var30 = 1;
            var31 = 0;
            var32 = 0;
            var33 = 1;
            var34 = 0;
            var35 = 1;
         } else {
            var30 = 0;
            var31 = 0;
            var32 = 1;
            var33 = 1;
            var34 = 0;
            var35 = 1;
         }
      } else if (var26 < var28) {
         var30 = 0;
         var31 = 0;
         var32 = 1;
         var33 = 0;
         var34 = 1;
         var35 = 1;
      } else if (var24 < var28) {
         var30 = 0;
         var31 = 1;
         var32 = 0;
         var33 = 0;
         var34 = 1;
         var35 = 1;
      } else {
         var30 = 0;
         var31 = 1;
         var32 = 0;
         var33 = 1;
         var34 = 1;
         var35 = 0;
      }

      double var36 = var24 - var30 + 0.16666666666666666;
      double var38 = var26 - var31 + 0.16666666666666666;
      double var40 = var28 - var32 + 0.16666666666666666;
      double var42 = var24 - var33 + 0.3333333333333333;
      double var44 = var26 - var34 + 0.3333333333333333;
      double var46 = var28 - var35 + 0.3333333333333333;
      double var48 = var24 - 1.0 + 0.5;
      double var50 = var26 - 1.0 + 0.5;
      double var52 = var28 - 1.0 + 0.5;
      int var54 = var11 & 0xFF;
      int var55 = var12 & 0xFF;
      int var56 = var13 & 0xFF;
      int var57 = this.p(var54 + this.p(var55 + this.p(var56))) % 12;
      int var58 = this.p(var54 + var30 + this.p(var55 + var31 + this.p(var56 + var32))) % 12;
      int var59 = this.p(var54 + var33 + this.p(var55 + var34 + this.p(var56 + var35))) % 12;
      int var60 = this.p(var54 + 1 + this.p(var55 + 1 + this.p(var56 + 1))) % 12;
      double var61 = this.getCornerNoise3D(var57, var24, var26, var28, 0.6);
      double var63 = this.getCornerNoise3D(var58, var36, var38, var40, 0.6);
      double var65 = this.getCornerNoise3D(var59, var42, var44, var46, 0.6);
      double var67 = this.getCornerNoise3D(var60, var48, var50, var52, 0.6);
      return 32.0 * (var61 + var63 + var65 + var67);
   }
}
