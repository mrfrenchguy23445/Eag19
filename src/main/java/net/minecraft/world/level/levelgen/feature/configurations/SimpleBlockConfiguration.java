package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockConfiguration(BlockStateProvider toPlace) implements FeatureConfiguration {
   public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(BlockStateProvider.CODEC.fieldOf("to_place").forGetter(l_param0 -> l_param0.toPlace)).apply(param0, SimpleBlockConfiguration::new)
   );
}
