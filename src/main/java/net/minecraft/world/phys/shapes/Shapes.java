package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class Shapes {
   public static final double EPSILON = 1.0E-7;
   public static final double BIG_EPSILON = 1.0E-6;
   private static final VoxelShape BLOCK = Util.make(() -> {
      DiscreteVoxelShape var0 = new BitSetDiscreteVoxelShape(1, 1, 1);
      var0.fill(0, 0, 0);
      return new CubeVoxelShape(var0);
   });
   public static final VoxelShape INFINITY = box(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );
   private static final VoxelShape EMPTY = new ArrayVoxelShape(
      new BitSetDiscreteVoxelShape(0, 0, 0),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0})
   );

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double param0, double param2, double param4, double param6, double param8, double param10) {
      if (!(param0 > param6) && !(param2 > param8) && !(param4 > param10)) {
         return create(param0, param2, param4, param6, param8, param10);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double param0, double param2, double param4, double param6, double param8, double param10) {
      if (!(param6 - param0 < 1.0E-7) && !(param8 - param2 < 1.0E-7) && !(param10 - param4 < 1.0E-7)) {
         int var12 = findBits(param0, param6);
         int var13 = findBits(param2, param8);
         int var14 = findBits(param4, param10);
         if (var12 < 0 || var13 < 0 || var14 < 0) {
            return new ArrayVoxelShape(
               BLOCK.shape,
               DoubleArrayList.wrap(new double[]{param0, param6}),
               DoubleArrayList.wrap(new double[]{param2, param8}),
               DoubleArrayList.wrap(new double[]{param4, param10})
            );
         }

         if (var12 == 0 && var13 == 0 && var14 == 0) {
            return block();
         }

         int var15 = 1 << var12;
         int var16 = 1 << var13;
         int var17 = 1 << var14;
         BitSetDiscreteVoxelShape var18 = BitSetDiscreteVoxelShape.withFilledBounds(
            var15,
            var16,
            var17,
            (int)Math.round(param0 * var15),
            (int)Math.round(param2 * var16),
            (int)Math.round(param4 * var17),
            (int)Math.round(param6 * var15),
            (int)Math.round(param8 * var16),
            (int)Math.round(param10 * var17)
         );
         return new CubeVoxelShape(var18);
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB param0) {
      return create(param0.minX, param0.minY, param0.minZ, param0.maxX, param0.maxY, param0.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double param0, double param2) {
      if (!(param0 < -1.0E-7) && !(param2 > 1.0000001)) {
         for (int var4 = 0; var4 <= 3; var4++) {
            int var5 = 1 << var4;
            double var6 = param0 * var5;
            double var8 = param2 * var5;
            boolean var10 = Math.abs(var6 - Math.round(var6)) < 1.0E-7 * var5;
            boolean var11 = Math.abs(var8 - Math.round(var8)) < 1.0E-7 * var5;
            if (var10 && var11) {
               return var4;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int param0, int param1) {
      return (long)param0 * (param1 / IntMath.gcd(param0, param1));
   }

   public static VoxelShape or(VoxelShape param0, VoxelShape param1) {
      return join(param0, param1, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape param0, VoxelShape... param1) {
      return Arrays.stream(param1).reduce(param0, Shapes::or);
   }

   public static VoxelShape join(VoxelShape param0, VoxelShape param1, BooleanOp param2) {
      return joinUnoptimized(param0, param1, param2).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape param0, VoxelShape param1, BooleanOp param2) {
      if (param2.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      }

      if (param0 == param1) {
         return param2.apply(true, true) ? param0 : empty();
      }

      boolean var3 = param2.apply(true, false);
      boolean var4 = param2.apply(false, true);
      if (param0.isEmpty()) {
         return var4 ? param1 : empty();
      }

      if (param1.isEmpty()) {
         return var3 ? param0 : empty();
      }

      IndexMerger var5 = createIndexMerger(1, param0.getCoords(Direction.Axis.X), param1.getCoords(Direction.Axis.X), var3, var4);
      IndexMerger var6 = createIndexMerger(var5.size() - 1, param0.getCoords(Direction.Axis.Y), param1.getCoords(Direction.Axis.Y), var3, var4);
      IndexMerger var7 = createIndexMerger(
         (var5.size() - 1) * (var6.size() - 1), param0.getCoords(Direction.Axis.Z), param1.getCoords(Direction.Axis.Z), var3, var4
      );
      BitSetDiscreteVoxelShape var8 = BitSetDiscreteVoxelShape.join(param0.shape, param1.shape, var5, var6, var7, param2);
      return var5 instanceof DiscreteCubeMerger && var6 instanceof DiscreteCubeMerger && var7 instanceof DiscreteCubeMerger
         ? new CubeVoxelShape(var8)
         : new ArrayVoxelShape(var8, var5.getList(), var6.getList(), var7.getList());
   }

   public static boolean joinIsNotEmpty(VoxelShape param0, VoxelShape param1, BooleanOp param2) {
      if (param2.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      }

      boolean var3 = param0.isEmpty();
      boolean var4 = param1.isEmpty();
      if (!var3 && !var4) {
         if (param0 == param1) {
            return param2.apply(true, true);
         }

         boolean var5 = param2.apply(true, false);
         boolean var6 = param2.apply(false, true);

         for (Direction.Axis var10 : AxisCycle.AXIS_VALUES) {
            if (param0.max(var10) < param1.min(var10) - 1.0E-7) {
               return var5 || var6;
            }

            if (param1.max(var10) < param0.min(var10) - 1.0E-7) {
               return var5 || var6;
            }
         }

         IndexMerger var7 = createIndexMerger(1, param0.getCoords(Direction.Axis.X), param1.getCoords(Direction.Axis.X), var5, var6);
         IndexMerger var8 = createIndexMerger(var7.size() - 1, param0.getCoords(Direction.Axis.Y), param1.getCoords(Direction.Axis.Y), var5, var6);
         IndexMerger var9 = createIndexMerger(
            (var7.size() - 1) * (var8.size() - 1), param0.getCoords(Direction.Axis.Z), param1.getCoords(Direction.Axis.Z), var5, var6
         );
         return joinIsNotEmpty(var7, var8, var9, param0.shape, param1.shape, param2);
      } else {
         return param2.apply(!var3, !var4);
      }
   }

   private static boolean joinIsNotEmpty(
      IndexMerger param0, IndexMerger param1, IndexMerger param2, DiscreteVoxelShape param3, DiscreteVoxelShape param4, BooleanOp param5
   ) {
      return !param0.forMergedIndexes(
         (l_param5, l_param6, l_param7) -> param1.forMergedIndexes(
            (l2_param6, l2_param7, l2_param8) -> param2.forMergedIndexes(
               (l3_param7, l3_param8, l3_param9) -> !param5.apply(
                  param3.isFullWide(l_param5, l2_param6, l3_param7), param4.isFullWide(l_param6, l2_param7, l3_param8)
               )
            )
         )
      );
   }

   public static double collide(Direction.Axis param0, AABB param1, Iterable<VoxelShape> param2, double param3) {
      for (VoxelShape var6 : param2) {
         if (Math.abs(param3) < 1.0E-7) {
            return 0.0;
         }

         param3 = var6.collide(param0, param1, param3);
      }

      return param3;
   }

   public static boolean blockOccudes(VoxelShape param0, VoxelShape param1, Direction param2) {
      if (param0 == block() && param1 == block()) {
         return true;
      }

      if (param1.isEmpty()) {
         return false;
      }

      Direction.Axis var3 = param2.getAxis();
      Direction.AxisDirection var4 = param2.getAxisDirection();
      VoxelShape var5 = var4 == Direction.AxisDirection.POSITIVE ? param0 : param1;
      VoxelShape var6 = var4 == Direction.AxisDirection.POSITIVE ? param1 : param0;
      BooleanOp var7 = var4 == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
      return DoubleMath.fuzzyEquals(var5.max(var3), 1.0, 1.0E-7)
         && DoubleMath.fuzzyEquals(var6.min(var3), 0.0, 1.0E-7)
         && !joinIsNotEmpty(new SliceShape(var5, var3, var5.shape.getSize(var3) - 1), new SliceShape(var6, var3, 0), var7);
   }

   public static VoxelShape getFaceShape(VoxelShape param0, Direction param1) {
      if (param0 == block()) {
         return block();
      }

      Direction.Axis var4 = param1.getAxis();
      boolean var2;
      int var3;
      if (param1.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
         var2 = DoubleMath.fuzzyEquals(param0.max(var4), 1.0, 1.0E-7);
         var3 = param0.shape.getSize(var4) - 1;
      } else {
         var2 = DoubleMath.fuzzyEquals(param0.min(var4), 0.0, 1.0E-7);
         var3 = 0;
      }

      return !var2 ? empty() : new SliceShape(param0, var4, var3);
   }

   public static boolean mergedFaceOccludes(VoxelShape param0, VoxelShape param1, Direction param2) {
      if (param0 != block() && param1 != block()) {
         Direction.Axis var3 = param2.getAxis();
         Direction.AxisDirection var4 = param2.getAxisDirection();
         VoxelShape var5 = var4 == Direction.AxisDirection.POSITIVE ? param0 : param1;
         VoxelShape var6 = var4 == Direction.AxisDirection.POSITIVE ? param1 : param0;
         if (!DoubleMath.fuzzyEquals(var5.max(var3), 1.0, 1.0E-7)) {
            var5 = empty();
         }

         if (!DoubleMath.fuzzyEquals(var6.min(var3), 0.0, 1.0E-7)) {
            var6 = empty();
         }

         return !joinIsNotEmpty(
            block(),
            joinUnoptimized(new SliceShape(var5, var3, var5.shape.getSize(var3) - 1), new SliceShape(var6, var3, 0), BooleanOp.OR),
            BooleanOp.ONLY_FIRST
         );
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape param0, VoxelShape param1) {
      if (param0 == block() || param1 == block()) {
         return true;
      } else {
         return param0.isEmpty() && param1.isEmpty() ? false : !joinIsNotEmpty(block(), joinUnoptimized(param0, param1, BooleanOp.OR), BooleanOp.ONLY_FIRST);
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int param0, DoubleList param1, DoubleList param2, boolean param3, boolean param4) {
      int var5 = param1.size() - 1;
      int var6 = param2.size() - 1;
      if (param1 instanceof CubePointRange && param2 instanceof CubePointRange) {
         long var7 = lcm(var5, var6);
         if (param0 * var7 <= 256L) {
            return new DiscreteCubeMerger(var5, var6);
         }
      }

      if (param1.getDouble(var5) < param2.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(param1, param2, false);
      } else if (param2.getDouble(var6) < param1.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(param2, param1, true);
      } else {
         return var5 == var6 && Objects.equals(param1, param2) ? new IdenticalMerger(param1) : new IndirectMerger(param1, param2, param3, param4);
      }
   }

   public interface DoubleLineConsumer {
      void consume(double var1, double var3, double var5, double var7, double var9, double var11);
   }
}
