package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

public class LargeDripstoneConfiguration implements FeatureConfiguration {
   public static final Codec<LargeDripstoneConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").orElse(30).forGetter(l_param0 -> l_param0.floorToCeilingSearchRange),
            IntProvider.codec(1, 60).fieldOf("column_radius").forGetter(l_param0 -> l_param0.columnRadius),
            FloatProvider.codec(0.0F, 20.0F).fieldOf("height_scale").forGetter(l_param0 -> l_param0.heightScale),
            Codec.floatRange(0.1F, 1.0F).fieldOf("max_column_radius_to_cave_height_ratio").forGetter(l_param0 -> l_param0.maxColumnRadiusToCaveHeightRatio),
            FloatProvider.codec(0.1F, 10.0F).fieldOf("stalactite_bluntness").forGetter(l_param0 -> l_param0.stalactiteBluntness),
            FloatProvider.codec(0.1F, 10.0F).fieldOf("stalagmite_bluntness").forGetter(l_param0 -> l_param0.stalagmiteBluntness),
            FloatProvider.codec(0.0F, 2.0F).fieldOf("wind_speed").forGetter(l_param0 -> l_param0.windSpeed),
            Codec.intRange(0, 100).fieldOf("min_radius_for_wind").forGetter(l_param0 -> l_param0.minRadiusForWind),
            Codec.floatRange(0.0F, 5.0F).fieldOf("min_bluntness_for_wind").forGetter(l_param0 -> l_param0.minBluntnessForWind)
         )
         .apply(param0, LargeDripstoneConfiguration::new)
   );
   public final int floorToCeilingSearchRange;
   public final IntProvider columnRadius;
   public final FloatProvider heightScale;
   public final float maxColumnRadiusToCaveHeightRatio;
   public final FloatProvider stalactiteBluntness;
   public final FloatProvider stalagmiteBluntness;
   public final FloatProvider windSpeed;
   public final int minRadiusForWind;
   public final float minBluntnessForWind;

   public LargeDripstoneConfiguration(
      int param0,
      IntProvider param1,
      FloatProvider param2,
      float param3,
      FloatProvider param4,
      FloatProvider param5,
      FloatProvider param6,
      int param7,
      float param8
   ) {
      this.floorToCeilingSearchRange = param0;
      this.columnRadius = param1;
      this.heightScale = param2;
      this.maxColumnRadiusToCaveHeightRatio = param3;
      this.stalactiteBluntness = param4;
      this.stalagmiteBluntness = param5;
      this.windSpeed = param6;
      this.minRadiusForWind = param7;
      this.minBluntnessForWind = param8;
   }
}
