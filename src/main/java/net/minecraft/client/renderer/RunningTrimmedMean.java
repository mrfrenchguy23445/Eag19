package net.minecraft.client.renderer;

public class RunningTrimmedMean {
   private final long[] values;
   private int count;
   private int cursor;

   public RunningTrimmedMean(int param0) {
      this.values = new long[param0];
   }

   public long registerValueAndGetMean(long param0) {
      if (this.count < this.values.length) {
         this.count++;
      }

      this.values[this.cursor] = param0;
      this.cursor = (this.cursor + 1) % this.values.length;
      long var3 = Long.MAX_VALUE;
      long var5 = Long.MIN_VALUE;
      long var7 = 0L;

      for (int var9 = 0; var9 < this.count; var9++) {
         long var10 = this.values[var9];
         var7 += var10;
         var3 = Math.min(var3, var10);
         var5 = Math.max(var5, var10);
      }

      if (this.count > 2) {
         var7 -= var3 + var5;
         return var7 / (this.count - 2);
      } else {
         return var7 > 0L ? this.count / var7 : 0L;
      }
   }
}
