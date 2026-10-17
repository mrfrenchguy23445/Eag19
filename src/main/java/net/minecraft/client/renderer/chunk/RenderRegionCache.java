package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class RenderRegionCache {
   private final Long2ObjectMap<RenderRegionCache.ChunkInfo> chunkInfoCache = new Long2ObjectOpenHashMap<>();

   @Nullable
   public RenderChunkRegion createRegion(Level param0, BlockPos param1, BlockPos param2, int param3) {
      int var5 = SectionPos.blockToSectionCoord(param1.getX() - param3);
      int var6 = SectionPos.blockToSectionCoord(param1.getZ() - param3);
      int var7 = SectionPos.blockToSectionCoord(param2.getX() + param3);
      int var8 = SectionPos.blockToSectionCoord(param2.getZ() + param3);
      RenderRegionCache.ChunkInfo[][] var9 = new RenderRegionCache.ChunkInfo[var7 - var5 + 1][var8 - var6 + 1];

      for (int var10 = var5; var10 <= var7; var10++) {
         for (int var11 = var6; var11 <= var8; var11++) {
            var9[var10 - var5][var11 - var6] = this.chunkInfoCache
               .computeIfAbsent(
                  ChunkPos.asLong(var10, var11), l_param1 -> new RenderRegionCache.ChunkInfo(param0.getChunk(ChunkPos.getX(l_param1), ChunkPos.getZ(l_param1)))
               );
         }
      }

      if (isAllEmpty(param1, param2, var5, var6, var9)) {
         return null;
      }

      RenderChunk[][] var10 = new RenderChunk[var7 - var5 + 1][var8 - var6 + 1];

      for (int var11 = var5; var11 <= var7; var11++) {
         for (int var12 = var6; var12 <= var8; var12++) {
            var10[var11 - var5][var12 - var6] = var9[var11 - var5][var12 - var6].renderChunk();
         }
      }

      return new RenderChunkRegion(param0, var5, var6, var10);
   }

   private static boolean isAllEmpty(BlockPos param0, BlockPos param1, int param2, int param3, RenderRegionCache.ChunkInfo[][] param4) {
      int var5 = SectionPos.blockToSectionCoord(param0.getX());
      int var6 = SectionPos.blockToSectionCoord(param0.getZ());
      int var7 = SectionPos.blockToSectionCoord(param1.getX());
      int var8 = SectionPos.blockToSectionCoord(param1.getZ());

      for (int var9 = var5; var9 <= var7; var9++) {
         for (int var10 = var6; var10 <= var8; var10++) {
            LevelChunk var11 = param4[var9 - param2][var10 - param3].chunk();
            if (!var11.isYSpaceEmpty(param0.getY(), param1.getY())) {
               return false;
            }
         }
      }

      return true;
   }

   static final class ChunkInfo {
      private final LevelChunk chunk;
      @Nullable
      private RenderChunk renderChunk;

      ChunkInfo(LevelChunk param0) {
         this.chunk = param0;
      }

      public LevelChunk chunk() {
         return this.chunk;
      }

      public RenderChunk renderChunk() {
         if (this.renderChunk == null) {
            this.renderChunk = new RenderChunk(this.chunk);
         }

         return this.renderChunk;
      }
   }
}
