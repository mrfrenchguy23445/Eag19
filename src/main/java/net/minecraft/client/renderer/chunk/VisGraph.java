package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class VisGraph {
   private static final int SIZE_IN_BITS = 4;
   private static final int LEN = 16;
   private static final int MASK = 15;
   private static final int SIZE = 4096;
   private static final int X_SHIFT = 0;
   private static final int Z_SHIFT = 4;
   private static final int Y_SHIFT = 8;
   private static final int DX = (int)Math.pow(16.0, 0.0);
   private static final int DZ = (int)Math.pow(16.0, 1.0);
   private static final int DY = (int)Math.pow(16.0, 2.0);
   private static final int INVALID_INDEX = -1;
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BitSet bitSet = new BitSet(4096);
   private static final int[] INDEX_OF_EDGES = Util.make(new int[1352], param0 -> {
      int var1 = 0;
      int var2 = 15;
      int var3 = 0;

      for (int var4 = 0; var4 < 16; var4++) {
         for (int var5 = 0; var5 < 16; var5++) {
            for (int var6 = 0; var6 < 16; var6++) {
               if (var4 == 0 || var4 == 15 || var5 == 0 || var5 == 15 || var6 == 0 || var6 == 15) {
                  param0[var3++] = getIndex(var4, var5, var6);
               }
            }
         }
      }
   });
   private int empty = 4096;

   public void setOpaque(BlockPos param0) {
      this.bitSet.set(getIndex(param0), true);
      this.empty--;
   }

   private static int getIndex(BlockPos param0) {
      return getIndex(param0.getX() & 15, param0.getY() & 15, param0.getZ() & 15);
   }

   private static int getIndex(int param0, int param1, int param2) {
      return param0 << 0 | param1 << 8 | param2 << 4;
   }

   public VisibilitySet resolve() {
      VisibilitySet var1 = new VisibilitySet();
      if (4096 - this.empty < 256) {
         var1.setAll(true);
      } else if (this.empty == 0) {
         var1.setAll(false);
      } else {
         for (int var5 : INDEX_OF_EDGES) {
            if (!this.bitSet.get(var5)) {
               var1.add(this.floodFill(var5));
            }
         }
      }

      return var1;
   }

   private Set<Direction> floodFill(int param0) {
      Set<Direction> var2 = EnumSet.noneOf(Direction.class);
      IntPriorityQueue var3 = new IntArrayFIFOQueue();
      var3.enqueue(param0);
      this.bitSet.set(param0, true);

      while (!var3.isEmpty()) {
         int var4 = var3.dequeueInt();
         this.addEdges(var4, var2);

         for (Direction var8 : DIRECTIONS) {
            int var9 = this.getNeighborIndexAtFace(var4, var8);
            if (var9 >= 0 && !this.bitSet.get(var9)) {
               this.bitSet.set(var9, true);
               var3.enqueue(var9);
            }
         }
      }

      return var2;
   }

   private void addEdges(int param0, Set<Direction> param1) {
      int var3 = param0 >> 0 & 15;
      if (var3 == 0) {
         param1.add(Direction.WEST);
      } else if (var3 == 15) {
         param1.add(Direction.EAST);
      }

      int var4 = param0 >> 8 & 15;
      if (var4 == 0) {
         param1.add(Direction.DOWN);
      } else if (var4 == 15) {
         param1.add(Direction.UP);
      }

      int var5 = param0 >> 4 & 15;
      if (var5 == 0) {
         param1.add(Direction.NORTH);
      } else if (var5 == 15) {
         param1.add(Direction.SOUTH);
      }
   }

   private int getNeighborIndexAtFace(int param0, Direction param1) {
      switch (param1) {
         case DOWN:
            if ((param0 >> 8 & 15) == 0) {
               return -1;
            }

            return param0 - DY;
         case UP:
            if ((param0 >> 8 & 15) == 15) {
               return -1;
            }

            return param0 + DY;
         case NORTH:
            if ((param0 >> 4 & 15) == 0) {
               return -1;
            }

            return param0 - DZ;
         case SOUTH:
            if ((param0 >> 4 & 15) == 15) {
               return -1;
            }

            return param0 + DZ;
         case WEST:
            if ((param0 >> 0 & 15) == 0) {
               return -1;
            }

            return param0 - DX;
         case EAST:
            if ((param0 >> 0 & 15) == 15) {
               return -1;
            }

            return param0 + DX;
         default:
            return -1;
      }
   }
}
