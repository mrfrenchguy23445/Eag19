package net.minecraft.world.phys.shapes;

import java.util.BitSet;
import net.minecraft.core.Direction;

public final class BitSetDiscreteVoxelShape extends DiscreteVoxelShape {
   private final BitSet storage;
   private int xMin;
   private int yMin;
   private int zMin;
   private int xMax;
   private int yMax;
   private int zMax;

   public BitSetDiscreteVoxelShape(int param0, int param1, int param2) {
      super(param0, param1, param2);
      this.storage = new BitSet(param0 * param1 * param2);
      this.xMin = param0;
      this.yMin = param1;
      this.zMin = param2;
   }

   public static BitSetDiscreteVoxelShape withFilledBounds(
      int param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8
   ) {
      BitSetDiscreteVoxelShape var9 = new BitSetDiscreteVoxelShape(param0, param1, param2);
      var9.xMin = param3;
      var9.yMin = param4;
      var9.zMin = param5;
      var9.xMax = param6;
      var9.yMax = param7;
      var9.zMax = param8;

      for (int var10 = param3; var10 < param6; var10++) {
         for (int var11 = param4; var11 < param7; var11++) {
            for (int var12 = param5; var12 < param8; var12++) {
               var9.fillUpdateBounds(var10, var11, var12, false);
            }
         }
      }

      return var9;
   }

   public BitSetDiscreteVoxelShape(DiscreteVoxelShape param0) {
      super(param0.xSize, param0.ySize, param0.zSize);
      if (param0 instanceof BitSetDiscreteVoxelShape) {
         this.storage = (BitSet)((BitSetDiscreteVoxelShape)param0).storage.clone();
      } else {
         this.storage = new BitSet(this.xSize * this.ySize * this.zSize);

         for (int var2 = 0; var2 < this.xSize; var2++) {
            for (int var3 = 0; var3 < this.ySize; var3++) {
               for (int var4 = 0; var4 < this.zSize; var4++) {
                  if (param0.isFull(var2, var3, var4)) {
                     this.storage.set(this.getIndex(var2, var3, var4));
                  }
               }
            }
         }
      }

      this.xMin = param0.firstFull(Direction.Axis.X);
      this.yMin = param0.firstFull(Direction.Axis.Y);
      this.zMin = param0.firstFull(Direction.Axis.Z);
      this.xMax = param0.lastFull(Direction.Axis.X);
      this.yMax = param0.lastFull(Direction.Axis.Y);
      this.zMax = param0.lastFull(Direction.Axis.Z);
   }

   protected int getIndex(int param0, int param1, int param2) {
      return (param0 * this.ySize + param1) * this.zSize + param2;
   }

   @Override
   public boolean isFull(int param0, int param1, int param2) {
      return this.storage.get(this.getIndex(param0, param1, param2));
   }

   private void fillUpdateBounds(int param0, int param1, int param2, boolean param3) {
      this.storage.set(this.getIndex(param0, param1, param2));
      if (param3) {
         this.xMin = Math.min(this.xMin, param0);
         this.yMin = Math.min(this.yMin, param1);
         this.zMin = Math.min(this.zMin, param2);
         this.xMax = Math.max(this.xMax, param0 + 1);
         this.yMax = Math.max(this.yMax, param1 + 1);
         this.zMax = Math.max(this.zMax, param2 + 1);
      }
   }

   @Override
   public void fill(int param0, int param1, int param2) {
      this.fillUpdateBounds(param0, param1, param2, true);
   }

   @Override
   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   @Override
   public int firstFull(Direction.Axis param0) {
      return param0.choose(this.xMin, this.yMin, this.zMin);
   }

   @Override
   public int lastFull(Direction.Axis param0) {
      return param0.choose(this.xMax, this.yMax, this.zMax);
   }

   static BitSetDiscreteVoxelShape join(
      DiscreteVoxelShape param0, DiscreteVoxelShape param1, IndexMerger param2, IndexMerger param3, IndexMerger param4, BooleanOp param5
   ) {
      BitSetDiscreteVoxelShape var6 = new BitSetDiscreteVoxelShape(param2.size() - 1, param3.size() - 1, param4.size() - 1);
      int[] var7 = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
      param2.forMergedIndexes((l_param7, l_param8, l_param9) -> {
         boolean[] l_var10 = new boolean[]{false};
         param3.forMergedIndexes((l2_param10, l2_param11, l2_param12) -> {
            boolean[] l2_var13 = new boolean[]{false};
            param4.forMergedIndexes((l3_param12, l3_param13, l3_param14) -> {
               if (param5.apply(param0.isFullWide(l_param7, l2_param10, l3_param12), param1.isFullWide(l_param8, l2_param11, l3_param13))) {
                  var6.storage.set(var6.getIndex(l_param9, l2_param12, l3_param14));
                  var7[2] = Math.min(var7[2], l3_param14);
                  var7[5] = Math.max(var7[5], l3_param14);
                  l2_var13[0] = true;
               }

               return true;
            });
            if (l2_var13[0]) {
               var7[1] = Math.min(var7[1], l2_param12);
               var7[4] = Math.max(var7[4], l2_param12);
               l_var10[0] = true;
            }

            return true;
         });
         if (l_var10[0]) {
            var7[0] = Math.min(var7[0], l_param9);
            var7[3] = Math.max(var7[3], l_param9);
         }

         return true;
      });
      var6.xMin = var7[0];
      var6.yMin = var7[1];
      var6.zMin = var7[2];
      var6.xMax = var7[3] + 1;
      var6.yMax = var7[4] + 1;
      var6.zMax = var7[5] + 1;
      return var6;
   }

   protected static void forAllBoxes(DiscreteVoxelShape param0, DiscreteVoxelShape.IntLineConsumer param1, boolean param2) {
      BitSetDiscreteVoxelShape var3 = new BitSetDiscreteVoxelShape(param0);

      for (int var4 = 0; var4 < var3.ySize; var4++) {
         for (int var5 = 0; var5 < var3.xSize; var5++) {
            int var6 = -1;

            for (int var7 = 0; var7 <= var3.zSize; var7++) {
               if (var3.isFullWide(var5, var4, var7)) {
                  if (param2) {
                     if (var6 == -1) {
                        var6 = var7;
                     }
                  } else {
                     param1.consume(var5, var4, var7, var5 + 1, var4 + 1, var7 + 1);
                  }
               } else if (var6 != -1) {
                  int var8 = var5;
                  int var9 = var4;
                  var3.clearZStrip(var6, var7, var5, var4);

                  while (var3.isZStripFull(var6, var7, var8 + 1, var4)) {
                     var3.clearZStrip(var6, var7, var8 + 1, var4);
                     var8++;
                  }

                  while (var3.isXZRectangleFull(var5, var8 + 1, var6, var7, var9 + 1)) {
                     for (int var10 = var5; var10 <= var8; var10++) {
                        var3.clearZStrip(var6, var7, var10, var9 + 1);
                     }

                     var9++;
                  }

                  param1.consume(var5, var4, var6, var8 + 1, var9 + 1, var7);
                  var6 = -1;
               }
            }
         }
      }
   }

   private boolean isZStripFull(int param0, int param1, int param2, int param3) {
      return param2 < this.xSize && param3 < this.ySize
         ? this.storage.nextClearBit(this.getIndex(param2, param3, param0)) >= this.getIndex(param2, param3, param1)
         : false;
   }

   private boolean isXZRectangleFull(int param0, int param1, int param2, int param3, int param4) {
      for (int var6 = param0; var6 < param1; var6++) {
         if (!this.isZStripFull(param2, param3, var6, param4)) {
            return false;
         }
      }

      return true;
   }

   private void clearZStrip(int param0, int param1, int param2, int param3) {
      this.storage.clear(this.getIndex(param2, param3, param0), this.getIndex(param2, param3, param1));
   }
}
