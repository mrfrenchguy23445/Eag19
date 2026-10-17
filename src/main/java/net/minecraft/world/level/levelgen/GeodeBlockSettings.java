package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class GeodeBlockSettings {
   public final BlockStateProvider fillingProvider;
   public final BlockStateProvider innerLayerProvider;
   public final BlockStateProvider alternateInnerLayerProvider;
   public final BlockStateProvider middleLayerProvider;
   public final BlockStateProvider outerLayerProvider;
   public final List<BlockState> innerPlacements;
   public final TagKey<Block> cannotReplace;
   public final TagKey<Block> invalidBlocks;
   public static final Codec<GeodeBlockSettings> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BlockStateProvider.CODEC.fieldOf("filling_provider").forGetter(l_param0 -> l_param0.fillingProvider),
            BlockStateProvider.CODEC.fieldOf("inner_layer_provider").forGetter(l_param0 -> l_param0.innerLayerProvider),
            BlockStateProvider.CODEC.fieldOf("alternate_inner_layer_provider").forGetter(l_param0 -> l_param0.alternateInnerLayerProvider),
            BlockStateProvider.CODEC.fieldOf("middle_layer_provider").forGetter(l_param0 -> l_param0.middleLayerProvider),
            BlockStateProvider.CODEC.fieldOf("outer_layer_provider").forGetter(l_param0 -> l_param0.outerLayerProvider),
            ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("inner_placements").forGetter(l_param0 -> l_param0.innerPlacements),
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("cannot_replace").forGetter(l_param0 -> l_param0.cannotReplace),
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("invalid_blocks").forGetter(l_param0 -> l_param0.invalidBlocks)
         )
         .apply(param0, GeodeBlockSettings::new)
   );

   public GeodeBlockSettings(
      BlockStateProvider param0,
      BlockStateProvider param1,
      BlockStateProvider param2,
      BlockStateProvider param3,
      BlockStateProvider param4,
      List<BlockState> param5,
      TagKey<Block> param6,
      TagKey<Block> param7
   ) {
      this.fillingProvider = param0;
      this.innerLayerProvider = param1;
      this.alternateInnerLayerProvider = param2;
      this.middleLayerProvider = param3;
      this.outerLayerProvider = param4;
      this.innerPlacements = param5;
      this.cannotReplace = param6;
      this.invalidBlocks = param7;
   }
}
