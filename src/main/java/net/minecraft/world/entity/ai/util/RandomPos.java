package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RandomPos {
   private static final int RANDOM_POS_ATTEMPTS = 10;

   public static BlockPos generateRandomDirection(RandomSource param0, int param1, int param2) {
      int var3 = param0.nextInt(2 * param1 + 1) - param1;
      int var4 = param0.nextInt(2 * param2 + 1) - param2;
      int var5 = param0.nextInt(2 * param1 + 1) - param1;
      return new BlockPos(var3, var4, var5);
   }

   @Nullable
   public static BlockPos generateRandomDirectionWithinRadians(
      RandomSource param0, int param1, int param2, int param3, double param4, double param6, double param8
   ) {
      double var10 = Mth.atan2(param6, param4) - (float) (Math.PI / 2);
      double var12 = var10 + (2.0F * param0.nextFloat() - 1.0F) * param8;
      double var14 = Math.sqrt(param0.nextDouble()) * Mth.SQRT_OF_TWO * param1;
      double var16 = -var14 * Math.sin(var12);
      double var18 = var14 * Math.cos(var12);
      if (!(Math.abs(var16) > param1) && !(Math.abs(var18) > param1)) {
         int var20 = param0.nextInt(2 * param2 + 1) - param2 + param3;
         return BlockPos.containing(var16, var20, var18);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpOutOfSolid(BlockPos param0, int param1, Predicate<BlockPos> param2) {
      if (!param2.test(param0)) {
         return param0;
      }

      BlockPos var3 = param0.above();

      while (var3.getY() < param1 && param2.test(var3)) {
         var3 = var3.above();
      }

      return var3;
   }

   @VisibleForTesting
   public static BlockPos moveUpToAboveSolid(BlockPos param0, int param1, int param2, Predicate<BlockPos> param3) {
      if (param1 < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + param1 + ", expected >= 0");
      }

      if (!param3.test(param0)) {
         return param0;
      }

      BlockPos var4 = param0.above();

      while (var4.getY() < param2 && param3.test(var4)) {
         var4 = var4.above();
      }

      BlockPos var5 = var4;

      while (var5.getY() < param2 && var5.getY() - var4.getY() < param1) {
         BlockPos var6 = var5.above();
         if (param3.test(var6)) {
            break;
         }

         var5 = var6;
      }

      return var5;
   }

   @Nullable
   public static Vec3 generateRandomPos(PathfinderMob param0, Supplier<BlockPos> param1) {
      return generateRandomPos(param1, param0::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 generateRandomPos(Supplier<BlockPos> param0, ToDoubleFunction<BlockPos> param1) {
      double var2 = Double.NEGATIVE_INFINITY;
      BlockPos var4 = null;

      for (int var5 = 0; var5 < 10; var5++) {
         BlockPos var6 = param0.get();
         if (var6 != null) {
            double var7 = param1.applyAsDouble(var6);
            if (var7 > var2) {
               var2 = var7;
               var4 = var6;
            }
         }
      }

      return var4 != null ? Vec3.atBottomCenterOf(var4) : null;
   }

   public static BlockPos generateRandomPosTowardDirection(PathfinderMob param0, int param1, RandomSource param2, BlockPos param3) {
      int var4 = param3.getX();
      int var5 = param3.getZ();
      if (param0.hasRestriction() && param1 > 1) {
         BlockPos var6 = param0.getRestrictCenter();
         if (param0.getX() > var6.getX()) {
            var4 -= param2.nextInt(param1 / 2);
         } else {
            var4 += param2.nextInt(param1 / 2);
         }

         if (param0.getZ() > var6.getZ()) {
            var5 -= param2.nextInt(param1 / 2);
         } else {
            var5 += param2.nextInt(param1 / 2);
         }
      }

      return BlockPos.containing(var4 + param0.getX(), param3.getY() + param0.getY(), var5 + param0.getZ());
   }
}
