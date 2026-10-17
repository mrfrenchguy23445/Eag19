package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ProbabilityFeatureConfiguration implements FeatureConfiguration {
   public static final Codec<ProbabilityFeatureConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(l_param0 -> l_param0.probability))
         .apply(param0, ProbabilityFeatureConfiguration::new)
   );
   public final float probability;

   public ProbabilityFeatureConfiguration(float param0) {
      this.probability = param0;
   }
}
