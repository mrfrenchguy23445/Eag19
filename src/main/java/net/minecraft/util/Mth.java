package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.NumberUtils;

public class Mth {
   private static final long UUID_VERSION = 61440L;
   private static final long UUID_VERSION_TYPE_4 = 16384L;
   private static final long UUID_VARIANT = -4611686018427387904L;
   private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
   public static final float PI = (float) Math.PI;
   public static final float HALF_PI = (float) (Math.PI / 2);
   public static final float TWO_PI = (float) (Math.PI * 2);
   public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
   public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
   public static final float EPSILON = 1.0E-5F;
   public static final float SQRT_OF_TWO = sqrt(2.0F);
   private static final float SIN_SCALE = 10430.378F;
   private static final float[] SIN = Util.make(new float[65536], param0 -> {
      for (int var1x = 0; var1x < param0.length; var1x++) {
         param0[var1x] = (float)Math.sin(var1x * Math.PI * 2.0 / 65536.0);
      }
   });
   private static final RandomSource RANDOM = RandomSource.createThreadSafe();
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
   };
   private static final double ONE_SIXTH = 0.16666666666666666;
   private static final int FRAC_EXP = 8;
   private static final int LUT_SIZE = 257;
   private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
   private static final double[] ASIN_TAB = new double[257];
   private static final double[] COS_TAB = new double[257];

   public static float sin(float param0) {
      return SIN[(int)(param0 * 10430.378F) & 65535];
   }

   public static float cos(float param0) {
      return SIN[(int)(param0 * 10430.378F + 16384.0F) & 65535];
   }

   public static float sqrt(float param0) {
      return (float)Math.sqrt(param0);
   }

   public static int floor(float param0) {
      int var1 = (int)param0;
      return param0 < var1 ? var1 - 1 : var1;
   }

   public static int floor(double param0) {
      int var2 = (int)param0;
      return param0 < var2 ? var2 - 1 : var2;
   }

   public static long lfloor(double param0) {
      long var2 = (long)param0;
      return param0 < var2 ? var2 - 1L : var2;
   }

   public static float abs(float param0) {
      return Math.abs(param0);
   }

   public static int abs(int param0) {
      return Math.abs(param0);
   }

   public static int ceil(float param0) {
      int var1 = (int)param0;
      return param0 > var1 ? var1 + 1 : var1;
   }

   public static int ceil(double param0) {
      int var2 = (int)param0;
      return param0 > var2 ? var2 + 1 : var2;
   }

   public static int clamp(int param0, int param1, int param2) {
      return Math.min(Math.max(param0, param1), param2);
   }

   public static float clamp(float param0, float param1, float param2) {
      return param0 < param1 ? param1 : Math.min(param0, param2);
   }

   public static double clamp(double param0, double param2, double param4) {
      return param0 < param2 ? param2 : Math.min(param0, param4);
   }

   public static double clampedLerp(double param0, double param2, double param4) {
      if (param4 < 0.0) {
         return param0;
      } else {
         return param4 > 1.0 ? param2 : lerp(param4, param0, param2);
      }
   }

   public static float clampedLerp(float param0, float param1, float param2) {
      if (param2 < 0.0F) {
         return param0;
      } else {
         return param2 > 1.0F ? param1 : lerp(param2, param0, param1);
      }
   }

   public static double absMax(double param0, double param2) {
      if (param0 < 0.0) {
         param0 = -param0;
      }

      if (param2 < 0.0) {
         param2 = -param2;
      }

      return Math.max(param0, param2);
   }

   public static int floorDiv(int param0, int param1) {
      return Math.floorDiv(param0, param1);
   }

   public static int nextInt(RandomSource param0, int param1, int param2) {
      return param1 >= param2 ? param1 : param0.nextInt(param2 - param1 + 1) + param1;
   }

   public static float nextFloat(RandomSource param0, float param1, float param2) {
      return param1 >= param2 ? param1 : param0.nextFloat() * (param2 - param1) + param1;
   }

   public static double nextDouble(RandomSource param0, double param1, double param3) {
      return param1 >= param3 ? param1 : param0.nextDouble() * (param3 - param1) + param1;
   }

   public static boolean equal(float param0, float param1) {
      return Math.abs(param1 - param0) < 1.0E-5F;
   }

   public static boolean equal(double param0, double param2) {
      return Math.abs(param2 - param0) < 1.0E-5F;
   }

   public static int positiveModulo(int param0, int param1) {
      return Math.floorMod(param0, param1);
   }

   public static float positiveModulo(float param0, float param1) {
      return (param0 % param1 + param1) % param1;
   }

   public static double positiveModulo(double param0, double param2) {
      return (param0 % param2 + param2) % param2;
   }

   public static boolean isMultipleOf(int param0, int param1) {
      return param0 % param1 == 0;
   }

   public static int wrapDegrees(int param0) {
      int var1 = param0 % 360;
      if (var1 >= 180) {
         var1 -= 360;
      }

      if (var1 < -180) {
         var1 += 360;
      }

      return var1;
   }

   public static float wrapDegrees(float param0) {
      float var1 = param0 % 360.0F;
      if (var1 >= 180.0F) {
         var1 -= 360.0F;
      }

      if (var1 < -180.0F) {
         var1 += 360.0F;
      }

      return var1;
   }

   public static double wrapDegrees(double param0) {
      double var2 = param0 % 360.0;
      if (var2 >= 180.0) {
         var2 -= 360.0;
      }

      if (var2 < -180.0) {
         var2 += 360.0;
      }

      return var2;
   }

   public static float degreesDifference(float param0, float param1) {
      return wrapDegrees(param1 - param0);
   }

   public static float degreesDifferenceAbs(float param0, float param1) {
      return abs(degreesDifference(param0, param1));
   }

   public static float rotateIfNecessary(float param0, float param1, float param2) {
      float var3 = degreesDifference(param0, param1);
      float var4 = clamp(var3, -param2, param2);
      return param1 - var4;
   }

   public static float approach(float param0, float param1, float param2) {
      param2 = abs(param2);
      return param0 < param1 ? clamp(param0 + param2, param0, param1) : clamp(param0 - param2, param1, param0);
   }

   public static float approachDegrees(float param0, float param1, float param2) {
      float var3 = degreesDifference(param0, param1);
      return approach(param0, param0 + var3, param2);
   }

   public static int getInt(String param0, int param1) {
      return NumberUtils.toInt(param0, param1);
   }

   public static int smallestEncompassingPowerOfTwo(int param0) {
      int var1 = param0 - 1;
      var1 |= var1 >> 1;
      var1 |= var1 >> 2;
      var1 |= var1 >> 4;
      var1 |= var1 >> 8;
      var1 |= var1 >> 16;
      return var1 + 1;
   }

   public static boolean isPowerOfTwo(int param0) {
      return param0 != 0 && (param0 & param0 - 1) == 0;
   }

   public static int ceillog2(int param0) {
      param0 = isPowerOfTwo(param0) ? param0 : smallestEncompassingPowerOfTwo(param0);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(param0 * 125613361L >> 27) & 31];
   }

   public static int log2(int param0) {
      return ceillog2(param0) - (isPowerOfTwo(param0) ? 0 : 1);
   }

   public static int color(float param0, float param1, float param2) {
      return FastColor.ARGB32.color(0, floor(param0 * 255.0F), floor(param1 * 255.0F), floor(param2 * 255.0F));
   }

   public static float frac(float param0) {
      return param0 - floor(param0);
   }

   public static double frac(double param0) {
      return param0 - lfloor(param0);
   }

   @Deprecated
   public static long getSeed(Vec3i param0) {
      return getSeed(param0.getX(), param0.getY(), param0.getZ());
   }

   @Deprecated
   public static long getSeed(int param0, int param1, int param2) {
      long var3 = param0 * 3129871 ^ param2 * 116129781L ^ param1;
      var3 = var3 * var3 * 42317861L + var3 * 11L;
      return var3 >> 16;
   }

   public static UUID createInsecureUUID(RandomSource param0) {
      long var1 = param0.nextLong() & -61441L | 16384L;
      long var3 = param0.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
      return new UUID(var1, var3);
   }

   public static UUID createInsecureUUID() {
      return createInsecureUUID(RANDOM);
   }

   public static double inverseLerp(double param0, double param2, double param4) {
      return (param0 - param2) / (param4 - param2);
   }

   public static float inverseLerp(float param0, float param1, float param2) {
      return (param0 - param1) / (param2 - param1);
   }

   public static boolean rayIntersectsAABB(Vec3 param0, Vec3 param1, AABB param2) {
      double var3 = (param2.minX + param2.maxX) * 0.5;
      double var5 = (param2.maxX - param2.minX) * 0.5;
      double var7 = param0.x - var3;
      if (Math.abs(var7) > var5 && var7 * param1.x >= 0.0) {
         return false;
      }

      double var9 = (param2.minY + param2.maxY) * 0.5;
      double var11 = (param2.maxY - param2.minY) * 0.5;
      double var13 = param0.y - var9;
      if (Math.abs(var13) > var11 && var13 * param1.y >= 0.0) {
         return false;
      }

      double var15 = (param2.minZ + param2.maxZ) * 0.5;
      double var17 = (param2.maxZ - param2.minZ) * 0.5;
      double var19 = param0.z - var15;
      if (Math.abs(var19) > var17 && var19 * param1.z >= 0.0) {
         return false;
      }

      double var21 = Math.abs(param1.x);
      double var23 = Math.abs(param1.y);
      double var25 = Math.abs(param1.z);
      double var27 = param1.y * var19 - param1.z * var13;
      if (Math.abs(var27) > var11 * var25 + var17 * var23) {
         return false;
      }

      var27 = param1.z * var7 - param1.x * var19;
      if (Math.abs(var27) > var5 * var25 + var17 * var21) {
         return false;
      }

      var27 = param1.x * var13 - param1.y * var7;
      return Math.abs(var27) < var5 * var23 + var11 * var21;
   }

   public static double atan2(double param0, double param2) {
      double var4 = param2 * param2 + param0 * param0;
      if (Double.isNaN(var4)) {
         return Double.NaN;
      }

      boolean var6 = param0 < 0.0;
      if (var6) {
         param0 = -param0;
      }

      boolean var7 = param2 < 0.0;
      if (var7) {
         param2 = -param2;
      }

      boolean var8 = param0 > param2;
      if (var8) {
         double var9 = param2;
         param2 = param0;
         param0 = var9;
      }

      double var9 = fastInvSqrt(var4);
      param2 *= var9;
      param0 *= var9;
      double var11 = FRAC_BIAS + param0;
      int var13 = (int)Double.doubleToRawLongBits(var11);
      double var14 = ASIN_TAB[var13];
      double var16 = COS_TAB[var13];
      double var18 = var11 - FRAC_BIAS;
      double var20 = param0 * var16 - param2 * var18;
      double var22 = (6.0 + var20 * var20) * var20 * 0.16666666666666666;
      double var24 = var14 + var22;
      if (var8) {
         var24 = (Math.PI / 2) - var24;
      }

      if (var7) {
         var24 = Math.PI - var24;
      }

      if (var6) {
         var24 = -var24;
      }

      return var24;
   }

   public static float invSqrt(float param0) {
      return org.joml.Math.invsqrt(param0);
   }

   public static double invSqrt(double param0) {
      return org.joml.Math.invsqrt(param0);
   }

   @Deprecated
   public static double fastInvSqrt(double param0) {
      double var2 = 0.5 * param0;
      long var4 = Double.doubleToRawLongBits(param0);
      var4 = 6910469410427058090L - (var4 >> 1);
      param0 = Double.longBitsToDouble(var4);
      return param0 * (1.5 - var2 * param0 * param0);
   }

   public static float fastInvCubeRoot(float param0) {
      int var1 = Float.floatToIntBits(param0);
      var1 = 1419967116 - var1 / 3;
      float var2 = Float.intBitsToFloat(var1);
      var2 = 0.6666667F * var2 + 1.0F / (3.0F * var2 * var2 * param0);
      return 0.6666667F * var2 + 1.0F / (3.0F * var2 * var2 * param0);
   }

   public static int hsvToRgb(float param0, float param1, float param2) {
      int var3 = (int)(param0 * 6.0F) % 6;
      float var4 = param0 * 6.0F - var3;
      float var5 = param2 * (1.0F - param1);
      float var6 = param2 * (1.0F - var4 * param1);
      float var7 = param2 * (1.0F - (1.0F - var4) * param1);
      float var8;
      float var9;
      float var10;
      switch (var3) {
         case 0:
            var8 = param2;
            var9 = var7;
            var10 = var5;
            break;
         case 1:
            var8 = var6;
            var9 = param2;
            var10 = var5;
            break;
         case 2:
            var8 = var5;
            var9 = param2;
            var10 = var7;
            break;
         case 3:
            var8 = var5;
            var9 = var6;
            var10 = param2;
            break;
         case 4:
            var8 = var7;
            var9 = var5;
            var10 = param2;
            break;
         case 5:
            var8 = param2;
            var9 = var5;
            var10 = var6;
            break;
         default:
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + param0 + ", " + param1 + ", " + param2);
      }

      return FastColor.ARGB32.color(0, clamp((int)(var8 * 255.0F), 0, 255), clamp((int)(var9 * 255.0F), 0, 255), clamp((int)(var10 * 255.0F), 0, 255));
   }

   public static int murmurHash3Mixer(int param0) {
      param0 ^= param0 >>> 16;
      param0 *= -2048144789;
      param0 ^= param0 >>> 13;
      param0 *= -1028477387;
      return param0 ^ param0 >>> 16;
   }

   public static int binarySearch(int param0, int param1, IntPredicate param2) {
      int var3 = param1 - param0;

      while (var3 > 0) {
         int var4 = var3 / 2;
         int var5 = param0 + var4;
         if (param2.test(var5)) {
            var3 = var4;
         } else {
            param0 = var5 + 1;
            var3 -= var4 + 1;
         }
      }

      return param0;
   }

   public static int lerpInt(float param0, int param1, int param2) {
      return param1 + floor(param0 * (param2 - param1));
   }

   public static float lerp(float param0, float param1, float param2) {
      return param1 + param0 * (param2 - param1);
   }

   public static double lerp(double param0, double param2, double param4) {
      return param2 + param0 * (param4 - param2);
   }

   public static double lerp2(double param0, double param2, double param4, double param6, double param8, double param10) {
      return lerp(param2, lerp(param0, param4, param6), lerp(param0, param8, param10));
   }

   public static double lerp3(
      double param0,
      double param2,
      double param4,
      double param6,
      double param8,
      double param10,
      double param12,
      double param14,
      double param16,
      double param18,
      double param20
   ) {
      return lerp(param4, lerp2(param0, param2, param6, param8, param10, param12), lerp2(param0, param2, param14, param16, param18, param20));
   }

   public static float catmullrom(float param0, float param1, float param2, float param3, float param4) {
      return 0.5F
         * (
            2.0F * param2
               + (param3 - param1) * param0
               + (2.0F * param1 - 5.0F * param2 + 4.0F * param3 - param4) * param0 * param0
               + (3.0F * param2 - param1 - 3.0F * param3 + param4) * param0 * param0 * param0
         );
   }

   public static double smoothstep(double param0) {
      return param0 * param0 * param0 * (param0 * (param0 * 6.0 - 15.0) + 10.0);
   }

   public static double smoothstepDerivative(double param0) {
      return 30.0 * param0 * param0 * (param0 - 1.0) * (param0 - 1.0);
   }

   public static int sign(double param0) {
      if (param0 == 0.0) {
         return 0;
      } else {
         return param0 > 0.0 ? 1 : -1;
      }
   }

   public static float rotLerp(float param0, float param1, float param2) {
      return param1 + param0 * wrapDegrees(param2 - param1);
   }

   public static float triangleWave(float param0, float param1) {
      return (Math.abs(param0 % param1 - param1 * 0.5F) - param1 * 0.25F) / (param1 * 0.25F);
   }

   public static float square(float param0) {
      return param0 * param0;
   }

   public static double square(double param0) {
      return param0 * param0;
   }

   public static int square(int param0) {
      return param0 * param0;
   }

   public static long square(long param0) {
      return param0 * param0;
   }

   public static double clampedMap(double param0, double param2, double param4, double param6, double param8) {
      return clampedLerp(param6, param8, inverseLerp(param0, param2, param4));
   }

   public static float clampedMap(float param0, float param1, float param2, float param3, float param4) {
      return clampedLerp(param3, param4, inverseLerp(param0, param1, param2));
   }

   public static double map(double param0, double param2, double param4, double param6, double param8) {
      return lerp(inverseLerp(param0, param2, param4), param6, param8);
   }

   public static float map(float param0, float param1, float param2, float param3, float param4) {
      return lerp(inverseLerp(param0, param1, param2), param3, param4);
   }

   public static double wobble(double param0) {
      return param0 + (2.0 * RandomSource.create(floor(param0 * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
   }

   public static int roundToward(int param0, int param1) {
      return positiveCeilDiv(param0, param1) * param1;
   }

   public static int positiveCeilDiv(int param0, int param1) {
      return -Math.floorDiv(-param0, param1);
   }

   public static int randomBetweenInclusive(RandomSource param0, int param1, int param2) {
      return param0.nextInt(param2 - param1 + 1) + param1;
   }

   public static float randomBetween(RandomSource param0, float param1, float param2) {
      return param0.nextFloat() * (param2 - param1) + param1;
   }

   public static float normal(RandomSource param0, float param1, float param2) {
      return param1 + (float)param0.nextGaussian() * param2;
   }

   public static double lengthSquared(double param0, double param2) {
      return param0 * param0 + param2 * param2;
   }

   public static double length(double param0, double param2) {
      return Math.sqrt(lengthSquared(param0, param2));
   }

   public static double lengthSquared(double param0, double param2, double param4) {
      return param0 * param0 + param2 * param2 + param4 * param4;
   }

   public static double length(double param0, double param2, double param4) {
      return Math.sqrt(lengthSquared(param0, param2, param4));
   }

   public static int quantize(double param0, int param2) {
      return floor(param0 / param2) * param2;
   }

   public static IntStream outFromOrigin(int param0, int param1, int param2) {
      return outFromOrigin(param0, param1, param2, 1);
   }

   public static IntStream outFromOrigin(int param0, int param1, int param2, int param3) {
      if (param1 > param2) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "upperbound %d expected to be > lowerBound %d", param2, param1));
      } else if (param3 < 1) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "steps expected to be >= 1, was %d", param3));
      } else {
         return param0 >= param1 && param0 <= param2 ? IntStream.iterate(param0, l_param3 -> {
            int l_var4 = Math.abs(param0 - l_param3);
            return param0 - l_var4 >= param1 || param0 + l_var4 <= param2;
         }, l_param4 -> {
            boolean l_var5 = l_param4 <= param0;
            int l_var6 = Math.abs(param0 - l_param4);
            boolean l_var7 = param0 + l_var6 + param3 <= param2;
            if (!l_var5 || !l_var7) {
               int l_var8 = param0 - l_var6 - (l_var5 ? param3 : 0);
               if (l_var8 >= param1) {
                  return l_var8;
               }
            }

            return param0 + l_var6 + param3;
         }) : IntStream.empty();
      }
   }

   static {
      for (int var0 = 0; var0 < 257; var0++) {
         double var1 = var0 / 256.0;
         double var3 = Math.asin(var1);
         COS_TAB[var0] = Math.cos(var3);
         ASIN_TAB[var0] = var3;
      }
   }
}
