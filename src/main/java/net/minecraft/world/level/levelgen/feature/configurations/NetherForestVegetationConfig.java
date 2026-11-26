package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class NetherForestVegetationConfig extends BlockPileConfiguration {
   public static final Codec<NetherForestVegetationConfig> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(l_param0 -> l_param0.stateProvider),
            ExtraCodecs.POSITIVE_INT.fieldOf("spread_width").forGetter(l_param0 -> l_param0.spreadWidth),
            ExtraCodecs.POSITIVE_INT.fieldOf("spread_height").forGetter(l_param0 -> l_param0.spreadHeight)
         )
         .apply(param0, NetherForestVegetationConfig::new)
   );
   public final int spreadWidth;
   public final int spreadHeight;

   public NetherForestVegetationConfig(BlockStateProvider param0, int param1, int param2) {
      super(param0);
      this.spreadWidth = param1;
      this.spreadHeight = param2;
   }
}
