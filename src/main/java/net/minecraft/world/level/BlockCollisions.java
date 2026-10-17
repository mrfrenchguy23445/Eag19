package net.minecraft.world.level;

import com.google.common.collect.AbstractIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCollisions extends AbstractIterator<VoxelShape> {
   private final AABB box;
   private final CollisionContext context;
   private final Cursor3D cursor;
   private final BlockPos.MutableBlockPos pos;
   private final VoxelShape entityShape;
   private final CollisionGetter collisionGetter;
   private final boolean onlySuffocatingBlocks;
   @Nullable
   private BlockGetter cachedBlockGetter;
   private long cachedBlockGetterPos;

   public BlockCollisions(CollisionGetter param0, @Nullable Entity param1, AABB param2) {
      this(param0, param1, param2, false);
   }

   public BlockCollisions(CollisionGetter param0, @Nullable Entity param1, AABB param2, boolean param3) {
      this.context = param1 == null ? CollisionContext.empty() : CollisionContext.of(param1);
      this.pos = new BlockPos.MutableBlockPos();
      this.entityShape = Shapes.create(param2);
      this.collisionGetter = param0;
      this.box = param2;
      this.onlySuffocatingBlocks = param3;
      int var5 = Mth.floor(param2.minX - 1.0E-7) - 1;
      int var6 = Mth.floor(param2.maxX + 1.0E-7) + 1;
      int var7 = Mth.floor(param2.minY - 1.0E-7) - 1;
      int var8 = Mth.floor(param2.maxY + 1.0E-7) + 1;
      int var9 = Mth.floor(param2.minZ - 1.0E-7) - 1;
      int var10 = Mth.floor(param2.maxZ + 1.0E-7) + 1;
      this.cursor = new Cursor3D(var5, var7, var9, var6, var8, var10);
   }

   @Nullable
   private BlockGetter getChunk(int param0, int param1) {
      int var3 = SectionPos.blockToSectionCoord(param0);
      int var4 = SectionPos.blockToSectionCoord(param1);
      long var5 = ChunkPos.asLong(var3, var4);
      if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == var5) {
         return this.cachedBlockGetter;
      }

      BlockGetter var7 = this.collisionGetter.getChunkForCollisions(var3, var4);
      this.cachedBlockGetter = var7;
      this.cachedBlockGetterPos = var5;
      return var7;
   }

   protected VoxelShape computeNext() {
      while (this.cursor.advance()) {
         int var1 = this.cursor.nextX();
         int var2 = this.cursor.nextY();
         int var3 = this.cursor.nextZ();
         int var4 = this.cursor.getNextType();
         if (var4 != 3) {
            BlockGetter var5 = this.getChunk(var1, var3);
            if (var5 != null) {
               this.pos.set(var1, var2, var3);
               BlockState var6 = var5.getBlockState(this.pos);
               if ((!this.onlySuffocatingBlocks || var6.isSuffocating(var5, this.pos))
                  && (var4 != 1 || var6.hasLargeCollisionShape())
                  && (var4 != 2 || var6.is(Blocks.MOVING_PISTON))) {
                  VoxelShape var7 = var6.getCollisionShape(this.collisionGetter, this.pos, this.context);
                  if (var7 == Shapes.block()) {
                     if (this.box.intersects(var1, var2, var3, var1 + 1.0, var2 + 1.0, var3 + 1.0)) {
                        return var7.move(var1, var2, var3);
                     }
                  } else {
                     VoxelShape var8 = var7.move(var1, var2, var3);
                     if (Shapes.joinIsNotEmpty(var8, this.entityShape, BooleanOp.AND)) {
                        return var8;
                     }
                  }
               }
            }
         }
      }

      return this.endOfData();
   }
}
