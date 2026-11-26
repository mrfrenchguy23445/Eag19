package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class OreFeature extends Feature<OreConfiguration> {
   public OreFeature(Codec<OreConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> param0) {
      RandomSource var2 = param0.random();
      BlockPos var3 = param0.origin();
      WorldGenLevel var4 = param0.level();
      OreConfiguration var5 = param0.config();
      float var6 = var2.nextFloat() * (float) Math.PI;
      float var7 = var5.size / 8.0F;
      int var8 = Mth.ceil((var5.size / 16.0F * 2.0F + 1.0F) / 2.0F);
      double var9 = var3.getX() + Math.sin(var6) * var7;
      double var11 = var3.getX() - Math.sin(var6) * var7;
      double var13 = var3.getZ() + Math.cos(var6) * var7;
      double var15 = var3.getZ() - Math.cos(var6) * var7;
      int var17 = 2;
      double var18 = var3.getY() + var2.nextInt(3) - 2;
      double var20 = var3.getY() + var2.nextInt(3) - 2;
      int var22 = var3.getX() - Mth.ceil(var7) - var8;
      int var23 = var3.getY() - 2 - var8;
      int var24 = var3.getZ() - Mth.ceil(var7) - var8;
      int var25 = 2 * (Mth.ceil(var7) + var8);
      int var26 = 2 * (2 + var8);

      for (int var27 = var22; var27 <= var22 + var25; var27++) {
         for (int var28 = var24; var28 <= var24 + var25; var28++) {
            if (var23 <= var4.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, var27, var28)) {
               return this.doPlace(var4, var2, var5, var9, var11, var13, var15, var18, var20, var22, var23, var24, var25, var26);
            }
         }
      }

      return false;
   }

   protected boolean doPlace(
      WorldGenLevel param0,
      RandomSource param1,
      OreConfiguration param2,
      double param3,
      double param5,
      double param7,
      double param9,
      double param11,
      double param13,
      int param15,
      int param16,
      int param17,
      int param18,
      int param19
   ) {
      int var21 = 0;
      BitSet var22 = new BitSet(param18 * param19 * param18);
      BlockPos.MutableBlockPos var23 = new BlockPos.MutableBlockPos();
      int var24 = param2.size;
      double[] var25 = new double[var24 * 4];

      for (int var26 = 0; var26 < var24; var26++) {
         float var27 = (float)var26 / var24;
         double var28 = Mth.lerp(var27, param3, param5);
         double var30 = Mth.lerp(var27, param11, param13);
         double var32 = Mth.lerp(var27, param7, param9);
         double var34 = param1.nextDouble() * var24 / 16.0;
         double var36 = ((Mth.sin((float) Math.PI * var27) + 1.0F) * var34 + 1.0) / 2.0;
         var25[var26 * 4 + 0] = var28;
         var25[var26 * 4 + 1] = var30;
         var25[var26 * 4 + 2] = var32;
         var25[var26 * 4 + 3] = var36;
      }

      for (int var26 = 0; var26 < var24 - 1; var26++) {
         if (!(var25[var26 * 4 + 3] <= 0.0)) {
            for (int var27 = var26 + 1; var27 < var24; var27++) {
               if (!(var25[var27 * 4 + 3] <= 0.0)) {
                  double var28 = var25[var26 * 4 + 0] - var25[var27 * 4 + 0];
                  double var30 = var25[var26 * 4 + 1] - var25[var27 * 4 + 1];
                  double var32 = var25[var26 * 4 + 2] - var25[var27 * 4 + 2];
                  double var34 = var25[var26 * 4 + 3] - var25[var27 * 4 + 3];
                  if (var34 * var34 > var28 * var28 + var30 * var30 + var32 * var32) {
                     if (var34 > 0.0) {
                        var25[var27 * 4 + 3] = -1.0;
                     } else {
                        var25[var26 * 4 + 3] = -1.0;
                     }
                  }
               }
            }
         }
      }

      try (BulkSectionAccess var26 = new BulkSectionAccess(param0)) {
         for (int var27 = 0; var27 < var24; var27++) {
            double var28 = var25[var27 * 4 + 3];
            if (!(var28 < 0.0)) {
               double var30 = var25[var27 * 4 + 0];
               double var32 = var25[var27 * 4 + 1];
               double var34 = var25[var27 * 4 + 2];
               int var36 = Math.max(Mth.floor(var30 - var28), param15);
               int var37 = Math.max(Mth.floor(var32 - var28), param16);
               int var38 = Math.max(Mth.floor(var34 - var28), param17);
               int var39 = Math.max(Mth.floor(var30 + var28), var36);
               int var40 = Math.max(Mth.floor(var32 + var28), var37);
               int var41 = Math.max(Mth.floor(var34 + var28), var38);

               for (int var42 = var36; var42 <= var39; var42++) {
                  double var43 = (var42 + 0.5 - var30) / var28;
                  if (var43 * var43 < 1.0) {
                     for (int var45 = var37; var45 <= var40; var45++) {
                        double var46 = (var45 + 0.5 - var32) / var28;
                        if (var43 * var43 + var46 * var46 < 1.0) {
                           for (int var48 = var38; var48 <= var41; var48++) {
                              double var49 = (var48 + 0.5 - var34) / var28;
                              if (var43 * var43 + var46 * var46 + var49 * var49 < 1.0 && !param0.isOutsideBuildHeight(var45)) {
                                 int var51 = var42 - param15 + (var45 - param16) * param18 + (var48 - param17) * param18 * param19;
                                 if (!var22.get(var51)) {
                                    var22.set(var51);
                                    var23.set(var42, var45, var48);
                                    if (param0.ensureCanWrite(var23)) {
                                       LevelChunkSection var52 = var26.getSection(var23);
                                       if (var52 != null) {
                                          int var53 = SectionPos.sectionRelative(var42);
                                          int var54 = SectionPos.sectionRelative(var45);
                                          int var55 = SectionPos.sectionRelative(var48);
                                          BlockState var56 = var52.getBlockState(var53, var54, var55);

                                          for (OreConfiguration.TargetBlockState var58 : param2.targetStates) {
                                             if (canPlaceOre(var56, var26::getBlockState, param1, param2, var58, var23)) {
                                                var52.setBlockState(var53, var54, var55, var58.state, false);
                                                var21++;
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return var21 > 0;
   }

   public static boolean canPlaceOre(
      BlockState param0,
      Function<BlockPos, BlockState> param1,
      RandomSource param2,
      OreConfiguration param3,
      OreConfiguration.TargetBlockState param4,
      BlockPos.MutableBlockPos param5
   ) {
      if (!param4.target.test(param0, param2)) {
         return false;
      } else {
         return shouldSkipAirCheck(param2, param3.discardChanceOnAirExposure) ? true : !isAdjacentToAir(param1, param5);
      }
   }

   protected static boolean shouldSkipAirCheck(RandomSource param0, float param1) {
      if (param1 <= 0.0F) {
         return true;
      } else {
         return param1 >= 1.0F ? false : param0.nextFloat() >= param1;
      }
   }
}
