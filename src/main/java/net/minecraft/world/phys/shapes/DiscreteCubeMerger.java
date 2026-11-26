package net.minecraft.world.phys.shapes;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class DiscreteCubeMerger implements IndexMerger {
   private final CubePointRange result;
   private final int firstDiv;
   private final int secondDiv;

   DiscreteCubeMerger(int param0, int param1) {
      this.result = new CubePointRange((int)Shapes.lcm(param0, param1));
      int var3 = IntMath.gcd(param0, param1);
      this.firstDiv = param0 / var3;
      this.secondDiv = param1 / var3;
   }

   @Override
   public boolean forMergedIndexes(IndexMerger.IndexConsumer param0) {
      int var2 = this.result.size() - 1;

      for (int var3 = 0; var3 < var2; var3++) {
         if (!param0.merge(var3 / this.secondDiv, var3 / this.firstDiv, var3)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.result.size();
   }

   @Override
   public DoubleList getList() {
      return this.result;
   }
}
