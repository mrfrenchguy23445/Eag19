package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Material;

public class SurfaceSystem {
   private static final BlockState WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
   private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
   private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
   private static final BlockState YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
   private static final BlockState BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.defaultBlockState();
   private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
   private static final BlockState LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
   private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
   private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
   private final BlockState defaultBlock;
   private final int seaLevel;
   private final BlockState[] clayBands;
   private final NormalNoise clayBandsOffsetNoise;
   private final NormalNoise badlandsPillarNoise;
   private final NormalNoise badlandsPillarRoofNoise;
   private final NormalNoise badlandsSurfaceNoise;
   private final NormalNoise icebergPillarNoise;
   private final NormalNoise icebergPillarRoofNoise;
   private final NormalNoise icebergSurfaceNoise;
   private final PositionalRandomFactory noiseRandom;
   private final NormalNoise surfaceNoise;
   private final NormalNoise surfaceSecondaryNoise;

   public SurfaceSystem(RandomState param0, BlockState param1, int param2, PositionalRandomFactory param3) {
      this.defaultBlock = param1;
      this.seaLevel = param2;
      this.noiseRandom = param3;
      this.clayBandsOffsetNoise = param0.getOrCreateNoise(Noises.CLAY_BANDS_OFFSET);
      this.clayBands = generateBands(param3.fromHashOf(new ResourceLocation("clay_bands")));
      this.surfaceNoise = param0.getOrCreateNoise(Noises.SURFACE);
      this.surfaceSecondaryNoise = param0.getOrCreateNoise(Noises.SURFACE_SECONDARY);
      this.badlandsPillarNoise = param0.getOrCreateNoise(Noises.BADLANDS_PILLAR);
      this.badlandsPillarRoofNoise = param0.getOrCreateNoise(Noises.BADLANDS_PILLAR_ROOF);
      this.badlandsSurfaceNoise = param0.getOrCreateNoise(Noises.BADLANDS_SURFACE);
      this.icebergPillarNoise = param0.getOrCreateNoise(Noises.ICEBERG_PILLAR);
      this.icebergPillarRoofNoise = param0.getOrCreateNoise(Noises.ICEBERG_PILLAR_ROOF);
      this.icebergSurfaceNoise = param0.getOrCreateNoise(Noises.ICEBERG_SURFACE);
   }

   public void buildSurface(
      RandomState param0,
      BiomeManager param1,
      Registry<Biome> param2,
      boolean param3,
      WorldGenerationContext param4,
      final ChunkAccess param5,
      NoiseChunk param6,
      SurfaceRules.RuleSource param7
   ) {
      final BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();
      final ChunkPos var10 = param5.getPos();
      int var11 = var10.getMinBlockX();
      int var12 = var10.getMinBlockZ();
      BlockColumn var13 = new BlockColumn() {
         @Override
         public BlockState getBlock(int l_param0) {
            return param5.getBlockState(var9.setY(l_param0));
         }

         @Override
         public void setBlock(int l_param0, BlockState l_param1) {
            LevelHeightAccessor l_var3 = param5.getHeightAccessorForGeneration();
            if (l_param0 >= l_var3.getMinBuildHeight() && l_param0 < l_var3.getMaxBuildHeight()) {
               param5.setBlockState(var9.setY(l_param0), l_param1, false);
               if (!l_param1.getFluidState().isEmpty()) {
                  param5.markPosForPostprocessing(var9);
               }
            }
         }

         @Override
         public String toString() {
            return "ChunkBlockColumn " + var10;
         }
      };
      SurfaceRules.Context var14 = new SurfaceRules.Context(this, param0, param5, param6, param1::getBiome, param2, param4);
      SurfaceRules.SurfaceRule var15 = param7.apply(var14);
      BlockPos.MutableBlockPos var16 = new BlockPos.MutableBlockPos();

      for (int var17 = 0; var17 < 16; var17++) {
         for (int var18 = 0; var18 < 16; var18++) {
            int var19 = var11 + var17;
            int var20 = var12 + var18;
            int var21 = param5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var17, var18) + 1;
            var9.setX(var19).setZ(var20);
            Holder<Biome> var22 = param1.getBiome(var16.set(var19, param3 ? 0 : var21, var20));
            if (var22.is(Biomes.ERODED_BADLANDS)) {
               this.erodedBadlandsExtension(var13, var19, var20, var21, param5);
            }

            int var23 = param5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var17, var18) + 1;
            var14.updateXZ(var19, var20);
            int var24 = 0;
            int var25 = Integer.MIN_VALUE;
            int var26 = Integer.MAX_VALUE;
            int var27 = param5.getMinBuildHeight();

            for (int var28 = var23; var28 >= var27; var28--) {
               BlockState var29 = var13.getBlock(var28);
               if (var29.isAir()) {
                  var24 = 0;
                  var25 = Integer.MIN_VALUE;
               } else if (!var29.getFluidState().isEmpty()) {
                  if (var25 == Integer.MIN_VALUE) {
                     var25 = var28 + 1;
                  }
               } else {
                  if (var26 >= var28) {
                     var26 = DimensionType.WAY_BELOW_MIN_Y;

                     for (int var30 = var28 - 1; var30 >= var27 - 1; var30--) {
                        BlockState var31 = var13.getBlock(var30);
                        if (!this.isStone(var31)) {
                           var26 = var30 + 1;
                           break;
                        }
                     }
                  }

                  var24++;
                  int var30 = var28 - var26 + 1;
                  var14.updateY(var24, var30, var25, var19, var28, var20);
                  if (var29 == this.defaultBlock) {
                     BlockState var31 = var15.tryApply(var19, var28, var20);
                     if (var31 != null) {
                        var13.setBlock(var28, var31);
                     }
                  }
               }
            }

            if (var22.is(Biomes.FROZEN_OCEAN) || var22.is(Biomes.DEEP_FROZEN_OCEAN)) {
               this.frozenOceanExtension(var14.getMinSurfaceLevel(), var22.value(), var13, var16, var19, var20, var21);
            }
         }
      }
   }

   protected int getSurfaceDepth(int param0, int param1) {
      double var3 = this.surfaceNoise.getValue(param0, 0.0, param1);
      return (int)(var3 * 2.75 + 3.0 + this.noiseRandom.at(param0, 0, param1).nextDouble() * 0.25);
   }

   protected double getSurfaceSecondary(int param0, int param1) {
      return this.surfaceSecondaryNoise.getValue(param0, 0.0, param1);
   }

   private boolean isStone(BlockState param0) {
      return !param0.isAir() && param0.getFluidState().isEmpty();
   }

   @Deprecated
   public Optional<BlockState> topMaterial(
      SurfaceRules.RuleSource param0,
      CarvingContext param1,
      Function<BlockPos, Holder<Biome>> param2,
      ChunkAccess param3,
      NoiseChunk param4,
      BlockPos param5,
      boolean param6
   ) {
      SurfaceRules.Context var8 = new SurfaceRules.Context(
         this, param1.randomState(), param3, param4, param2, param1.registryAccess().registryOrThrow(Registries.BIOME), param1
      );
      SurfaceRules.SurfaceRule var9 = param0.apply(var8);
      int var10 = param5.getX();
      int var11 = param5.getY();
      int var12 = param5.getZ();
      var8.updateXZ(var10, var12);
      var8.updateY(1, 1, param6 ? var11 + 1 : Integer.MIN_VALUE, var10, var11, var12);
      BlockState var13 = var9.tryApply(var10, var11, var12);
      return Optional.ofNullable(var13);
   }

   private void erodedBadlandsExtension(BlockColumn param0, int param1, int param2, int param3, LevelHeightAccessor param4) {
      double var6 = 0.2;
      double var8 = Math.min(
         Math.abs(this.badlandsSurfaceNoise.getValue(param1, 0.0, param2) * 8.25), this.badlandsPillarNoise.getValue(param1 * 0.2, 0.0, param2 * 0.2) * 15.0
      );
      if (!(var8 <= 0.0)) {
         double var10 = 0.75;
         double var12 = 1.5;
         double var14 = Math.abs(this.badlandsPillarRoofNoise.getValue(param1 * 0.75, 0.0, param2 * 0.75) * 1.5);
         double var16 = 64.0 + Math.min(var8 * var8 * 2.5, Math.ceil(var14 * 50.0) + 24.0);
         int var18 = Mth.floor(var16);
         if (param3 <= var18) {
            for (int var19 = var18; var19 >= param4.getMinBuildHeight(); var19--) {
               BlockState var20 = param0.getBlock(var19);
               if (var20.is(this.defaultBlock.getBlock())) {
                  break;
               }

               if (var20.is(Blocks.WATER)) {
                  return;
               }
            }

            for (int var19 = var18; var19 >= param4.getMinBuildHeight() && param0.getBlock(var19).isAir(); var19--) {
               param0.setBlock(var19, this.defaultBlock);
            }
         }
      }
   }

   private void frozenOceanExtension(int param0, Biome param1, BlockColumn param2, BlockPos.MutableBlockPos param3, int param4, int param5, int param6) {
      double var8 = 1.28;
      double var10 = Math.min(
         Math.abs(this.icebergSurfaceNoise.getValue(param4, 0.0, param5) * 8.25), this.icebergPillarNoise.getValue(param4 * 1.28, 0.0, param5 * 1.28) * 15.0
      );
      if (!(var10 <= 1.8)) {
         double var14 = 1.17;
         double var16 = 1.5;
         double var18 = Math.abs(this.icebergPillarRoofNoise.getValue(param4 * 1.17, 0.0, param5 * 1.17) * 1.5);
         double var20 = Math.min(var10 * var10 * 1.2, Math.ceil(var18 * 40.0) + 14.0);
         if (param1.shouldMeltFrozenOceanIcebergSlightly(param3.set(param4, 63, param5))) {
            var20 -= 2.0;
         }

         double var12;
         if (var20 > 2.0) {
            var12 = this.seaLevel - var20 - 7.0;
            var20 += this.seaLevel;
         } else {
            var20 = 0.0;
            var12 = 0.0;
         }

         double var22 = var20;
         RandomSource var24 = this.noiseRandom.at(param4, 0, param5);
         int var25 = 2 + var24.nextInt(4);
         int var26 = this.seaLevel + 18 + var24.nextInt(10);
         int var27 = 0;

         for (int var28 = Math.max(param6, (int)var22 + 1); var28 >= param0; var28--) {
            if (param2.getBlock(var28).isAir() && var28 < (int)var22 && var24.nextDouble() > 0.01
               || param2.getBlock(var28).getMaterial() == Material.WATER
                  && var28 > (int)var12
                  && var28 < this.seaLevel
                  && var12 != 0.0
                  && var24.nextDouble() > 0.15) {
               if (var27 <= var25 && var28 > var26) {
                  param2.setBlock(var28, SNOW_BLOCK);
                  var27++;
               } else {
                  param2.setBlock(var28, PACKED_ICE);
               }
            }
         }
      }
   }

   private static BlockState[] generateBands(RandomSource param0) {
      BlockState[] var1 = new BlockState[192];
      Arrays.fill(var1, TERRACOTTA);

      for (int var2 = 0; var2 < var1.length; var2++) {
         var2 += param0.nextInt(5) + 1;
         if (var2 < var1.length) {
            var1[var2] = ORANGE_TERRACOTTA;
         }
      }

      makeBands(param0, var1, 1, YELLOW_TERRACOTTA);
      makeBands(param0, var1, 2, BROWN_TERRACOTTA);
      makeBands(param0, var1, 1, RED_TERRACOTTA);
      int var2 = param0.nextIntBetweenInclusive(9, 15);
      int var3 = 0;

      for (int var4 = 0; var3 < var2 && var4 < var1.length; var4 += param0.nextInt(16) + 4) {
         var1[var4] = WHITE_TERRACOTTA;
         if (var4 - 1 > 0 && param0.nextBoolean()) {
            var1[var4 - 1] = LIGHT_GRAY_TERRACOTTA;
         }

         if (var4 + 1 < var1.length && param0.nextBoolean()) {
            var1[var4 + 1] = LIGHT_GRAY_TERRACOTTA;
         }

         var3++;
      }

      return var1;
   }

   private static void makeBands(RandomSource param0, BlockState[] param1, int param2, BlockState param3) {
      int var4 = param0.nextIntBetweenInclusive(6, 15);

      for (int var5 = 0; var5 < var4; var5++) {
         int var6 = param2 + param0.nextInt(3);
         int var7 = param0.nextInt(param1.length);

         for (int var8 = 0; var7 + var8 < param1.length && var8 < var6; var8++) {
            param1[var7 + var8] = param3;
         }
      }
   }

   protected BlockState getBand(int param0, int param1, int param2) {
      int var4 = (int)Math.round(this.clayBandsOffsetNoise.getValue(param0, 0.0, param2) * 4.0);
      return this.clayBands[(param1 + var4 + this.clayBands.length) % this.clayBands.length];
   }
}
