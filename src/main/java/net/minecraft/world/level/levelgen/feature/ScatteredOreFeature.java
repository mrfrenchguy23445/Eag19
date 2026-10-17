package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature extends Feature<OreConfiguration> {
   private static final int MAX_DIST_FROM_ORIGIN = 7;

   ScatteredOreFeature(Codec<OreConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      RandomSource var3 = param0.random();
      OreConfiguration var4 = param0.config();
      BlockPos var5 = param0.origin();
      int var6 = var3.nextInt(var4.size + 1);
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for (int var8 = 0; var8 < var6; var8++) {
         this.offsetTargetPos(var7, var3, var5, Math.min(var8, 7));
         BlockState var9 = var2.getBlockState(var7);

         for (OreConfiguration.TargetBlockState var11 : var4.targetStates) {
            if (OreFeature.canPlaceOre(var9, var2::getBlockState, var3, var4, var11, var7)) {
               var2.setBlock(var7, var11.state, 2);
               break;
            }
         }
      }

      return true;
   }

   private void offsetTargetPos(BlockPos.MutableBlockPos param0, RandomSource param1, BlockPos param2, int param3) {
      int var5 = this.getRandomPlacementInOneAxisRelativeToOrigin(param1, param3);
      int var6 = this.getRandomPlacementInOneAxisRelativeToOrigin(param1, param3);
      int var7 = this.getRandomPlacementInOneAxisRelativeToOrigin(param1, param3);
      param0.setWithOffset(param2, var5, var6, var7);
   }

   private int getRandomPlacementInOneAxisRelativeToOrigin(RandomSource param0, int param1) {
      return Math.round((param0.nextFloat() - param0.nextFloat()) * param1);
   }
}
