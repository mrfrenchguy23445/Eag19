package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;

public class StructurePiecesBuilder implements StructurePieceAccessor {
   private final List<StructurePiece> pieces = Lists.newArrayList();

   @Override
   public void addPiece(StructurePiece param0) {
      this.pieces.add(param0);
   }

   @Nullable
   @Override
   public StructurePiece findCollisionPiece(BoundingBox param0) {
      return StructurePiece.findCollisionPiece(this.pieces, param0);
   }

   @Deprecated
   public void offsetPiecesVertically(int param0) {
      for (StructurePiece var3 : this.pieces) {
         var3.move(0, param0, 0);
      }
   }

   @Deprecated
   public int moveBelowSeaLevel(int param0, int param1, RandomSource param2, int param3) {
      int var5 = param0 - param3;
      BoundingBox var6 = this.getBoundingBox();
      int var7 = var6.getYSpan() + param1 + 1;
      if (var7 < var5) {
         var7 += param2.nextInt(var5 - var7);
      }

      int var8 = var7 - var6.maxY();
      this.offsetPiecesVertically(var8);
      return var8;
   }

   /** @deprecated */
   public void moveInsideHeights(RandomSource param0, int param1, int param2) {
      BoundingBox var4 = this.getBoundingBox();
      int var5 = param2 - param1 + 1 - var4.getYSpan();
      int var6;
      if (var5 > 1) {
         var6 = param1 + param0.nextInt(var5);
      } else {
         var6 = param1;
      }

      int var7 = var6 - var4.minY();
      this.offsetPiecesVertically(var7);
   }

   public PiecesContainer build() {
      return new PiecesContainer(this.pieces);
   }

   public void clear() {
      this.pieces.clear();
   }

   public boolean isEmpty() {
      return this.pieces.isEmpty();
   }

   public BoundingBox getBoundingBox() {
      return StructurePiece.createBoundingBox(this.pieces.stream());
   }
}
