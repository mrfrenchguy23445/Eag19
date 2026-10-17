package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public abstract class HitResult {
   protected final Vec3 location;

   protected HitResult(Vec3 param0) {
      this.location = param0;
   }

   public double distanceTo(Entity param0) {
      double var2 = this.location.x - param0.getX();
      double var4 = this.location.y - param0.getY();
      double var6 = this.location.z - param0.getZ();
      return var2 * var2 + var4 * var4 + var6 * var6;
   }

   public abstract HitResult.Type getType();

   public Vec3 getLocation() {
      return this.location;
   }

   public enum Type {
      MISS,
      BLOCK,
      ENTITY;
   }
}
