package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;
   private final double maxValue;

   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource param0, IntStream param1) {
      return new PerlinNoise(param0, makeAmplitudes(new IntRBTreeSet(param1.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   @Deprecated
   public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource param0, int param1, DoubleList param2) {
      return new PerlinNoise(param0, Pair.of(param1, param2), false);
   }

   public static PerlinNoise create(RandomSource param0, IntStream param1) {
      return create(param0, param1.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource param0, List<Integer> param1) {
      return new PerlinNoise(param0, makeAmplitudes(new IntRBTreeSet(param1)), true);
   }

   public static PerlinNoise create(RandomSource param0, int param1, double param2, double... param4) {
      DoubleArrayList var5 = new DoubleArrayList(param4);
      var5.add(0, param2);
      return new PerlinNoise(param0, Pair.of(param1, var5), true);
   }

   public static PerlinNoise create(RandomSource param0, int param1, DoubleList param2) {
      return new PerlinNoise(param0, Pair.of(param1, param2), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet param0) {
      if (param0.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      }

      int var1 = -param0.firstInt();
      int var2 = param0.lastInt();
      int var3 = var1 + var2 + 1;
      if (var3 < 1) {
         throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
      }

      DoubleList var4 = new DoubleArrayList(new double[var3]);
      IntBidirectionalIterator var5 = param0.iterator();

      while (var5.hasNext()) {
         int var6 = var5.nextInt();
         var4.set(var6 + var1, 1.0);
      }

      return Pair.of(-var1, var4);
   }

   protected PerlinNoise(RandomSource param0, Pair<Integer, DoubleList> param1, boolean param2) {
      this.firstOctave = param1.getFirst();
      this.amplitudes = param1.getSecond();
      int var4 = this.amplitudes.size();
      int var5 = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[var4];
      if (param2) {
         PositionalRandomFactory var6 = param0.forkPositional();

         for (int var7 = 0; var7 < var4; var7++) {
            if (this.amplitudes.getDouble(var7) != 0.0) {
               int var8 = this.firstOctave + var7;
               this.noiseLevels[var7] = new ImprovedNoise(var6.fromHashOf("octave_" + var8));
            }
         }
      } else {
         ImprovedNoise var6 = new ImprovedNoise(param0);
         if (var5 >= 0 && var5 < var4) {
            double var7 = this.amplitudes.getDouble(var5);
            if (var7 != 0.0) {
               this.noiseLevels[var5] = var6;
            }
         }

         for (int var7 = var5 - 1; var7 >= 0; var7--) {
            if (var7 < var4) {
               double var8 = this.amplitudes.getDouble(var7);
               if (var8 != 0.0) {
                  this.noiseLevels[var7] = new ImprovedNoise(param0);
               } else {
                  skipOctave(param0);
               }
            } else {
               skipOctave(param0);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(l_param0 -> l_param0 != 0.0).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if (var5 < var4 - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0, -var5);
      this.lowestFreqValueFactor = Math.pow(2.0, var4 - 1) / (Math.pow(2.0, var4) - 1.0);
      this.maxValue = this.edgeValue(2.0);
   }

   protected double maxValue() {
      return this.maxValue;
   }

   private static void skipOctave(RandomSource param0) {
      param0.consumeCount(262);
   }

   public double getValue(double param0, double param2, double param4) {
      return this.getValue(param0, param2, param4, 0.0, 0.0, false);
   }

   @Deprecated
   public double getValue(double param0, double param2, double param4, double param6, double param8, boolean param10) {
      double var12 = 0.0;
      double var14 = this.lowestFreqInputFactor;
      double var16 = this.lowestFreqValueFactor;

      for (int var18 = 0; var18 < this.noiseLevels.length; var18++) {
         ImprovedNoise var19 = this.noiseLevels[var18];
         if (var19 != null) {
            double var20 = var19.noise(wrap(param0 * var14), param10 ? -var19.yo : wrap(param2 * var14), wrap(param4 * var14), param6 * var14, param8 * var14);
            var12 += this.amplitudes.getDouble(var18) * var20 * var16;
         }

         var14 *= 2.0;
         var16 /= 2.0;
      }

      return var12;
   }

   public double maxBrokenValue(double param0) {
      return this.edgeValue(param0 + 2.0);
   }

   private double edgeValue(double param0) {
      double var3 = 0.0;
      double var5 = this.lowestFreqValueFactor;

      for (int var7 = 0; var7 < this.noiseLevels.length; var7++) {
         ImprovedNoise var8 = this.noiseLevels[var7];
         if (var8 != null) {
            var3 += this.amplitudes.getDouble(var7) * param0 * var5;
         }

         var5 /= 2.0;
      }

      return var3;
   }

   @Nullable
   public ImprovedNoise getOctaveNoise(int param0) {
      return this.noiseLevels[this.noiseLevels.length - 1 - param0];
   }

   public static double wrap(double param0) {
      return param0 - Mth.lfloor(param0 / 3.3554432E7 + 0.5) * 3.3554432E7;
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder param0) {
      param0.append("PerlinNoise{");
      List<String> var2 = this.amplitudes.stream().map(l_param0 -> String.format(Locale.ROOT, "%.2f", l_param0)).toList();
      param0.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(var2).append(", noise levels: [");

      for (int var3 = 0; var3 < this.noiseLevels.length; var3++) {
         param0.append(var3).append(": ");
         ImprovedNoise var4 = this.noiseLevels[var3];
         if (var4 == null) {
            param0.append("null");
         } else {
            var4.parityConfigString(param0);
         }

         param0.append(", ");
      }

      param0.append("]");
      param0.append("}");
   }
}
