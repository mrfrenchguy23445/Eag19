package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;

public class GeodeCrackSettings {
   public static final Codec<GeodeCrackSettings> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            GeodeConfiguration.CHANCE_RANGE.fieldOf("generate_crack_chance").orElse(1.0).forGetter(l_param0 -> l_param0.generateCrackChance),
            Codec.doubleRange(0.0, 5.0).fieldOf("base_crack_size").orElse(2.0).forGetter(l_param0 -> l_param0.baseCrackSize),
            Codec.intRange(0, 10).fieldOf("crack_point_offset").orElse(2).forGetter(l_param0 -> l_param0.crackPointOffset)
         )
         .apply(param0, GeodeCrackSettings::new)
   );
   public final double generateCrackChance;
   public final double baseCrackSize;
   public final int crackPointOffset;

   public GeodeCrackSettings(double param0, double param2, int param4) {
      this.generateCrackChance = param0;
      this.baseCrackSize = param2;
      this.crackPointOffset = param4;
   }
}
