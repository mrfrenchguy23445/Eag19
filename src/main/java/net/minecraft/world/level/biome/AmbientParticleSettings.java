package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public class AmbientParticleSettings {
   public static final Codec<AmbientParticleSettings> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ParticleTypes.CODEC.fieldOf("options").forGetter(l_param0 -> l_param0.options),
            Codec.FLOAT.fieldOf("probability").forGetter(l_param0 -> l_param0.probability)
         )
         .apply(param0, AmbientParticleSettings::new)
   );
   private final ParticleOptions options;
   private final float probability;

   public AmbientParticleSettings(ParticleOptions param0, float param1) {
      this.options = param0;
      this.probability = param1;
   }

   public ParticleOptions getOptions() {
      return this.options;
   }

   public boolean canSpawn(RandomSource param0) {
      return param0.nextFloat() <= this.probability;
   }
}
