package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;

public class DebugLevelSource extends ChunkGenerator {
   public static final Codec<DebugLevelSource> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(RegistryOps.retrieveElement(Biomes.PLAINS)).apply(param0, param0.stable(DebugLevelSource::new))
   );
   private static final int BLOCK_MARGIN = 2;
   private static final List<BlockState> ALL_BLOCKS = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
      .flatMap(param0 -> param0.getStateDefinition().getPossibleStates().stream())
      .collect(Collectors.toList());
   private static final int GRID_WIDTH = Mth.ceil(Mth.sqrt(ALL_BLOCKS.size()));
   private static final int GRID_HEIGHT = Mth.ceil((float)ALL_BLOCKS.size() / GRID_WIDTH);
   protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
   protected static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();
   public static final int HEIGHT = 70;
   public static final int BARRIER_HEIGHT = 60;

   public DebugLevelSource(Holder.Reference<Biome> param0) {
      super(new FixedBiomeSource(param0));
   }

   @Override
   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   @Override
   public void buildSurface(WorldGenRegion param0, StructureManager param1, RandomState param2, ChunkAccess param3) {
   }

   @Override
   public void applyBiomeDecoration(WorldGenLevel param0, ChunkAccess param1, StructureManager param2) {
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();
      ChunkPos var5 = param1.getPos();
      int var6 = var5.x;
      int var7 = var5.z;

      for (int var8 = 0; var8 < 16; var8++) {
         for (int var9 = 0; var9 < 16; var9++) {
            int var10 = SectionPos.sectionToBlockCoord(var6, var8);
            int var11 = SectionPos.sectionToBlockCoord(var7, var9);
            param0.setBlock(var4.set(var10, 60, var11), BARRIER, 2);
            BlockState var12 = getBlockStateFor(var10, var11);
            param0.setBlock(var4.set(var10, 70, var11), var12, 2);
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Executor param0, Blender param1, RandomState param2, StructureManager param3, ChunkAccess param4) {
      return CompletableFuture.completedFuture(param4);
   }

   @Override
   public int getBaseHeight(int param0, int param1, Heightmap.Types param2, LevelHeightAccessor param3, RandomState param4) {
      return 0;
   }

   @Override
   public NoiseColumn getBaseColumn(int param0, int param1, LevelHeightAccessor param2, RandomState param3) {
      return new NoiseColumn(0, new BlockState[0]);
   }

   @Override
   public void addDebugScreenInfo(List<String> param0, RandomState param1, BlockPos param2) {
   }

   public static BlockState getBlockStateFor(int param0, int param1) {
      BlockState var2 = AIR;
      if (param0 > 0 && param1 > 0 && param0 % 2 != 0 && param1 % 2 != 0) {
         param0 /= 2;
         param1 /= 2;
         if (param0 <= GRID_WIDTH && param1 <= GRID_HEIGHT) {
            int var3 = Mth.abs(param0 * GRID_WIDTH + param1);
            if (var3 < ALL_BLOCKS.size()) {
               var2 = ALL_BLOCKS.get(var3);
            }
         }
      }

      return var2;
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
      return 63;
   }
}
