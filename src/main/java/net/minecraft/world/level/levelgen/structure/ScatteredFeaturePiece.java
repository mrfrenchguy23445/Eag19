package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
   protected final int width;
   protected final int height;
   protected final int depth;
   protected int heightPosition = -1;

   protected ScatteredFeaturePiece(StructurePieceType param0, int param1, int param2, int param3, int param4, int param5, int param6, Direction param7) {
      super(param0, 0, StructurePiece.makeBoundingBox(param1, param2, param3, param7, param4, param5, param6));
      this.width = param4;
      this.height = param5;
      this.depth = param6;
      this.setOrientation(param7);
   }

   protected ScatteredFeaturePiece(StructurePieceType param0, CompoundTag param1) {
      super(param0, param1);
      this.width = param1.getInt("Width");
      this.height = param1.getInt("Height");
      this.depth = param1.getInt("Depth");
      this.heightPosition = param1.getInt("HPos");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
      param1.putInt("Width", this.width);
      param1.putInt("Height", this.height);
      param1.putInt("Depth", this.depth);
      param1.putInt("HPos", this.heightPosition);
   }

   protected boolean updateAverageGroundHeight(LevelAccessor param0, BoundingBox param1, int param2) {
      if (this.heightPosition >= 0) {
         return true;
      }

      int var4 = 0;
      int var5 = 0;
      BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

      for (int var7 = this.boundingBox.minZ(); var7 <= this.boundingBox.maxZ(); var7++) {
         for (int var8 = this.boundingBox.minX(); var8 <= this.boundingBox.maxX(); var8++) {
            var6.set(var8, 64, var7);
            if (param1.isInside(var6)) {
               var4 += param0.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var6).getY();
               var5++;
            }
         }
      }

      if (var5 == 0) {
         return false;
      }

      this.heightPosition = var4 / var5;
      this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + param2, 0);
      return true;
   }

   protected boolean updateHeightPositionToLowestGroundHeight(LevelAccessor param0, int param1) {
      if (this.heightPosition >= 0) {
         return true;
      }

      int var3 = param0.getMaxBuildHeight();
      boolean var4 = false;
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int var6 = this.boundingBox.minZ(); var6 <= this.boundingBox.maxZ(); var6++) {
         for (int var7 = this.boundingBox.minX(); var7 <= this.boundingBox.maxX(); var7++) {
            var5.set(var7, 0, var6);
            var3 = Math.min(var3, param0.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var5).getY());
            var4 = true;
         }
      }

      if (!var4) {
         return false;
      }

      this.heightPosition = var3;
      this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + param1, 0);
      return true;
   }
}
