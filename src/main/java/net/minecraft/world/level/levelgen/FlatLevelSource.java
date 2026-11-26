package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class FlatLevelSource extends ChunkGenerator {
   public static final Codec<FlatLevelSource> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings))
         .apply(param0, param0.stable(FlatLevelSource::new))
   );
   private final FlatLevelGeneratorSettings settings;

   public FlatLevelSource(FlatLevelGeneratorSettings param0) {
      super(new FixedBiomeSource(param0.getBiome()), Util.memoize(param0::adjustGenerationSettings));
      this.settings = param0;
   }

   @Override
   public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> param0, RandomState param1, long param2) {
      Stream<Holder<StructureSet>> var5 = this.settings
         .structureOverrides()
         .map(HolderSet::stream)
         .orElseGet(() -> param0.listElements().map(l2_param0 -> (Holder<StructureSet>)l2_param0));
      return ChunkGeneratorStructureState.createForFlat(param1, param2, this.biomeSource, var5);
   }

   @Override
   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.settings;
   }

   @Override
   public void buildSurface(WorldGenRegion param0, StructureManager param1, RandomState param2, ChunkAccess param3) {
   }

   @Override
   public int getSpawnHeight(LevelHeightAccessor param0) {
      return param0.getMinBuildHeight() + Math.min(param0.getHeight(), this.settings.getLayers().size());
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Executor param0, Blender param1, RandomState param2, StructureManager param3, ChunkAccess param4) {
      List<BlockState> var6 = this.settings.getLayers();
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();
      Heightmap var8 = param4.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap var9 = param4.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

      for (int var10 = 0; var10 < Math.min(param4.getHeight(), var6.size()); var10++) {
         BlockState var11 = var6.get(var10);
         if (var11 != null) {
            int var12 = param4.getMinBuildHeight() + var10;

            for (int var13 = 0; var13 < 16; var13++) {
               for (int var14 = 0; var14 < 16; var14++) {
                  param4.setBlockState(var7.set(var13, var12, var14), var11, false);
                  var8.update(var13, var12, var14, var11);
                  var9.update(var13, var12, var14, var11);
               }
            }
         }
      }

      return CompletableFuture.completedFuture(param4);
   }

   @Override
   public int getBaseHeight(int param0, int param1, Heightmap.Types param2, LevelHeightAccessor param3, RandomState param4) {
      List<BlockState> var6 = this.settings.getLayers();

      for (int var7 = Math.min(var6.size(), param3.getMaxBuildHeight()) - 1; var7 >= 0; var7--) {
         BlockState var8 = var6.get(var7);
         if (var8 != null && param2.isOpaque().test(var8)) {
            return param3.getMinBuildHeight() + var7 + 1;
         }
      }

      return param3.getMinBuildHeight();
   }

   @Override
   public NoiseColumn getBaseColumn(int param0, int param1, LevelHeightAccessor param2, RandomState param3) {
      return new NoiseColumn(
         param2.getMinBuildHeight(),
         this.settings
            .getLayers()
            .stream()
            .limit(param2.getHeight())
            .map(l_param0 -> l_param0 == null ? Blocks.AIR.defaultBlockState() : l_param0)
            .toArray(BlockState[]::new)
      );
   }

   @Override
   public void addDebugScreenInfo(List<String> param0, RandomState param1, BlockPos param2) {
   }

   @Override
   public void applyCarvers(
      WorldGenRegion param0, long param1, RandomState param3, BiomeManager param4, StructureManager param5, ChunkAccess param6, GenerationStep.Carving param7
   ) {
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion param0) {
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getGenDepth() {
      return 384;
   }

   @Override
   public int getSeaLevel() {
      return -63;
   }
}
