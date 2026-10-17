package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class ImprovedNoise {
   private static final float SHIFT_UP_EPSILON = 1.0E-7F;
   private final byte[] p;
   public final double xo;
   public final double yo;
   public final double zo;

   public ImprovedNoise(RandomSource param0) {
      this.xo = param0.nextDouble() * 256.0;
      this.yo = param0.nextDouble() * 256.0;
      this.zo = param0.nextDouble() * 256.0;
      this.p = new byte[256];

      for (int var2 = 0; var2 < 256; var2++) {
         this.p[var2] = (byte)var2;
      }

      for (int var2 = 0; var2 < 256; var2++) {
         int var3 = param0.nextInt(256 - var2);
         byte var4 = this.p[var2];
         this.p[var2] = this.p[var2 + var3];
         this.p[var2 + var3] = var4;
      }
   }

   public double noise(double param0, double param2, double param4) {
      return this.noise(param0, param2, param4, 0.0, 0.0);
   }

   @Deprecated
   public double noise(double param0, double param2, double param4, double param6, double param8) {
      double var11 = param0 + this.xo;
      double var13 = param2 + this.yo;
      double var15 = param4 + this.zo;
      int var17 = Mth.floor(var11);
      int var18 = Mth.floor(var13);
      int var19 = Mth.floor(var15);
      double var20 = var11 - var17;
      double var22 = var13 - var18;
      double var24 = var15 - var19;
      double var26;
      if (param6 != 0.0) {
         double var28;
         if (param8 >= 0.0 && param8 < var22) {
            var28 = param8;
         } else {
            var28 = var22;
         }

         var26 = Mth.floor(var28 / param6 + 1.0E-7F) * param6;
      } else {
         var26 = 0.0;
      }

      return this.sampleAndLerp(var17, var18, var19, var20, var22 - var26, var24, var22);
   }

   public double noiseWithDerivative(double param0, double param2, double param4, double[] param6) {
      double var8 = param0 + this.xo;
      double var10 = param2 + this.yo;
      double var12 = param4 + this.zo;
      int var14 = Mth.floor(var8);
      int var15 = Mth.floor(var10);
      int var16 = Mth.floor(var12);
      double var17 = var8 - var14;
      double var19 = var10 - var15;
      double var21 = var12 - var16;
      return this.sampleWithDerivative(var14, var15, var16, var17, var19, var21, param6);
   }

   private static double gradDot(int param0, double param1, double param3, double param5) {
      return SimplexNoise.dot(SimplexNoise.GRADIENT[param0 & 15], param1, param3, param5);
   }

   private int p(int param0) {
      return this.p[param0 & 0xFF] & 0xFF;
   }

   private double sampleAndLerp(int param0, int param1, int param2, double param3, double param5, double param7, double param9) {
      int var12 = this.p(param0);
      int var13 = this.p(param0 + 1);
      int var14 = this.p(var12 + param1);
      int var15 = this.p(var12 + param1 + 1);
      int var16 = this.p(var13 + param1);
      int var17 = this.p(var13 + param1 + 1);
      double var18 = gradDot(this.p(var14 + param2), param3, param5, param7);
      double var20 = gradDot(this.p(var16 + param2), param3 - 1.0, param5, param7);
      double var22 = gradDot(this.p(var15 + param2), param3, param5 - 1.0, param7);
      double var24 = gradDot(this.p(var17 + param2), param3 - 1.0, param5 - 1.0, param7);
      double var26 = gradDot(this.p(var14 + param2 + 1), param3, param5, param7 - 1.0);
      double var28 = gradDot(this.p(var16 + param2 + 1), param3 - 1.0, param5, param7 - 1.0);
      double var30 = gradDot(this.p(var15 + param2 + 1), param3, param5 - 1.0, param7 - 1.0);
      double var32 = gradDot(this.p(var17 + param2 + 1), param3 - 1.0, param5 - 1.0, param7 - 1.0);
      double var34 = Mth.smoothstep(param3);
      double var36 = Mth.smoothstep(param9);
      double var38 = Mth.smoothstep(param7);
      return Mth.lerp3(var34, var36, var38, var18, var20, var22, var24, var26, var28, var30, var32);
   }

   private double sampleWithDerivative(int param0, int param1, int param2, double param3, double param5, double param7, double[] param9) {
      int var11 = this.p(param0);
      int var12 = this.p(param0 + 1);
      int var13 = this.p(var11 + param1);
      int var14 = this.p(var11 + param1 + 1);
      int var15 = this.p(var12 + param1);
      int var16 = this.p(var12 + param1 + 1);
      int var17 = this.p(var13 + param2);
      int var18 = this.p(var15 + param2);
      int var19 = this.p(var14 + param2);
      int var20 = this.p(var16 + param2);
      int var21 = this.p(var13 + param2 + 1);
      int var22 = this.p(var15 + param2 + 1);
      int var23 = this.p(var14 + param2 + 1);
      int var24 = this.p(var16 + param2 + 1);
      int[] var25 = SimplexNoise.GRADIENT[var17 & 15];
      int[] var26 = SimplexNoise.GRADIENT[var18 & 15];
      int[] var27 = SimplexNoise.GRADIENT[var19 & 15];
      int[] var28 = SimplexNoise.GRADIENT[var20 & 15];
      int[] var29 = SimplexNoise.GRADIENT[var21 & 15];
      int[] var30 = SimplexNoise.GRADIENT[var22 & 15];
      int[] var31 = SimplexNoise.GRADIENT[var23 & 15];
      int[] var32 = SimplexNoise.GRADIENT[var24 & 15];
      double var33 = SimplexNoise.dot(var25, param3, param5, param7);
      double var35 = SimplexNoise.dot(var26, param3 - 1.0, param5, param7);
      double var37 = SimplexNoise.dot(var27, param3, param5 - 1.0, param7);
      double var39 = SimplexNoise.dot(var28, param3 - 1.0, param5 - 1.0, param7);
      double var41 = SimplexNoise.dot(var29, param3, param5, param7 - 1.0);
      double var43 = SimplexNoise.dot(var30, param3 - 1.0, param5, param7 - 1.0);
      double var45 = SimplexNoise.dot(var31, param3, param5 - 1.0, param7 - 1.0);
      double var47 = SimplexNoise.dot(var32, param3 - 1.0, param5 - 1.0, param7 - 1.0);
      double var49 = Mth.smoothstep(param3);
      double var51 = Mth.smoothstep(param5);
      double var53 = Mth.smoothstep(param7);
      double var55 = Mth.lerp3(var49, var51, var53, var25[0], var26[0], var27[0], var28[0], var29[0], var30[0], var31[0], var32[0]);
      double var57 = Mth.lerp3(var49, var51, var53, var25[1], var26[1], var27[1], var28[1], var29[1], var30[1], var31[1], var32[1]);
      double var59 = Mth.lerp3(var49, var51, var53, var25[2], var26[2], var27[2], var28[2], var29[2], var30[2], var31[2], var32[2]);
      double var61 = Mth.lerp2(var51, var53, var35 - var33, var39 - var37, var43 - var41, var47 - var45);
      double var63 = Mth.lerp2(var53, var49, var37 - var33, var45 - var41, var39 - var35, var47 - var43);
      double var65 = Mth.lerp2(var49, var51, var41 - var33, var43 - var35, var45 - var37, var47 - var39);
      double var67 = Mth.smoothstepDerivative(param3);
      double var69 = Mth.smoothstepDerivative(param5);
      double var71 = Mth.smoothstepDerivative(param7);
      double var73 = var55 + var67 * var61;
      double var75 = var57 + var69 * var63;
      double var77 = var59 + var71 * var65;
      param9[0] += var73;
      param9[1] += var75;
      param9[2] += var77;
      return Mth.lerp3(var49, var51, var53, var33, var35, var37, var39, var41, var43, var45, var47);
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder param0) {
      NoiseUtils.parityNoiseOctaveConfigString(param0, this.xo, this.yo, this.zo, this.p);
   }
}
