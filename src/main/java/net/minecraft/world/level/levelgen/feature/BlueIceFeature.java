package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Material;

public class BlueIceFeature extends Feature<NoneFeatureConfiguration> {
   public BlueIceFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      BlockPos var2 = param0.origin();
      WorldGenLevel var3 = param0.level();
      RandomSource var4 = param0.random();
      if (var2.getY() > var3.getSeaLevel() - 1) {
         return false;
      }

      if (!var3.getBlockState(var2).is(Blocks.WATER) && !var3.getBlockState(var2.below()).is(Blocks.WATER)) {
         return false;
      }

      boolean var5 = false;

      for (Direction var9 : Direction.values()) {
         if (var9 != Direction.DOWN && var3.getBlockState(var2.relative(var9)).is(Blocks.PACKED_ICE)) {
            var5 = true;
            break;
         }
      }

      if (!var5) {
         return false;
      }

      var3.setBlock(var2, Blocks.BLUE_ICE.defaultBlockState(), 2);

      for (int var6 = 0; var6 < 200; var6++) {
         int var7 = var4.nextInt(5) - var4.nextInt(6);
         int var8 = 3;
         if (var7 < 2) {
            var8 += var7 / 2;
         }

         if (var8 >= 1) {
            BlockPos var9 = var2.offset(var4.nextInt(var8) - var4.nextInt(var8), var7, var4.nextInt(var8) - var4.nextInt(var8));
            BlockState var10 = var3.getBlockState(var9);
            if (var10.getMaterial() == Material.AIR || var10.is(Blocks.WATER) || var10.is(Blocks.PACKED_ICE) || var10.is(Blocks.ICE)) {
               for (Direction var14 : Direction.values()) {
                  BlockState var15 = var3.getBlockState(var9.relative(var14));
                  if (var15.is(Blocks.BLUE_ICE)) {
                     var3.setBlock(var9, Blocks.BLUE_ICE.defaultBlockState(), 2);
                     break;
                  }
               }
            }
         }
      }

      return true;
   }
}
