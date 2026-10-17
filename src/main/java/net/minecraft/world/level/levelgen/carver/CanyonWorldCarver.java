package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
   public CanyonWorldCarver(Codec<CanyonCarverConfiguration> param0) {
      super(param0);
   }

   public boolean isStartChunk(CanyonCarverConfiguration param0, RandomSource param1) {
      return param1.nextFloat() <= param0.probability;
   }

   public boolean carve(
      CarvingContext param0,
      CanyonCarverConfiguration param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      RandomSource param4,
      Aquifer param5,
      ChunkPos param6,
      CarvingMask param7
   ) {
      int var9 = (this.getRange() * 2 - 1) * 16;
      double var10 = param6.getBlockX(param4.nextInt(16));
      int var12 = param1.y.sample(param4, param0);
      double var13 = param6.getBlockZ(param4.nextInt(16));
      float var15 = param4.nextFloat() * (float) (Math.PI * 2);
      float var16 = param1.verticalRotation.sample(param4);
      double var17 = param1.yScale.sample(param4);
      float var19 = param1.shape.thickness.sample(param4);
      int var20 = (int)(var9 * param1.shape.distanceFactor.sample(param4));
      int var21 = 0;
      this.doCarve(param0, param1, param2, param3, param4.nextLong(), param5, var10, var12, var13, var19, var15, var16, 0, var20, var17, param7);
      return true;
   }

   private void doCarve(
      CarvingContext param0,
      CanyonCarverConfiguration param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      long param4,
      Aquifer param6,
      double param7,
      double param9,
      double param11,
      float param13,
      float param14,
      float param15,
      int param16,
      int param17,
      double param18,
      CarvingMask param20
   ) {
      RandomSource var22 = RandomSource.create(param4);
      float[] var23 = this.initWidthFactors(param0, param1, var22);
      float var24 = 0.0F;
      float var25 = 0.0F;

      for (int var26 = param16; var26 < param17; var26++) {
         double var27 = 1.5 + Mth.sin(var26 * (float) Math.PI / param17) * param13;
         double var29 = var27 * param18;
         var27 *= param1.shape.horizontalRadiusFactor.sample(var22);
         var29 = this.updateVerticalRadius(param1, var22, var29, param17, var26);
         float var31 = Mth.cos(param15);
         float var32 = Mth.sin(param15);
         param7 += Mth.cos(param14) * var31;
         param9 += var32;
         param11 += Mth.sin(param14) * var31;
         param15 *= 0.7F;
         param15 += var25 * 0.05F;
         param14 += var24 * 0.05F;
         var25 *= 0.8F;
         var24 *= 0.5F;
         var25 += (var22.nextFloat() - var22.nextFloat()) * var22.nextFloat() * 2.0F;
         var24 += (var22.nextFloat() - var22.nextFloat()) * var22.nextFloat() * 4.0F;
         if (var22.nextInt(4) != 0) {
            if (!canReach(param2.getPos(), param7, param11, var26, param17, param13)) {
               return;
            }

            this.carveEllipsoid(
               param0,
               param1,
               param2,
               param3,
               param6,
               param7,
               param9,
               param11,
               var27,
               var29,
               param20,
               (l_param1, l_param2, l_param4, l_param6, l_param8) -> this.shouldSkip(l_param1, var23, l_param2, l_param4, l_param6, l_param8)
            );
         }
      }
   }

   private float[] initWidthFactors(CarvingContext param0, CanyonCarverConfiguration param1, RandomSource param2) {
      int var4 = param0.getGenDepth();
      float[] var5 = new float[var4];
      float var6 = 1.0F;

      for (int var7 = 0; var7 < var4; var7++) {
         if (var7 == 0 || param2.nextInt(param1.shape.widthSmoothness) == 0) {
            var6 = 1.0F + param2.nextFloat() * param2.nextFloat();
         }

         var5[var7] = var6 * var6;
      }

      return var5;
   }

   private double updateVerticalRadius(CanyonCarverConfiguration param0, RandomSource param1, double param2, float param4, float param5) {
      float var7 = 1.0F - Mth.abs(0.5F - param5 / param4) * 2.0F;
      float var8 = param0.shape.verticalRadiusDefaultFactor + param0.shape.verticalRadiusCenterFactor * var7;
      return var8 * param2 * Mth.randomBetween(param1, 0.75F, 1.0F);
   }

   private boolean shouldSkip(CarvingContext param0, float[] param1, double param2, double param4, double param6, int param8) {
      int var10 = param8 - param0.getMinGenY();
      return (param2 * param2 + param6 * param6) * param1[var10 - 1] + param4 * param4 / 6.0 >= 1.0;
   }
}
