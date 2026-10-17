package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class NoiseThresholdCountPlacement extends RepeatingPlacement {
   public static final Codec<NoiseThresholdCountPlacement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.DOUBLE.fieldOf("noise_level").forGetter(l_param0 -> l_param0.noiseLevel),
            Codec.INT.fieldOf("below_noise").forGetter(l_param0 -> l_param0.belowNoise),
            Codec.INT.fieldOf("above_noise").forGetter(l_param0 -> l_param0.aboveNoise)
         )
         .apply(param0, NoiseThresholdCountPlacement::new)
   );
   private final double noiseLevel;
   private final int belowNoise;
   private final int aboveNoise;

   private NoiseThresholdCountPlacement(double param0, int param2, int param3) {
      this.noiseLevel = param0;
      this.belowNoise = param2;
      this.aboveNoise = param3;
   }

   public static NoiseThresholdCountPlacement of(double param0, int param2, int param3) {
      return new NoiseThresholdCountPlacement(param0, param2, param3);
   }

   @Override
   protected int count(RandomSource param0, BlockPos param1) {
      double var3 = Biome.BIOME_INFO_NOISE.getValue(param1.getX() / 200.0, param1.getZ() / 200.0, false);
      return var3 < this.noiseLevel ? this.belowNoise : this.aboveNoise;
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.NOISE_THRESHOLD_COUNT;
   }
}
