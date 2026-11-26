package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SimpleRandomSelectorFeature extends Feature<SimpleRandomFeatureConfiguration> {
   public SimpleRandomSelectorFeature(Codec<SimpleRandomFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<SimpleRandomFeatureConfiguration> param0) {
      RandomSource var2 = param0.random();
      SimpleRandomFeatureConfiguration var3 = param0.config();
      WorldGenLevel var4 = param0.level();
      BlockPos var5 = param0.origin();
      ChunkGenerator var6 = param0.chunkGenerator();
      int var7 = var2.nextInt(var3.features.size());
      PlacedFeature var8 = var3.features.get(var7).value();
      return var8.place(var4, var6, var2, var5);
   }
}
