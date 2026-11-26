package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;

public class RandomSelectorFeature extends Feature<RandomFeatureConfiguration> {
   public RandomSelectorFeature(Codec<RandomFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<RandomFeatureConfiguration> param0) {
      RandomFeatureConfiguration var2 = param0.config();
      RandomSource var3 = param0.random();
      WorldGenLevel var4 = param0.level();
      ChunkGenerator var5 = param0.chunkGenerator();
      BlockPos var6 = param0.origin();

      for (WeightedPlacedFeature var8 : var2.features) {
         if (var3.nextFloat() < var8.chance) {
            return var8.place(var4, var5, var3, var6);
         }
      }

      return var2.defaultFeature.value().place(var4, var5, var3, var6);
   }
}
