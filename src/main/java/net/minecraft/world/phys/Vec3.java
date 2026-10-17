package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class Vec3 implements Position {
   public static final Codec<Vec3> CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         param0 -> Util.fixedSize((List<Double>)param0, 3).map(l_param0 -> new Vec3(l_param0.get(0), l_param0.get(1), l_param0.get(2))),
         param0 -> List.of(param0.x(), param0.y(), param0.z())
      );
   public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
   public final double x;
   public final double y;
   public final double z;

   public static Vec3 fromRGB24(int param0) {
      double var1 = (param0 >> 16 & 0xFF) / 255.0;
      double var3 = (param0 >> 8 & 0xFF) / 255.0;
      double var5 = (param0 & 0xFF) / 255.0;
      return new Vec3(var1, var3, var5);
   }

   public static Vec3 atLowerCornerOf(Vec3i param0) {
      return new Vec3(param0.getX(), param0.getY(), param0.getZ());
   }

   public static Vec3 atLowerCornerWithOffset(Vec3i param0, double param1, double param3, double param5) {
      return new Vec3(param0.getX() + param1, param0.getY() + param3, param0.getZ() + param5);
   }

   public static Vec3 atCenterOf(Vec3i param0) {
      return atLowerCornerWithOffset(param0, 0.5, 0.5, 0.5);
   }

   public static Vec3 atBottomCenterOf(Vec3i param0) {
      return atLowerCornerWithOffset(param0, 0.5, 0.0, 0.5);
   }

   public static Vec3 upFromBottomCenterOf(Vec3i param0, double param1) {
      return atLowerCornerWithOffset(param0, 0.5, param1, 0.5);
   }

   public Vec3(double param0, double param2, double param4) {
      this.x = param0;
      this.y = param2;
      this.z = param4;
   }

   public Vec3(Vector3f param0) {
      this(param0.x(), param0.y(), param0.z());
   }

   public Vec3 vectorTo(Vec3 param0) {
      return new Vec3(param0.x - this.x, param0.y - this.y, param0.z - this.z);
   }

   public Vec3 normalize() {
      double var1 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
      return var1 < 1.0E-4 ? ZERO : new Vec3(this.x / var1, this.y / var1, this.z / var1);
   }

   public double dot(Vec3 param0) {
      return this.x * param0.x + this.y * param0.y + this.z * param0.z;
   }

   public Vec3 cross(Vec3 param0) {
      return new Vec3(this.y * param0.z - this.z * param0.y, this.z * param0.x - this.x * param0.z, this.x * param0.y - this.y * param0.x);
   }

   public Vec3 subtract(Vec3 param0) {
      return this.subtract(param0.x, param0.y, param0.z);
   }

   public Vec3 subtract(double param0, double param2, double param4) {
      return this.add(-param0, -param2, -param4);
   }

   public Vec3 add(Vec3 param0) {
      return this.add(param0.x, param0.y, param0.z);
   }

   public Vec3 add(double param0, double param2, double param4) {
      return new Vec3(this.x + param0, this.y + param2, this.z + param4);
   }

   public boolean closerThan(Position param0, double param1) {
      return this.distanceToSqr(param0.x(), param0.y(), param0.z()) < param1 * param1;
   }

   public double distanceTo(Vec3 param0) {
      double var2 = param0.x - this.x;
      double var4 = param0.y - this.y;
      double var6 = param0.z - this.z;
      return Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
   }

   public double distanceToSqr(Vec3 param0) {
      double var2 = param0.x - this.x;
      double var4 = param0.y - this.y;
      double var6 = param0.z - this.z;
      return var2 * var2 + var4 * var4 + var6 * var6;
   }

   public double distanceToSqr(double param0, double param2, double param4) {
      double var7 = param0 - this.x;
      double var9 = param2 - this.y;
      double var11 = param4 - this.z;
      return var7 * var7 + var9 * var9 + var11 * var11;
   }

   public Vec3 scale(double param0) {
      return this.multiply(param0, param0, param0);
   }

   public Vec3 reverse() {
      return this.scale(-1.0);
   }

   public Vec3 multiply(Vec3 param0) {
      return this.multiply(param0.x, param0.y, param0.z);
   }

   public Vec3 multiply(double param0, double param2, double param4) {
      return new Vec3(this.x * param0, this.y * param2, this.z * param4);
   }

   public Vec3 offsetRandom(RandomSource param0, float param1) {
      return this.add((param0.nextFloat() - 0.5F) * param1, (param0.nextFloat() - 0.5F) * param1, (param0.nextFloat() - 0.5F) * param1);
   }

   public double length() {
      return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
   }

   public double lengthSqr() {
      return this.x * this.x + this.y * this.y + this.z * this.z;
   }

   public double horizontalDistance() {
      return Math.sqrt(this.x * this.x + this.z * this.z);
   }

   public double horizontalDistanceSqr() {
      return this.x * this.x + this.z * this.z;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (!(param0 instanceof Vec3 var2)) {
         return false;
      } else if (Double.compare(var2.x, this.x) != 0) {
         return false;
      } else {
         return Double.compare(var2.y, this.y) != 0 ? false : Double.compare(var2.z, this.z) == 0;
      }
   }

   @Override
   public int hashCode() {
      long var2 = Double.doubleToLongBits(this.x);
      int var1 = (int)(var2 ^ var2 >>> 32);
      var2 = Double.doubleToLongBits(this.y);
      var1 = 31 * var1 + (int)(var2 ^ var2 >>> 32);
      var2 = Double.doubleToLongBits(this.z);
      return 31 * var1 + (int)(var2 ^ var2 >>> 32);
   }

   @Override
   public String toString() {
      return "(" + this.x + ", " + this.y + ", " + this.z + ")";
   }

   public Vec3 lerp(Vec3 param0, double param1) {
      return new Vec3(Mth.lerp(param1, this.x, param0.x), Mth.lerp(param1, this.y, param0.y), Mth.lerp(param1, this.z, param0.z));
   }

   public Vec3 xRot(float param0) {
      float var2 = Mth.cos(param0);
      float var3 = Mth.sin(param0);
      double var4 = this.x;
      double var6 = this.y * var2 + this.z * var3;
      double var8 = this.z * var2 - this.y * var3;
      return new Vec3(var4, var6, var8);
   }

   public Vec3 yRot(float param0) {
      float var2 = Mth.cos(param0);
      float var3 = Mth.sin(param0);
      double var4 = this.x * var2 + this.z * var3;
      double var6 = this.y;
      double var8 = this.z * var2 - this.x * var3;
      return new Vec3(var4, var6, var8);
   }

   public Vec3 zRot(float param0) {
      float var2 = Mth.cos(param0);
      float var3 = Mth.sin(param0);
      double var4 = this.x * var2 + this.y * var3;
      double var6 = this.y * var2 - this.x * var3;
      double var8 = this.z;
      return new Vec3(var4, var6, var8);
   }

   public static Vec3 directionFromRotation(Vec2 param0) {
      return directionFromRotation(param0.x, param0.y);
   }

   public static Vec3 directionFromRotation(float param0, float param1) {
      float var2 = Mth.cos(-param1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float var3 = Mth.sin(-param1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float var4 = -Mth.cos(-param0 * (float) (Math.PI / 180.0));
      float var5 = Mth.sin(-param0 * (float) (Math.PI / 180.0));
      return new Vec3(var3 * var4, var5, var2 * var4);
   }

   public Vec3 align(EnumSet<Direction.Axis> param0) {
      double var2 = param0.contains(Direction.Axis.X) ? Mth.floor(this.x) : this.x;
      double var4 = param0.contains(Direction.Axis.Y) ? Mth.floor(this.y) : this.y;
      double var6 = param0.contains(Direction.Axis.Z) ? Mth.floor(this.z) : this.z;
      return new Vec3(var2, var4, var6);
   }

   public double get(Direction.Axis param0) {
      return param0.choose(this.x, this.y, this.z);
   }

   public Vec3 with(Direction.Axis param0, double param1) {
      double var4 = param0 == Direction.Axis.X ? param1 : this.x;
      double var6 = param0 == Direction.Axis.Y ? param1 : this.y;
      double var8 = param0 == Direction.Axis.Z ? param1 : this.z;
      return new Vec3(var4, var6, var8);
   }

   public Vec3 relative(Direction param0, double param1) {
      Vec3i var4 = param0.getNormal();
      return new Vec3(this.x + param1 * var4.getX(), this.y + param1 * var4.getY(), this.z + param1 * var4.getZ());
   }

   @Override
   public final double x() {
      return this.x;
   }

   @Override
   public final double y() {
      return this.y;
   }

   @Override
   public final double z() {
      return this.z;
   }

   public Vector3f toVector3f() {
      return new Vector3f((float)this.x, (float)this.y, (float)this.z);
   }
}
