package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class VoxelShape {
   protected final DiscreteVoxelShape shape;
   @Nullable
   private VoxelShape[] faces;

   VoxelShape(DiscreteVoxelShape param0) {
      this.shape = param0;
   }

   public double min(Direction.Axis param0) {
      int var2 = this.shape.firstFull(param0);
      return var2 >= this.shape.getSize(param0) ? Double.POSITIVE_INFINITY : this.get(param0, var2);
   }

   public double max(Direction.Axis param0) {
      int var2 = this.shape.lastFull(param0);
      return var2 <= 0 ? Double.NEGATIVE_INFINITY : this.get(param0, var2);
   }

   public AABB bounds() {
      if (this.isEmpty()) {
         throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
      } else {
         return new AABB(
            this.min(Direction.Axis.X),
            this.min(Direction.Axis.Y),
            this.min(Direction.Axis.Z),
            this.max(Direction.Axis.X),
            this.max(Direction.Axis.Y),
            this.max(Direction.Axis.Z)
         );
      }
   }

   protected double get(Direction.Axis param0, int param1) {
      return this.getCoords(param0).getDouble(param1);
   }

   protected abstract DoubleList getCoords(Direction.Axis var1);

   public boolean isEmpty() {
      return this.shape.isEmpty();
   }

   public VoxelShape move(double param0, double param2, double param4) {
      return this.isEmpty()
         ? Shapes.empty()
         : new ArrayVoxelShape(
            this.shape,
            new OffsetDoubleList(this.getCoords(Direction.Axis.X), param0),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Y), param2),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Z), param4)
         );
   }

   public VoxelShape optimize() {
      VoxelShape[] var1 = new VoxelShape[]{Shapes.empty()};
      this.forAllBoxes(
         (l_param1, l_param3, l_param5, l_param7, l_param9, l_param11) -> var1[0] = Shapes.joinUnoptimized(
            var1[0], Shapes.box(l_param1, l_param3, l_param5, l_param7, l_param9, l_param11), BooleanOp.OR
         )
      );
      return var1[0];
   }

   public void forAllEdges(Shapes.DoubleLineConsumer param0) {
      this.shape
         .forAllEdges(
            (l_param1, l_param2, l_param3, l_param4, l_param5, l_param6) -> param0.consume(
               this.get(Direction.Axis.X, l_param1),
               this.get(Direction.Axis.Y, l_param2),
               this.get(Direction.Axis.Z, l_param3),
               this.get(Direction.Axis.X, l_param4),
               this.get(Direction.Axis.Y, l_param5),
               this.get(Direction.Axis.Z, l_param6)
            ),
            true
         );
   }

   public void forAllBoxes(Shapes.DoubleLineConsumer param0) {
      DoubleList var2 = this.getCoords(Direction.Axis.X);
      DoubleList var3 = this.getCoords(Direction.Axis.Y);
      DoubleList var4 = this.getCoords(Direction.Axis.Z);
      this.shape
         .forAllBoxes(
            (l_param4, l_param5, l_param6, l_param7, l_param8, l_param9) -> param0.consume(
               var2.getDouble(l_param4),
               var3.getDouble(l_param5),
               var4.getDouble(l_param6),
               var2.getDouble(l_param7),
               var3.getDouble(l_param8),
               var4.getDouble(l_param9)
            ),
            true
         );
   }

   public List<AABB> toAabbs() {
      List<AABB> var1 = Lists.newArrayList();
      this.forAllBoxes(
         (l_param1, l_param3, l_param5, l_param7, l_param9, l_param11) -> var1.add(new AABB(l_param1, l_param3, l_param5, l_param7, l_param9, l_param11))
      );
      return var1;
   }

   public double min(Direction.Axis param0, double param1, double param3) {
      Direction.Axis var6 = AxisCycle.FORWARD.cycle(param0);
      Direction.Axis var7 = AxisCycle.BACKWARD.cycle(param0);
      int var8 = this.findIndex(var6, param1);
      int var9 = this.findIndex(var7, param3);
      int var10 = this.shape.firstFull(param0, var8, var9);
      return var10 >= this.shape.getSize(param0) ? Double.POSITIVE_INFINITY : this.get(param0, var10);
   }

   public double max(Direction.Axis param0, double param1, double param3) {
      Direction.Axis var6 = AxisCycle.FORWARD.cycle(param0);
      Direction.Axis var7 = AxisCycle.BACKWARD.cycle(param0);
      int var8 = this.findIndex(var6, param1);
      int var9 = this.findIndex(var7, param3);
      int var10 = this.shape.lastFull(param0, var8, var9);
      return var10 <= 0 ? Double.NEGATIVE_INFINITY : this.get(param0, var10);
   }

   protected int findIndex(Direction.Axis param0, double param1) {
      return Mth.binarySearch(0, this.shape.getSize(param0) + 1, l_param3 -> param1 < this.get(param0, l_param3)) - 1;
   }

   @Nullable
   public BlockHitResult clip(Vec3 param0, Vec3 param1, BlockPos param2) {
      if (this.isEmpty()) {
         return null;
      }

      Vec3 var4 = param1.subtract(param0);
      if (var4.lengthSqr() < 1.0E-7) {
         return null;
      }

      Vec3 var5 = param0.add(var4.scale(0.001));
      return this.shape
            .isFullWide(
               this.findIndex(Direction.Axis.X, var5.x - param2.getX()),
               this.findIndex(Direction.Axis.Y, var5.y - param2.getY()),
               this.findIndex(Direction.Axis.Z, var5.z - param2.getZ())
            )
         ? new BlockHitResult(var5, Direction.getNearest(var4.x, var4.y, var4.z).getOpposite(), param2, true)
         : AABB.clip(this.toAabbs(), param0, param1, param2);
   }

   public Optional<Vec3> closestPointTo(Vec3 param0) {
      if (this.isEmpty()) {
         return Optional.empty();
      }

      Vec3[] var2 = new Vec3[1];
      this.forAllBoxes((l_param2, l_param4, l_param6, l_param8, l_param10, l_param12) -> {
         double l_var14 = Mth.clamp(param0.x(), l_param2, l_param8);
         double l_var16 = Mth.clamp(param0.y(), l_param4, l_param10);
         double l_var18 = Mth.clamp(param0.z(), l_param6, l_param12);
         if (var2[0] == null || param0.distanceToSqr(l_var14, l_var16, l_var18) < param0.distanceToSqr(var2[0])) {
            var2[0] = new Vec3(l_var14, l_var16, l_var18);
         }
      });
      return Optional.of(var2[0]);
   }

   public VoxelShape getFaceShape(Direction param0) {
      if (!this.isEmpty() && this != Shapes.block()) {
         if (this.faces != null) {
            VoxelShape var2 = this.faces[param0.ordinal()];
            if (var2 != null) {
               return var2;
            }
         } else {
            this.faces = new VoxelShape[6];
         }

         VoxelShape var2 = this.calculateFace(param0);
         this.faces[param0.ordinal()] = var2;
         return var2;
      } else {
         return this;
      }
   }

   private VoxelShape calculateFace(Direction param0) {
      Direction.Axis var2 = param0.getAxis();
      DoubleList var3 = this.getCoords(var2);
      if (var3.size() == 2 && DoubleMath.fuzzyEquals(var3.getDouble(0), 0.0, 1.0E-7) && DoubleMath.fuzzyEquals(var3.getDouble(1), 1.0, 1.0E-7)) {
         return this;
      }

      Direction.AxisDirection var4 = param0.getAxisDirection();
      int var5 = this.findIndex(var2, var4 == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
      return new SliceShape(this, var2, var5);
   }

   public double collide(Direction.Axis param0, AABB param1, double param2) {
      return this.collideX(AxisCycle.between(param0, Direction.Axis.X), param1, param2);
   }

   protected double collideX(AxisCycle param0, AABB param1, double param2) {
      if (this.isEmpty()) {
         return param2;
      }

      if (Math.abs(param2) < 1.0E-7) {
         return 0.0;
      }

      AxisCycle var5 = param0.inverse();
      Direction.Axis var6 = var5.cycle(Direction.Axis.X);
      Direction.Axis var7 = var5.cycle(Direction.Axis.Y);
      Direction.Axis var8 = var5.cycle(Direction.Axis.Z);
      double var9 = param1.max(var6);
      double var11 = param1.min(var6);
      int var13 = this.findIndex(var6, var11 + 1.0E-7);
      int var14 = this.findIndex(var6, var9 - 1.0E-7);
      int var15 = Math.max(0, this.findIndex(var7, param1.min(var7) + 1.0E-7));
      int var16 = Math.min(this.shape.getSize(var7), this.findIndex(var7, param1.max(var7) - 1.0E-7) + 1);
      int var17 = Math.max(0, this.findIndex(var8, param1.min(var8) + 1.0E-7));
      int var18 = Math.min(this.shape.getSize(var8), this.findIndex(var8, param1.max(var8) - 1.0E-7) + 1);
      int var19 = this.shape.getSize(var6);
      if (param2 > 0.0) {
         for (int var20 = var14 + 1; var20 < var19; var20++) {
            for (int var21 = var15; var21 < var16; var21++) {
               for (int var22 = var17; var22 < var18; var22++) {
                  if (this.shape.isFullWide(var5, var20, var21, var22)) {
                     double var23 = this.get(var6, var20) - var9;
                     if (var23 >= -1.0E-7) {
                        param2 = Math.min(param2, var23);
                     }

                     return param2;
                  }
               }
            }
         }
      } else if (param2 < 0.0) {
         for (int var20 = var13 - 1; var20 >= 0; var20--) {
            for (int var21 = var15; var21 < var16; var21++) {
               for (int var22 = var17; var22 < var18; var22++) {
                  if (this.shape.isFullWide(var5, var20, var21, var22)) {
                     double var23 = this.get(var6, var20 + 1) - var11;
                     if (var23 <= 1.0E-7) {
                        param2 = Math.max(param2, var23);
                     }

                     return param2;
                  }
               }
            }
         }
      }

      return param2;
   }

   @Override
   public String toString() {
      return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
   }
}
