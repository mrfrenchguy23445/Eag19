package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CarverConfiguration extends ProbabilityFeatureConfiguration {
   public static final MapCodec<CarverConfiguration> CODEC = RecordCodecBuilder.mapCodec(
      param0 -> param0.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(l_param0 -> l_param0.probability),
            HeightProvider.CODEC.fieldOf("y").forGetter(l_param0 -> l_param0.y),
            FloatProvider.CODEC.fieldOf("yScale").forGetter(l_param0 -> l_param0.yScale),
            VerticalAnchor.CODEC.fieldOf("lava_level").forGetter(l_param0 -> l_param0.lavaLevel),
            CarverDebugSettings.CODEC.optionalFieldOf("debug_settings", CarverDebugSettings.DEFAULT).forGetter(l_param0 -> l_param0.debugSettings),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(l_param0 -> l_param0.replaceable)
         )
         .apply(param0, CarverConfiguration::new)
   );
   public final HeightProvider y;
   public final FloatProvider yScale;
   public final VerticalAnchor lavaLevel;
   public final CarverDebugSettings debugSettings;
   public final HolderSet<Block> replaceable;

   public CarverConfiguration(
      float param0, HeightProvider param1, FloatProvider param2, VerticalAnchor param3, CarverDebugSettings param4, HolderSet<Block> param5
   ) {
      super(param0);
      this.y = param1;
      this.yScale = param2;
      this.lavaLevel = param3;
      this.debugSettings = param4;
      this.replaceable = param5;
   }
}
