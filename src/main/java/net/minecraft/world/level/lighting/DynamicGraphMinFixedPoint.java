package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;

public abstract class DynamicGraphMinFixedPoint {
   private static final int NO_COMPUTED_LEVEL = 255;
   private final int levelCount;
   private final LongLinkedOpenHashSet[] queues;
   private final Long2ByteMap computedLevels;
   private int firstQueuedLevel;
   private volatile boolean hasWork;

   protected DynamicGraphMinFixedPoint(int param0, final int param1, final int param2) {
      if (param0 >= 254) {
         throw new IllegalArgumentException("Level count must be < 254.");
      }

      this.levelCount = param0;
      this.queues = new LongLinkedOpenHashSet[param0];

      for (int var4 = 0; var4 < param0; var4++) {
         this.queues[var4] = new LongLinkedOpenHashSet(param1, 0.5F) {
            @Override
            protected void rehash(int l_param0) {
               if (l_param0 > param1) {
                  super.rehash(l_param0);
               }
            }
         };
      }

      this.computedLevels = new Long2ByteOpenHashMap(param2, 0.5F) {
         @Override
         protected void rehash(int l_param0) {
            if (l_param0 > param2) {
               super.rehash(l_param0);
            }
         }
      };
      this.computedLevels.defaultReturnValue((byte)-1);
      this.firstQueuedLevel = param0;
   }

   private int getKey(int param0, int param1) {
      int var3 = param0;
      if (var3 > param1) {
         var3 = param1;
      }

      if (var3 > this.levelCount - 1) {
         var3 = this.levelCount - 1;
      }

      return var3;
   }

   private void checkFirstQueuedLevel(int param0) {
      int var2 = this.firstQueuedLevel;
      this.firstQueuedLevel = param0;

      for (int var3 = var2 + 1; var3 < param0; var3++) {
         if (!this.queues[var3].isEmpty()) {
            this.firstQueuedLevel = var3;
            break;
         }
      }
   }

   protected void removeFromQueue(long param0) {
      int var3 = this.computedLevels.get(param0) & 255;
      if (var3 != 255) {
         int var4 = this.getLevel(param0);
         int var5 = this.getKey(var4, var3);
         this.dequeue(param0, var5, this.levelCount, true);
         this.hasWork = this.firstQueuedLevel < this.levelCount;
      }
   }

   public void removeIf(LongPredicate param0) {
      LongList var2 = new LongArrayList();
      this.computedLevels.keySet().forEach(l_param2 -> {
         if (param0.test(l_param2)) {
            var2.add(l_param2);
         }
      });
      var2.forEach(this::removeFromQueue);
   }

   private void dequeue(long param0, int param2, int param3, boolean param4) {
      if (param4) {
         this.computedLevels.remove(param0);
      }

      this.queues[param2].remove(param0);
      if (this.queues[param2].isEmpty() && this.firstQueuedLevel == param2) {
         this.checkFirstQueuedLevel(param3);
      }
   }

   private void enqueue(long param0, int param2, int param3) {
      this.computedLevels.put(param0, (byte)param2);
      this.queues[param3].add(param0);
      if (this.firstQueuedLevel > param3) {
         this.firstQueuedLevel = param3;
      }
   }

   protected void checkNode(long param0) {
      this.checkEdge(param0, param0, this.levelCount - 1, false);
   }

   protected void checkEdge(long param0, long param2, int param4, boolean param5) {
      this.checkEdge(param0, param2, param4, this.getLevel(param2), this.computedLevels.get(param2) & 255, param5);
      this.hasWork = this.firstQueuedLevel < this.levelCount;
   }

   private void checkEdge(long param0, long param2, int param4, int param5, int param6, boolean param7) {
      if (!this.isSource(param2)) {
         param4 = Mth.clamp(param4, 0, this.levelCount - 1);
         param5 = Mth.clamp(param5, 0, this.levelCount - 1);
         boolean var9;
         if (param6 == 255) {
            var9 = true;
            param6 = param5;
         } else {
            var9 = false;
         }

         int var10;
         if (param7) {
            var10 = Math.min(param6, param4);
         } else {
            var10 = Mth.clamp(this.getComputedLevel(param2, param0, param4), 0, this.levelCount - 1);
         }

         int var11 = this.getKey(param5, param6);
         if (param5 != var10) {
            int var12 = this.getKey(param5, var10);
            if (var11 != var12 && !var9) {
               this.dequeue(param2, var11, var12, false);
            }

            this.enqueue(param2, var10, var12);
         } else if (!var9) {
            this.dequeue(param2, var11, this.levelCount, true);
         }
      }
   }

   protected final void checkNeighbor(long param0, long param2, int param4, boolean param5) {
      int var7 = this.computedLevels.get(param2) & 255;
      int var8 = Mth.clamp(this.computeLevelFromNeighbor(param0, param2, param4), 0, this.levelCount - 1);
      if (param5) {
         this.checkEdge(param0, param2, var8, this.getLevel(param2), var7, true);
      } else {
         int var9;
         boolean var10;
         if (var7 == 255) {
            var10 = true;
            var9 = Mth.clamp(this.getLevel(param2), 0, this.levelCount - 1);
         } else {
            var9 = var7;
            var10 = false;
         }

         if (var8 == var9) {
            this.checkEdge(param0, param2, this.levelCount - 1, var10 ? var9 : this.getLevel(param2), var7, false);
         }
      }
   }

   protected final boolean hasWork() {
      return this.hasWork;
   }

   protected final int runUpdates(int param0) {
      if (this.firstQueuedLevel >= this.levelCount) {
         return param0;
      }

      while (this.firstQueuedLevel < this.levelCount && param0 > 0) {
         param0--;
         LongLinkedOpenHashSet var2 = this.queues[this.firstQueuedLevel];
         long var3 = var2.removeFirstLong();
         int var5 = Mth.clamp(this.getLevel(var3), 0, this.levelCount - 1);
         if (var2.isEmpty()) {
            this.checkFirstQueuedLevel(this.levelCount);
         }

         int var6 = this.computedLevels.remove(var3) & 255;
         if (var6 < var5) {
            this.setLevel(var3, var6);
            this.checkNeighborsAfterUpdate(var3, var6, true);
         } else if (var6 > var5) {
            this.enqueue(var3, var6, this.getKey(this.levelCount - 1, var6));
            this.setLevel(var3, this.levelCount - 1);
            this.checkNeighborsAfterUpdate(var3, var5, false);
         }
      }

      this.hasWork = this.firstQueuedLevel < this.levelCount;
      return param0;
   }

   public int getQueueSize() {
      return this.computedLevels.size();
   }

   protected abstract boolean isSource(long var1);

   protected abstract int getComputedLevel(long var1, long var3, int var5);

   protected abstract void checkNeighborsAfterUpdate(long var1, int var3, boolean var4);

   protected abstract int getLevel(long var1);

   protected abstract void setLevel(long var1, int var3);

   protected abstract int computeLevelFromNeighbor(long var1, long var3, int var5);
}
