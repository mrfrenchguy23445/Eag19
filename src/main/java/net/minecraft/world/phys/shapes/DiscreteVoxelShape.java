package net.minecraft.world.phys.shapes;

import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;

public abstract class DiscreteVoxelShape {
   private static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
   protected final int xSize;
   protected final int ySize;
   protected final int zSize;

   protected DiscreteVoxelShape(int param0, int param1, int param2) {
      if (param0 >= 0 && param1 >= 0 && param2 >= 0) {
         this.xSize = param0;
         this.ySize = param1;
         this.zSize = param2;
      } else {
         throw new IllegalArgumentException("Need all positive sizes: x: " + param0 + ", y: " + param1 + ", z: " + param2);
      }
   }

   public boolean isFullWide(AxisCycle param0, int param1, int param2, int param3) {
      return this.isFullWide(
         param0.cycle(param1, param2, param3, Direction.Axis.X),
         param0.cycle(param1, param2, param3, Direction.Axis.Y),
         param0.cycle(param1, param2, param3, Direction.Axis.Z)
      );
   }

   public boolean isFullWide(int param0, int param1, int param2) {
      if (param0 < 0 || param1 < 0 || param2 < 0) {
         return false;
      } else {
         return param0 < this.xSize && param1 < this.ySize && param2 < this.zSize ? this.isFull(param0, param1, param2) : false;
      }
   }

   public boolean isFull(AxisCycle param0, int param1, int param2, int param3) {
      return this.isFull(
         param0.cycle(param1, param2, param3, Direction.Axis.X),
         param0.cycle(param1, param2, param3, Direction.Axis.Y),
         param0.cycle(param1, param2, param3, Direction.Axis.Z)
      );
   }

   public abstract boolean isFull(int var1, int var2, int var3);

   public abstract void fill(int var1, int var2, int var3);

   public boolean isEmpty() {
      for (Direction.Axis var4 : AXIS_VALUES) {
         if (this.firstFull(var4) >= this.lastFull(var4)) {
            return true;
         }
      }

      return false;
   }

   public abstract int firstFull(Direction.Axis var1);

   public abstract int lastFull(Direction.Axis var1);

   public int firstFull(Direction.Axis param0, int param1, int param2) {
      int var4 = this.getSize(param0);
      if (param1 >= 0 && param2 >= 0) {
         Direction.Axis var5 = AxisCycle.FORWARD.cycle(param0);
         Direction.Axis var6 = AxisCycle.BACKWARD.cycle(param0);
         if (param1 < this.getSize(var5) && param2 < this.getSize(var6)) {
            AxisCycle var7 = AxisCycle.between(Direction.Axis.X, param0);

            for (int var8 = 0; var8 < var4; var8++) {
               if (this.isFull(var7, var8, param1, param2)) {
                  return var8;
               }
            }

            return var4;
         } else {
            return var4;
         }
      } else {
         return var4;
      }
   }

   public int lastFull(Direction.Axis param0, int param1, int param2) {
      if (param1 >= 0 && param2 >= 0) {
         Direction.Axis var4 = AxisCycle.FORWARD.cycle(param0);
         Direction.Axis var5 = AxisCycle.BACKWARD.cycle(param0);
         if (param1 < this.getSize(var4) && param2 < this.getSize(var5)) {
            int var6 = this.getSize(param0);
            AxisCycle var7 = AxisCycle.between(Direction.Axis.X, param0);

            for (int var8 = var6 - 1; var8 >= 0; var8--) {
               if (this.isFull(var7, var8, param1, param2)) {
                  return var8 + 1;
               }
            }

            return 0;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public int getSize(Direction.Axis param0) {
      return param0.choose(this.xSize, this.ySize, this.zSize);
   }

   public int getXSize() {
      return this.getSize(Direction.Axis.X);
   }

   public int getYSize() {
      return this.getSize(Direction.Axis.Y);
   }

   public int getZSize() {
      return this.getSize(Direction.Axis.Z);
   }

   public void forAllEdges(DiscreteVoxelShape.IntLineConsumer param0, boolean param1) {
      this.forAllAxisEdges(param0, AxisCycle.NONE, param1);
      this.forAllAxisEdges(param0, AxisCycle.FORWARD, param1);
      this.forAllAxisEdges(param0, AxisCycle.BACKWARD, param1);
   }

   private void forAllAxisEdges(DiscreteVoxelShape.IntLineConsumer param0, AxisCycle param1, boolean param2) {
      AxisCycle var5 = param1.inverse();
      int var6 = this.getSize(var5.cycle(Direction.Axis.X));
      int var7 = this.getSize(var5.cycle(Direction.Axis.Y));
      int var8 = this.getSize(var5.cycle(Direction.Axis.Z));

      for (int var9 = 0; var9 <= var6; var9++) {
         for (int var10 = 0; var10 <= var7; var10++) {
            int var4 = -1;

            for (int var11 = 0; var11 <= var8; var11++) {
               int var12 = 0;
               int var13 = 0;

               for (int var14 = 0; var14 <= 1; var14++) {
                  for (int var15 = 0; var15 <= 1; var15++) {
                     if (this.isFullWide(var5, var9 + var14 - 1, var10 + var15 - 1, var11)) {
                        var12++;
                        var13 ^= var14 ^ var15;
                     }
                  }
               }

               if (var12 == 1 || var12 == 3 || var12 == 2 && (var13 & 1) == 0) {
                  if (param2) {
                     if (var4 == -1) {
                        var4 = var11;
                     }
                  } else {
                     param0.consume(
                        var5.cycle(var9, var10, var11, Direction.Axis.X),
                        var5.cycle(var9, var10, var11, Direction.Axis.Y),
                        var5.cycle(var9, var10, var11, Direction.Axis.Z),
                        var5.cycle(var9, var10, var11 + 1, Direction.Axis.X),
                        var5.cycle(var9, var10, var11 + 1, Direction.Axis.Y),
                        var5.cycle(var9, var10, var11 + 1, Direction.Axis.Z)
                     );
                  }
               } else if (var4 != -1) {
                  param0.consume(
                     var5.cycle(var9, var10, var4, Direction.Axis.X),
                     var5.cycle(var9, var10, var4, Direction.Axis.Y),
                     var5.cycle(var9, var10, var4, Direction.Axis.Z),
                     var5.cycle(var9, var10, var11, Direction.Axis.X),
                     var5.cycle(var9, var10, var11, Direction.Axis.Y),
                     var5.cycle(var9, var10, var11, Direction.Axis.Z)
                  );
                  var4 = -1;
               }
            }
         }
      }
   }

   public void forAllBoxes(DiscreteVoxelShape.IntLineConsumer param0, boolean param1) {
      BitSetDiscreteVoxelShape.forAllBoxes(this, param0, param1);
   }

   public void forAllFaces(DiscreteVoxelShape.IntFaceConsumer param0) {
      this.forAllAxisFaces(param0, AxisCycle.NONE);
      this.forAllAxisFaces(param0, AxisCycle.FORWARD);
      this.forAllAxisFaces(param0, AxisCycle.BACKWARD);
   }

   private void forAllAxisFaces(DiscreteVoxelShape.IntFaceConsumer param0, AxisCycle param1) {
      AxisCycle var3 = param1.inverse();
      Direction.Axis var4 = var3.cycle(Direction.Axis.Z);
      int var5 = this.getSize(var3.cycle(Direction.Axis.X));
      int var6 = this.getSize(var3.cycle(Direction.Axis.Y));
      int var7 = this.getSize(var4);
      Direction var8 = Direction.fromAxisAndDirection(var4, Direction.AxisDirection.NEGATIVE);
      Direction var9 = Direction.fromAxisAndDirection(var4, Direction.AxisDirection.POSITIVE);

      for (int var10 = 0; var10 < var5; var10++) {
         for (int var11 = 0; var11 < var6; var11++) {
            boolean var12 = false;

            for (int var13 = 0; var13 <= var7; var13++) {
               boolean var14 = var13 != var7 && this.isFull(var3, var10, var11, var13);
               if (!var12 && var14) {
                  param0.consume(
                     var8,
                     var3.cycle(var10, var11, var13, Direction.Axis.X),
                     var3.cycle(var10, var11, var13, Direction.Axis.Y),
                     var3.cycle(var10, var11, var13, Direction.Axis.Z)
                  );
               }

               if (var12 && !var14) {
                  param0.consume(
                     var9,
                     var3.cycle(var10, var11, var13 - 1, Direction.Axis.X),
                     var3.cycle(var10, var11, var13 - 1, Direction.Axis.Y),
                     var3.cycle(var10, var11, var13 - 1, Direction.Axis.Z)
                  );
               }

               var12 = var14;
            }
         }
      }
   }

   public interface IntFaceConsumer {
      void consume(Direction var1, int var2, int var3, int var4);
   }

   public interface IntLineConsumer {
      void consume(int var1, int var2, int var3, int var4, int var5, int var6);
   }
}
