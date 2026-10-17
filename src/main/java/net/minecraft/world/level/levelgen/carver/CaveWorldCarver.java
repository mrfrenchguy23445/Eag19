package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
   public CaveWorldCarver(Codec<CaveCarverConfiguration> param0) {
      super(param0);
   }

   public boolean isStartChunk(CaveCarverConfiguration param0, RandomSource param1) {
      return param1.nextFloat() <= param0.probability;
   }

   public boolean carve(
      CarvingContext param0,
      CaveCarverConfiguration param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      RandomSource param4,
      Aquifer param5,
      ChunkPos param6,
      CarvingMask param7
   ) {
      int var9 = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int var10 = param4.nextInt(param4.nextInt(param4.nextInt(this.getCaveBound()) + 1) + 1);

      for (int var11 = 0; var11 < var10; var11++) {
         double var12 = param6.getBlockX(param4.nextInt(16));
         double var14 = param1.y.sample(param4, param0);
         double var16 = param6.getBlockZ(param4.nextInt(16));
         double var18 = param1.horizontalRadiusMultiplier.sample(param4);
         double var20 = param1.verticalRadiusMultiplier.sample(param4);
         double var22 = param1.floorLevel.sample(param4);
         WorldCarver.CarveSkipChecker var24 = (l_param2, l_param3, l_param5, l_param7, l_param9) -> shouldSkip(l_param3, l_param5, l_param7, var22);
         int var25 = 1;
         if (param4.nextInt(4) == 0) {
            double var26 = param1.yScale.sample(param4);
            float var28 = 1.0F + param4.nextFloat() * 6.0F;
            this.createRoom(param0, param1, param2, param3, param5, var12, var14, var16, var28, var26, param7, var24);
            var25 += param4.nextInt(4);
         }

         for (int var26 = 0; var26 < var25; var26++) {
            float var27 = param4.nextFloat() * (float) (Math.PI * 2);
            float var28 = (param4.nextFloat() - 0.5F) / 4.0F;
            float var29 = this.getThickness(param4);
            int var30 = var9 - param4.nextInt(var9 / 4);
            int var31 = 0;
            this.createTunnel(
               param0,
               param1,
               param2,
               param3,
               param4.nextLong(),
               param5,
               var12,
               var14,
               var16,
               var18,
               var20,
               var29,
               var27,
               var28,
               0,
               var30,
               this.getYScale(),
               param7,
               var24
            );
         }
      }

      return true;
   }

   protected int getCaveBound() {
      return 15;
   }

   protected float getThickness(RandomSource param0) {
      float var2 = param0.nextFloat() * 2.0F + param0.nextFloat();
      if (param0.nextInt(10) == 0) {
         var2 *= param0.nextFloat() * param0.nextFloat() * 3.0F + 1.0F;
      }

      return var2;
   }

   protected double getYScale() {
      return 1.0;
   }

   protected void createRoom(
      CarvingContext param0,
      CaveCarverConfiguration param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      Aquifer param4,
      double param5,
      double param7,
      double param9,
      float param11,
      double param12,
      CarvingMask param14,
      WorldCarver.CarveSkipChecker param15
   ) {
      double var17 = 1.5 + Mth.sin((float) (Math.PI / 2)) * param11;
      double var19 = var17 * param12;
      this.carveEllipsoid(param0, param1, param2, param3, param4, param5 + 1.0, param7, param9, var17, var19, param14, param15);
   }

   protected void createTunnel(
      CarvingContext param0,
      CaveCarverConfiguration param1,
      ChunkAccess param2,
      Function<BlockPos, Holder<Biome>> param3,
      long param4,
      Aquifer param6,
      double param7,
      double param9,
      double param11,
      double param13,
      double param15,
      float param17,
      float param18,
      float param19,
      int param20,
      int param21,
      double param22,
      CarvingMask param24,
      WorldCarver.CarveSkipChecker param25
   ) {
      RandomSource var27 = RandomSource.create(param4);
      int var28 = var27.nextInt(param21 / 2) + param21 / 4;
      boolean var29 = var27.nextInt(6) == 0;
      float var30 = 0.0F;
      float var31 = 0.0F;

      for (int var32 = param20; var32 < param21; var32++) {
         double var33 = 1.5 + Mth.sin((float) Math.PI * var32 / param21) * param17;
         double var35 = var33 * param22;
         float var37 = Mth.cos(param19);
         param7 += Mth.cos(param18) * var37;
         param9 += Mth.sin(param19);
         param11 += Mth.sin(param18) * var37;
         param19 *= var29 ? 0.92F : 0.7F;
         param19 += var31 * 0.1F;
         param18 += var30 * 0.1F;
         var31 *= 0.9F;
         var30 *= 0.75F;
         var31 += (var27.nextFloat() - var27.nextFloat()) * var27.nextFloat() * 2.0F;
         var30 += (var27.nextFloat() - var27.nextFloat()) * var27.nextFloat() * 4.0F;
         if (var32 == var28 && param17 > 1.0F) {
            this.createTunnel(
               param0,
               param1,
               param2,
               param3,
               var27.nextLong(),
               param6,
               param7,
               param9,
               param11,
               param13,
               param15,
               var27.nextFloat() * 0.5F + 0.5F,
               param18 - (float) (Math.PI / 2),
               param19 / 3.0F,
               var32,
               param21,
               1.0,
               param24,
               param25
            );
            this.createTunnel(
               param0,
               param1,
               param2,
               param3,
               var27.nextLong(),
               param6,
               param7,
               param9,
               param11,
               param13,
               param15,
               var27.nextFloat() * 0.5F + 0.5F,
               param18 + (float) (Math.PI / 2),
               param19 / 3.0F,
               var32,
               param21,
               1.0,
               param24,
               param25
            );
            return;
         }

         if (var27.nextInt(4) != 0) {
            if (!canReach(param2.getPos(), param7, param11, var32, param21, param17)) {
               return;
            }

            this.carveEllipsoid(param0, param1, param2, param3, param6, param7, param9, param11, var33 * param13, var35 * param15, param24, param25);
         }
      }
   }

   private static boolean shouldSkip(double param0, double param2, double param4, double param6) {
      return param2 <= param6 ? true : param0 * param0 + param2 * param2 + param4 * param4 >= 1.0;
   }
}
