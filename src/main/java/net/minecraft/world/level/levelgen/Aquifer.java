package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
   static Aquifer create(
      NoiseChunk param0, ChunkPos param1, NoiseRouter param2, PositionalRandomFactory param3, int param4, int param5, Aquifer.FluidPicker param6
   ) {
      return new Aquifer.NoiseBasedAquifer(param0, param1, param2, param3, param4, param5, param6);
   }

   static Aquifer createDisabled(final Aquifer.FluidPicker param0) {
      return new Aquifer() {
         @Nullable
         @Override
         public BlockState computeSubstance(DensityFunction.FunctionContext l_param0, double l_param1) {
            return l_param1 > 0.0 ? null : param0.computeFluid(l_param0.blockX(), l_param0.blockY(), l_param0.blockZ()).at(l_param0.blockY());
         }

         @Override
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(DensityFunction.FunctionContext var1, double var2);

   boolean shouldScheduleFluidUpdate();

   interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int var1, int var2, int var3);
   }

   final class FluidStatus {
      final int fluidLevel;
      final BlockState fluidType;

      public FluidStatus(int param0, BlockState param1) {
         this.fluidLevel = param0;
         this.fluidType = param1;
      }

      public BlockState at(int param0) {
         return param0 < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   class NoiseBasedAquifer implements Aquifer {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private final NoiseChunk noiseChunk;
      private final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      private final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      private final Aquifer.FluidStatus[] aquiferCache;
      private final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      private boolean shouldScheduleFluidUpdate;
      private final int minGridX;
      private final int minGridY;
      private final int minGridZ;
      private final int gridSizeX;
      private final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
         {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
      };

      NoiseBasedAquifer(
         NoiseChunk param0, ChunkPos param1, NoiseRouter param2, PositionalRandomFactory param3, int param4, int param5, Aquifer.FluidPicker param6
      ) {
         this.noiseChunk = param0;
         this.barrierNoise = param2.barrierNoise();
         this.fluidLevelFloodednessNoise = param2.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = param2.fluidLevelSpreadNoise();
         this.lavaNoise = param2.lavaNoise();
         this.erosion = param2.erosion();
         this.depth = param2.depth();
         this.positionalRandomFactory = param3;
         this.minGridX = this.gridX(param1.getMinBlockX()) - 1;
         this.globalFluidPicker = param6;
         int var8 = this.gridX(param1.getMaxBlockX()) + 1;
         this.gridSizeX = var8 - this.minGridX + 1;
         this.minGridY = this.gridY(param4) - 1;
         int var9 = this.gridY(param4 + param5) + 1;
         int var10 = var9 - this.minGridY + 1;
         this.minGridZ = this.gridZ(param1.getMinBlockZ()) - 1;
         int var11 = this.gridZ(param1.getMaxBlockZ()) + 1;
         this.gridSizeZ = var11 - this.minGridZ + 1;
         int var12 = this.gridSizeX * var10 * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[var12];
         this.aquiferLocationCache = new long[var12];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
      }

      private int getIndex(int param0, int param1, int param2) {
         int var4 = param0 - this.minGridX;
         int var5 = param1 - this.minGridY;
         int var6 = param2 - this.minGridZ;
         return (var5 * this.gridSizeZ + var6) * this.gridSizeX + var4;
      }

      @Nullable
      @Override
      public BlockState computeSubstance(DensityFunction.FunctionContext param0, double param1) {
         int var4 = param0.blockX();
         int var5 = param0.blockY();
         int var6 = param0.blockZ();
         if (param1 > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         }

         Aquifer.FluidStatus var7 = this.globalFluidPicker.computeFluid(var4, var5, var6);
         if (var7.at(var5).is(Blocks.LAVA)) {
            this.shouldScheduleFluidUpdate = false;
            return Blocks.LAVA.defaultBlockState();
         }

         int var8 = Math.floorDiv(var4 - 5, 16);
         int var9 = Math.floorDiv(var5 + 1, 12);
         int var10 = Math.floorDiv(var6 - 5, 16);
         int var11 = Integer.MAX_VALUE;
         int var12 = Integer.MAX_VALUE;
         int var13 = Integer.MAX_VALUE;
         long var14 = 0L;
         long var16 = 0L;
         long var18 = 0L;

         for (int var20 = 0; var20 <= 1; var20++) {
            for (int var21 = -1; var21 <= 1; var21++) {
               for (int var22 = 0; var22 <= 1; var22++) {
                  int var23 = var8 + var20;
                  int var24 = var9 + var21;
                  int var25 = var10 + var22;
                  int var26 = this.getIndex(var23, var24, var25);
                  long var29 = this.aquiferLocationCache[var26];
                  long var27;
                  if (var29 != Long.MAX_VALUE) {
                     var27 = var29;
                  } else {
                     RandomSource var31 = this.positionalRandomFactory.at(var23, var24, var25);
                     var27 = BlockPos.asLong(var23 * 16 + var31.nextInt(10), var24 * 12 + var31.nextInt(9), var25 * 16 + var31.nextInt(10));
                     this.aquiferLocationCache[var26] = var27;
                  }

                  int var31 = BlockPos.getX(var27) - var4;
                  int var32 = BlockPos.getY(var27) - var5;
                  int var33 = BlockPos.getZ(var27) - var6;
                  int var34 = var31 * var31 + var32 * var32 + var33 * var33;
                  if (var11 >= var34) {
                     var18 = var16;
                     var16 = var14;
                     var14 = var27;
                     var13 = var12;
                     var12 = var11;
                     var11 = var34;
                  } else if (var12 >= var34) {
                     var18 = var16;
                     var16 = var27;
                     var13 = var12;
                     var12 = var34;
                  } else if (var13 >= var34) {
                     var18 = var27;
                     var13 = var34;
                  }
               }
            }
         }

         Aquifer.FluidStatus var20 = this.getAquiferStatus(var14);
         double var21 = similarity(var11, var12);
         BlockState var23 = var20.at(var5);
         BlockState var24 = var23;
         if (var21 <= 0.0) {
            this.shouldScheduleFluidUpdate = var21 >= FLOWING_UPDATE_SIMULARITY;
            return var24;
         }

         if (var23.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(var4, var5 - 1, var6).at(var5 - 1).is(Blocks.LAVA)) {
            this.shouldScheduleFluidUpdate = true;
            return var24;
         }

         MutableDouble var25 = new MutableDouble(Double.NaN);
         Aquifer.FluidStatus var26 = this.getAquiferStatus(var16);
         double var27 = var21 * this.calculatePressure(param0, var25, var20, var26);
         if (param1 + var27 > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         }

         Aquifer.FluidStatus var29 = this.getAquiferStatus(var18);
         double var30 = similarity(var11, var13);
         if (var30 > 0.0) {
            double var32 = var21 * var30 * this.calculatePressure(param0, var25, var20, var29);
            if (param1 + var32 > 0.0) {
               this.shouldScheduleFluidUpdate = false;
               return null;
            }
         }

         double var32 = similarity(var12, var13);
         if (var32 > 0.0) {
            double var34 = var21 * var32 * this.calculatePressure(param0, var25, var26, var29);
            if (param1 + var34 > 0.0) {
               this.shouldScheduleFluidUpdate = false;
               return null;
            }
         }

         this.shouldScheduleFluidUpdate = true;
         return var24;
      }

      @Override
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      private static double similarity(int param0, int param1) {
         double var2 = 25.0;
         return 1.0 - Math.abs(param1 - param0) / 25.0;
      }

      private double calculatePressure(DensityFunction.FunctionContext param0, MutableDouble param1, Aquifer.FluidStatus param2, Aquifer.FluidStatus param3) {
         int var5 = param0.blockY();
         BlockState var6 = param2.at(var5);
         BlockState var7 = param3.at(var5);
         if ((!var6.is(Blocks.LAVA) || !var7.is(Blocks.WATER)) && (!var6.is(Blocks.WATER) || !var7.is(Blocks.LAVA))) {
            int var8 = Math.abs(param2.fluidLevel - param3.fluidLevel);
            if (var8 == 0) {
               return 0.0;
            }

            double var9 = 0.5 * (param2.fluidLevel + param3.fluidLevel);
            double var11 = var5 + 0.5 - var9;
            double var13 = var8 / 2.0;
            double var15 = 0.0;
            double var17 = 2.5;
            double var19 = 1.5;
            double var21 = 3.0;
            double var23 = 10.0;
            double var25 = 3.0;
            double var27 = var13 - Math.abs(var11);
            double var29;
            if (var11 > 0.0) {
               double var31 = 0.0 + var27;
               if (var31 > 0.0) {
                  var29 = var31 / 1.5;
               } else {
                  var29 = var31 / 2.5;
               }
            } else {
               double var31 = 3.0 + var27;
               if (var31 > 0.0) {
                  var29 = var31 / 3.0;
               } else {
                  var29 = var31 / 10.0;
               }
            }

            double var31 = 2.0;
            double var33;
            if (!(var29 < -2.0) && !(var29 > 2.0)) {
               double var35 = param1.getValue();
               if (Double.isNaN(var35)) {
                  double var37 = this.barrierNoise.compute(param0);
                  param1.setValue(var37);
                  var33 = var37;
               } else {
                  var33 = var35;
               }
            } else {
               var33 = 0.0;
            }

            return 2.0 * (var33 + var29);
         } else {
            return 2.0;
         }
      }

      private int gridX(int param0) {
         return Math.floorDiv(param0, 16);
      }

      private int gridY(int param0) {
         return Math.floorDiv(param0, 12);
      }

      private int gridZ(int param0) {
         return Math.floorDiv(param0, 16);
      }

      private Aquifer.FluidStatus getAquiferStatus(long param0) {
         int var3 = BlockPos.getX(param0);
         int var4 = BlockPos.getY(param0);
         int var5 = BlockPos.getZ(param0);
         int var6 = this.gridX(var3);
         int var7 = this.gridY(var4);
         int var8 = this.gridZ(var5);
         int var9 = this.getIndex(var6, var7, var8);
         Aquifer.FluidStatus var10 = this.aquiferCache[var9];
         if (var10 != null) {
            return var10;
         }

         Aquifer.FluidStatus var11 = this.computeFluid(var3, var4, var5);
         this.aquiferCache[var9] = var11;
         return var11;
      }

      private Aquifer.FluidStatus computeFluid(int param0, int param1, int param2) {
         Aquifer.FluidStatus var4 = this.globalFluidPicker.computeFluid(param0, param1, param2);
         int var5 = Integer.MAX_VALUE;
         int var6 = param1 + 12;
         int var7 = param1 - 12;
         boolean var8 = false;

         for (int[] var12 : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int var13 = param0 + SectionPos.sectionToBlockCoord(var12[0]);
            int var14 = param2 + SectionPos.sectionToBlockCoord(var12[1]);
            int var15 = this.noiseChunk.preliminarySurfaceLevel(var13, var14);
            int var16 = var15 + 8;
            boolean var17 = var12[0] == 0 && var12[1] == 0;
            if (var17 && var7 > var16) {
               return var4;
            }

            boolean var18 = var6 > var16;
            if (var18 || var17) {
               Aquifer.FluidStatus var19 = this.globalFluidPicker.computeFluid(var13, var16, var14);
               if (!var19.at(var16).isAir()) {
                  if (var17) {
                     var8 = true;
                  }

                  if (var18) {
                     return var19;
                  }
               }
            }

            var5 = Math.min(var5, var15);
         }

         int var9 = this.computeSurfaceLevel(param0, param1, param2, var4, var5, var8);
         return new Aquifer.FluidStatus(var9, this.computeFluidType(param0, param1, param2, var4, var9));
      }

      private int computeSurfaceLevel(int param0, int param1, int param2, Aquifer.FluidStatus param3, int param4, boolean param5) {
         DensityFunction.SinglePointContext var7 = new DensityFunction.SinglePointContext(param0, param1, param2);
         double var8;
         double var10;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, var7)) {
            var8 = -1.0;
            var10 = -1.0;
         } else {
            int var12 = param4 + 8 - param1;
            int var13 = 64;
            double var14 = param5 ? Mth.clampedMap(var12, 0.0, 64.0, 1.0, 0.0) : 0.0;
            double var16 = Mth.clamp(this.fluidLevelFloodednessNoise.compute(var7), -1.0, 1.0);
            double var18 = Mth.map(var14, 1.0, 0.0, -0.3, 0.8);
            double var20 = Mth.map(var14, 1.0, 0.0, -0.8, 0.4);
            var8 = var16 - var20;
            var10 = var16 - var18;
         }

         int var12;
         if (var10 > 0.0) {
            var12 = param3.fluidLevel;
         } else if (var8 > 0.0) {
            var12 = this.computeRandomizedFluidSurfaceLevel(param0, param1, param2, param4);
         } else {
            var12 = DimensionType.WAY_BELOW_MIN_Y;
         }

         return var12;
      }

      private int computeRandomizedFluidSurfaceLevel(int param0, int param1, int param2, int param3) {
         int var5 = 16;
         int var6 = 40;
         int var7 = Math.floorDiv(param0, 16);
         int var8 = Math.floorDiv(param1, 40);
         int var9 = Math.floorDiv(param2, 16);
         int var10 = var8 * 40 + 20;
         int var11 = 10;
         double var12 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(var7, var8, var9)) * 10.0;
         int var14 = Mth.quantize(var12, 3);
         int var15 = var10 + var14;
         return Math.min(param3, var15);
      }

      private BlockState computeFluidType(int param0, int param1, int param2, Aquifer.FluidStatus param3, int param4) {
         BlockState var6 = param3.fluidType;
         if (param4 <= -10 && param4 != DimensionType.WAY_BELOW_MIN_Y && param3.fluidType != Blocks.LAVA.defaultBlockState()) {
            int var7 = 64;
            int var8 = 40;
            int var9 = Math.floorDiv(param0, 64);
            int var10 = Math.floorDiv(param1, 40);
            int var11 = Math.floorDiv(param2, 64);
            double var12 = this.lavaNoise.compute(new DensityFunction.SinglePointContext(var9, var10, var11));
            if (Math.abs(var12) > 0.3) {
               var6 = Blocks.LAVA.defaultBlockState();
            }
         }

         return var6;
      }
   }
}
