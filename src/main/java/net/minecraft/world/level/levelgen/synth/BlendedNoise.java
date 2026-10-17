package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
   private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001, 1000.0);
   private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
      param0 -> param0.group(
            SCALE_RANGE.fieldOf("xz_scale").forGetter(l_param0 -> l_param0.xzScale),
            SCALE_RANGE.fieldOf("y_scale").forGetter(l_param0 -> l_param0.yScale),
            SCALE_RANGE.fieldOf("xz_factor").forGetter(l_param0 -> l_param0.xzFactor),
            SCALE_RANGE.fieldOf("y_factor").forGetter(l_param0 -> l_param0.yFactor),
            Codec.doubleRange(1.0, 8.0).fieldOf("smear_scale_multiplier").forGetter(l_param0 -> l_param0.smearScaleMultiplier)
         )
         .apply(param0, BlendedNoise::createUnseeded)
   );
   public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;
   private final double xzMultiplier;
   private final double yMultiplier;
   private final double xzFactor;
   private final double yFactor;
   private final double smearScaleMultiplier;
   private final double maxValue;
   private final double xzScale;
   private final double yScale;

   public static BlendedNoise createUnseeded(double param0, double param2, double param4, double param6, double param8) {
      return new BlendedNoise(new XoroshiroRandomSource(0L), param0, param2, param4, param6, param8);
   }

   private BlendedNoise(PerlinNoise param0, PerlinNoise param1, PerlinNoise param2, double param3, double param5, double param7, double param9, double param11) {
      this.minLimitNoise = param0;
      this.maxLimitNoise = param1;
      this.mainNoise = param2;
      this.xzScale = param3;
      this.yScale = param5;
      this.xzFactor = param7;
      this.yFactor = param9;
      this.smearScaleMultiplier = param11;
      this.xzMultiplier = 684.412 * this.xzScale;
      this.yMultiplier = 684.412 * this.yScale;
      this.maxValue = param0.maxBrokenValue(this.yMultiplier);
   }

   @VisibleForTesting
   public BlendedNoise(RandomSource param0, double param1, double param3, double param5, double param7, double param9) {
      this(
         PerlinNoise.createLegacyForBlendedNoise(param0, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(param0, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(param0, IntStream.rangeClosed(-7, 0)),
         param1,
         param3,
         param5,
         param7,
         param9
      );
   }

   public BlendedNoise withNewRandom(RandomSource param0) {
      return new BlendedNoise(param0, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
   }

   @Override
   public double compute(DensityFunction.FunctionContext param0) {
      double var2 = param0.blockX() * this.xzMultiplier;
      double var4 = param0.blockY() * this.yMultiplier;
      double var6 = param0.blockZ() * this.xzMultiplier;
      double var8 = var2 / this.xzFactor;
      double var10 = var4 / this.yFactor;
      double var12 = var6 / this.xzFactor;
      double var14 = this.yMultiplier * this.smearScaleMultiplier;
      double var16 = var14 / this.yFactor;
      double var18 = 0.0;
      double var20 = 0.0;
      double var22 = 0.0;
      boolean var24 = true;
      double var25 = 1.0;

      for (int var27 = 0; var27 < 8; var27++) {
         ImprovedNoise var28 = this.mainNoise.getOctaveNoise(var27);
         if (var28 != null) {
            var22 += var28.noise(PerlinNoise.wrap(var8 * var25), PerlinNoise.wrap(var10 * var25), PerlinNoise.wrap(var12 * var25), var16 * var25, var10 * var25)
               / var25;
         }

         var25 /= 2.0;
      }

      double var27 = (var22 / 10.0 + 1.0) / 2.0;
      boolean var29 = var27 >= 1.0;
      boolean var30 = var27 <= 0.0;
      var25 = 1.0;

      for (int var31 = 0; var31 < 16; var31++) {
         double var32 = PerlinNoise.wrap(var2 * var25);
         double var34 = PerlinNoise.wrap(var4 * var25);
         double var36 = PerlinNoise.wrap(var6 * var25);
         double var38 = var14 * var25;
         if (!var29) {
            ImprovedNoise var40 = this.minLimitNoise.getOctaveNoise(var31);
            if (var40 != null) {
               var18 += var40.noise(var32, var34, var36, var38, var4 * var25) / var25;
            }
         }

         if (!var30) {
            ImprovedNoise var40 = this.maxLimitNoise.getOctaveNoise(var31);
            if (var40 != null) {
               var20 += var40.noise(var32, var34, var36, var38, var4 * var25) / var25;
            }
         }

         var25 /= 2.0;
      }

      return Mth.clampedLerp(var18 / 512.0, var20 / 512.0, var27) / 128.0;
   }

   @Override
   public double minValue() {
      return -this.maxValue();
   }

   @Override
   public double maxValue() {
      return this.maxValue;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder param0) {
      param0.append("BlendedNoise{minLimitNoise=");
      this.minLimitNoise.parityConfigString(param0);
      param0.append(", maxLimitNoise=");
      this.maxLimitNoise.parityConfigString(param0);
      param0.append(", mainNoise=");
      this.mainNoise.parityConfigString(param0);
      param0.append(
            String.format(
               Locale.ROOT,
               ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8",
               684.412,
               684.412,
               8.555150000000001,
               4.277575000000001
            )
         )
         .append('}');
   }

   @Override
   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return CODEC;
   }
}
