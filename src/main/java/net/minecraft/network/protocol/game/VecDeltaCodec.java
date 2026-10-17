package net.minecraft.network.protocol.game;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.VisibleForTesting;

public class VecDeltaCodec {
   private static final double TRUNCATION_STEPS = 4096.0;
   private Vec3 base = Vec3.ZERO;

   @VisibleForTesting
   static long encode(double param0) {
      return Math.round(param0 * 4096.0);
   }

   @VisibleForTesting
   static double decode(long param0) {
      return param0 / 4096.0;
   }

   public Vec3 decode(long param0, long param2, long param4) {
      if (param0 == 0L && param2 == 0L && param4 == 0L) {
         return this.base;
      }

      double var7 = param0 == 0L ? this.base.x : decode(encode(this.base.x) + param0);
      double var9 = param2 == 0L ? this.base.y : decode(encode(this.base.y) + param2);
      double var11 = param4 == 0L ? this.base.z : decode(encode(this.base.z) + param4);
      return new Vec3(var7, var9, var11);
   }

   public long encodeX(Vec3 param0) {
      return encode(param0.x) - encode(this.base.x);
   }

   public long encodeY(Vec3 param0) {
      return encode(param0.y) - encode(this.base.y);
   }

   public long encodeZ(Vec3 param0) {
      return encode(param0.z) - encode(this.base.z);
   }

   public Vec3 delta(Vec3 param0) {
      return param0.subtract(this.base);
   }

   public void setBase(Vec3 param0) {
      this.base = param0;
   }
}
