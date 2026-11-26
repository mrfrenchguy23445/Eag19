package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
   public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> param0) {
      super(param0);
   }

   protected void placeTrunk(
      LevelAccessor param0, RandomSource param1, BlockPos param2, HugeMushroomFeatureConfiguration param3, int param4, BlockPos.MutableBlockPos param5
   ) {
      for (int var7 = 0; var7 < param4; var7++) {
         param5.set(param2).move(Direction.UP, var7);
         if (!param0.getBlockState(param5).isSolidRender(param0, param5)) {
            this.setBlock(param0, param5, param3.stemProvider.getState(param1, param2));
         }
      }
   }

   protected int getTreeHeight(RandomSource param0) {
      int var2 = param0.nextInt(3) + 4;
      if (param0.nextInt(12) == 0) {
         var2 *= 2;
      }

      return var2;
   }

   protected boolean isValidPosition(
      LevelAccessor param0, BlockPos param1, int param2, BlockPos.MutableBlockPos param3, HugeMushroomFeatureConfiguration param4
   ) {
      int var6 = param1.getY();
      if (var6 >= param0.getMinBuildHeight() + 1 && var6 + param2 + 1 < param0.getMaxBuildHeight()) {
         BlockState var7 = param0.getBlockState(param1.below());
         if (!isDirt(var7) && !var7.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return false;
         }

         for (int var8 = 0; var8 <= param2; var8++) {
            int var9 = this.getTreeRadiusForHeight(-1, -1, param4.foliageRadius, var8);

            for (int var10 = -var9; var10 <= var9; var10++) {
               for (int var11 = -var9; var11 <= var9; var11++) {
                  BlockState var12 = param0.getBlockState(param3.setWithOffset(param1, var10, var8, var11));
                  if (!var12.isAir() && !var12.is(BlockTags.LEAVES)) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      HugeMushroomFeatureConfiguration var5 = param0.config();
      int var6 = this.getTreeHeight(var4);
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition(var2, var3, var6, var7, var5)) {
         return false;
      }

      this.makeCap(var2, var4, var3, var6, var7, var5);
      this.placeTrunk(var2, var4, var3, var5, var6, var7);
      return true;
   }

   protected abstract int getTreeRadiusForHeight(int var1, int var2, int var3, int var4);

   protected abstract void makeCap(
      LevelAccessor var1, RandomSource var2, BlockPos var3, int var4, BlockPos.MutableBlockPos var5, HugeMushroomFeatureConfiguration var6
   );
}
