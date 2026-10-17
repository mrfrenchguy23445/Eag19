package net.minecraft.world.level;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public interface LevelReader extends BlockAndTintGetter, CollisionGetter, BiomeManager.NoiseBiomeSource {
   @Nullable
   ChunkAccess getChunk(int var1, int var2, ChunkStatus var3, boolean var4);

   @Deprecated
   boolean hasChunk(int var1, int var2);

   int getHeight(Heightmap.Types var1, int var2, int var3);

   int getSkyDarken();

   BiomeManager getBiomeManager();

   default Holder<Biome> getBiome(BlockPos param0) {
      return this.getBiomeManager().getBiome(param0);
   }

   default Stream<BlockState> getBlockStatesIfLoaded(AABB param0) {
      int var2 = Mth.floor(param0.minX);
      int var3 = Mth.floor(param0.maxX);
      int var4 = Mth.floor(param0.minY);
      int var5 = Mth.floor(param0.maxY);
      int var6 = Mth.floor(param0.minZ);
      int var7 = Mth.floor(param0.maxZ);
      return this.hasChunksAt(var2, var4, var6, var3, var5, var7) ? this.getBlockStates(param0) : Stream.empty();
   }

   @Override
   default int getBlockTint(BlockPos param0, ColorResolver param1) {
      return param1.getColor(this.getBiome(param0).value(), param0.getX(), param0.getZ());
   }

   @Override
   default Holder<Biome> getNoiseBiome(int param0, int param1, int param2) {
      ChunkAccess var4 = this.getChunk(QuartPos.toSection(param0), QuartPos.toSection(param2), ChunkStatus.BIOMES, false);
      return var4 != null ? var4.getNoiseBiome(param0, param1, param2) : this.getUncachedNoiseBiome(param0, param1, param2);
   }

   Holder<Biome> getUncachedNoiseBiome(int var1, int var2, int var3);

   boolean isClientSide();

   @Deprecated
   int getSeaLevel();

   DimensionType dimensionType();

   @Override
   default int getMinBuildHeight() {
      return this.dimensionType().minY();
   }

   @Override
   default int getHeight() {
      return this.dimensionType().height();
   }

   default BlockPos getHeightmapPos(Heightmap.Types param0, BlockPos param1) {
      return new BlockPos(param1.getX(), this.getHeight(param0, param1.getX(), param1.getZ()), param1.getZ());
   }

   default boolean isEmptyBlock(BlockPos param0) {
      return this.getBlockState(param0).isAir();
   }

   default boolean canSeeSkyFromBelowWater(BlockPos param0) {
      if (param0.getY() >= this.getSeaLevel()) {
         return this.canSeeSky(param0);
      }

      BlockPos var2 = new BlockPos(param0.getX(), this.getSeaLevel(), param0.getZ());
      if (!this.canSeeSky(var2)) {
         return false;
      }

      for (BlockPos var4 = var2.below(); var4.getY() > param0.getY(); var4 = var4.below()) {
         BlockState var3 = this.getBlockState(var4);
         if (var3.getLightBlock(this, var4) > 0 && !var3.getMaterial().isLiquid()) {
            return false;
         }
      }

      return true;
   }

   default float getPathfindingCostFromLightLevels(BlockPos param0) {
      return this.getLightLevelDependentMagicValue(param0) - 0.5F;
   }

   @Deprecated
   default float getLightLevelDependentMagicValue(BlockPos param0) {
      float var2 = this.getMaxLocalRawBrightness(param0) / 15.0F;
      float var3 = var2 / (4.0F - 3.0F * var2);
      return Mth.lerp(this.dimensionType().ambientLight(), var3, 1.0F);
   }

   default int getDirectSignal(BlockPos param0, Direction param1) {
      return this.getBlockState(param0).getDirectSignal(this, param0, param1);
   }

   default ChunkAccess getChunk(BlockPos param0) {
      return this.getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
   }

   default ChunkAccess getChunk(int param0, int param1) {
      return this.getChunk(param0, param1, ChunkStatus.FULL, true);
   }

   default ChunkAccess getChunk(int param0, int param1, ChunkStatus param2) {
      return this.getChunk(param0, param1, param2, true);
   }

   @Nullable
   @Override
   default BlockGetter getChunkForCollisions(int param0, int param1) {
      return this.getChunk(param0, param1, ChunkStatus.EMPTY, false);
   }

   default boolean isWaterAt(BlockPos param0) {
      return this.getFluidState(param0).is(FluidTags.WATER);
   }

   default boolean containsAnyLiquid(AABB param0) {
      int var2 = Mth.floor(param0.minX);
      int var3 = Mth.ceil(param0.maxX);
      int var4 = Mth.floor(param0.minY);
      int var5 = Mth.ceil(param0.maxY);
      int var6 = Mth.floor(param0.minZ);
      int var7 = Mth.ceil(param0.maxZ);
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      for (int var9 = var2; var9 < var3; var9++) {
         for (int var10 = var4; var10 < var5; var10++) {
            for (int var11 = var6; var11 < var7; var11++) {
               BlockState var12 = this.getBlockState(var8.set(var9, var10, var11));
               if (!var12.getFluidState().isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   default int getMaxLocalRawBrightness(BlockPos param0) {
      return this.getMaxLocalRawBrightness(param0, this.getSkyDarken());
   }

   default int getMaxLocalRawBrightness(BlockPos param0, int param1) {
      return param0.getX() >= -30000000 && param0.getZ() >= -30000000 && param0.getX() < 30000000 && param0.getZ() < 30000000
         ? this.getRawBrightness(param0, param1)
         : 15;
   }

   @Deprecated
   default boolean hasChunkAt(int param0, int param1) {
      return this.hasChunk(SectionPos.blockToSectionCoord(param0), SectionPos.blockToSectionCoord(param1));
   }

   @Deprecated
   default boolean hasChunkAt(BlockPos param0) {
      return this.hasChunkAt(param0.getX(), param0.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(BlockPos param0, BlockPos param1) {
      return this.hasChunksAt(param0.getX(), param0.getY(), param0.getZ(), param1.getX(), param1.getY(), param1.getZ());
   }

   @Deprecated
   default boolean hasChunksAt(int param0, int param1, int param2, int param3, int param4, int param5) {
      return param4 >= this.getMinBuildHeight() && param1 < this.getMaxBuildHeight() ? this.hasChunksAt(param0, param2, param3, param5) : false;
   }

   @Deprecated
   default boolean hasChunksAt(int param0, int param1, int param2, int param3) {
      int var5 = SectionPos.blockToSectionCoord(param0);
      int var6 = SectionPos.blockToSectionCoord(param2);
      int var7 = SectionPos.blockToSectionCoord(param1);
      int var8 = SectionPos.blockToSectionCoord(param3);

      for (int var9 = var5; var9 <= var6; var9++) {
         for (int var10 = var7; var10 <= var8; var10++) {
            if (!this.hasChunk(var9, var10)) {
               return false;
            }
         }
      }

      return true;
   }

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> param0) {
      Registry<T> var2 = this.registryAccess().registryOrThrow(param0);
      return var2.asLookup().filterFeatures(this.enabledFeatures());
   }
}
