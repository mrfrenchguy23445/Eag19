package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record AboveRootPlacement(BlockStateProvider aboveRootProvider, float aboveRootPlacementChance) {
   public static final Codec<AboveRootPlacement> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BlockStateProvider.CODEC.fieldOf("above_root_provider").forGetter(l_param0 -> l_param0.aboveRootProvider),
            Codec.floatRange(0.0F, 1.0F).fieldOf("above_root_placement_chance").forGetter(l_param0 -> l_param0.aboveRootPlacementChance)
         )
         .apply(param0, AboveRootPlacement::new)
   );
}
