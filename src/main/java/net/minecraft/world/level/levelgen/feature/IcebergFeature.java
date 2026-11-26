package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.material.Material;

public class IcebergFeature extends Feature<BlockStateConfiguration> {
   public IcebergFeature(Codec<BlockStateConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockStateConfiguration> param0) {
      BlockPos var2 = param0.origin();
      WorldGenLevel var3 = param0.level();
      var2 = new BlockPos(var2.getX(), param0.chunkGenerator().getSeaLevel(), var2.getZ());
      RandomSource var4 = param0.random();
      boolean var5 = var4.nextDouble() > 0.7;
      BlockState var6 = param0.config().state;
      double var7 = var4.nextDouble() * 2.0 * Math.PI;
      int var9 = 11 - var4.nextInt(5);
      int var10 = 3 + var4.nextInt(3);
      boolean var11 = var4.nextDouble() > 0.7;
      int var12 = 11;
      int var13 = var11 ? var4.nextInt(6) + 6 : var4.nextInt(15) + 3;
      if (!var11 && var4.nextDouble() > 0.9) {
         var13 += var4.nextInt(19) + 7;
      }

      int var14 = Math.min(var13 + var4.nextInt(11), 18);
      int var15 = Math.min(var13 + var4.nextInt(7) - var4.nextInt(5), 11);
      int var16 = var11 ? var9 : 11;

      for (int var17 = -var16; var17 < var16; var17++) {
         for (int var18 = -var16; var18 < var16; var18++) {
            for (int var19 = 0; var19 < var13; var19++) {
               int var20 = var11 ? this.heightDependentRadiusEllipse(var19, var13, var15) : this.heightDependentRadiusRound(var4, var19, var13, var15);
               if (var11 || var17 < var20) {
                  this.generateIcebergBlock(var3, var4, var2, var13, var17, var19, var18, var20, var16, var11, var10, var7, var5, var6);
               }
            }
         }
      }

      this.smooth(var3, var2, var15, var13, var11, var9);

      for (int var17 = -var16; var17 < var16; var17++) {
         for (int var18 = -var16; var18 < var16; var18++) {
            for (int var19 = -1; var19 > -var14; var19--) {
               int var20 = var11 ? Mth.ceil(var16 * (1.0F - (float)Math.pow(var19, 2.0) / (var14 * 8.0F))) : var16;
               int var21 = this.heightDependentRadiusSteep(var4, -var19, var14, var15);
               if (var17 < var21) {
                  this.generateIcebergBlock(var3, var4, var2, var14, var17, var19, var18, var21, var20, var11, var10, var7, var5, var6);
               }
            }
         }
      }

      boolean var17 = var11 ? var4.nextDouble() > 0.1 : var4.nextDouble() > 0.7;
      if (var17) {
         this.generateCutOut(var4, var3, var15, var13, var2, var11, var9, var7, var10);
      }

      return true;
   }

   private void generateCutOut(
      RandomSource param0, LevelAccessor param1, int param2, int param3, BlockPos param4, boolean param5, int param6, double param7, int param9
   ) {
      int var11 = param0.nextBoolean() ? -1 : 1;
      int var12 = param0.nextBoolean() ? -1 : 1;
      int var13 = param0.nextInt(Math.max(param2 / 2 - 2, 1));
      if (param0.nextBoolean()) {
         var13 = param2 / 2 + 1 - param0.nextInt(Math.max(param2 - param2 / 2 - 1, 1));
      }

      int var14 = param0.nextInt(Math.max(param2 / 2 - 2, 1));
      if (param0.nextBoolean()) {
         var14 = param2 / 2 + 1 - param0.nextInt(Math.max(param2 - param2 / 2 - 1, 1));
      }

      if (param5) {
         var13 = var14 = param0.nextInt(Math.max(param6 - 5, 1));
      }

      BlockPos var15 = new BlockPos(var11 * var13, 0, var12 * var14);
      double var16 = param5 ? param7 + (Math.PI / 2) : param0.nextDouble() * 2.0 * Math.PI;

      for (int var18 = 0; var18 < param3 - 3; var18++) {
         int var19 = this.heightDependentRadiusRound(param0, var18, param3, param2);
         this.carve(var19, var18, param4, param1, false, var16, var15, param6, param9);
      }

      for (int var18 = -1; var18 > -param3 + param0.nextInt(5); var18--) {
         int var19 = this.heightDependentRadiusSteep(param0, -var18, param3, param2);
         this.carve(var19, var18, param4, param1, true, var16, var15, param6, param9);
      }
   }

   private void carve(int param0, int param1, BlockPos param2, LevelAccessor param3, boolean param4, double param5, BlockPos param7, int param8, int param9) {
      int var11 = param0 + 1 + param8 / 3;
      int var12 = Math.min(param0 - 3, 3) + param9 / 2 - 1;

      for (int var13 = -var11; var13 < var11; var13++) {
         for (int var14 = -var11; var14 < var11; var14++) {
            double var15 = this.signedDistanceEllipse(var13, var14, param7, var11, var12, param5);
            if (var15 < 0.0) {
               BlockPos var17 = param2.offset(var13, param1, var14);
               BlockState var18 = param3.getBlockState(var17);
               if (isIcebergState(var18) || var18.is(Blocks.SNOW_BLOCK)) {
                  if (param4) {
                     this.setBlock(param3, var17, Blocks.WATER.defaultBlockState());
                  } else {
                     this.setBlock(param3, var17, Blocks.AIR.defaultBlockState());
                     this.removeFloatingSnowLayer(param3, var17);
                  }
               }
            }
         }
      }
   }

   private void removeFloatingSnowLayer(LevelAccessor param0, BlockPos param1) {
      if (param0.getBlockState(param1.above()).is(Blocks.SNOW)) {
         this.setBlock(param0, param1.above(), Blocks.AIR.defaultBlockState());
      }
   }

   private void generateIcebergBlock(
      LevelAccessor param0,
      RandomSource param1,
      BlockPos param2,
      int param3,
      int param4,
      int param5,
      int param6,
      int param7,
      int param8,
      boolean param9,
      int param10,
      double param11,
      boolean param13,
      BlockState param14
   ) {
      double var16 = param9
         ? this.signedDistanceEllipse(param4, param6, BlockPos.ZERO, param8, this.getEllipseC(param5, param3, param10), param11)
         : this.signedDistanceCircle(param4, param6, BlockPos.ZERO, param7, param1);
      if (var16 < 0.0) {
         BlockPos var18 = param2.offset(param4, param5, param6);
         double var19 = param9 ? -0.5 : -6 - param1.nextInt(3);
         if (var16 > var19 && param1.nextDouble() > 0.9) {
            return;
         }

         this.setIcebergBlock(var18, param0, param1, param3 - param5, param3, param9, param13, param14);
      }
   }

   private void setIcebergBlock(
      BlockPos param0, LevelAccessor param1, RandomSource param2, int param3, int param4, boolean param5, boolean param6, BlockState param7
   ) {
      BlockState var9 = param1.getBlockState(param0);
      if (var9.getMaterial() == Material.AIR || var9.is(Blocks.SNOW_BLOCK) || var9.is(Blocks.ICE) || var9.is(Blocks.WATER)) {
         boolean var10 = !param5 || param2.nextDouble() > 0.05;
         int var11 = param5 ? 3 : 2;
         if (param6 && !var9.is(Blocks.WATER) && param3 <= param2.nextInt(Math.max(1, param4 / var11)) + param4 * 0.6 && var10) {
            this.setBlock(param1, param0, Blocks.SNOW_BLOCK.defaultBlockState());
         } else {
            this.setBlock(param1, param0, param7);
         }
      }
   }

   private int getEllipseC(int param0, int param1, int param2) {
      int var4 = param2;
      if (param0 > 0 && param1 - param0 <= 3) {
         var4 -= 4 - (param1 - param0);
      }

      return var4;
   }

   private double signedDistanceCircle(int param0, int param1, BlockPos param2, int param3, RandomSource param4) {
      float var6 = 10.0F * Mth.clamp(param4.nextFloat(), 0.2F, 0.8F) / param3;
      return var6 + Math.pow(param0 - param2.getX(), 2.0) + Math.pow(param1 - param2.getZ(), 2.0) - Math.pow(param3, 2.0);
   }

   private double signedDistanceEllipse(int param0, int param1, BlockPos param2, int param3, int param4, double param5) {
      return Math.pow(((param0 - param2.getX()) * Math.cos(param5) - (param1 - param2.getZ()) * Math.sin(param5)) / param3, 2.0)
         + Math.pow(((param0 - param2.getX()) * Math.sin(param5) + (param1 - param2.getZ()) * Math.cos(param5)) / param4, 2.0)
         - 1.0;
   }

   private int heightDependentRadiusRound(RandomSource param0, int param1, int param2, int param3) {
      float var5 = 3.5F - param0.nextFloat();
      float var6 = (1.0F - (float)Math.pow(param1, 2.0) / (param2 * var5)) * param3;
      if (param2 > 15 + param0.nextInt(5)) {
         int var7 = param1 < 3 + param0.nextInt(6) ? param1 / 2 : param1;
         var6 = (1.0F - var7 / (param2 * var5 * 0.4F)) * param3;
      }

      return Mth.ceil(var6 / 2.0F);
   }

   private int heightDependentRadiusEllipse(int param0, int param1, int param2) {
      float var4 = 1.0F;
      float var5 = (1.0F - (float)Math.pow(param0, 2.0) / (param1 * 1.0F)) * param2;
      return Mth.ceil(var5 / 2.0F);
   }

   private int heightDependentRadiusSteep(RandomSource param0, int param1, int param2, int param3) {
      float var5 = 1.0F + param0.nextFloat() / 2.0F;
      float var6 = (1.0F - param1 / (param2 * var5)) * param3;
      return Mth.ceil(var6 / 2.0F);
   }

   private static boolean isIcebergState(BlockState param0) {
      return param0.is(Blocks.PACKED_ICE) || param0.is(Blocks.SNOW_BLOCK) || param0.is(Blocks.BLUE_ICE);
   }

   private boolean belowIsAir(BlockGetter param0, BlockPos param1) {
      return param0.getBlockState(param1.below()).getMaterial() == Material.AIR;
   }

   private void smooth(LevelAccessor param0, BlockPos param1, int param2, int param3, boolean param4, int param5) {
      int var7 = param4 ? param5 : param2 / 2;

      for (int var8 = -var7; var8 <= var7; var8++) {
         for (int var9 = -var7; var9 <= var7; var9++) {
            for (int var10 = 0; var10 <= param3; var10++) {
               BlockPos var11 = param1.offset(var8, var10, var9);
               BlockState var12 = param0.getBlockState(var11);
               if (isIcebergState(var12) || var12.is(Blocks.SNOW)) {
                  if (this.belowIsAir(param0, var11)) {
                     this.setBlock(param0, var11, Blocks.AIR.defaultBlockState());
                     this.setBlock(param0, var11.above(), Blocks.AIR.defaultBlockState());
                  } else if (isIcebergState(var12)) {
                     BlockState[] var13 = new BlockState[]{
                        param0.getBlockState(var11.west()),
                        param0.getBlockState(var11.east()),
                        param0.getBlockState(var11.north()),
                        param0.getBlockState(var11.south())
                     };
                     int var14 = 0;

                     for (BlockState var18 : var13) {
                        if (!isIcebergState(var18)) {
                           var14++;
                        }
                     }

                     if (var14 >= 3) {
                        this.setBlock(param0, var11, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            }
         }
      }
   }
}
