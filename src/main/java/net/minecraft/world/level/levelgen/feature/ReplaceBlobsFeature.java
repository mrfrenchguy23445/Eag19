package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
   public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> param0) {
      ReplaceSphereConfiguration var2 = param0.config();
      WorldGenLevel var3 = param0.level();
      RandomSource var4 = param0.random();
      Block var5 = var2.targetState.getBlock();
      BlockPos var6 = findTarget(var3, param0.origin().mutable().clamp(Direction.Axis.Y, var3.getMinBuildHeight() + 1, var3.getMaxBuildHeight() - 1), var5);
      if (var6 == null) {
         return false;
      }

      int var7 = var2.radius().sample(var4);
      int var8 = var2.radius().sample(var4);
      int var9 = var2.radius().sample(var4);
      int var10 = Math.max(var7, Math.max(var8, var9));
      boolean var11 = false;

      for (BlockPos var13 : BlockPos.withinManhattan(var6, var7, var8, var9)) {
         if (var13.distManhattan(var6) > var10) {
            break;
         }

         BlockState var14 = var3.getBlockState(var13);
         if (var14.is(var5)) {
            this.setBlock(var3, var13, var2.replaceState);
            var11 = true;
         }
      }

      return var11;
   }

   @Nullable
   private static BlockPos findTarget(LevelAccessor param0, BlockPos.MutableBlockPos param1, Block param2) {
      while (param1.getY() > param0.getMinBuildHeight() + 1) {
         BlockState var3 = param0.getBlockState(param1);
         if (var3.is(param2)) {
            return param1;
         }

         param1.move(Direction.DOWN);
      }

      return null;
   }
}
