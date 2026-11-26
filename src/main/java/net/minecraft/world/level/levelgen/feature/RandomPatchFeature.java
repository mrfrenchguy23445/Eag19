package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

public class RandomPatchFeature extends Feature<RandomPatchConfiguration> {
   public RandomPatchFeature(Codec<RandomPatchConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<RandomPatchConfiguration> param0) {
      RandomPatchConfiguration var2 = param0.config();
      RandomSource var3 = param0.random();
      BlockPos var4 = param0.origin();
      WorldGenLevel var5 = param0.level();
      int var6 = 0;
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
      int var8 = var2.xzSpread() + 1;
      int var9 = var2.ySpread() + 1;

      for (int var10 = 0; var10 < var2.tries(); var10++) {
         var7.setWithOffset(var4, var3.nextInt(var8) - var3.nextInt(var8), var3.nextInt(var9) - var3.nextInt(var9), var3.nextInt(var8) - var3.nextInt(var8));
         if (var2.feature().value().place(var5, param0.chunkGenerator(), var3, var7)) {
            var6++;
         }
      }

      return var6 > 0;
   }
}
