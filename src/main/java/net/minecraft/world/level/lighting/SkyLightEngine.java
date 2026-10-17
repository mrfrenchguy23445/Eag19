package net.minecraft.world.level.lighting;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class SkyLightEngine extends LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

   public SkyLightEngine(LightChunkGetter param0) {
      super(param0, LightLayer.SKY, new SkyLightSectionStorage(param0));
   }

   @Override
   protected int computeLevelFromNeighbor(long param0, long param2, int param4) {
      if (param2 == Long.MAX_VALUE || param0 == Long.MAX_VALUE) {
         return 15;
      }

      if (param4 >= 15) {
         return param4;
      }

      MutableInt var6 = new MutableInt();
      BlockState var7 = this.getStateAndOpacity(param2, var6);
      if (var6.getValue() >= 15) {
         return 15;
      }

      int var8 = BlockPos.getX(param0);
      int var9 = BlockPos.getY(param0);
      int var10 = BlockPos.getZ(param0);
      int var11 = BlockPos.getX(param2);
      int var12 = BlockPos.getY(param2);
      int var13 = BlockPos.getZ(param2);
      int var14 = Integer.signum(var11 - var8);
      int var15 = Integer.signum(var12 - var9);
      int var16 = Integer.signum(var13 - var10);
      Direction var17 = Direction.fromNormal(var14, var15, var16);
      if (var17 == null) {
         throw new IllegalStateException(String.format(Locale.ROOT, "Light was spread in illegal direction %d, %d, %d", var14, var15, var16));
      }

      BlockState var18 = this.getStateAndOpacity(param0, null);
      VoxelShape var19 = this.getShape(var18, param0, var17);
      VoxelShape var20 = this.getShape(var7, param2, var17.getOpposite());
      if (Shapes.faceShapeOccludes(var19, var20)) {
         return 15;
      }

      boolean var21 = var8 == var11 && var10 == var13;
      boolean var22 = var21 && var9 > var12;
      return var22 && param4 == 0 && var6.getValue() == 0 ? 0 : param4 + Math.max(1, var6.getValue());
   }

   @Override
   protected void checkNeighborsAfterUpdate(long param0, int param2, boolean param3) {
      long var5 = SectionPos.blockToSection(param0);
      int var7 = BlockPos.getY(param0);
      int var8 = SectionPos.sectionRelative(var7);
      int var9 = SectionPos.blockToSectionCoord(var7);
      int var10;
      if (var8 != 0) {
         var10 = 0;
      } else {
         int var11 = 0;

         while (!this.storage.storingLightForSection(SectionPos.offset(var5, 0, -var11 - 1, 0)) && this.storage.hasSectionsBelow(var9 - var11 - 1)) {
            var11++;
         }

         var10 = var11;
      }

      long var11 = BlockPos.offset(param0, 0, -1 - var10 * 16, 0);
      long var13 = SectionPos.blockToSection(var11);
      if (var5 == var13 || this.storage.storingLightForSection(var13)) {
         this.checkNeighbor(param0, var11, param2, param3);
      }

      long var15 = BlockPos.offset(param0, Direction.UP);
      long var17 = SectionPos.blockToSection(var15);
      if (var5 == var17 || this.storage.storingLightForSection(var17)) {
         this.checkNeighbor(param0, var15, param2, param3);
      }

      for (Direction var22 : HORIZONTALS) {
         int var23 = 0;

         do {
            long var24 = BlockPos.offset(param0, var22.getStepX(), -var23, var22.getStepZ());
            long var26 = SectionPos.blockToSection(var24);
            if (var5 == var26) {
               this.checkNeighbor(param0, var24, param2, param3);
               break;
            }

            if (this.storage.storingLightForSection(var26)) {
               long var28 = BlockPos.offset(param0, 0, -var23, 0);
               this.checkNeighbor(var28, var24, param2, param3);
            }
         } while (++var23 <= var10 * 16);
      }
   }

   @Override
   protected int getComputedLevel(long param0, long param2, int param4) {
      int var6 = param4;
      long var7 = SectionPos.blockToSection(param0);
      DataLayer var9 = this.storage.getDataLayer(var7, true);

      for (Direction var13 : DIRECTIONS) {
         long var14 = BlockPos.offset(param0, var13);
         if (var14 != param2) {
            long var16 = SectionPos.blockToSection(var14);
            DataLayer var18;
            if (var7 == var16) {
               var18 = var9;
            } else {
               var18 = this.storage.getDataLayer(var16, true);
            }

            int var19;
            if (var18 != null) {
               var19 = this.getLevel(var18, var14);
            } else {
               if (var13 == Direction.DOWN) {
                  continue;
               }

               var19 = 15 - this.storage.getLightValue(var14, true);
            }

            int var20 = this.computeLevelFromNeighbor(var14, param0, var19);
            if (var6 > var20) {
               var6 = var20;
            }

            if (var6 == 0) {
               return var6;
            }
         }
      }

      return var6;
   }

   @Override
   protected void checkNode(long param0) {
      this.storage.runAllUpdates();
      long var3 = SectionPos.blockToSection(param0);
      if (this.storage.storingLightForSection(var3)) {
         super.checkNode(param0);
      } else {
         for (param0 = BlockPos.getFlatIndex(param0);
            !this.storage.storingLightForSection(var3) && !this.storage.isAboveData(var3);
            param0 = BlockPos.offset(param0, 0, 16, 0)
         ) {
            var3 = SectionPos.offset(var3, Direction.UP);
         }

         if (this.storage.storingLightForSection(var3)) {
            super.checkNode(param0);
         }
      }
   }

   @Override
   public String getDebugData(long param0) {
      return super.getDebugData(param0) + (this.storage.isAboveData(param0) ? "*" : "");
   }
}
