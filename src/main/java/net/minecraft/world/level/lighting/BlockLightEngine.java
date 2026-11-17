package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class BlockLightEngine extends LayerLightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
   // See SkyLightEngine.opacity: reused to keep the propagation inner loop
   // allocation-free.
   private final MutableInt opacity = new MutableInt();

   public BlockLightEngine(LightChunkGetter param0) {
      super(param0, LightLayer.BLOCK, new BlockLightSectionStorage(param0));
   }

   private int getLightEmission(long param0) {
      int var3 = BlockPos.getX(param0);
      int var4 = BlockPos.getY(param0);
      int var5 = BlockPos.getZ(param0);
      BlockGetter var6 = this.chunkSource.getChunkForLighting(SectionPos.blockToSectionCoord(var3), SectionPos.blockToSectionCoord(var5));
      return var6 != null ? var6.getLightEmission(this.pos.set(var3, var4, var5)) : 0;
   }

   @Override
   protected int computeLevelFromNeighbor(long param0, long param2, int param4) {
      if (param2 == Long.MAX_VALUE) {
         return 15;
      }

      if (param0 == Long.MAX_VALUE) {
         return param4 + 15 - this.getLightEmission(param2);
      }

      if (param4 >= 15) {
         return param4;
      }

      int var6 = Integer.signum(BlockPos.getX(param2) - BlockPos.getX(param0));
      int var7 = Integer.signum(BlockPos.getY(param2) - BlockPos.getY(param0));
      int var8 = Integer.signum(BlockPos.getZ(param2) - BlockPos.getZ(param0));
      Direction var9 = Direction.fromNormal(var6, var7, var8);
      if (var9 == null) {
         return 15;
      }

      BlockState var11 = this.getStateAndOpacity(param2, this.opacity);
      if (this.opacity.getValue() >= 15) {
         return 15;
      }

      BlockState var12 = this.getStateAndOpacity(param0, null);
      VoxelShape var13 = this.getShape(var12, param0, var9);
      VoxelShape var14 = this.getShape(var11, param2, var9.getOpposite());
      return Shapes.faceShapeOccludes(var13, var14) ? 15 : param4 + Math.max(1, this.opacity.getValue());
   }

   @Override
   protected void checkNeighborsAfterUpdate(long param0, int param2, boolean param3) {
      long var5 = SectionPos.blockToSection(param0);

      for (Direction var10 : DIRECTIONS) {
         long var11 = BlockPos.offset(param0, var10);
         long var13 = SectionPos.blockToSection(var11);
         if (var5 == var13 || this.storage.storingLightForSection(var13)) {
            this.checkNeighbor(param0, var11, param2, param3);
         }
      }
   }

   @Override
   protected int getComputedLevel(long param0, long param2, int param4) {
      int var6 = param4;
      if (Long.MAX_VALUE != param2) {
         int var7 = this.computeLevelFromNeighbor(Long.MAX_VALUE, param0, 0);
         if (var6 > var7) {
            var6 = var7;
         }

         if (var6 == 0) {
            return var6;
         }
      }

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

            if (var18 != null) {
               int var19 = this.computeLevelFromNeighbor(var14, param0, this.getLevel(var18, var14));
               if (var6 > var19) {
                  var6 = var19;
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
   public void onBlockEmissionIncrease(BlockPos param0, int param1) {
      this.storage.runAllUpdates();
      this.checkEdge(Long.MAX_VALUE, param0.asLong(), 15 - param1, true);
   }
}
