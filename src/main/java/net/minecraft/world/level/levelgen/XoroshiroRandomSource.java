package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class XoroshiroRandomSource implements RandomSource {
   private static final float FLOAT_UNIT = 5.9604645E-8F;
   private static final double DOUBLE_UNIT = 1.110223E-16F;
   private Xoroshiro128PlusPlus randomNumberGenerator;
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public XoroshiroRandomSource(long param0) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(param0));
   }

   public XoroshiroRandomSource(long param0, long param2) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(param0, param2);
   }

   @Override
   public RandomSource fork() {
      return new XoroshiroRandomSource(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public void setSeed(long param0) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(param0));
      this.gaussianSource.reset();
   }

   @Override
   public int nextInt() {
      return (int)this.randomNumberGenerator.nextLong();
   }

   @Override
   public int nextInt(int param0) {
      if (param0 <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      }

      // TeaVM's classlib does not provide Integer.toUnsignedLong; masking the int to 32 bits
      // and widening is exactly equivalent, and keeps the result non-negative in a long.
      long var2 = this.nextInt() & 4294967295L;
      long var4 = var2 * param0;
      long var6 = var4 & 4294967295L;
      if (var6 < param0) {
         for (int var8 = Integer.remainderUnsigned(~param0 + 1, param0); var6 < var8; var6 = var4 & 4294967295L) {
            var2 = this.nextInt() & 4294967295L;
            var4 = var2 * param0;
         }
      }

      long var8 = var4 >> 32;
      return (int)var8;
   }

   @Override
   public long nextLong() {
      return this.randomNumberGenerator.nextLong();
   }

   @Override
   public boolean nextBoolean() {
      return (this.randomNumberGenerator.nextLong() & 1L) != 0L;
   }

   @Override
   public float nextFloat() {
      return (float)this.nextBits(24) * 5.9604645E-8F;
   }

   @Override
   public double nextDouble() {
      return this.nextBits(53) * 1.110223E-16F;
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }

   @Override
   public void consumeCount(int param0) {
      for (int var2 = 0; var2 < param0; var2++) {
         this.randomNumberGenerator.nextLong();
      }
   }

   private long nextBits(int param0) {
      return this.randomNumberGenerator.nextLong() >>> 64 - param0;
   }

   public static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
      private static final HashFunction MD5_128 = Hashing.md5();
      private final long seedLo;
      private final long seedHi;

      public XoroshiroPositionalRandomFactory(long param0, long param2) {
         this.seedLo = param0;
         this.seedHi = param2;
      }

      @Override
      public RandomSource at(int param0, int param1, int param2) {
         long var4 = Mth.getSeed(param0, param1, param2);
         long var6 = var4 ^ this.seedLo;
         return new XoroshiroRandomSource(var6, this.seedHi);
      }

      @Override
      public RandomSource fromHashOf(String param0) {
         byte[] var2 = MD5_128.hashString(param0, Charsets.UTF_8).asBytes();
         long var3 = Longs.fromBytes(var2[0], var2[1], var2[2], var2[3], var2[4], var2[5], var2[6], var2[7]);
         long var5 = Longs.fromBytes(var2[8], var2[9], var2[10], var2[11], var2[12], var2[13], var2[14], var2[15]);
         return new XoroshiroRandomSource(var3 ^ this.seedLo, var5 ^ this.seedHi);
      }

      @VisibleForTesting
      @Override
      public void parityConfigString(StringBuilder param0) {
         param0.append("seedLo: ").append(this.seedLo).append(", seedHi: ").append(this.seedHi);
      }
   }
}
