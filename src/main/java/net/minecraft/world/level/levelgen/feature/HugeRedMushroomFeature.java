package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   protected void makeCap(
      LevelAccessor param0, RandomSource param1, BlockPos param2, int param3, BlockPos.MutableBlockPos param4, HugeMushroomFeatureConfiguration param5
   ) {
      for (int var7 = param3 - 3; var7 <= param3; var7++) {
         int var8 = var7 < param3 ? param5.foliageRadius : param5.foliageRadius - 1;
         int var9 = param5.foliageRadius - 2;

         for (int var10 = -var8; var10 <= var8; var10++) {
            for (int var11 = -var8; var11 <= var8; var11++) {
               boolean var12 = var10 == -var8;
               boolean var13 = var10 == var8;
               boolean var14 = var11 == -var8;
               boolean var15 = var11 == var8;
               boolean var16 = var12 || var13;
               boolean var17 = var14 || var15;
               if (var7 >= param3 || var16 != var17) {
                  param4.setWithOffset(param2, var10, var7, var11);
                  if (!param0.getBlockState(param4).isSolidRender(param0, param4)) {
                     BlockState var18 = param5.capProvider.getState(param1, param2);
                     if (var18.hasProperty(HugeMushroomBlock.WEST)
                        && var18.hasProperty(HugeMushroomBlock.EAST)
                        && var18.hasProperty(HugeMushroomBlock.NORTH)
                        && var18.hasProperty(HugeMushroomBlock.SOUTH)
                        && var18.hasProperty(HugeMushroomBlock.UP)) {
                        var18 = var18.setValue(HugeMushroomBlock.UP, var7 >= param3 - 1)
                           .setValue(HugeMushroomBlock.WEST, var10 < -var9)
                           .setValue(HugeMushroomBlock.EAST, var10 > var9)
                           .setValue(HugeMushroomBlock.NORTH, var11 < -var9)
                           .setValue(HugeMushroomBlock.SOUTH, var11 > var9);
                     }

                     this.setBlock(param0, param4, var18);
                  }
               }
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int param0, int param1, int param2, int param3) {
      int var5 = 0;
      if (param3 < param1 && param3 >= param1 - 3) {
         var5 = param2;
      } else if (param3 == param1) {
         var5 = param2;
      }

      return var5;
   }
}
