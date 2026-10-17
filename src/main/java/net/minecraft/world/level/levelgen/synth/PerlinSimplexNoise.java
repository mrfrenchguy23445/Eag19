package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class PerlinSimplexNoise {
   private final SimplexNoise[] noiseLevels;
   private final double highestFreqValueFactor;
   private final double highestFreqInputFactor;

   public PerlinSimplexNoise(RandomSource param0, List<Integer> param1) {
      this(param0, new IntRBTreeSet(param1));
   }

   private PerlinSimplexNoise(RandomSource param0, IntSortedSet param1) {
      if (param1.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      }

      int var3 = -param1.firstInt();
      int var4 = param1.lastInt();
      int var5 = var3 + var4 + 1;
      if (var5 < 1) {
         throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
      }

      SimplexNoise var6 = new SimplexNoise(param0);
      int var7 = var4;
      this.noiseLevels = new SimplexNoise[var5];
      if (var7 >= 0 && var7 < var5 && param1.contains(0)) {
         this.noiseLevels[var7] = var6;
      }

      for (int var8 = var7 + 1; var8 < var5; var8++) {
         if (var8 >= 0 && param1.contains(var7 - var8)) {
            this.noiseLevels[var8] = new SimplexNoise(param0);
         } else {
            param0.consumeCount(262);
         }
      }

      if (var4 > 0) {
         long var8 = (long)(var6.getValue(var6.xo, var6.yo, var6.zo) * 9.223372E18F);
         RandomSource var10 = new WorldgenRandom(new LegacyRandomSource(var8));

         for (int var11 = var7 - 1; var11 >= 0; var11--) {
            if (var11 < var5 && param1.contains(var7 - var11)) {
               this.noiseLevels[var11] = new SimplexNoise(var10);
            } else {
               var10.consumeCount(262);
            }
         }
      }

      this.highestFreqInputFactor = Math.pow(2.0, var4);
      this.highestFreqValueFactor = 1.0 / (Math.pow(2.0, var5) - 1.0);
   }

   public double getValue(double param0, double param2, boolean param4) {
      double var6 = 0.0;
      double var8 = this.highestFreqInputFactor;
      double var10 = this.highestFreqValueFactor;

      for (SimplexNoise var15 : this.noiseLevels) {
         if (var15 != null) {
            var6 += var15.getValue(param0 * var8 + (param4 ? var15.xo : 0.0), param2 * var8 + (param4 ? var15.yo : 0.0)) * var10;
         }

         var8 /= 2.0;
         var10 *= 2.0;
      }

      return var6;
   }
}
