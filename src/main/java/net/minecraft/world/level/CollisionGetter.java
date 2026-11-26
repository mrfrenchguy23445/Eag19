package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
   WorldBorder getWorldBorder();

   @Nullable
   BlockGetter getChunkForCollisions(int var1, int var2);

   default boolean isUnobstructed(@Nullable Entity param0, VoxelShape param1) {
      return true;
   }

   default boolean isUnobstructed(BlockState param0, BlockPos param1, CollisionContext param2) {
      VoxelShape var4 = param0.getCollisionShape(this, param1, param2);
      return var4.isEmpty() || this.isUnobstructed(null, var4.move(param1.getX(), param1.getY(), param1.getZ()));
   }

   default boolean isUnobstructed(Entity param0) {
      return this.isUnobstructed(param0, Shapes.create(param0.getBoundingBox()));
   }

   default boolean noCollision(AABB param0) {
      return this.noCollision(null, param0);
   }

   default boolean noCollision(Entity param0) {
      return this.noCollision(param0, param0.getBoundingBox());
   }

   default boolean noCollision(@Nullable Entity param0, AABB param1) {
      for (VoxelShape var4 : this.getBlockCollisions(param0, param1)) {
         if (!var4.isEmpty()) {
            return false;
         }
      }

      if (!this.getEntityCollisions(param0, param1).isEmpty()) {
         return false;
      }

      if (param0 == null) {
         return true;
      }

      VoxelShape var3 = this.borderCollision(param0, param1);
      return var3 == null || !Shapes.joinIsNotEmpty(var3, Shapes.create(param1), BooleanOp.AND);
   }

   List<VoxelShape> getEntityCollisions(@Nullable Entity var1, AABB var2);

   default Iterable<VoxelShape> getCollisions(@Nullable Entity param0, AABB param1) {
      List<VoxelShape> var3 = this.getEntityCollisions(param0, param1);
      Iterable<VoxelShape> var4 = this.getBlockCollisions(param0, param1);
      return var3.isEmpty() ? var4 : Iterables.concat(var3, var4);
   }

   default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity param0, AABB param1) {
      return () -> new BlockCollisions(this, param0, param1);
   }

   @Nullable
   private VoxelShape borderCollision(Entity param0, AABB param1) {
      WorldBorder var3 = this.getWorldBorder();
      return var3.isInsideCloseToBorder(param0, param1) ? var3.getCollisionShape() : null;
   }

   default boolean collidesWithSuffocatingBlock(@Nullable Entity param0, AABB param1) {
      BlockCollisions var3 = new BlockCollisions(this, param0, param1, true);

      while (var3.hasNext()) {
         if (!var3.next().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   default Optional<Vec3> findFreePosition(@Nullable Entity param0, VoxelShape param1, Vec3 param2, double param3, double param5, double param7) {
      if (param1.isEmpty()) {
         return Optional.empty();
      }

      AABB var10 = param1.bounds().inflate(param3, param5, param7);
      VoxelShape var11 = StreamSupport.stream(this.getBlockCollisions(param0, var10).spliterator(), false)
         .filter(l_param0 -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(l_param0.bounds()))
         .flatMap(l_param0 -> l_param0.toAabbs().stream())
         .map(l_param6 -> l_param6.inflate(param3 / 2.0, param5 / 2.0, param7 / 2.0))
         .map(Shapes::create)
         .reduce(Shapes.empty(), Shapes::or);
      VoxelShape var12 = Shapes.join(param1, var11, BooleanOp.ONLY_FIRST);
      return var12.closestPointTo(param2);
   }
}
