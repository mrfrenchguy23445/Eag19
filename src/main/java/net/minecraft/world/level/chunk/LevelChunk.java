package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
      @Override
      public void tick() {
      }

      @Override
      public boolean isRemoved() {
         return true;
      }

      @Override
      public BlockPos getPos() {
         return BlockPos.ZERO;
      }

      @Override
      public String getType() {
         return "<null>";
      }
   };
   private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
   private boolean loaded;
   private boolean clientLightReady = false;
   final Level level;
   @Nullable
   private Supplier<ChunkHolder.FullChunkStatus> fullStatus;
   @Nullable
   private LevelChunk.PostLoadProcessor postLoad;
   private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
   private final LevelChunkTicks<Block> blockTicks;
   private final LevelChunkTicks<Fluid> fluidTicks;

   public LevelChunk(Level param0, ChunkPos param1) {
      this(param0, param1, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
   }

   public LevelChunk(
      Level param0,
      ChunkPos param1,
      UpgradeData param2,
      LevelChunkTicks<Block> param3,
      LevelChunkTicks<Fluid> param4,
      long param5,
      @Nullable LevelChunkSection[] param7,
      @Nullable LevelChunk.PostLoadProcessor param8,
      @Nullable BlendingData param9
   ) {
      super(param1, param2, param0, param0.registryAccess().registryOrThrow(Registries.BIOME), param5, param7, param9);
      this.level = param0;
      this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap<>();

      for (Heightmap.Types var14 : Heightmap.Types.values()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(var14)) {
            this.heightmaps.put(var14, new Heightmap(this, var14));
         }
      }

      this.postLoad = param8;
      this.blockTicks = param3;
      this.fluidTicks = param4;
   }

   public LevelChunk(ServerLevel param0, ProtoChunk param1, @Nullable LevelChunk.PostLoadProcessor param2) {
      this(
         param0,
         param1.getPos(),
         param1.getUpgradeData(),
         param1.unpackBlockTicks(),
         param1.unpackFluidTicks(),
         param1.getInhabitedTime(),
         param1.getSections(),
         param2,
         param1.getBlendingData()
      );

      for (BlockEntity var5 : param1.getBlockEntities().values()) {
         this.setBlockEntity(var5);
      }

      this.pendingBlockEntities.putAll(param1.getBlockEntityNbts());

      for (int var4 = 0; var4 < param1.getPostProcessing().length; var4++) {
         this.postProcessing[var4] = param1.getPostProcessing()[var4];
      }

      this.setAllStarts(param1.getAllStarts());
      this.setAllReferences(param1.getAllReferences());

      for (Entry<Heightmap.Types, Heightmap> var5 : param1.getHeightmaps()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(var5.getKey())) {
            this.setHeightmap(var5.getKey(), var5.getValue().getRawData());
         }
      }

      this.setLightCorrect(param1.isLightCorrect());
      this.unsaved = true;
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
   public GameEventListenerRegistry getListenerRegistry(int param0) {
      return this.level instanceof ServerLevel var2
         ? this.gameEventListenerRegistrySections.computeIfAbsent(param0, l_param1 -> new EuclideanGameEventListenerRegistry(var2))
         : super.getListenerRegistry(param0);
   }

   @Override
   public BlockState getBlockState(BlockPos param0) {
      int var2 = param0.getX();
      int var3 = param0.getY();
      int var4 = param0.getZ();
      if (this.level.isDebug()) {
         BlockState var5 = null;
         if (var3 == 60) {
            var5 = Blocks.BARRIER.defaultBlockState();
         }

         if (var3 == 70) {
            var5 = DebugLevelSource.getBlockStateFor(var2, var4);
         }

         return var5 == null ? Blocks.AIR.defaultBlockState() : var5;
      } else {
         try {
            int var5 = this.getSectionIndex(var3);
            if (var5 >= 0 && var5 < this.sections.length) {
               LevelChunkSection var6 = this.sections[var5];
               if (!var6.hasOnlyAir()) {
                  return var6.getBlockState(var2 & 15, var3 & 15, var4 & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable var5) {
            CrashReport var6 = CrashReport.forThrowable(var5, "Getting block state");
            CrashReportCategory var7 = var6.addCategory("Block being got");
            var7.setDetail("Location", () -> CrashReportCategory.formatLocation(this, var2, var3, var4));
            throw new ReportedException(var6);
         }
      }
   }

   @Override
   public FluidState getFluidState(BlockPos param0) {
      return this.getFluidState(param0.getX(), param0.getY(), param0.getZ());
   }

   public FluidState getFluidState(int param0, int param1, int param2) {
      try {
         int var4 = this.getSectionIndex(param1);
         if (var4 >= 0 && var4 < this.sections.length) {
            LevelChunkSection var5 = this.sections[var4];
            if (!var5.hasOnlyAir()) {
               return var5.getFluidState(param0 & 15, param1 & 15, param2 & 15);
            }
         }

         return Fluids.EMPTY.defaultFluidState();
      } catch (Throwable var4) {
         CrashReport var5 = CrashReport.forThrowable(var4, "Getting fluid state");
         CrashReportCategory var6 = var5.addCategory("Block being got");
         var6.setDetail("Location", () -> CrashReportCategory.formatLocation(this, param0, param1, param2));
         throw new ReportedException(var5);
      }
   }

   @Nullable
   @Override
   public BlockState setBlockState(BlockPos param0, BlockState param1, boolean param2) {
      int var4 = param0.getY();
      LevelChunkSection var5 = this.getSection(this.getSectionIndex(var4));
      boolean var6 = var5.hasOnlyAir();
      if (var6 && param1.isAir()) {
         return null;
      }

      int var7 = param0.getX() & 15;
      int var8 = var4 & 15;
      int var9 = param0.getZ() & 15;
      BlockState var10 = var5.setBlockState(var7, var8, var9, param1);
      if (var10 == param1) {
         return null;
      }

      Block var11 = param1.getBlock();
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(var7, var4, var9, param1);
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(var7, var4, var9, param1);
      this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(var7, var4, var9, param1);
      this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(var7, var4, var9, param1);
      boolean var12 = var5.hasOnlyAir();
      if (var6 != var12) {
         this.level.getChunkSource().getLightEngine().updateSectionStatus(param0, var12);
      }

      boolean var13 = var10.hasBlockEntity();
      if (!this.level.isClientSide) {
         var10.onRemove(this.level, param0, param1, param2);
      } else if (!var10.is(var11) && var13) {
         this.removeBlockEntity(param0);
      }

      if (!var5.getBlockState(var7, var8, var9).is(var11)) {
         return null;
      }

      if (!this.level.isClientSide) {
         param1.onPlace(this.level, param0, var10, param2);
      }

      if (param1.hasBlockEntity()) {
         BlockEntity var14 = this.getBlockEntity(param0, LevelChunk.EntityCreationType.CHECK);
         if (var14 == null) {
            var14 = ((EntityBlock)var11).newBlockEntity(param0, param1);
            if (var14 != null) {
               this.addAndRegisterBlockEntity(var14);
            }
         } else {
            var14.setBlockState(param1);
            this.updateBlockEntityTicker(var14);
         }
      }

      this.unsaved = true;
      return var10;
   }

   @Deprecated
   @Override
   public void addEntity(Entity param0) {
   }

   @Nullable
   private BlockEntity createBlockEntity(BlockPos param0) {
      BlockState var2 = this.getBlockState(param0);
      return !var2.hasBlockEntity() ? null : ((EntityBlock)var2.getBlock()).newBlockEntity(param0, var2);
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos param0) {
      return this.getBlockEntity(param0, LevelChunk.EntityCreationType.CHECK);
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos param0, LevelChunk.EntityCreationType param1) {
      BlockEntity var3 = this.blockEntities.get(param0);
      if (var3 == null) {
         CompoundTag var4 = this.pendingBlockEntities.remove(param0);
         if (var4 != null) {
            BlockEntity var5 = this.promotePendingBlockEntity(param0, var4);
            if (var5 != null) {
               return var5;
            }
         }
      }

      if (var3 == null) {
         if (param1 == LevelChunk.EntityCreationType.IMMEDIATE) {
            var3 = this.createBlockEntity(param0);
            if (var3 != null) {
               this.addAndRegisterBlockEntity(var3);
            }
         }
      } else if (var3.isRemoved()) {
         this.blockEntities.remove(param0);
         return null;
      }

      return var3;
   }

   public void addAndRegisterBlockEntity(BlockEntity param0) {
      this.setBlockEntity(param0);
      if (this.isInLevel()) {
         if (this.level instanceof ServerLevel var2) {
            this.addGameEventListener(param0, var2);
         }

         this.updateBlockEntityTicker(param0);
      }
   }

   private boolean isInLevel() {
      return this.loaded || this.level.isClientSide();
   }

   boolean isTicking(BlockPos param0) {
      if (!this.level.getWorldBorder().isWithinBounds(param0)) {
         return false;
      } else {
         return !(this.level instanceof ServerLevel var2)
            ? true
            : this.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING) && var2.areEntitiesLoaded(ChunkPos.asLong(param0));
      }
   }

   @Override
   public void setBlockEntity(BlockEntity param0) {
      BlockPos var2 = param0.getBlockPos();
      if (this.getBlockState(var2).hasBlockEntity()) {
         param0.setLevel(this.level);
         param0.clearRemoved();
         BlockEntity var3 = this.blockEntities.put(var2.immutable(), param0);
         if (var3 != null && var3 != param0) {
            var3.setRemoved();
         }
      }
   }

   @Nullable
   @Override
   public CompoundTag getBlockEntityNbtForSaving(BlockPos param0) {
      BlockEntity var2 = this.getBlockEntity(param0);
      if (var2 != null && !var2.isRemoved()) {
         CompoundTag var3 = var2.saveWithFullMetadata();
         var3.putBoolean("keepPacked", false);
         return var3;
      }

      CompoundTag var3 = this.pendingBlockEntities.get(param0);
      if (var3 != null) {
         var3 = var3.copy();
         var3.putBoolean("keepPacked", true);
      }

      return var3;
   }

   @Override
   public void removeBlockEntity(BlockPos param0) {
      if (this.isInLevel()) {
         BlockEntity var2 = this.blockEntities.remove(param0);
         if (var2 != null) {
            if (this.level instanceof ServerLevel var3) {
               this.removeGameEventListener(var2, var3);
            }

            var2.setRemoved();
         }
      }

      this.removeBlockEntityTicker(param0);
   }

   private <T extends BlockEntity> void removeGameEventListener(T param0, ServerLevel param1) {
      Block var3 = param0.getBlockState().getBlock();
      if (var3 instanceof EntityBlock) {
         GameEventListener var4 = ((EntityBlock)var3).getListener(param1, param0);
         if (var4 != null) {
            int var5 = SectionPos.blockToSectionCoord(param0.getBlockPos().getY());
            GameEventListenerRegistry var6 = this.getListenerRegistry(var5);
            var6.unregister(var4);
            if (var6.isEmpty()) {
               this.gameEventListenerRegistrySections.remove(var5);
            }
         }
      }
   }

   private void removeBlockEntityTicker(BlockPos param0) {
      LevelChunk.RebindableTickingBlockEntityWrapper var2 = this.tickersInLevel.remove(param0);
      if (var2 != null) {
         var2.rebind(NULL_TICKER);
      }
   }

   public void runPostLoad() {
      if (this.postLoad != null) {
         this.postLoad.run(this);
         this.postLoad = null;
      }
   }

   public boolean isEmpty() {
      return false;
   }

   public void replaceWithPacketData(FriendlyByteBuf param0, CompoundTag param1, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> param2) {
      this.clearAllBlockEntities();

      for (LevelChunkSection var7 : this.sections) {
         var7.read(param0);
      }

      for (Heightmap.Types var7 : Heightmap.Types.values()) {
         String var8 = var7.getSerializationKey();
         if (param1.contains(var8, 12)) {
            this.setHeightmap(var7, param1.getLongArray(var8));
         }
      }

      param2.accept((l_param0, l_param1, l_param2) -> {
         BlockEntity l_var4 = this.getBlockEntity(l_param0, LevelChunk.EntityCreationType.IMMEDIATE);
         if (l_var4 != null && l_param2 != null && l_var4.getType() == l_param1) {
            l_var4.load(l_param2);
         }
      });
   }

   public void replaceBiomes(FriendlyByteBuf param0) {
      for (LevelChunkSection var5 : this.sections) {
         var5.readBiomes(param0);
      }
   }

   public void setLoaded(boolean param0) {
      this.loaded = param0;
   }

   public Level getLevel() {
      return this.level;
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   @Override
   public Stream<BlockPos> getLights() {
      return StreamSupport.stream(
            BlockPos.betweenClosed(
                  this.chunkPos.getMinBlockX(),
                  this.getMinBuildHeight(),
                  this.chunkPos.getMinBlockZ(),
                  this.chunkPos.getMaxBlockX(),
                  this.getMaxBuildHeight() - 1,
                  this.chunkPos.getMaxBlockZ()
               )
               .spliterator(),
            false
         )
         .filter(l_param0 -> this.getBlockState(l_param0).getLightEmission() != 0);
   }

   public void postProcessGeneration() {
      ChunkPos var1 = this.getPos();

      for (int var2 = 0; var2 < this.postProcessing.length; var2++) {
         if (this.postProcessing[var2] != null) {
            for (Short var4 : this.postProcessing[var2]) {
               BlockPos var5 = ProtoChunk.unpackOffsetCoordinates(var4, this.getSectionYFromSectionIndex(var2), var1);
               BlockState var6 = this.getBlockState(var5);
               FluidState var7 = var6.getFluidState();
               if (!var7.isEmpty()) {
                  var7.tick(this.level, var5);
               }

               if (!(var6.getBlock() instanceof LiquidBlock)) {
                  BlockState var8 = Block.updateFromNeighbourShapes(var6, this.level, var5);
                  this.level.setBlock(var5, var8, 20);
               }
            }

            this.postProcessing[var2].clear();
         }
      }

      for (BlockPos var3 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
         this.getBlockEntity(var3);
      }

      this.pendingBlockEntities.clear();
      this.upgradeData.upgrade(this);
   }

   @Nullable
   private BlockEntity promotePendingBlockEntity(BlockPos param0, CompoundTag param1) {
      BlockState var4 = this.getBlockState(param0);
      BlockEntity var3;
      if ("DUMMY".equals(param1.getString("id"))) {
         if (var4.hasBlockEntity()) {
            var3 = ((EntityBlock)var4.getBlock()).newBlockEntity(param0, var4);
         } else {
            var3 = null;
            LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", param0, var4);
         }
      } else {
         var3 = BlockEntity.loadStatic(param0, var4, param1);
      }

      if (var3 != null) {
         var3.setLevel(this.level);
         this.addAndRegisterBlockEntity(var3);
      } else {
         LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", var4, param0);
      }

      return var3;
   }

   public void unpackTicks(long param0) {
      this.blockTicks.unpack(param0);
      this.fluidTicks.unpack(param0);
   }

   public void registerTickContainerInLevel(ServerLevel param0) {
      param0.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
      param0.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
   }

   public void unregisterTickContainerFromLevel(ServerLevel param0) {
      param0.getBlockTicks().removeContainer(this.chunkPos);
      param0.getFluidTicks().removeContainer(this.chunkPos);
   }

   @Override
   public ChunkStatus getStatus() {
      return ChunkStatus.FULL;
   }

   public ChunkHolder.FullChunkStatus getFullStatus() {
      return this.fullStatus == null ? ChunkHolder.FullChunkStatus.BORDER : this.fullStatus.get();
   }

   public void setFullStatus(Supplier<ChunkHolder.FullChunkStatus> param0) {
      this.fullStatus = param0;
   }

   public void clearAllBlockEntities() {
      this.blockEntities.values().forEach(BlockEntity::setRemoved);
      this.blockEntities.clear();
      this.tickersInLevel.values().forEach(l_param0 -> l_param0.rebind(NULL_TICKER));
      this.tickersInLevel.clear();
   }

   public void registerAllBlockEntitiesAfterLevelLoad() {
      this.blockEntities.values().forEach(l_param0 -> {
         if (this.level instanceof ServerLevel l_var2) {
            this.addGameEventListener(l_param0, l_var2);
         }

         this.updateBlockEntityTicker(l_param0);
      });
   }

   private <T extends BlockEntity> void addGameEventListener(T param0, ServerLevel param1) {
      Block var3 = param0.getBlockState().getBlock();
      if (var3 instanceof EntityBlock) {
         GameEventListener var4 = ((EntityBlock)var3).getListener(param1, param0);
         if (var4 != null) {
            this.getListenerRegistry(SectionPos.blockToSectionCoord(param0.getBlockPos().getY())).register(var4);
         }
      }
   }

   private <T extends BlockEntity> void updateBlockEntityTicker(T param0) {
      BlockState var2 = param0.getBlockState();
      BlockEntityTicker<T> var3 = var2.getTicker(this.level, (BlockEntityType<T>)param0.getType());
      if (var3 == null) {
         this.removeBlockEntityTicker(param0.getBlockPos());
      } else {
         this.tickersInLevel.compute(param0.getBlockPos(), (l_param2, l_param3) -> {
            TickingBlockEntity l_var5 = this.createTicker(param0, var3);
            if (l_param3 != null) {
               l_param3.rebind(l_var5);
               return (LevelChunk.RebindableTickingBlockEntityWrapper)l_param3;
            } else if (this.isInLevel()) {
               LevelChunk.RebindableTickingBlockEntityWrapper l_var6 = new LevelChunk.RebindableTickingBlockEntityWrapper(l_var5);
               this.level.addBlockEntityTicker(l_var6);
               return l_var6;
            } else {
               return null;
            }
         });
      }
   }

   private <T extends BlockEntity> TickingBlockEntity createTicker(T param0, BlockEntityTicker<T> param1) {
      return new LevelChunk.BoundTickingBlockEntity<>(param0, param1);
   }

   public boolean isClientLightReady() {
      return this.clientLightReady;
   }

   public void setClientLightReady(boolean param0) {
      this.clientLightReady = param0;
   }

   class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
      private final T blockEntity;
      private final BlockEntityTicker<T> ticker;
      private boolean loggedInvalidBlockState;

      BoundTickingBlockEntity(T param1, BlockEntityTicker<T> param2) {
         this.blockEntity = param1;
         this.ticker = param2;
      }

      @Override
      public void tick() {
         if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
            BlockPos var1 = this.blockEntity.getBlockPos();
            if (LevelChunk.this.isTicking(var1)) {
               try {
                  ProfilerFiller var2 = LevelChunk.this.level.getProfiler();
                  var2.push(this::getType);
                  BlockState var3 = LevelChunk.this.getBlockState(var1);
                  if (this.blockEntity.getType().isValid(var3)) {
                     this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), var3, this.blockEntity);
                     this.loggedInvalidBlockState = false;
                  } else if (!this.loggedInvalidBlockState) {
                     this.loggedInvalidBlockState = true;
                     LevelChunk.LOGGER
                        .warn("Block entity {} @ {} state {} invalid for ticking:", LogUtils.defer(this::getType), LogUtils.defer(this::getPos), var3);
                  }

                  var2.pop();
               } catch (Throwable var2) {
                  CrashReport var3 = CrashReport.forThrowable(var2, "Ticking block entity");
                  CrashReportCategory var4 = var3.addCategory("Block entity being ticked");
                  this.blockEntity.fillCrashReportCategory(var4);
                  throw new ReportedException(var3);
               }
            }
         }
      }

      @Override
      public boolean isRemoved() {
         return this.blockEntity.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.blockEntity.getBlockPos();
      }

      @Override
      public String getType() {
         return BlockEntityType.getKey(this.blockEntity.getType()).toString();
      }

      @Override
      public String toString() {
         return "Level ticker for " + this.getType() + "@" + this.getPos();
      }
   }

   public enum EntityCreationType {
      IMMEDIATE,
      QUEUED,
      CHECK;
   }

   @FunctionalInterface
   public interface PostLoadProcessor {
      void run(LevelChunk var1);
   }

   class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
      private TickingBlockEntity ticker;

      RebindableTickingBlockEntityWrapper(TickingBlockEntity param1) {
         this.ticker = param1;
      }

      void rebind(TickingBlockEntity param0) {
         this.ticker = param0;
      }

      @Override
      public void tick() {
         this.ticker.tick();
      }

      @Override
      public boolean isRemoved() {
         return this.ticker.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.ticker.getPos();
      }

      @Override
      public String getType() {
         return this.ticker.getType();
      }

      @Override
      public String toString() {
         return this.ticker.toString() + " <wrapped>";
      }
   }
}
