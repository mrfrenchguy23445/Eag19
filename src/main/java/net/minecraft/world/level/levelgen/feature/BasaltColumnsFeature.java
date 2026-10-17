package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;

public class BasaltColumnsFeature extends Feature<ColumnFeatureConfiguration> {
   private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of(
      Blocks.LAVA,
      Blocks.BEDROCK,
      Blocks.MAGMA_BLOCK,
      Blocks.SOUL_SAND,
      Blocks.NETHER_BRICKS,
      Blocks.NETHER_BRICK_FENCE,
      Blocks.NETHER_BRICK_STAIRS,
      Blocks.NETHER_WART,
      Blocks.CHEST,
      Blocks.SPAWNER
   );
   private static final int CLUSTERED_REACH = 5;
   private static final int CLUSTERED_SIZE = 50;
   private static final int UNCLUSTERED_REACH = 8;
   private static final int UNCLUSTERED_SIZE = 15;

   public BasaltColumnsFeature(Codec<ColumnFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<ColumnFeatureConfiguration> param0) {
      int var2 = param0.chunkGenerator().getSeaLevel();
      BlockPos var3 = param0.origin();
      WorldGenLevel var4 = param0.level();
      RandomSource var5 = param0.random();
      ColumnFeatureConfiguration var6 = param0.config();
      if (!canPlaceAt(var4, var2, var3.mutable())) {
         return false;
      }

      int var7 = var6.height().sample(var5);
      boolean var8 = var5.nextFloat() < 0.9F;
      int var9 = Math.min(var7, var8 ? 5 : 8);
      int var10 = var8 ? 50 : 15;
      boolean var11 = false;

      for (BlockPos var13 : BlockPos.randomBetweenClosed(
         var5, var10, var3.getX() - var9, var3.getY(), var3.getZ() - var9, var3.getX() + var9, var3.getY(), var3.getZ() + var9
      )) {
         int var14 = var7 - var13.distManhattan(var3);
         if (var14 >= 0) {
            var11 |= this.placeColumn(var4, var2, var13, var14, var6.reach().sample(var5));
         }
      }

      return var11;
   }

   private boolean placeColumn(LevelAccessor param0, int param1, BlockPos param2, int param3, int param4) {
      boolean var6 = false;

      for (BlockPos var8 : BlockPos.betweenClosed(
         param2.getX() - param4, param2.getY(), param2.getZ() - param4, param2.getX() + param4, param2.getY(), param2.getZ() + param4
      )) {
         int var9 = var8.distManhattan(param2);
         BlockPos var10 = isAirOrLavaOcean(param0, param1, var8) ? findSurface(param0, param1, var8.mutable(), var9) : findAir(param0, var8.mutable(), var9);
         if (var10 != null) {
            int var11 = param3 - var9 / 2;
            BlockPos.MutableBlockPos var12 = var10.mutable();

            while (var11 >= 0) {
               if (isAirOrLavaOcean(param0, param1, var12)) {
                  this.setBlock(param0, var12, Blocks.BASALT.defaultBlockState());
                  var12.move(Direction.UP);
                  var6 = true;
               } else {
                  if (!param0.getBlockState(var12).is(Blocks.BASALT)) {
                     break;
                  }

                  var12.move(Direction.UP);
               }

               var11--;
            }
         }
      }

      return var6;
   }

   @Nullable
   private static BlockPos findSurface(LevelAccessor param0, int param1, BlockPos.MutableBlockPos param2, int param3) {
      while (param2.getY() > param0.getMinBuildHeight() + 1 && param3 > 0) {
         param3--;
         if (canPlaceAt(param0, param1, param2)) {
            return param2;
         }

         param2.move(Direction.DOWN);
      }

      return null;
   }

   private static boolean canPlaceAt(LevelAccessor param0, int param1, BlockPos.MutableBlockPos param2) {
      if (!isAirOrLavaOcean(param0, param1, param2)) {
         return false;
      }

      BlockState var3 = param0.getBlockState(param2.move(Direction.DOWN));
      param2.move(Direction.UP);
      return !var3.isAir() && !CANNOT_PLACE_ON.contains(var3.getBlock());
   }

   @Nullable
   private static BlockPos findAir(LevelAccessor param0, BlockPos.MutableBlockPos param1, int param2) {
      while (param1.getY() < param0.getMaxBuildHeight() && param2 > 0) {
         param2--;
         BlockState var3 = param0.getBlockState(param1);
         if (CANNOT_PLACE_ON.contains(var3.getBlock())) {
            return null;
         }

         if (var3.isAir()) {
            return param1;
         }

         param1.move(Direction.UP);
      }

      return null;
   }

   private static boolean isAirOrLavaOcean(LevelAccessor param0, int param1, BlockPos param2) {
      BlockState var3 = param0.getBlockState(param2);
      return var3.isAir() || var3.is(Blocks.LAVA) && param2.getY() <= param1;
   }
}
