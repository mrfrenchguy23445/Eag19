package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.Material;

public class HugeFungusFeature extends Feature<HugeFungusConfiguration> {
   private static final float HUGE_PROBABILITY = 0.06F;

   public HugeFungusFeature(Codec<HugeFungusConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeFungusConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      ChunkGenerator var5 = param0.chunkGenerator();
      HugeFungusConfiguration var6 = param0.config();
      Block var7 = var6.validBaseState.getBlock();
      BlockPos var8 = null;
      BlockState var9 = var2.getBlockState(var3.below());
      if (var9.is(var7)) {
         var8 = var3;
      }

      if (var8 == null) {
         return false;
      }

      int var10 = Mth.nextInt(var4, 4, 13);
      if (var4.nextInt(12) == 0) {
         var10 *= 2;
      }

      if (!var6.planted) {
         int var11 = var5.getGenDepth();
         if (var8.getY() + var10 + 1 >= var11) {
            return false;
         }
      }

      boolean var11 = !var6.planted && var4.nextFloat() < 0.06F;
      var2.setBlock(var3, Blocks.AIR.defaultBlockState(), 4);
      this.placeStem(var2, var4, var6, var8, var10, var11);
      this.placeHat(var2, var4, var6, var8, var10, var11);
      return true;
   }

   private static boolean isReplaceable(LevelAccessor param0, BlockPos param1, boolean param2) {
      return param0.isStateAtPosition(param1, l_param1 -> {
         Material l_var2 = l_param1.getMaterial();
         return l_param1.canBeReplaced() || param2 && l_var2 == Material.PLANT;
      });
   }

   private void placeStem(LevelAccessor param0, RandomSource param1, HugeFungusConfiguration param2, BlockPos param3, int param4, boolean param5) {
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
      BlockState var8 = param2.stemState;
      int var9 = param5 ? 1 : 0;

      for (int var10 = -var9; var10 <= var9; var10++) {
         for (int var11 = -var9; var11 <= var9; var11++) {
            boolean var12 = param5 && Mth.abs(var10) == var9 && Mth.abs(var11) == var9;

            for (int var13 = 0; var13 < param4; var13++) {
               var7.setWithOffset(param3, var10, var13, var11);
               if (isReplaceable(param0, var7, true)) {
                  if (param2.planted) {
                     if (!param0.getBlockState(var7.below()).isAir()) {
                        param0.destroyBlock(var7, true);
                     }

                     param0.setBlock(var7, var8, 3);
                  } else if (var12) {
                     if (param1.nextFloat() < 0.1F) {
                        this.setBlock(param0, var7, var8);
                     }
                  } else {
                     this.setBlock(param0, var7, var8);
                  }
               }
            }
         }
      }
   }

   private void placeHat(LevelAccessor param0, RandomSource param1, HugeFungusConfiguration param2, BlockPos param3, int param4, boolean param5) {
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
      boolean var8 = param2.hatState.is(Blocks.NETHER_WART_BLOCK);
      int var9 = Math.min(param1.nextInt(1 + param4 / 3) + 5, param4);
      int var10 = param4 - var9;

      for (int var11 = var10; var11 <= param4; var11++) {
         int var12 = var11 < param4 - param1.nextInt(3) ? 2 : 1;
         if (var9 > 8 && var11 < var10 + 4) {
            var12 = 3;
         }

         if (param5) {
            var12++;
         }

         for (int var13 = -var12; var13 <= var12; var13++) {
            for (int var14 = -var12; var14 <= var12; var14++) {
               boolean var15 = var13 == -var12 || var13 == var12;
               boolean var16 = var14 == -var12 || var14 == var12;
               boolean var17 = !var15 && !var16 && var11 != param4;
               boolean var18 = var15 && var16;
               boolean var19 = var11 < var10 + 3;
               var7.setWithOffset(param3, var13, var11, var14);
               if (isReplaceable(param0, var7, false)) {
                  if (param2.planted && !param0.getBlockState(var7.below()).isAir()) {
                     param0.destroyBlock(var7, true);
                  }

                  if (var19) {
                     if (!var17) {
                        this.placeHatDropBlock(param0, param1, var7, param2.hatState, var8);
                     }
                  } else if (var17) {
                     this.placeHatBlock(param0, param1, param2, var7, 0.1F, 0.2F, var8 ? 0.1F : 0.0F);
                  } else if (var18) {
                     this.placeHatBlock(param0, param1, param2, var7, 0.01F, 0.7F, var8 ? 0.083F : 0.0F);
                  } else {
                     this.placeHatBlock(param0, param1, param2, var7, 5.0E-4F, 0.98F, var8 ? 0.07F : 0.0F);
                  }
               }
            }
         }
      }
   }

   private void placeHatBlock(
      LevelAccessor param0, RandomSource param1, HugeFungusConfiguration param2, BlockPos.MutableBlockPos param3, float param4, float param5, float param6
   ) {
      if (param1.nextFloat() < param4) {
         this.setBlock(param0, param3, param2.decorState);
      } else if (param1.nextFloat() < param5) {
         this.setBlock(param0, param3, param2.hatState);
         if (param1.nextFloat() < param6) {
            tryPlaceWeepingVines(param3, param0, param1);
         }
      }
   }

   private void placeHatDropBlock(LevelAccessor param0, RandomSource param1, BlockPos param2, BlockState param3, boolean param4) {
      if (param0.getBlockState(param2.below()).is(param3.getBlock())) {
         this.setBlock(param0, param2, param3);
      } else if (param1.nextFloat() < 0.15) {
         this.setBlock(param0, param2, param3);
         if (param4 && param1.nextInt(11) == 0) {
            tryPlaceWeepingVines(param2, param0, param1);
         }
      }
   }

   private static void tryPlaceWeepingVines(BlockPos param0, LevelAccessor param1, RandomSource param2) {
      BlockPos.MutableBlockPos var3 = param0.mutable().move(Direction.DOWN);
      if (param1.isEmptyBlock(var3)) {
         int var4 = Mth.nextInt(param2, 1, 5);
         if (param2.nextInt(7) == 0) {
            var4 *= 2;
         }

         int var5 = 23;
         int var6 = 25;
         WeepingVinesFeature.placeWeepingVinesColumn(param1, param2, var3, var4, 23, 25);
      }
   }
}
