package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
   protected ChunkTracker(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected boolean isSource(long param0) {
      return param0 == ChunkPos.INVALID_CHUNK_POS;
   }

   @Override
   protected void checkNeighborsAfterUpdate(long param0, int param2, boolean param3) {
      ChunkPos var5 = new ChunkPos(param0);
      int var6 = var5.x;
      int var7 = var5.z;

      for (int var8 = -1; var8 <= 1; var8++) {
         for (int var9 = -1; var9 <= 1; var9++) {
            long var10 = ChunkPos.asLong(var6 + var8, var7 + var9);
            if (var10 != param0) {
               this.checkNeighbor(param0, var10, param2, param3);
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long param0, long param2, int param4) {
      int var6 = param4;
      ChunkPos var7 = new ChunkPos(param0);
      int var8 = var7.x;
      int var9 = var7.z;

      for (int var10 = -1; var10 <= 1; var10++) {
         for (int var11 = -1; var11 <= 1; var11++) {
            long var12 = ChunkPos.asLong(var8 + var10, var9 + var11);
            if (var12 == param0) {
               var12 = ChunkPos.INVALID_CHUNK_POS;
            }

            if (var12 != param2) {
               int var14 = this.computeLevelFromNeighbor(var12, param0, this.getLevel(var12));
               if (var6 > var14) {
                  var6 = var14;
               }

               if (var6 == 0) {
                  return var6;
               }
            }
         }
      }

      return var6;
   }

   @Override
   protected int computeLevelFromNeighbor(long param0, long param2, int param4) {
      return param0 == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(param2) : param4 + 1;
   }

   protected abstract int getLevelFromSource(long var1);

   public void update(long param0, int param2, boolean param3) {
      this.checkEdge(ChunkPos.INVALID_CHUNK_POS, param0, param2, param3);
   }
}
