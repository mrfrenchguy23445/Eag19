package net.minecraft.world.level.levelgen;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.RandomSource;

@Deprecated
public class ThreadSafeLegacyRandomSource implements BitRandomSource {
   private static final int MODULUS_BITS = 48;
   private static final long MODULUS_MASK = 281474976710655L;
   private static final long MULTIPLIER = 25214903917L;
   private static final long INCREMENT = 11L;
   private final AtomicLong seed = new AtomicLong();
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public ThreadSafeLegacyRandomSource(long param0) {
      this.setSeed(param0);
   }

   @Override
   public RandomSource fork() {
      return new ThreadSafeLegacyRandomSource(this.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
   }

   @Override
   public void setSeed(long param0) {
      this.seed.set((param0 ^ 25214903917L) & 281474976710655L);
   }

   @Override
   public int next(int param0) {
      long var2;
      long var4;
      do {
         var2 = this.seed.get();
         var4 = var2 * 25214903917L + 11L & 281474976710655L;
      } while (!this.seed.compareAndSet(var2, var4));

      return (int)(var4 >>> 48 - param0);
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }
}
