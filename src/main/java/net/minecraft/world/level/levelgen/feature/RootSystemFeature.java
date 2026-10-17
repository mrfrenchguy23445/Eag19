package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
   public RootSystemFeature(Codec<RootSystemConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<RootSystemConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      if (!var2.getBlockState(var3).isAir()) {
         return false;
      }

      RandomSource var4 = param0.random();
      BlockPos var5 = param0.origin();
      RootSystemConfiguration var6 = param0.config();
      BlockPos.MutableBlockPos var7 = var5.mutable();
      if (placeDirtAndTree(var2, param0.chunkGenerator(), var6, var4, var7, var5)) {
         placeRoots(var2, var6, var4, var5, var7);
      }

      return true;
   }

   private static boolean spaceForTree(WorldGenLevel param0, RootSystemConfiguration param1, BlockPos param2) {
      BlockPos.MutableBlockPos var3 = param2.mutable();

      for (int var4 = 1; var4 <= param1.requiredVerticalSpaceForTree; var4++) {
         var3.move(Direction.UP);
         BlockState var5 = param0.getBlockState(var3);
         if (!isAllowedTreeSpace(var5, var4, param1.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(BlockState param0, int param1, int param2) {
      if (param0.isAir()) {
         return true;
      }

      int var3 = param1 + 1;
      return var3 <= param2 && param0.getFluidState().is(FluidTags.WATER);
   }

   private static boolean placeDirtAndTree(
      WorldGenLevel param0, ChunkGenerator param1, RootSystemConfiguration param2, RandomSource param3, BlockPos.MutableBlockPos param4, BlockPos param5
   ) {
      for (int var6 = 0; var6 < param2.rootColumnMaxHeight; var6++) {
         param4.move(Direction.UP);
         if (param2.allowedTreePosition.test(param0, param4) && spaceForTree(param0, param2, param4)) {
            BlockPos var7 = param4.below();
            if (param0.getFluidState(var7).is(FluidTags.LAVA) || !param0.getBlockState(var7).getMaterial().isSolid()) {
               return false;
            }

            if (param2.treeFeature.value().place(param0, param1, param3, param4)) {
               placeDirt(param5, param5.getY() + var6, param0, param2, param3);
               return true;
            }
         }
      }

      return false;
   }

   private static void placeDirt(BlockPos param0, int param1, WorldGenLevel param2, RootSystemConfiguration param3, RandomSource param4) {
      int var5 = param0.getX();
      int var6 = param0.getZ();
      BlockPos.MutableBlockPos var7 = param0.mutable();

      for (int var8 = param0.getY(); var8 < param1; var8++) {
         placeRootedDirt(param2, param3, param4, var5, var6, var7.set(var5, var8, var6));
      }
   }

   private static void placeRootedDirt(
      WorldGenLevel param0, RootSystemConfiguration param1, RandomSource param2, int param3, int param4, BlockPos.MutableBlockPos param5
   ) {
      int var6 = param1.rootRadius;
      Predicate<BlockState> var7 = l_param1 -> l_param1.is(param1.rootReplaceable);

      for (int var8 = 0; var8 < param1.rootPlacementAttempts; var8++) {
         param5.setWithOffset(param5, param2.nextInt(var6) - param2.nextInt(var6), 0, param2.nextInt(var6) - param2.nextInt(var6));
         if (var7.test(param0.getBlockState(param5))) {
            param0.setBlock(param5, param1.rootStateProvider.getState(param2, param5), 2);
         }

         param5.setX(param3);
         param5.setZ(param4);
      }
   }

   private static void placeRoots(WorldGenLevel param0, RootSystemConfiguration param1, RandomSource param2, BlockPos param3, BlockPos.MutableBlockPos param4) {
      int var5 = param1.hangingRootRadius;
      int var6 = param1.hangingRootsVerticalSpan;

      for (int var7 = 0; var7 < param1.hangingRootPlacementAttempts; var7++) {
         param4.setWithOffset(
            param3, param2.nextInt(var5) - param2.nextInt(var5), param2.nextInt(var6) - param2.nextInt(var6), param2.nextInt(var5) - param2.nextInt(var5)
         );
         if (param0.isEmptyBlock(param4)) {
            BlockState var8 = param1.hangingRootStateProvider.getState(param2, param4);
            if (var8.canSurvive(param0, param4) && param0.getBlockState(param4.above()).isFaceSturdy(param0, param4, Direction.DOWN)) {
               param0.setBlock(param4, var8, 2);
            }
         }
      }
   }
}
