package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.NoSuchElementException;
import net.minecraft.util.Mth;

public class SpatialLongSet extends LongLinkedOpenHashSet {
   private final SpatialLongSet.InternalMap map;

   public SpatialLongSet(int param0, float param1) {
      super(param0, param1);
      this.map = new SpatialLongSet.InternalMap(param0 / 64, param1);
   }

   @Override
   public boolean add(long param0) {
      return this.map.addBit(param0);
   }

   @Override
   public boolean rem(long param0) {
      return this.map.removeBit(param0);
   }

   @Override
   public long removeFirstLong() {
      return this.map.removeFirstBit();
   }

   @Override
   public int size() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   protected static class InternalMap extends Long2LongLinkedOpenHashMap {
      private static final int X_BITS = Mth.log2(60000000);
      private static final int Z_BITS = Mth.log2(60000000);
      private static final int Y_BITS = 64 - X_BITS - Z_BITS;
      private static final int Y_OFFSET = 0;
      private static final int Z_OFFSET = Y_BITS;
      private static final int X_OFFSET = Y_BITS + Z_BITS;
      private static final long OUTER_MASK = 3L << X_OFFSET | 3L | 3L << Z_OFFSET;
      private int lastPos = -1;
      private long lastOuterKey;
      private final int minSize;

      public InternalMap(int param0, float param1) {
         super(param0, param1);
         this.minSize = param0;
      }

      static long getOuterKey(long param0) {
         return param0 & ~OUTER_MASK;
      }

      static int getInnerKey(long param0) {
         int var2 = (int)(param0 >>> X_OFFSET & 3L);
         int var3 = (int)(param0 >>> 0 & 3L);
         int var4 = (int)(param0 >>> Z_OFFSET & 3L);
         return var2 << 4 | var4 << 2 | var3;
      }

      static long getFullKey(long param0, int param2) {
         param0 |= (long)(param2 >>> 4 & 3) << X_OFFSET;
         param0 |= (long)(param2 >>> 2 & 3) << Z_OFFSET;
         return param0 | (long)(param2 >>> 0 & 3) << 0;
      }

      public boolean addBit(long param0) {
         long var3 = getOuterKey(param0);
         int var5 = getInnerKey(param0);
         long var6 = 1L << var5;
         int var8;
         if (var3 == 0L) {
            if (this.containsNullKey) {
               return this.replaceBit(this.n, var6);
            }

            this.containsNullKey = true;
            var8 = this.n;
         } else {
            if (this.lastPos != -1 && var3 == this.lastOuterKey) {
               return this.replaceBit(this.lastPos, var6);
            }

            long[] var9 = this.key;
            var8 = (int)HashCommon.mix(var3) & this.mask;

            for (long var10 = var9[var8]; var10 != 0L; var10 = var9[var8]) {
               if (var10 == var3) {
                  this.lastPos = var8;
                  this.lastOuterKey = var3;
                  return this.replaceBit(var8, var6);
               }

               var8 = var8 + 1 & this.mask;
            }
         }

         this.key[var8] = var3;
         this.value[var8] = var6;
         if (this.size == 0) {
            this.first = this.last = var8;
            this.link[var8] = -1L;
         } else {
            this.link[this.last] = this.link[this.last] ^ (this.link[this.last] ^ var8 & 4294967295L) & 4294967295L;
            this.link[var8] = (this.last & 4294967295L) << 32 | 4294967295L;
            this.last = var8;
         }

         if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
         }

         return false;
      }

      private boolean replaceBit(int param0, long param1) {
         boolean var4 = (this.value[param0] & param1) != 0L;
         this.value[param0] = this.value[param0] | param1;
         return var4;
      }

      public boolean removeBit(long param0) {
         long var3 = getOuterKey(param0);
         int var5 = getInnerKey(param0);
         long var6 = 1L << var5;
         if (var3 == 0L) {
            return this.containsNullKey ? this.removeFromNullEntry(var6) : false;
         }

         if (this.lastPos != -1 && var3 == this.lastOuterKey) {
            return this.removeFromEntry(this.lastPos, var6);
         }

         long[] var8 = this.key;
         int var9 = (int)HashCommon.mix(var3) & this.mask;

         for (long var10 = var8[var9]; var10 != 0L; var10 = var8[var9]) {
            if (var3 == var10) {
               this.lastPos = var9;
               this.lastOuterKey = var3;
               return this.removeFromEntry(var9, var6);
            }

            var9 = var9 + 1 & this.mask;
         }

         return false;
      }

      private boolean removeFromNullEntry(long param0) {
         if ((this.value[this.n] & param0) == 0L) {
            return false;
         }

         this.value[this.n] = this.value[this.n] & ~param0;
         if (this.value[this.n] != 0L) {
            return true;
         }

         this.containsNullKey = false;
         this.size--;
         this.fixPointers(this.n);
         if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
         }

         return true;
      }

      private boolean removeFromEntry(int param0, long param1) {
         if ((this.value[param0] & param1) == 0L) {
            return false;
         }

         this.value[param0] = this.value[param0] & ~param1;
         if (this.value[param0] != 0L) {
            return true;
         }

         this.lastPos = -1;
         this.size--;
         this.fixPointers(param0);
         this.shiftKeys(param0);
         if (this.size < this.maxFill / 4 && this.n > 16) {
            this.rehash(this.n / 2);
         }

         return true;
      }

      public long removeFirstBit() {
         if (this.size == 0) {
            throw new NoSuchElementException();
         }

         int var1 = this.first;
         long var2 = this.key[var1];
         int var4 = Long.numberOfTrailingZeros(this.value[var1]);
         this.value[var1] = this.value[var1] & ~(1L << var4);
         if (this.value[var1] == 0L) {
            this.removeFirstLong();
            this.lastPos = -1;
         }

         return getFullKey(var2, var4);
      }

      @Override
      protected void rehash(int param0) {
         if (param0 > this.minSize) {
            super.rehash(param0);
         }
      }
   }
}
