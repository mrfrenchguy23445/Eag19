package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
   public BlockPileFeature(Codec<BlockPileConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockPileConfiguration> param0) {
      BlockPos var2 = param0.origin();
      WorldGenLevel var3 = param0.level();
      RandomSource var4 = param0.random();
      BlockPileConfiguration var5 = param0.config();
      if (var2.getY() < var3.getMinBuildHeight() + 5) {
         return false;
      }

      int var6 = 2 + var4.nextInt(2);
      int var7 = 2 + var4.nextInt(2);

      for (BlockPos var9 : BlockPos.betweenClosed(var2.offset(-var6, 0, -var7), var2.offset(var6, 1, var7))) {
         int var10 = var2.getX() - var9.getX();
         int var11 = var2.getZ() - var9.getZ();
         if (var10 * var10 + var11 * var11 <= var4.nextFloat() * 10.0F - var4.nextFloat() * 6.0F) {
            this.tryPlaceBlock(var3, var9, var4, var5);
         } else if (var4.nextFloat() < 0.031) {
            this.tryPlaceBlock(var3, var9, var4, var5);
         }
      }

      return true;
   }

   private boolean mayPlaceOn(LevelAccessor param0, BlockPos param1, RandomSource param2) {
      BlockPos var4 = param1.below();
      BlockState var5 = param0.getBlockState(var4);
      return var5.is(Blocks.DIRT_PATH) ? param2.nextBoolean() : var5.isFaceSturdy(param0, var4, Direction.UP);
   }

   private void tryPlaceBlock(LevelAccessor param0, BlockPos param1, RandomSource param2, BlockPileConfiguration param3) {
      if (param0.isEmptyBlock(param1) && this.mayPlaceOn(param0, param1, param2)) {
         param0.setBlock(param1, param3.stateProvider.getState(param2, param1), 4);
      }
   }
}
