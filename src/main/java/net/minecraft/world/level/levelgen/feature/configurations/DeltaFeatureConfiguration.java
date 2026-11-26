package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;

public class DeltaFeatureConfiguration implements FeatureConfiguration {
   public static final Codec<DeltaFeatureConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BlockState.CODEC.fieldOf("contents").forGetter(l_param0 -> l_param0.contents),
            BlockState.CODEC.fieldOf("rim").forGetter(l_param0 -> l_param0.rim),
            IntProvider.codec(0, 16).fieldOf("size").forGetter(l_param0 -> l_param0.size),
            IntProvider.codec(0, 16).fieldOf("rim_size").forGetter(l_param0 -> l_param0.rimSize)
         )
         .apply(param0, DeltaFeatureConfiguration::new)
   );
   private final BlockState contents;
   private final BlockState rim;
   private final IntProvider size;
   private final IntProvider rimSize;

   public DeltaFeatureConfiguration(BlockState param0, BlockState param1, IntProvider param2, IntProvider param3) {
      this.contents = param0;
      this.rim = param1;
      this.size = param2;
      this.rimSize = param3;
   }

   public BlockState contents() {
      return this.contents;
   }

   public BlockState rim() {
      return this.rim;
   }

   public IntProvider size() {
      return this.size;
   }

   public IntProvider rimSize() {
      return this.rimSize;
   }
}
