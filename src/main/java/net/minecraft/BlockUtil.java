package net.minecraft;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {
   public static BlockUtil.FoundRectangle getLargestRectangleAround(
      BlockPos param0, Direction.Axis param1, int param2, Direction.Axis param3, int param4, Predicate<BlockPos> param5
   ) {
      BlockPos.MutableBlockPos var6 = param0.mutable();
      Direction var7 = Direction.get(Direction.AxisDirection.NEGATIVE, param1);
      Direction var8 = var7.getOpposite();
      Direction var9 = Direction.get(Direction.AxisDirection.NEGATIVE, param3);
      Direction var10 = var9.getOpposite();
      int var11 = getLimit(param5, var6.set(param0), var7, param2);
      int var12 = getLimit(param5, var6.set(param0), var8, param2);
      int var13 = var11;
      BlockUtil.IntBounds[] var14 = new BlockUtil.IntBounds[var13 + 1 + var12];
      var14[var13] = new BlockUtil.IntBounds(getLimit(param5, var6.set(param0), var9, param4), getLimit(param5, var6.set(param0), var10, param4));
      int var15 = var14[var13].min;

      for (int var16 = 1; var16 <= var11; var16++) {
         BlockUtil.IntBounds var17 = var14[var13 - (var16 - 1)];
         var14[var13 - var16] = new BlockUtil.IntBounds(
            getLimit(param5, var6.set(param0).move(var7, var16), var9, var17.min), getLimit(param5, var6.set(param0).move(var7, var16), var10, var17.max)
         );
      }

      for (int var16 = 1; var16 <= var12; var16++) {
         BlockUtil.IntBounds var17 = var14[var13 + var16 - 1];
         var14[var13 + var16] = new BlockUtil.IntBounds(
            getLimit(param5, var6.set(param0).move(var8, var16), var9, var17.min), getLimit(param5, var6.set(param0).move(var8, var16), var10, var17.max)
         );
      }

      int var16 = 0;
      int var17 = 0;
      int var18 = 0;
      int var19 = 0;
      int[] var20 = new int[var14.length];

      for (int var21 = var15; var21 >= 0; var21--) {
         for (int var22 = 0; var22 < var14.length; var22++) {
            BlockUtil.IntBounds var23 = var14[var22];
            int var24 = var15 - var23.min;
            int var25 = var15 + var23.max;
            var20[var22] = var21 >= var24 && var21 <= var25 ? var25 + 1 - var21 : 0;
         }

         Pair<BlockUtil.IntBounds, Integer> var22 = getMaxRectangleLocation(var20);
         BlockUtil.IntBounds var23 = var22.getFirst();
         int var24 = 1 + var23.max - var23.min;
         int var25 = var22.getSecond();
         if (var24 * var25 > var18 * var19) {
            var16 = var23.min;
            var17 = var21;
            var18 = var24;
            var19 = var25;
         }
      }

      return new BlockUtil.FoundRectangle(param0.relative(param1, var16 - var13).relative(param3, var17 - var15), var18, var19);
   }

   private static int getLimit(Predicate<BlockPos> param0, BlockPos.MutableBlockPos param1, Direction param2, int param3) {
      int var4 = 0;

      while (var4 < param3 && param0.test(param1.move(param2))) {
         var4++;
      }

      return var4;
   }

   @VisibleForTesting
   static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(int[] param0) {
      int var1 = 0;
      int var2 = 0;
      int var3 = 0;
      IntStack var4 = new IntArrayList();
      var4.push(0);

      for (int var5 = 1; var5 <= param0.length; var5++) {
         int var6 = var5 == param0.length ? 0 : param0[var5];

         while (!var4.isEmpty()) {
            int var7 = param0[var4.topInt()];
            if (var6 >= var7) {
               var4.push(var5);
               break;
            }

            var4.popInt();
            int var8 = var4.isEmpty() ? 0 : var4.topInt() + 1;
            if (var7 * (var5 - var8) > var3 * (var2 - var1)) {
               var2 = var5;
               var1 = var8;
               var3 = var7;
            }
         }

         if (var4.isEmpty()) {
            var4.push(var5);
         }
      }

      return new Pair<>(new BlockUtil.IntBounds(var1, var2 - 1), var3);
   }

   public static Optional<BlockPos> getTopConnectedBlock(BlockGetter param0, BlockPos param1, Block param2, Direction param3, Block param4) {
      BlockPos.MutableBlockPos var5 = param1.mutable();

      BlockState var6;
      do {
         var5.move(param3);
         var6 = param0.getBlockState(var5);
      } while (var6.is(param2));

      return var6.is(param4) ? Optional.of(var5) : Optional.empty();
   }

   public static class FoundRectangle {
      public final BlockPos minCorner;
      public final int axis1Size;
      public final int axis2Size;

      public FoundRectangle(BlockPos param0, int param1, int param2) {
         this.minCorner = param0;
         this.axis1Size = param1;
         this.axis2Size = param2;
      }
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(int param0, int param1) {
         this.min = param0;
         this.max = param1;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }
}
