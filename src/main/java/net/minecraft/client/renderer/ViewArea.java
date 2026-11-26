package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class ViewArea {
   protected final LevelRenderer levelRenderer;
   protected final Level level;
   protected int chunkGridSizeY;
   protected int chunkGridSizeX;
   protected int chunkGridSizeZ;
   public ChunkRenderDispatcher.RenderChunk[] chunks;

   public ViewArea(ChunkRenderDispatcher param0, Level param1, int param2, LevelRenderer param3) {
      this.levelRenderer = param3;
      this.level = param1;
      this.setViewDistance(param2);
      this.createChunks(param0);
   }

   protected void createChunks(ChunkRenderDispatcher param0) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("createChunks called from wrong thread: " + Thread.currentThread().getName());
      }

      int var2 = this.chunkGridSizeX * this.chunkGridSizeY * this.chunkGridSizeZ;
      this.chunks = new ChunkRenderDispatcher.RenderChunk[var2];

      for (int var3 = 0; var3 < this.chunkGridSizeX; var3++) {
         for (int var4 = 0; var4 < this.chunkGridSizeY; var4++) {
            for (int var5 = 0; var5 < this.chunkGridSizeZ; var5++) {
               int var6 = this.getChunkIndex(var3, var4, var5);
               this.chunks[var6] = param0.new RenderChunk(var6, var3 * 16, var4 * 16, var5 * 16);
            }
         }
      }
   }

   public void releaseAllBuffers() {
      for (ChunkRenderDispatcher.RenderChunk var4 : this.chunks) {
         var4.releaseBuffers();
      }
   }

   private int getChunkIndex(int param0, int param1, int param2) {
      return (param2 * this.chunkGridSizeY + param1) * this.chunkGridSizeX + param0;
   }

   protected void setViewDistance(int param0) {
      int var2 = param0 * 2 + 1;
      this.chunkGridSizeX = var2;
      this.chunkGridSizeY = this.level.getSectionsCount();
      this.chunkGridSizeZ = var2;
   }

   public void repositionCamera(double param0, double param2) {
      int var5 = Mth.ceil(param0);
      int var6 = Mth.ceil(param2);

      for (int var7 = 0; var7 < this.chunkGridSizeX; var7++) {
         int var8 = this.chunkGridSizeX * 16;
         int var9 = var5 - 8 - var8 / 2;
         int var10 = var9 + Math.floorMod(var7 * 16 - var9, var8);

         for (int var11 = 0; var11 < this.chunkGridSizeZ; var11++) {
            int var12 = this.chunkGridSizeZ * 16;
            int var13 = var6 - 8 - var12 / 2;
            int var14 = var13 + Math.floorMod(var11 * 16 - var13, var12);

            for (int var15 = 0; var15 < this.chunkGridSizeY; var15++) {
               int var16 = this.level.getMinBuildHeight() + var15 * 16;
               ChunkRenderDispatcher.RenderChunk var17 = this.chunks[this.getChunkIndex(var7, var15, var11)];
               BlockPos var18 = var17.getOrigin();
               if (var10 != var18.getX() || var16 != var18.getY() || var14 != var18.getZ()) {
                  var17.setOrigin(var10, var16, var14);
               }
            }
         }
      }
   }

   public void setDirty(int param0, int param1, int param2, boolean param3) {
      int var5 = Math.floorMod(param0, this.chunkGridSizeX);
      int var6 = Math.floorMod(param1 - this.level.getMinSection(), this.chunkGridSizeY);
      int var7 = Math.floorMod(param2, this.chunkGridSizeZ);
      ChunkRenderDispatcher.RenderChunk var8 = this.chunks[this.getChunkIndex(var5, var6, var7)];
      var8.setDirty(param3);
   }

   @Nullable
   protected ChunkRenderDispatcher.RenderChunk getRenderChunkAt(BlockPos param0) {
      int var2 = Mth.floorDiv(param0.getX(), 16);
      int var3 = Mth.floorDiv(param0.getY() - this.level.getMinBuildHeight(), 16);
      int var4 = Mth.floorDiv(param0.getZ(), 16);
      if (var3 >= 0 && var3 < this.chunkGridSizeY) {
         var2 = Mth.positiveModulo(var2, this.chunkGridSizeX);
         var4 = Mth.positiveModulo(var4, this.chunkGridSizeZ);
         return this.chunks[this.getChunkIndex(var2, var3, var4)];
      } else {
         return null;
      }
   }
}
