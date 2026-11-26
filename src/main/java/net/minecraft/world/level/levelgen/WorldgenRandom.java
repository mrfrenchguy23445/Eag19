package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(RandomSource param0) {
      super(0L);
      this.randomSource = param0;
   }

   public int getCount() {
      return this.count;
   }

   @Override
   public RandomSource fork() {
      return this.randomSource.fork();
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return this.randomSource.forkPositional();
   }

   @Override
   public int next(int param0) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource var2 ? var2.next(param0) : (int)(this.randomSource.nextLong() >>> 64 - param0);
   }

   @Override
   public synchronized void setSeed(long param0) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(param0);
      }
   }

   public long setDecorationSeed(long param0, int param2, int param3) {
      this.setSeed(param0);
      long var5 = this.nextLong() | 1L;
      long var7 = this.nextLong() | 1L;
      long var9 = param2 * var5 + param3 * var7 ^ param0;
      this.setSeed(var9);
      return var9;
   }

   public void setFeatureSeed(long param0, int param2, int param3) {
      long var5 = param0 + param2 + 10000 * param3;
      this.setSeed(var5);
   }

   public void setLargeFeatureSeed(long param0, int param2, int param3) {
      this.setSeed(param0);
      long var5 = this.nextLong();
      long var7 = this.nextLong();
      long var9 = param2 * var5 ^ param3 * var7 ^ param0;
      this.setSeed(var9);
   }

   public void setLargeFeatureWithSalt(long param0, int param2, int param3, int param4) {
      long var6 = param2 * 341873128712L + param3 * 132897987541L + param0 + param4;
      this.setSeed(var6);
   }

   public static RandomSource seedSlimeChunk(int param0, int param1, long param2, long param4) {
      return RandomSource.create(param2 + param0 * param0 * 4987142 + param0 * 5947611 + param1 * param1 * 4392871L + param1 * 389711 ^ param4);
   }

   public enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      Algorithm(LongFunction<RandomSource> param2) {
         this.constructor = param2;
      }

      public RandomSource newInstance(long param0) {
         return this.constructor.apply(param0);
      }
   }
}
