package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker extends DynamicGraphMinFixedPoint {
   protected SectionTracker(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected boolean isSource(long param0) {
      return param0 == Long.MAX_VALUE;
   }

   @Override
   protected void checkNeighborsAfterUpdate(long param0, int param2, boolean param3) {
      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            for (int var7 = -1; var7 <= 1; var7++) {
               long var8 = SectionPos.offset(param0, var5, var6, var7);
               if (var8 != param0) {
                  this.checkNeighbor(param0, var8, param2, param3);
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long param0, long param2, int param4) {
      int var6 = param4;

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            for (int var9 = -1; var9 <= 1; var9++) {
               long var10 = SectionPos.offset(param0, var7, var8, var9);
               if (var10 == param0) {
                  var10 = Long.MAX_VALUE;
               }

               if (var10 != param2) {
                  int var12 = this.computeLevelFromNeighbor(var10, param0, this.getLevel(var10));
                  if (var6 > var12) {
                     var6 = var12;
                  }

                  if (var6 == 0) {
                     return var6;
                  }
               }
            }
         }
      }

      return var6;
   }

   @Override
   protected int computeLevelFromNeighbor(long param0, long param2, int param4) {
      return param0 == Long.MAX_VALUE ? this.getLevelFromSource(param2) : param4 + 1;
   }

   protected abstract int getLevelFromSource(long var1);

   public void update(long param0, int param2, boolean param3) {
      this.checkEdge(Long.MAX_VALUE, param0, param2, param3);
   }
}
