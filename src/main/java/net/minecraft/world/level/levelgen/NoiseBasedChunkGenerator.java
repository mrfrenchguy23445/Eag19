package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(l_param0 -> l_param0.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(l_param0 -> l_param0.settings)
         )
         .apply(param0, param0.stable(NoiseBasedChunkGenerator::new))
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final Holder<NoiseGeneratorSettings> settings;
   private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

   public NoiseBasedChunkGenerator(BiomeSource param0, Holder<NoiseGeneratorSettings> param1) {
      super(param0);
      this.settings = param1;
      this.globalFluidPicker = Suppliers.<Aquifer.FluidPicker>memoize(() -> createFluidPicker(param1.value()));
   }

   private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings param0) {
      Aquifer.FluidStatus var1 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int var2 = param0.seaLevel();
      Aquifer.FluidStatus var3 = new Aquifer.FluidStatus(var2, param0.defaultFluid());
      Aquifer.FluidStatus var4 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
      return (l_param4, l_param5, l_param6) -> l_param5 < Math.min(-54, var2) ? var1 : var3;
   }

   @Override
   public CompletableFuture<ChunkAccess> createBiomes(Executor param0, RandomState param1, Blender param2, StructureManager param3, ChunkAccess param4) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         this.doCreateBiomes(param2, param1, param3, param4);
         return param4;
      }), Util.backgroundExecutor());
   }

   private void doCreateBiomes(Blender param0, RandomState param1, StructureManager param2, ChunkAccess param3) {
      NoiseChunk var5 = param3.getOrCreateNoiseChunk(l_param3 -> this.createNoiseChunk(l_param3, param2, param0, param1));
      BiomeResolver var6 = BelowZeroRetrogen.getBiomeResolver(param0.getBiomeResolver(this.biomeSource), param3);
      param3.fillBiomesFromNoise(var6, var5.cachedClimateSampler(param1.router(), this.settings.value().spawnTarget()));
   }

   private NoiseChunk createNoiseChunk(ChunkAccess param0, StructureManager param1, Blender param2, RandomState param3) {
      return NoiseChunk.forChunk(
         param0, param3, Beardifier.forStructuresInChunk(param1, param0.getPos()), this.settings.value(), this.globalFluidPicker.get(), param2
      );
   }

   @Override
   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   public boolean stable(ResourceKey<NoiseGeneratorSettings> param0) {
      return this.settings.is(param0);
   }

   @Override
   public int getBaseHeight(int param0, int param1, Heightmap.Types param2, LevelHeightAccessor param3, RandomState param4) {
      return this.iterateNoiseColumn(param3, param4, param0, param1, null, param2.isOpaque()).orElse(param3.getMinBuildHeight());
   }

   @Override
   public NoiseColumn getBaseColumn(int param0, int param1, LevelHeightAccessor param2, RandomState param3) {
      MutableObject<NoiseColumn> var5 = new MutableObject<>();
      this.iterateNoiseColumn(param2, param3, param0, param1, var5, null);
      return var5.getValue();
   }

   @Override
   public void addDebugScreenInfo(List<String> param0, RandomState param1, BlockPos param2) {
      DecimalFormat var4 = new DecimalFormat("0.000");
      NoiseRouter var5 = param1.router();
      DensityFunction.SinglePointContext var6 = new DensityFunction.SinglePointContext(param2.getX(), param2.getY(), param2.getZ());
      double var7 = var5.ridges().compute(var6);
      param0.add(
         "NoiseRouter T: "
            + var4.format(var5.temperature().compute(var6))
            + " V: "
            + var4.format(var5.vegetation().compute(var6))
            + " C: "
            + var4.format(var5.continents().compute(var6))
            + " E: "
            + var4.format(var5.erosion().compute(var6))
            + " D: "
            + var4.format(var5.depth().compute(var6))
            + " W: "
            + var4.format(var7)
            + " PV: "
            + var4.format(NoiseRouterData.peaksAndValleys((float)var7))
            + " AS: "
            + var4.format(var5.initialDensityWithoutJaggedness().compute(var6))
            + " N: "
            + var4.format(var5.finalDensity().compute(var6))
      );
   }

   private OptionalInt iterateNoiseColumn(
      LevelHeightAccessor param0,
      RandomState param1,
      int param2,
      int param3,
      @Nullable MutableObject<NoiseColumn> param4,
      @Nullable Predicate<BlockState> param5
   ) {
      NoiseSettings var7 = this.settings.value().noiseSettings().clampToHeightAccessor(param0);
      int var8 = var7.getCellHeight();
      int var9 = var7.minY();
      int var10 = Mth.floorDiv(var9, var8);
      int var11 = Mth.floorDiv(var7.height(), var8);
      if (var11 <= 0) {
         return OptionalInt.empty();
      }

      BlockState[] var12;
      if (param4 == null) {
         var12 = null;
      } else {
         var12 = new BlockState[var7.height()];
         param4.setValue(new NoiseColumn(var9, var12));
      }

      int var13 = var7.getCellWidth();
      int var14 = Math.floorDiv(param2, var13);
      int var15 = Math.floorDiv(param3, var13);
      int var16 = Math.floorMod(param2, var13);
      int var17 = Math.floorMod(param3, var13);
      int var18 = var14 * var13;
      int var19 = var15 * var13;
      double var20 = (double)var16 / var13;
      double var22 = (double)var17 / var13;
      NoiseChunk var24 = new NoiseChunk(
         1, param1, var18, var19, var7, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty()
      );
      var24.initializeForFirstCellX();
      var24.advanceCellX(0);

      for (int var25 = var11 - 1; var25 >= 0; var25--) {
         var24.selectCellYZ(var25, 0);

         for (int var26 = var8 - 1; var26 >= 0; var26--) {
            int var27 = (var10 + var25) * var8 + var26;
            double var28 = (double)var26 / var8;
            var24.updateForY(var27, var28);
            var24.updateForX(param2, var20);
            var24.updateForZ(param3, var22);
            BlockState var30 = var24.getInterpolatedState();
            BlockState var31 = var30 == null ? this.settings.value().defaultBlock() : var30;
            if (var12 != null) {
               int var32 = var25 * var8 + var26;
               var12[var32] = var31;
            }

            if (param5 != null && param5.test(var31)) {
               var24.stopInterpolation();
               return OptionalInt.of(var27 + 1);
            }
         }
      }

      var24.stopInterpolation();
      return OptionalInt.empty();
   }

   @Override
   public void buildSurface(WorldGenRegion param0, StructureManager param1, RandomState param2, ChunkAccess param3) {
      if (!SharedConstants.debugVoidTerrain(param3.getPos())) {
         WorldGenerationContext var5 = new WorldGenerationContext(this, param0);
         this.buildSurface(
            param3, var5, param2, param1, param0.getBiomeManager(), param0.registryAccess().registryOrThrow(Registries.BIOME), Blender.of(param0)
         );
      }
   }

   @VisibleForTesting
   public void buildSurface(
      ChunkAccess param0,
      WorldGenerationContext param1,
      RandomState param2,
      StructureManager param3,
      BiomeManager param4,
      Registry<Biome> param5,
      Blender param6
   ) {
      NoiseChunk var8 = param0.getOrCreateNoiseChunk(l_param3 -> this.createNoiseChunk(l_param3, param3, param6, param2));
      NoiseGeneratorSettings var9 = this.settings.value();
      param2.surfaceSystem().buildSurface(param2, param4, param5, var9.useLegacyRandomSource(), param1, param0, var8, var9.surfaceRule());
   }

   @Override
   public void applyCarvers(
      WorldGenRegion param0, long param1, RandomState param3, BiomeManager param4, StructureManager param5, ChunkAccess param6, GenerationStep.Carving param7
   ) {
      BiomeManager var9 = param4.withDifferentSource(
         (l_param1, l_param2, l_param3) -> this.biomeSource.getNoiseBiome(l_param1, l_param2, l_param3, param3.sampler())
      );
      WorldgenRandom var10 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      int var11 = 8;
      ChunkPos var12 = param6.getPos();
      NoiseChunk var13 = param6.getOrCreateNoiseChunk(l_param3 -> this.createNoiseChunk(l_param3, param5, Blender.of(param0), param3));
      Aquifer var14 = var13.aquifer();
      CarvingContext var15 = new CarvingContext(
         this, param0.registryAccess(), param6.getHeightAccessorForGeneration(), var13, param3, this.settings.value().surfaceRule()
      );
      CarvingMask var16 = ((ProtoChunk)param6).getOrCreateCarvingMask(param7);

      for (int var17 = -8; var17 <= 8; var17++) {
         for (int var18 = -8; var18 <= 8; var18++) {
            ChunkPos var19 = new ChunkPos(var12.x + var17, var12.z + var18);
            ChunkAccess var20 = param0.getChunk(var19.x, var19.z);
            BiomeGenerationSettings var21 = var20.carverBiome(
               () -> this.getBiomeGenerationSettings(
                  this.biomeSource.getNoiseBiome(QuartPos.fromBlock(var19.getMinBlockX()), 0, QuartPos.fromBlock(var19.getMinBlockZ()), param3.sampler())
               )
            );
            Iterable<Holder<ConfiguredWorldCarver<?>>> var22 = var21.getCarvers(param7);
            int var23 = 0;

            for (Holder<ConfiguredWorldCarver<?>> var25 : var22) {
               ConfiguredWorldCarver<?> var26 = var25.value();
               var10.setLargeFeatureSeed(param1 + var23, var19.x, var19.z);
               if (var26.isStartChunk(var10)) {
                  var26.carve(var15, param6, var9::getBiome, var10, var14, var19, var16);
               }

               var23++;
            }
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Executor param0, Blender param1, RandomState param2, StructureManager param3, ChunkAccess param4) {
      NoiseSettings var6 = this.settings.value().noiseSettings().clampToHeightAccessor(param4.getHeightAccessorForGeneration());
      int var7 = var6.minY();
      int var8 = Mth.floorDiv(var7, var6.getCellHeight());
      int var9 = Mth.floorDiv(var6.height(), var6.getCellHeight());
      if (var9 <= 0) {
         return CompletableFuture.completedFuture(param4);
      }

      int var10 = param4.getSectionIndex(var9 * var6.getCellHeight() - 1 + var7);
      int var11 = param4.getSectionIndex(var7);
      Set<LevelChunkSection> var12 = Sets.newHashSet();

      for (int var13 = var10; var13 >= var11; var13--) {
         LevelChunkSection var14 = param4.getSection(var13);
         var14.acquire();
         var12.add(var14);
      }

      return CompletableFuture.supplyAsync(
            Util.wrapThreadWithTaskName("wgen_fill_noise", () -> this.doFill(param1, param3, param2, param4, var8, var9)), Util.backgroundExecutor()
         )
         .whenCompleteAsync((l_param1, l_param2) -> {
            for (LevelChunkSection l_var4 : var12) {
               l_var4.release();
            }
         }, param0);
   }

   private ChunkAccess doFill(Blender param0, StructureManager param1, RandomState param2, ChunkAccess param3, int param4, int param5) {
      NoiseChunk var7 = param3.getOrCreateNoiseChunk(l_param3 -> this.createNoiseChunk(l_param3, param1, param0, param2));
      Heightmap var8 = param3.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap var9 = param3.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos var10 = param3.getPos();
      int var11 = var10.getMinBlockX();
      int var12 = var10.getMinBlockZ();
      Aquifer var13 = var7.aquifer();
      var7.initializeForFirstCellX();
      BlockPos.MutableBlockPos var14 = new BlockPos.MutableBlockPos();
      int var15 = var7.cellWidth();
      int var16 = var7.cellHeight();
      int var17 = 16 / var15;
      int var18 = 16 / var15;

      for (int var19 = 0; var19 < var17; var19++) {
         var7.advanceCellX(var19);

         for (int var20 = 0; var20 < var18; var20++) {
            LevelChunkSection var21 = param3.getSection(param3.getSectionsCount() - 1);

            for (int var22 = param5 - 1; var22 >= 0; var22--) {
               var7.selectCellYZ(var22, var20);

               for (int var23 = var16 - 1; var23 >= 0; var23--) {
                  int var24 = (param4 + var22) * var16 + var23;
                  int var25 = var24 & 15;
                  int var26 = param3.getSectionIndex(var24);
                  if (param3.getSectionIndex(var21.bottomBlockY()) != var26) {
                     var21 = param3.getSection(var26);
                  }

                  double var27 = (double)var23 / var16;
                  var7.updateForY(var24, var27);

                  for (int var29 = 0; var29 < var15; var29++) {
                     int var30 = var11 + var19 * var15 + var29;
                     int var31 = var30 & 15;
                     double var32 = (double)var29 / var15;
                     var7.updateForX(var30, var32);

                     for (int var34 = 0; var34 < var15; var34++) {
                        int var35 = var12 + var20 * var15 + var34;
                        int var36 = var35 & 15;
                        double var37 = (double)var34 / var15;
                        var7.updateForZ(var35, var37);
                        BlockState var39 = var7.getInterpolatedState();
                        if (var39 == null) {
                           var39 = this.settings.value().defaultBlock();
                        }

                        var39 = this.debugPreliminarySurfaceLevel(var7, var30, var24, var35, var39);
                        if (var39 != AIR && !SharedConstants.debugVoidTerrain(param3.getPos())) {
                           if (var39.getLightEmission() != 0 && param3 instanceof ProtoChunk) {
                              var14.set(var30, var24, var35);
                              ((ProtoChunk)param3).addLight(var14);
                           }

                           var21.setBlockState(var31, var25, var36, var39, false);
                           var8.update(var31, var24, var36, var39);
                           var9.update(var31, var24, var36, var39);
                           if (var13.shouldScheduleFluidUpdate() && !var39.getFluidState().isEmpty()) {
                              var14.set(var30, var24, var35);
                              param3.markPosForPostprocessing(var14);
                           }
                        }
                     }
                  }
               }
            }
         }

         var7.swapSlices();
      }

      var7.stopInterpolation();
      return param3;
   }

   private BlockState debugPreliminarySurfaceLevel(NoiseChunk param0, int param1, int param2, int param3, BlockState param4) {
      return param4;
   }

   @Override
   public int getGenDepth() {
      return this.settings.value().noiseSettings().height();
   }

   @Override
   public int getSeaLevel() {
      return this.settings.value().seaLevel();
   }

   @Override
   public int getMinY() {
      return this.settings.value().noiseSettings().minY();
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion param0) {
      if (!this.settings.value().disableMobGeneration()) {
         ChunkPos var2 = param0.getCenter();
         Holder<Biome> var3 = param0.getBiome(var2.getWorldPosition().atY(param0.getMaxBuildHeight() - 1));
         WorldgenRandom var4 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         var4.setDecorationSeed(param0.getSeed(), var2.getMinBlockX(), var2.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(param0, var3, var2, var4);
      }
   }
}
