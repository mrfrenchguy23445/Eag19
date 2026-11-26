package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class UnderwaterMagmaConfiguration implements FeatureConfiguration {
   public static final Codec<UnderwaterMagmaConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.intRange(0, 512).fieldOf("floor_search_range").forGetter(l_param0 -> l_param0.floorSearchRange),
            Codec.intRange(0, 64).fieldOf("placement_radius_around_floor").forGetter(l_param0 -> l_param0.placementRadiusAroundFloor),
            Codec.floatRange(0.0F, 1.0F)
               .fieldOf("placement_probability_per_valid_position")
               .forGetter(l_param0 -> l_param0.placementProbabilityPerValidPosition)
         )
         .apply(param0, UnderwaterMagmaConfiguration::new)
   );
   public final int floorSearchRange;
   public final int placementRadiusAroundFloor;
   public final float placementProbabilityPerValidPosition;

   public UnderwaterMagmaConfiguration(int param0, int param1, float param2) {
      this.floorSearchRange = param0;
      this.placementRadiusAroundFloor = param1;
      this.placementProbabilityPerValidPosition = param2;
   }
}
