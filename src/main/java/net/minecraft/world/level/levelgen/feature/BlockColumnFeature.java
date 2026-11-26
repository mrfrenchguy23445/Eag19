package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;

public class BlockColumnFeature extends Feature<BlockColumnConfiguration> {
   public BlockColumnFeature(Codec<BlockColumnConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockColumnConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockColumnConfiguration var3 = param0.config();
      RandomSource var4 = param0.random();
      int var5 = var3.layers().size();
      int[] var6 = new int[var5];
      int var7 = 0;

      for (int var8 = 0; var8 < var5; var8++) {
         var6[var8] = var3.layers().get(var8).height().sample(var4);
         var7 += var6[var8];
      }

      if (var7 == 0) {
         return false;
      }

      BlockPos.MutableBlockPos var8 = param0.origin().mutable();
      BlockPos.MutableBlockPos var9 = var8.mutable().move(var3.direction());

      for (int var10 = 0; var10 < var7; var10++) {
         if (!var3.allowedPlacement().test(var2, var9)) {
            truncate(var6, var7, var10, var3.prioritizeTip());
            break;
         }

         var9.move(var3.direction());
      }

      for (int var10 = 0; var10 < var5; var10++) {
         int var11 = var6[var10];
         if (var11 != 0) {
            BlockColumnConfiguration.Layer var12 = var3.layers().get(var10);

            for (int var13 = 0; var13 < var11; var13++) {
               var2.setBlock(var8, var12.state().getState(var4, var8), 2);
               var8.move(var3.direction());
            }
         }
      }

      return true;
   }

   private static void truncate(int[] param0, int param1, int param2, boolean param3) {
      int var4 = param1 - param2;
      int var5 = param3 ? 1 : -1;
      int var6 = param3 ? 0 : param0.length - 1;
      int var7 = param3 ? param0.length : -1;

      for (int var8 = var6; var8 != var7 && var4 > 0; var8 += var5) {
         int var9 = param0[var8];
         int var10 = Math.min(var9, var4);
         var4 -= var10;
         param0[var8] -= var10;
      }
   }
}
