package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
   private final BitSet used = new BitSet();

   public void force(int param0, int param1) {
      this.used.set(param0, param0 + param1);
   }

   public void free(int param0, int param1) {
      this.used.clear(param0, param0 + param1);
   }

   public int allocate(int param0) {
      int var2 = 0;

      while (true) {
         int var3 = this.used.nextClearBit(var2);
         int var4 = this.used.nextSetBit(var3);
         if (var4 == -1 || var4 - var3 >= param0) {
            this.force(var3, param0);
            return var3;
         }

         var2 = var4;
      }
   }

   @VisibleForTesting
   public IntSet getUsed() {
      return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
   }
}
