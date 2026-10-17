package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BlockGetter, BiomeManager.NoiseBiomeSource, StructureAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
   protected final ShortList[] postProcessing;
   protected volatile boolean unsaved;
   private volatile boolean isLightCorrect;
   protected final ChunkPos chunkPos;
   private long inhabitedTime;
   @Nullable
   @Deprecated
   private BiomeGenerationSettings carverBiomeSettings;
   @Nullable
   protected NoiseChunk noiseChunk;
   protected final UpgradeData upgradeData;
   @Nullable
   protected BlendingData blendingData;
   protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
   private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
   private final Map<Structure, LongSet> structuresRefences = Maps.newHashMap();
   protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
   protected final Map<BlockPos, BlockEntity> blockEntities = Maps.newHashMap();
   protected final LevelHeightAccessor levelHeightAccessor;
   protected final LevelChunkSection[] sections;

   public ChunkAccess(
      ChunkPos param0,
      UpgradeData param1,
      LevelHeightAccessor param2,
      Registry<Biome> param3,
      long param4,
      @Nullable LevelChunkSection[] param6,
      @Nullable BlendingData param7
   ) {
      this.chunkPos = param0;
      this.upgradeData = param1;
      this.levelHeightAccessor = param2;
      this.sections = new LevelChunkSection[param2.getSectionsCount()];
      this.inhabitedTime = param4;
      this.postProcessing = new ShortList[param2.getSectionsCount()];
      this.blendingData = param7;
      if (param6 != null) {
         if (this.sections.length == param6.length) {
            System.arraycopy(param6, 0, this.sections, 0, this.sections.length);
         } else {
            LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", param6.length, this.sections.length);
         }
      }

      replaceMissingSections(param2, param3, this.sections);
   }

   private static void replaceMissingSections(LevelHeightAccessor param0, Registry<Biome> param1, LevelChunkSection[] param2) {
      for (int var3 = 0; var3 < param2.length; var3++) {
         if (param2[var3] == null) {
            param2[var3] = new LevelChunkSection(param0.getSectionYFromSectionIndex(var3), param1);
         }
      }
   }

   public GameEventListenerRegistry getListenerRegistry(int param0) {
      return GameEventListenerRegistry.NOOP;
   }

   @Nullable
   public abstract BlockState setBlockState(BlockPos var1, BlockState var2, boolean var3);

   public abstract void setBlockEntity(BlockEntity var1);

   public abstract void addEntity(Entity var1);

   @Nullable
   public LevelChunkSection getHighestSection() {
      LevelChunkSection[] var1 = this.getSections();

      for (int var2 = var1.length - 1; var2 >= 0; var2--) {
         LevelChunkSection var3 = var1[var2];
         if (!var3.hasOnlyAir()) {
            return var3;
         }
      }

      return null;
   }

   public int getHighestSectionPosition() {
      LevelChunkSection var1 = this.getHighestSection();
      return var1 == null ? this.getMinBuildHeight() : var1.bottomBlockY();
   }

   public Set<BlockPos> getBlockEntitiesPos() {
      Set<BlockPos> var1 = Sets.newHashSet(this.pendingBlockEntities.keySet());
      var1.addAll(this.blockEntities.keySet());
      return var1;
   }

   public LevelChunkSection[] getSections() {
      return this.sections;
   }

   public LevelChunkSection getSection(int param0) {
      return this.getSections()[param0];
   }

   public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
      return Collections.unmodifiableSet(this.heightmaps.entrySet());
   }

   public void setHeightmap(Heightmap.Types param0, long[] param1) {
      this.getOrCreateHeightmapUnprimed(param0).setRawData(this, param0, param1);
   }

   public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types param0) {
      return this.heightmaps.computeIfAbsent(param0, l_param0 -> new Heightmap(this, l_param0));
   }

   public boolean hasPrimedHeightmap(Heightmap.Types param0) {
      return this.heightmaps.get(param0) != null;
   }

   public int getHeight(Heightmap.Types param0, int param1, int param2) {
      Heightmap var4 = this.heightmaps.get(param0);
      if (var4 == null) {
         if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
            LOGGER.error("Unprimed heightmap: " + param0 + " " + param1 + " " + param2);
         }

         Heightmap.primeHeightmaps(this, EnumSet.of(param0));
         var4 = this.heightmaps.get(param0);
      }

      return var4.getFirstAvailable(param1 & 15, param2 & 15) - 1;
   }

   public ChunkPos getPos() {
      return this.chunkPos;
   }

   @Nullable
   @Override
   public StructureStart getStartForStructure(Structure param0) {
      return this.structureStarts.get(param0);
   }

   @Override
   public void setStartForStructure(Structure param0, StructureStart param1) {
      this.structureStarts.put(param0, param1);
      this.unsaved = true;
   }

   public Map<Structure, StructureStart> getAllStarts() {
      return Collections.unmodifiableMap(this.structureStarts);
   }

   public void setAllStarts(Map<Structure, StructureStart> param0) {
      this.structureStarts.clear();
      this.structureStarts.putAll(param0);
      this.unsaved = true;
   }

   @Override
   public LongSet getReferencesForStructure(Structure param0) {
      return this.structuresRefences.getOrDefault(param0, EMPTY_REFERENCE_SET);
   }

   @Override
   public void addReferenceForStructure(Structure param0, long param1) {
      this.structuresRefences.computeIfAbsent(param0, l_param0 -> new LongOpenHashSet()).add(param1);
      this.unsaved = true;
   }

   @Override
   public Map<Structure, LongSet> getAllReferences() {
      return Collections.unmodifiableMap(this.structuresRefences);
   }

   @Override
   public void setAllReferences(Map<Structure, LongSet> param0) {
      this.structuresRefences.clear();
      this.structuresRefences.putAll(param0);
      this.unsaved = true;
   }

   public boolean isYSpaceEmpty(int param0, int param1) {
      if (param0 < this.getMinBuildHeight()) {
         param0 = this.getMinBuildHeight();
      }

      if (param1 >= this.getMaxBuildHeight()) {
         param1 = this.getMaxBuildHeight() - 1;
      }

      for (int var3 = param0; var3 <= param1; var3 += 16) {
         if (!this.getSection(this.getSectionIndex(var3)).hasOnlyAir()) {
            return false;
         }
      }

      return true;
   }

   public void setUnsaved(boolean param0) {
      this.unsaved = param0;
   }

   public boolean isUnsaved() {
      return this.unsaved;
   }

   public abstract ChunkStatus getStatus();

   public abstract void removeBlockEntity(BlockPos var1);

   public void markPosForPostprocessing(BlockPos param0) {
      LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", param0);
   }

   public ShortList[] getPostProcessing() {
      return this.postProcessing;
   }

   public void addPackedPostProcess(short param0, int param1) {
      getOrCreateOffsetList(this.getPostProcessing(), param1).add(param0);
   }

   public void setBlockEntityNbt(CompoundTag param0) {
      this.pendingBlockEntities.put(BlockEntity.getPosFromTag(param0), param0);
   }

   @Nullable
   public CompoundTag getBlockEntityNbt(BlockPos param0) {
      return this.pendingBlockEntities.get(param0);
   }

   @Nullable
   public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos var1);

   public abstract Stream<BlockPos> getLights();

   public abstract TickContainerAccess<Block> getBlockTicks();

   public abstract TickContainerAccess<Fluid> getFluidTicks();

   public abstract ChunkAccess.TicksToSave getTicksForSerialization();

   public UpgradeData getUpgradeData() {
      return this.upgradeData;
   }

   public boolean isOldNoiseGeneration() {
      return this.blendingData != null;
   }

   @Nullable
   public BlendingData getBlendingData() {
      return this.blendingData;
   }

   public void setBlendingData(BlendingData param0) {
      this.blendingData = param0;
   }

   public long getInhabitedTime() {
      return this.inhabitedTime;
   }

   public void incrementInhabitedTime(long param0) {
      this.inhabitedTime += param0;
   }

   public void setInhabitedTime(long param0) {
      this.inhabitedTime = param0;
   }

   public static ShortList getOrCreateOffsetList(ShortList[] param0, int param1) {
      if (param0[param1] == null) {
         param0[param1] = new ShortArrayList();
      }

      return param0[param1];
   }

   public boolean isLightCorrect() {
      return this.isLightCorrect;
   }

   public void setLightCorrect(boolean param0) {
      this.isLightCorrect = param0;
      this.setUnsaved(true);
   }

   @Override
   public int getMinBuildHeight() {
      return this.levelHeightAccessor.getMinBuildHeight();
   }

   @Override
   public int getHeight() {
      return this.levelHeightAccessor.getHeight();
   }

   public NoiseChunk getOrCreateNoiseChunk(Function<ChunkAccess, NoiseChunk> param0) {
      if (this.noiseChunk == null) {
         this.noiseChunk = param0.apply(this);
      }

      return this.noiseChunk;
   }

   @Deprecated
   public BiomeGenerationSettings carverBiome(Supplier<BiomeGenerationSettings> param0) {
      if (this.carverBiomeSettings == null) {
         this.carverBiomeSettings = param0.get();
      }

      return this.carverBiomeSettings;
   }

   @Override
   public Holder<Biome> getNoiseBiome(int param0, int param1, int param2) {
      try {
         int var4 = QuartPos.fromBlock(this.getMinBuildHeight());
         int var5 = var4 + QuartPos.fromBlock(this.getHeight()) - 1;
         int var6 = Mth.clamp(param1, var4, var5);
         int var7 = this.getSectionIndex(QuartPos.toBlock(var6));
         return this.sections[var7].getNoiseBiome(param0 & 3, var6 & 3, param2 & 3);
      } catch (Throwable var4) {
         CrashReport var5 = CrashReport.forThrowable(var4, "Getting biome");
         CrashReportCategory var6 = var5.addCategory("Biome being got");
         var6.setDetail("Location", () -> CrashReportCategory.formatLocation(this, param0, param1, param2));
         throw new ReportedException(var5);
      }
   }

   public void fillBiomesFromNoise(BiomeResolver param0, Climate.Sampler param1) {
      ChunkPos var3 = this.getPos();
      int var4 = QuartPos.fromBlock(var3.getMinBlockX());
      int var5 = QuartPos.fromBlock(var3.getMinBlockZ());
      LevelHeightAccessor var6 = this.getHeightAccessorForGeneration();

      for (int var7 = var6.getMinSection(); var7 < var6.getMaxSection(); var7++) {
         LevelChunkSection var8 = this.getSection(this.getSectionIndexFromSectionY(var7));
         var8.fillBiomesFromNoise(param0, param1, var4, var5);
      }
   }

   public boolean hasAnyStructureReferences() {
      return !this.getAllReferences().isEmpty();
   }

   @Nullable
   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return null;
   }

   public boolean isUpgrading() {
      return this.getBelowZeroRetrogen() != null;
   }

   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this;
   }

   public record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {
   }
}
