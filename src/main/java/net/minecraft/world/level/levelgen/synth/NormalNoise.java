package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;

public class NormalNoise {
   private static final double INPUT_FACTOR = 1.0181268882175227;
   private static final double TARGET_DEVIATION = 0.3333333333333333;
   private final double valueFactor;
   private final PerlinNoise first;
   private final PerlinNoise second;
   private final double maxValue;
   private final NormalNoise.NoiseParameters parameters;

   @Deprecated
   public static NormalNoise createLegacyNetherBiome(RandomSource param0, NormalNoise.NoiseParameters param1) {
      return new NormalNoise(param0, param1, false);
   }

   public static NormalNoise create(RandomSource param0, int param1, double... param2) {
      return create(param0, new NormalNoise.NoiseParameters(param1, new DoubleArrayList(param2)));
   }

   public static NormalNoise create(RandomSource param0, NormalNoise.NoiseParameters param1) {
      return new NormalNoise(param0, param1, true);
   }

   private NormalNoise(RandomSource param0, NormalNoise.NoiseParameters param1, boolean param2) {
      int var4 = param1.firstOctave;
      DoubleList var5 = param1.amplitudes;
      this.parameters = param1;
      if (param2) {
         this.first = PerlinNoise.create(param0, var4, var5);
         this.second = PerlinNoise.create(param0, var4, var5);
      } else {
         this.first = PerlinNoise.createLegacyForLegacyNetherBiome(param0, var4, var5);
         this.second = PerlinNoise.createLegacyForLegacyNetherBiome(param0, var4, var5);
      }

      int var6 = Integer.MAX_VALUE;
      int var7 = Integer.MIN_VALUE;
      DoubleListIterator var8 = var5.iterator();

      while (var8.hasNext()) {
         int var9 = var8.nextIndex();
         double var10 = var8.nextDouble();
         if (var10 != 0.0) {
            var6 = Math.min(var6, var9);
            var7 = Math.max(var7, var9);
         }
      }

      this.valueFactor = 0.16666666666666666 / expectedDeviation(var7 - var6);
      this.maxValue = (this.first.maxValue() + this.second.maxValue()) * this.valueFactor;
   }

   public double maxValue() {
      return this.maxValue;
   }

   private static double expectedDeviation(int param0) {
      return 0.1 * (1.0 + 1.0 / (param0 + 1));
   }

   public double getValue(double param0, double param2, double param4) {
      double var7 = param0 * 1.0181268882175227;
      double var9 = param2 * 1.0181268882175227;
      double var11 = param4 * 1.0181268882175227;
      return (this.first.getValue(param0, param2, param4) + this.second.getValue(var7, var9, var11)) * this.valueFactor;
   }

   public NormalNoise.NoiseParameters parameters() {
      return this.parameters;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder param0) {
      param0.append("NormalNoise {");
      param0.append("first: ");
      this.first.parityConfigString(param0);
      param0.append(", second: ");
      this.second.parityConfigString(param0);
      param0.append("}");
   }

   public record NoiseParameters(int firstOctave, DoubleList amplitudes) {
      public static final Codec<NormalNoise.NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise.NoiseParameters::firstOctave),
               Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise.NoiseParameters::amplitudes)
            )
            .apply(param0, NormalNoise.NoiseParameters::new)
      );
      public static final Codec<Holder<NormalNoise.NoiseParameters>> CODEC = RegistryFileCodec.create(Registries.NOISE, DIRECT_CODEC);

      public NoiseParameters(int param0, List<Double> param1) {
         this(param0, new DoubleArrayList(param1));
      }

      public NoiseParameters(int param0, double param1, double... param3) {
         this(param0, Util.make(new DoubleArrayList(param3), l_param2 -> l_param2.add(0, param1)));
      }
   }
}
