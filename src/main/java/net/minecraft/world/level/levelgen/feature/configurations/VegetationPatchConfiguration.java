package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class VegetationPatchConfiguration implements FeatureConfiguration {
   public static final Codec<VegetationPatchConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("replaceable").forGetter(l_param0 -> l_param0.replaceable),
            BlockStateProvider.CODEC.fieldOf("ground_state").forGetter(l_param0 -> l_param0.groundState),
            PlacedFeature.CODEC.fieldOf("vegetation_feature").forGetter(l_param0 -> l_param0.vegetationFeature),
            CaveSurface.CODEC.fieldOf("surface").forGetter(l_param0 -> l_param0.surface),
            IntProvider.codec(1, 128).fieldOf("depth").forGetter(l_param0 -> l_param0.depth),
            Codec.floatRange(0.0F, 1.0F).fieldOf("extra_bottom_block_chance").forGetter(l_param0 -> l_param0.extraBottomBlockChance),
            Codec.intRange(1, 256).fieldOf("vertical_range").forGetter(l_param0 -> l_param0.verticalRange),
            Codec.floatRange(0.0F, 1.0F).fieldOf("vegetation_chance").forGetter(l_param0 -> l_param0.vegetationChance),
            IntProvider.CODEC.fieldOf("xz_radius").forGetter(l_param0 -> l_param0.xzRadius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("extra_edge_column_chance").forGetter(l_param0 -> l_param0.extraEdgeColumnChance)
         )
         .apply(param0, VegetationPatchConfiguration::new)
   );
   public final TagKey<Block> replaceable;
   public final BlockStateProvider groundState;
   public final Holder<PlacedFeature> vegetationFeature;
   public final CaveSurface surface;
   public final IntProvider depth;
   public final float extraBottomBlockChance;
   public final int verticalRange;
   public final float vegetationChance;
   public final IntProvider xzRadius;
   public final float extraEdgeColumnChance;

   public VegetationPatchConfiguration(
      TagKey<Block> param0,
      BlockStateProvider param1,
      Holder<PlacedFeature> param2,
      CaveSurface param3,
      IntProvider param4,
      float param5,
      int param6,
      float param7,
      IntProvider param8,
      float param9
   ) {
      this.replaceable = param0;
      this.groundState = param1;
      this.vegetationFeature = param2;
      this.surface = param3;
      this.depth = param4;
      this.extraBottomBlockChance = param5;
      this.verticalRange = param6;
      this.vegetationChance = param7;
      this.xzRadius = param8;
      this.extraEdgeColumnChance = param9;
   }
}
