package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature extends Feature<DiskConfiguration> {
   public DiskFeature(Codec<DiskConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<DiskConfiguration> param0) {
      DiskConfiguration var2 = param0.config();
      BlockPos var3 = param0.origin();
      WorldGenLevel var4 = param0.level();
      RandomSource var5 = param0.random();
      boolean var6 = false;
      int var7 = var3.getY();
      int var8 = var7 + var2.halfHeight();
      int var9 = var7 - var2.halfHeight() - 1;
      int var10 = var2.radius().sample(var5);
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();

      for (BlockPos var13 : BlockPos.betweenClosed(var3.offset(-var10, 0, -var10), var3.offset(var10, 0, var10))) {
         int var14 = var13.getX() - var3.getX();
         int var15 = var13.getZ() - var3.getZ();
         if (var14 * var14 + var15 * var15 <= var10 * var10) {
            var6 |= this.placeColumn(var2, var4, var5, var8, var9, var11.set(var13));
         }
      }

      return var6;
   }

   protected boolean placeColumn(DiskConfiguration param0, WorldGenLevel param1, RandomSource param2, int param3, int param4, BlockPos.MutableBlockPos param5) {
      boolean var7 = false;
      BlockState var8 = null;

      for (int var9 = param3; var9 > param4; var9--) {
         param5.setY(var9);
         if (param0.target().test(param1, param5)) {
            BlockState var10 = param0.stateProvider().getState(param1, param2, param5);
            param1.setBlock(param5, var10, 2);
            this.markAboveForPostProcessing(param1, param5);
            var7 = true;
         }
      }

      return var7;
   }
}
