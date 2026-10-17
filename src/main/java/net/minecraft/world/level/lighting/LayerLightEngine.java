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
   private static final int CACHE_SIZE = 2;
   private final long[] lastChunkPos = new long[2];
   private final BlockGetter[] lastChunk = new BlockGetter[2];

   public LayerLightEngine(LightChunkGetter param0, LightLayer param1, S param2) {
      super(16, 256, 8192);
      this.chunkSource = param0;
      this.layer = param1;
      this.storage = param2;
      this.clearCache();
   }

   @Override
   protected void checkNode(long param0) {
      this.storage.runAllUpdates();
      if (this.storage.storingLightForSection(SectionPos.blockToSection(param0))) {
         super.checkNode(param0);
      }
   }

   @Nullable
   private BlockGetter getChunk(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);

      for (int var5 = 0; var5 < 2; var5++) {
         if (var3 == this.lastChunkPos[var5]) {
            return this.lastChunk[var5];
         }
      }

      BlockGetter var5 = this.chunkSource.getChunkForLighting(param0, param1);

      for (int var6 = 1; var6 > 0; var6--) {
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
      if (param0 == Long.MAX_VALUE) {
         if (param2 != null) {
            param2.setValue(0);
         }

         return Blocks.AIR.defaultBlockState();
      } else {
         int var4 = SectionPos.blockToSectionCoord(BlockPos.getX(param0));
         int var5 = SectionPos.blockToSectionCoord(BlockPos.getZ(param0));
         BlockGetter var6 = this.getChunk(var4, var5);
         if (var6 == null) {
            if (param2 != null) {
               param2.setValue(16);
            }

            return Blocks.BEDROCK.defaultBlockState();
         } else {
            this.pos.set(param0);
            BlockState var7 = var6.getBlockState(this.pos);
            boolean var8 = var7.canOcclude() && var7.useShapeForLightOcclusion();
            if (param2 != null) {
               param2.setValue(var7.getLightBlock(this.chunkSource.getLevel(), this.pos));
            }

            return var8 ? var7 : Blocks.AIR.defaultBlockState();
         }
      }
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
      long var2 = param0.asLong();
      this.checkNode(var2);

      for (Direction var7 : DIRECTIONS) {
         this.checkNode(BlockPos.offset(var2, var7));
      }
   }

   @Override
   public void onBlockEmissionIncrease(BlockPos param0, int param1) {
   }

   @Override
   public void updateSectionStatus(SectionPos param0, boolean param1) {
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
