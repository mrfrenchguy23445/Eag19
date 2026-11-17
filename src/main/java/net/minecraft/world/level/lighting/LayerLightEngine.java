package net.minecraft.world.level.lighting;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class LayerLightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>>
   extends DynamicGraphMinFixedPoint
   implements LayerLightEventListener {
   public static final long SELF_SOURCE = Long.MAX_VALUE;
   private static final Direction[] DIRECTIONS = Direction.values();
   protected final LightChunkGetter chunkSource;
   protected final LightLayer layer;
   protected final S storage;
   private boolean runningLightUpdates;
   protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
   // Lithium-style cache widening: vanilla keeps only a 2-slot chunk MRU, and the
   // light BFS regularly skips backwards and forwards between 2 chunks (column
   // edges, under-tree air columns), turning a hit into an eviction of the OTHER
   // hot chunk - a 50% thrash rate on getChunkForLighting. 4 slots covers the
   // observed working set without changing lookup order semantics (most-recently
   // used is still scanned first, on-miss still shifts the whole MRU).
   private static final int CACHE_SIZE = 4;
   private final long[] lastChunkPos = new long[CACHE_SIZE];
   private final BlockGetter[] lastChunk = new BlockGetter[CACHE_SIZE];
   // A single-entry block-state cache for the propagation source. getComputedLevel()
   // asks computeLevelFromNeighbor() about all six neighbours in a row, and every one
   // of those calls re-reads the block state of the same center position. A chunk
   // lookup + palette decode + light-block/shape query per edge is the dominant cost
   // of the BFS, so one slot removes ~6/7 of those reads. The cache is dropped at
   // the start of every checkBlock and every BFS batch, so a block edit can never be
   // observed through a stale entry.
   private long cachedStatePos = Long.MAX_VALUE;
   private BlockState cachedState = Blocks.AIR.defaultBlockState();
   private int cachedStateOpacity;
   private boolean suppressStorageFlush;

   public LayerLightEngine(LightChunkGetter param0, LightLayer param1, S param2) {
      super(16, 256, 8192);
      this.chunkSource = param0;
      this.layer = param1;
      this.storage = param2;
      this.clearCache();
   }

   @Override
   protected void checkNode(long param0) {
      if (!this.suppressStorageFlush) {
         this.storage.runAllUpdates();
      }

      if (this.storage.storingLightForSection(SectionPos.blockToSection(param0))) {
         super.checkNode(param0);
      }
   }

   @Nullable
   private BlockGetter getChunk(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);

      for (int var5 = 0; var5 < CACHE_SIZE; var5++) {
         if (var3 == this.lastChunkPos[var5]) {
            return this.lastChunk[var5];
         }
      }

      BlockGetter var5 = this.chunkSource.getChunkForLighting(param0, param1);

      for (int var6 = CACHE_SIZE - 1; var6 > 0; var6--) {
         this.lastChunkPos[var6] = this.lastChunkPos[var6 - 1];
         this.lastChunk[var6] = this.lastChunk[var6 - 1];
      }

      this.lastChunkPos[0] = var3;
      this.lastChunk[0] = var5;
      return var5;
   }

   private void clearCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunk, null);
   }

   protected BlockState getStateAndOpacity(long param0, @Nullable MutableInt param2) {
      if (param0 == this.cachedStatePos) {
         if (param2 != null) {
            param2.setValue(this.cachedStateOpacity);
         }

         return this.cachedState;
      }

      BlockState var7;
      int var9;
      if (param0 == Long.MAX_VALUE) {
         var9 = 0;
         var7 = Blocks.AIR.defaultBlockState();
      } else {
         int var4 = SectionPos.blockToSectionCoord(BlockPos.getX(param0));
         int var5 = SectionPos.blockToSectionCoord(BlockPos.getZ(param0));
         BlockGetter var6 = this.getChunk(var4, var5);
         if (var6 == null) {
            var9 = 16;
            var7 = Blocks.BEDROCK.defaultBlockState();
         } else {
            this.pos.set(param0);
            BlockState var8 = var6.getBlockState(this.pos);
            boolean var10 = var8.canOcclude() && var8.useShapeForLightOcclusion();
            var9 = var8.getLightBlock(this.chunkSource.getLevel(), this.pos);
            var7 = var10 ? var8 : Blocks.AIR.defaultBlockState();
         }
      }

      this.cachedStatePos = param0;
      this.cachedState = var7;
      this.cachedStateOpacity = var9;
      if (param2 != null) {
         param2.setValue(var9);
      }

      return var7;
   }

   /** Drops the cached source state; called whenever positions may have changed. */
   protected final void invalidateCachedState() {
      this.cachedStatePos = Long.MAX_VALUE;
      // Long.MAX_VALUE is also the SELF_SOURCE sentinel, so the slot must hold the
      // sentinel's answer (air, no opacity) rather than whatever block was cached
      // before the invalidation.
      this.cachedState = Blocks.AIR.defaultBlockState();
      this.cachedStateOpacity = 0;
   }

   protected VoxelShape getShape(BlockState param0, long param1, Direction param3) {
      return param0.canOcclude() ? param0.getFaceOcclusionShape(this.chunkSource.getLevel(), this.pos.set(param1), param3) : Shapes.empty();
   }

   public static int getLightBlockInto(BlockGetter param0, BlockState param1, BlockPos param2, BlockState param3, BlockPos param4, Direction param5, int param6) {
      boolean var7 = param1.canOcclude() && param1.useShapeForLightOcclusion();
      boolean var8 = param3.canOcclude() && param3.useShapeForLightOcclusion();
      if (!var7 && !var8) {
         return param6;
      }

      VoxelShape var9 = var7 ? param1.getOcclusionShape(param0, param2) : Shapes.empty();
      VoxelShape var10 = var8 ? param3.getOcclusionShape(param0, param4) : Shapes.empty();
      return Shapes.mergedFaceOccludes(var9, var10, param5) ? 16 : param6;
   }

   @Override
   protected boolean isSource(long param0) {
      return param0 == Long.MAX_VALUE;
   }

   @Override
   protected int getComputedLevel(long param0, long param2, int param4) {
      return 0;
   }

   @Override
   protected int getLevel(long param0) {
      return param0 == Long.MAX_VALUE ? 0 : 15 - this.storage.getStoredLevel(param0);
   }

   protected int getLevel(DataLayer param0, long param1) {
      return 15
         - param0.get(
            SectionPos.sectionRelative(BlockPos.getX(param1)),
            SectionPos.sectionRelative(BlockPos.getY(param1)),
            SectionPos.sectionRelative(BlockPos.getZ(param1))
         );
   }

   @Override
   protected void setLevel(long param0, int param2) {
      this.storage.setStoredLevel(param0, Math.min(15, 15 - param2));
   }

   @Override
   protected int computeLevelFromNeighbor(long param0, long param2, int param4) {
      return 0;
   }

   @Override
   public boolean hasLightWork() {
      return this.hasWork() || this.storage.hasWork() || this.storage.hasInconsistencies();
   }

   @Override
   public int runUpdates(int param0, boolean param1, boolean param2) {
      this.invalidateCachedState();
      if (!this.runningLightUpdates) {
         if (this.storage.hasWork()) {
            param0 = this.storage.runUpdates(param0);
            if (param0 == 0) {
               return param0;
            }
         }

         this.storage.markNewInconsistencies(this, param1, param2);
      }

      this.runningLightUpdates = true;
      if (this.hasWork()) {
         param0 = this.runUpdates(param0);
         this.clearCache();
         if (param0 == 0) {
            return param0;
         }
      }

      this.runningLightUpdates = false;
      this.storage.swapSectionMap();
      return param0;
   }

   protected void queueSectionData(long param0, @Nullable DataLayer param2, boolean param3) {
      this.storage.queueSectionData(param0, param2, param3);
   }

   @Nullable
   @Override
   public DataLayer getDataLayerData(SectionPos param0) {
      return this.storage.getDataLayerData(param0.asLong());
   }

   @Override
   public int getLightValue(BlockPos param0) {
      return this.storage.getLightValue(param0.asLong());
   }

   public String getDebugData(long param0) {
      return this.storage.getLevel(param0) + "";
   }

   @Override
   public void checkBlock(BlockPos param0) {
      // The state cache must never survive a block edit. This is also the only
      // place where vanilla can see a changed block, so it doubles as the
      // invalidation point for the whole engine.
      this.invalidateCachedState();
      // Vanilla flushes queued section data once per checkNode(), i.e. seven
      // times for one block edit. One flush is enough for the whole edit.
      this.storage.runAllUpdates();
      this.suppressStorageFlush = true;

      try {
         long var2 = param0.asLong();
         this.checkNode(var2);

         for (Direction var7 : DIRECTIONS) {
            this.checkNode(BlockPos.offset(var2, var7));
         }
      } finally {
         this.suppressStorageFlush = false;
      }
   }

   @Override
   public void onBlockEmissionIncrease(BlockPos param0, int param1) {
   }

   @Override
   public void updateSectionStatus(SectionPos param0, boolean param1) {
      // A section appearing or disappearing can change which chunk backs a
      // position, so the cached source state cannot outlive it.
      this.invalidateCachedState();
      this.storage.updateSectionStatus(param0.asLong(), param1);
   }

   @Override
   public void enableLightSources(ChunkPos param0, boolean param1) {
      long var3 = SectionPos.getZeroNode(SectionPos.asLong(param0.x, 0, param0.z));
      this.storage.enableLightSources(var3, param1);
   }

   public void retainData(ChunkPos param0, boolean param1) {
      long var3 = SectionPos.getZeroNode(SectionPos.asLong(param0.x, 0, param0.z));
      this.storage.retainData(var3, param1);
   }
}
