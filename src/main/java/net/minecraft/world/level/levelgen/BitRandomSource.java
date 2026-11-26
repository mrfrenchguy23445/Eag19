package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;

public interface BitRandomSource extends RandomSource {
   float FLOAT_MULTIPLIER = 5.9604645E-8F;
   double DOUBLE_MULTIPLIER = 1.110223E-16F;

   int next(int var1);

   @Override
   default int nextInt() {
      return this.next(32);
   }

   @Override
   default int nextInt(int param0) {
      if (param0 <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      }

      if ((param0 & param0 - 1) == 0) {
         return (int)((long)param0 * this.next(31) >> 31);
      }

      int var2;
      int var3;
      do {
         var2 = this.next(31);
         var3 = var2 % param0;
      } while (var2 - var3 + (param0 - 1) < 0);

      return var3;
   }

   @Override
   default long nextLong() {
      int var1 = this.next(32);
      int var2 = this.next(32);
      long var3 = (long)var1 << 32;
      return var3 + var2;
   }

   @Override
   default boolean nextBoolean() {
      return this.next(1) != 0;
   }

   @Override
   default float nextFloat() {
      return this.next(24) * 5.9604645E-8F;
   }

   @Override
   default double nextDouble() {
      int var1 = this.next(26);
      int var2 = this.next(27);
      long var3 = ((long)var1 << 27) + var2;
      return var3 * 1.110223E-16F;
   }
}
