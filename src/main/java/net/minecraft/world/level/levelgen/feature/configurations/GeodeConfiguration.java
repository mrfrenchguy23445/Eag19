package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;

public class GeodeConfiguration implements FeatureConfiguration {
   public static final Codec<Double> CHANCE_RANGE = Codec.doubleRange(0.0, 1.0);
   public static final Codec<GeodeConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            GeodeBlockSettings.CODEC.fieldOf("blocks").forGetter(l_param0 -> l_param0.geodeBlockSettings),
            GeodeLayerSettings.CODEC.fieldOf("layers").forGetter(l_param0 -> l_param0.geodeLayerSettings),
            GeodeCrackSettings.CODEC.fieldOf("crack").forGetter(l_param0 -> l_param0.geodeCrackSettings),
            CHANCE_RANGE.fieldOf("use_potential_placements_chance").orElse(0.35).forGetter(l_param0 -> l_param0.usePotentialPlacementsChance),
            CHANCE_RANGE.fieldOf("use_alternate_layer0_chance").orElse(0.0).forGetter(l_param0 -> l_param0.useAlternateLayer0Chance),
            Codec.BOOL.fieldOf("placements_require_layer0_alternate").orElse(true).forGetter(l_param0 -> l_param0.placementsRequireLayer0Alternate),
            IntProvider.codec(1, 20).fieldOf("outer_wall_distance").orElse(UniformInt.of(4, 5)).forGetter(l_param0 -> l_param0.outerWallDistance),
            IntProvider.codec(1, 20).fieldOf("distribution_points").orElse(UniformInt.of(3, 4)).forGetter(l_param0 -> l_param0.distributionPoints),
            IntProvider.codec(0, 10).fieldOf("point_offset").orElse(UniformInt.of(1, 2)).forGetter(l_param0 -> l_param0.pointOffset),
            Codec.INT.fieldOf("min_gen_offset").orElse(-16).forGetter(l_param0 -> l_param0.minGenOffset),
            Codec.INT.fieldOf("max_gen_offset").orElse(16).forGetter(l_param0 -> l_param0.maxGenOffset),
            CHANCE_RANGE.fieldOf("noise_multiplier").orElse(0.05).forGetter(l_param0 -> l_param0.noiseMultiplier),
            Codec.INT.fieldOf("invalid_blocks_threshold").forGetter(l_param0 -> l_param0.invalidBlocksThreshold)
         )
         .apply(param0, GeodeConfiguration::new)
   );
   public final GeodeBlockSettings geodeBlockSettings;
   public final GeodeLayerSettings geodeLayerSettings;
   public final GeodeCrackSettings geodeCrackSettings;
   public final double usePotentialPlacementsChance;
   public final double useAlternateLayer0Chance;
   public final boolean placementsRequireLayer0Alternate;
   public final IntProvider outerWallDistance;
   public final IntProvider distributionPoints;
   public final IntProvider pointOffset;
   public final int minGenOffset;
   public final int maxGenOffset;
   public final double noiseMultiplier;
   public final int invalidBlocksThreshold;

   public GeodeConfiguration(
      GeodeBlockSettings param0,
      GeodeLayerSettings param1,
      GeodeCrackSettings param2,
      double param3,
      double param5,
      boolean param7,
      IntProvider param8,
      IntProvider param9,
      IntProvider param10,
      int param11,
      int param12,
      double param13,
      int param15
   ) {
      this.geodeBlockSettings = param0;
      this.geodeLayerSettings = param1;
      this.geodeCrackSettings = param2;
      this.usePotentialPlacementsChance = param3;
      this.useAlternateLayer0Chance = param5;
      this.placementsRequireLayer0Alternate = param7;
      this.outerWallDistance = param8;
      this.distributionPoints = param9;
      this.pointOffset = param10;
      this.minGenOffset = param11;
      this.maxGenOffset = param12;
      this.noiseMultiplier = param13;
      this.invalidBlocksThreshold = param15;
   }
}
