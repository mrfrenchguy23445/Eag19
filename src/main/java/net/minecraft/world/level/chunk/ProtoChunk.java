package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunk extends ChunkAccess {
   @Nullable
   private volatile LevelLightEngine lightEngine;
   private volatile ChunkStatus status = ChunkStatus.EMPTY;
   private final List<CompoundTag> entities = Lists.newArrayList();
   private final List<BlockPos> lights = Lists.newArrayList();
   private final Map<GenerationStep.Carving, CarvingMask> carvingMasks = new Object2ObjectArrayMap<>();
   @Nullable
   private BelowZeroRetrogen belowZeroRetrogen;
   private final ProtoChunkTicks<Block> blockTicks;
   private final ProtoChunkTicks<Fluid> fluidTicks;

   public ProtoChunk(ChunkPos param0, UpgradeData param1, LevelHeightAccessor param2, Registry<Biome> param3, @Nullable BlendingData param4) {
      this(param0, param1, null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), param2, param3, param4);
   }

   public ProtoChunk(
      ChunkPos param0,
      UpgradeData param1,
      @Nullable LevelChunkSection[] param2,
      ProtoChunkTicks<Block> param3,
      ProtoChunkTicks<Fluid> param4,
      LevelHeightAccessor param5,
      Registry<Biome> param6,
      @Nullable BlendingData param7
   ) {
      super(param0, param1, param5, param6, 0L, param2, param7);
      this.blockTicks = param3;
      this.fluidTicks = param4;
   }

   @Override
   public TickContainerAccess<Block> getBlockTicks() {
      return this.blockTicks;
   }

   @Override
   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public ChunkAccess.TicksToSave getTicksForSerialization() {
      return new ChunkAccess.TicksToSave(this.blockTicks, this.fluidTicks);
   }

   @Override
   public BlockState getBlockState(BlockPos param0) {
      int var2 = param0.getY();
      if (this.isOutsideBuildHeight(var2)) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      LevelChunkSection var3 = this.getSection(this.getSectionIndex(var2));
      return var3.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : var3.getBlockState(param0.getX() & 15, var2 & 15, param0.getZ() & 15);
   }

   @Override
   public FluidState getFluidState(BlockPos param0) {
      int var2 = param0.getY();
      if (this.isOutsideBuildHeight(var2)) {
         return Fluids.EMPTY.defaultFluidState();
      }

      LevelChunkSection var3 = this.getSection(this.getSectionIndex(var2));
      return var3.hasOnlyAir() ? Fluids.EMPTY.defaultFluidState() : var3.getFluidState(param0.getX() & 15, var2 & 15, param0.getZ() & 15);
   }

   @Override
   public Stream<BlockPos> getLights() {
      return this.lights.stream();
   }

   public ShortList[] getPackedLights() {
      ShortList[] var1 = new ShortList[this.getSectionsCount()];

      for (BlockPos var3 : this.lights) {
         ChunkAccess.getOrCreateOffsetList(var1, this.getSectionIndex(var3.getY())).add(packOffsetCoordinates(var3));
      }

      return var1;
   }

   public void addLight(short param0, int param1) {
      this.addLight(unpackOffsetCoordinates(param0, this.getSectionYFromSectionIndex(param1), this.chunkPos));
   }

   public void addLight(BlockPos param0) {
      this.lights.add(param0.immutable());
   }

   @Nullable
   @Override
   public BlockState setBlockState(BlockPos param0, BlockState param1, boolean param2) {
      int var4 = param0.getX();
      int var5 = param0.getY();
      int var6 = param0.getZ();
      if (var5 >= this.getMinBuildHeight() && var5 < this.getMaxBuildHeight()) {
         int var7 = this.getSectionIndex(var5);
         if (this.sections[var7].hasOnlyAir() && param1.is(Blocks.AIR)) {
            return param1;
         }

         if (param1.getLightEmission() > 0) {
            this.lights.add(new BlockPos((var4 & 15) + this.getPos().getMinBlockX(), var5, (var6 & 15) + this.getPos().getMinBlockZ()));
         }

         LevelChunkSection var8 = this.getSection(var7);
         BlockState var9 = var8.setBlockState(var4 & 15, var5 & 15, var6 & 15, param1);
         if (this.status.isOrAfter(ChunkStatus.FEATURES)
            && param1 != var9
            && (
               param1.getLightBlock(this, param0) != var9.getLightBlock(this, param0)
                  || param1.getLightEmission() != var9.getLightEmission()
                  || param1.useShapeForLightOcclusion()
                  || var9.useShapeForLightOcclusion()
            )) {
            this.lightEngine.checkBlock(param0);
         }

         Heightmap.Types[] var10 = this.heightmapsAfterCached();
         EnumSet<Heightmap.Types> var11 = null;

         for (int var12 = 0; var12 < var10.length; var12++) {
            Heightmap var14 = this.heightmaps.get(var10[var12]);
            if (var14 == null) {
               if (var11 == null) {
                  var11 = EnumSet.noneOf(Heightmap.Types.class);
               }

               var11.add(var10[var12]);
            }
         }

         if (var11 != null) {
            Heightmap.primeHeightmaps(this, var11);
         }

         for (int var12 = 0; var12 < var10.length; var12++) {
            this.heightmaps.get(var10[var12]).update(var4 & 15, var5, var6 & 15, param1);
         }

         return var9;
      } else {
         return Blocks.VOID_AIR.defaultBlockState();
      }
   }

   @Override
   public void setBlockEntity(BlockEntity param0) {
      this.blockEntities.put(param0.getBlockPos(), param0);
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos param0) {
      return this.blockEntities.get(param0);
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void addEntity(CompoundTag param0) {
      this.entities.add(param0);
   }

   @Override
   public void addEntity(Entity param0) {
      if (!param0.isPassenger()) {
         CompoundTag var2 = new CompoundTag();
         param0.save(var2);
         this.addEntity(var2);
      }
   }

   @Override
   public void setStartForStructure(Structure param0, StructureStart param1) {
      BelowZeroRetrogen var3 = this.getBelowZeroRetrogen();
      if (var3 != null && param1.isValid()) {
         BoundingBox var4 = param1.getBoundingBox();
         LevelHeightAccessor var5 = this.getHeightAccessorForGeneration();
         if (var4.minY() < var5.getMinBuildHeight() || var4.maxY() >= var5.getMaxBuildHeight()) {
            return;
         }
      }

      super.setStartForStructure(param0, param1);
   }

   public List<CompoundTag> getEntities() {
      return this.entities;
   }

   // Cooperative-port optimisation: getStatus().heightmapsAfter() used to build a
   // fresh EnumSet on EVERY setBlockState call (i.e. every block write in both the
   // noise and features stages). Cached per status; indexed array loops replace the
   // EnumSet iterators below.
   @Nullable
   private Heightmap.Types[] heightmapsAfterCache;

   private Heightmap.Types[] heightmapsAfterCached() {
      Heightmap.Types[] var1 = this.heightmapsAfterCache;
      if (var1 == null) {
         var1 = this.status.heightmapsAfter().toArray(new Heightmap.Types[0]);
         this.heightmapsAfterCache = var1;
      }

      return var1;
   }

   @Override
   public ChunkStatus getStatus() {
      return this.status;
   }

   public void setStatus(ChunkStatus param0) {
      this.status = param0;
      this.heightmapsAfterCache = null;
      if (this.belowZeroRetrogen != null && param0.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
         this.setBelowZeroRetrogen(null);
      }

      this.setUnsaved(true);
   }

   @Override
   public Holder<Biome> getNoiseBiome(int param0, int param1, int param2) {
      if (!this.getStatus().isOrAfter(ChunkStatus.BIOMES)
         && (this.belowZeroRetrogen == null || !this.belowZeroRetrogen.targetStatus().isOrAfter(ChunkStatus.BIOMES))) {
         throw new IllegalStateException("Asking for biomes before we have biomes");
      } else {
         return super.getNoiseBiome(param0, param1, param2);
      }
   }

   public static short packOffsetCoordinates(BlockPos param0) {
      int var1 = param0.getX();
      int var2 = param0.getY();
      int var3 = param0.getZ();
      int var4 = var1 & 15;
      int var5 = var2 & 15;
      int var6 = var3 & 15;
      return (short)(var4 | var5 << 4 | var6 << 8);
   }

   public static BlockPos unpackOffsetCoordinates(short param0, int param1, ChunkPos param2) {
      int var3 = SectionPos.sectionToBlockCoord(param2.x, param0 & 15);
      int var4 = SectionPos.sectionToBlockCoord(param1, param0 >>> 4 & 15);
      int var5 = SectionPos.sectionToBlockCoord(param2.z, param0 >>> 8 & 15);
      return new BlockPos(var3, var4, var5);
   }

   @Override
   public void markPosForPostprocessing(BlockPos param0) {
      if (!this.isOutsideBuildHeight(param0)) {
         ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(param0.getY())).add(packOffsetCoordinates(param0));
      }
   }

   @Override
   public void addPackedPostProcess(short param0, int param1) {
      ChunkAccess.getOrCreateOffsetList(this.postProcessing, param1).add(param0);
   }

   public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
      return Collections.unmodifiableMap(this.pendingBlockEntities);
   }

   @Nullable
   @Override
   public CompoundTag getBlockEntityNbtForSaving(BlockPos param0) {
      BlockEntity var2 = this.getBlockEntity(param0);
      return var2 != null ? var2.saveWithFullMetadata() : this.pendingBlockEntities.get(param0);
   }

   @Override
   public void removeBlockEntity(BlockPos param0) {
      this.blockEntities.remove(param0);
      this.pendingBlockEntities.remove(param0);
   }

   @Nullable
   public CarvingMask getCarvingMask(GenerationStep.Carving param0) {
      return this.carvingMasks.get(param0);
   }

   public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving param0) {
      return this.carvingMasks.computeIfAbsent(param0, l_param0 -> new CarvingMask(this.getHeight(), this.getMinBuildHeight()));
   }

   public void setCarvingMask(GenerationStep.Carving param0, CarvingMask param1) {
      this.carvingMasks.put(param0, param1);
   }

   public void setLightEngine(LevelLightEngine param0) {
      this.lightEngine = param0;
   }

   public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen param0) {
      this.belowZeroRetrogen = param0;
   }

   @Nullable
   @Override
   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return this.belowZeroRetrogen;
   }

   private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> param0) {
      return new LevelChunkTicks<>(param0.scheduledTicks());
   }

   public LevelChunkTicks<Block> unpackBlockTicks() {
      return unpackTicks(this.blockTicks);
   }

   public LevelChunkTicks<Fluid> unpackFluidTicks() {
      return unpackTicks(this.fluidTicks);
   }

   @Override
   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this;
   }
}
