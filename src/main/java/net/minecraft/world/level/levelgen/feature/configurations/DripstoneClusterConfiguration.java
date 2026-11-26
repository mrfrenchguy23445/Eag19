package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

public class DripstoneClusterConfiguration implements FeatureConfiguration {
   public static final Codec<DripstoneClusterConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").forGetter(l_param0 -> l_param0.floorToCeilingSearchRange),
            IntProvider.codec(1, 128).fieldOf("height").forGetter(l_param0 -> l_param0.height),
            IntProvider.codec(1, 128).fieldOf("radius").forGetter(l_param0 -> l_param0.radius),
            Codec.intRange(0, 64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter(l_param0 -> l_param0.maxStalagmiteStalactiteHeightDiff),
            Codec.intRange(1, 64).fieldOf("height_deviation").forGetter(l_param0 -> l_param0.heightDeviation),
            IntProvider.codec(0, 128).fieldOf("dripstone_block_layer_thickness").forGetter(l_param0 -> l_param0.dripstoneBlockLayerThickness),
            FloatProvider.codec(0.0F, 2.0F).fieldOf("density").forGetter(l_param0 -> l_param0.density),
            FloatProvider.codec(0.0F, 2.0F).fieldOf("wetness").forGetter(l_param0 -> l_param0.wetness),
            Codec.floatRange(0.0F, 1.0F)
               .fieldOf("chance_of_dripstone_column_at_max_distance_from_center")
               .forGetter(l_param0 -> l_param0.chanceOfDripstoneColumnAtMaxDistanceFromCenter),
            Codec.intRange(1, 64)
               .fieldOf("max_distance_from_edge_affecting_chance_of_dripstone_column")
               .forGetter(l_param0 -> l_param0.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn),
            Codec.intRange(1, 64)
               .fieldOf("max_distance_from_center_affecting_height_bias")
               .forGetter(l_param0 -> l_param0.maxDistanceFromCenterAffectingHeightBias)
         )
         .apply(param0, DripstoneClusterConfiguration::new)
   );
   public final int floorToCeilingSearchRange;
   public final IntProvider height;
   public final IntProvider radius;
   public final int maxStalagmiteStalactiteHeightDiff;
   public final int heightDeviation;
   public final IntProvider dripstoneBlockLayerThickness;
   public final FloatProvider density;
   public final FloatProvider wetness;
   public final float chanceOfDripstoneColumnAtMaxDistanceFromCenter;
   public final int maxDistanceFromEdgeAffectingChanceOfDripstoneColumn;
   public final int maxDistanceFromCenterAffectingHeightBias;

   public DripstoneClusterConfiguration(
      int param0,
      IntProvider param1,
      IntProvider param2,
      int param3,
      int param4,
      IntProvider param5,
      FloatProvider param6,
      FloatProvider param7,
      float param8,
      int param9,
      int param10
   ) {
      this.floorToCeilingSearchRange = param0;
      this.height = param1;
      this.radius = param2;
      this.maxStalagmiteStalactiteHeightDiff = param3;
      this.heightDeviation = param4;
      this.dripstoneBlockLayerThickness = param5;
      this.density = param6;
      this.wetness = param7;
      this.chanceOfDripstoneColumnAtMaxDistanceFromCenter = param8;
      this.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn = param9;
      this.maxDistanceFromCenterAffectingHeightBias = param10;
   }
}
