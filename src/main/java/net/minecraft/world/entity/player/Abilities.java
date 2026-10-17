package net.minecraft.world.entity.player;

import net.minecraft.nbt.CompoundTag;

public class Abilities {
   public boolean invulnerable;
   public boolean flying;
   public boolean mayfly;
   public boolean instabuild;
   public boolean mayBuild = true;
   private float flyingSpeed = 0.05F;
   private float walkingSpeed = 0.1F;

   public void addSaveData(CompoundTag param0) {
      CompoundTag var2 = new CompoundTag();
      var2.putBoolean("invulnerable", this.invulnerable);
      var2.putBoolean("flying", this.flying);
      var2.putBoolean("mayfly", this.mayfly);
      var2.putBoolean("instabuild", this.instabuild);
      var2.putBoolean("mayBuild", this.mayBuild);
      var2.putFloat("flySpeed", this.flyingSpeed);
      var2.putFloat("walkSpeed", this.walkingSpeed);
      param0.put("abilities", var2);
   }

   public void loadSaveData(CompoundTag param0) {
      if (param0.contains("abilities", 10)) {
         CompoundTag var2 = param0.getCompound("abilities");
         this.invulnerable = var2.getBoolean("invulnerable");
         this.flying = var2.getBoolean("flying");
         this.mayfly = var2.getBoolean("mayfly");
         this.instabuild = var2.getBoolean("instabuild");
         if (var2.contains("flySpeed", 99)) {
            this.flyingSpeed = var2.getFloat("flySpeed");
            this.walkingSpeed = var2.getFloat("walkSpeed");
         }

         if (var2.contains("mayBuild", 1)) {
            this.mayBuild = var2.getBoolean("mayBuild");
         }
      }
   }

   public float getFlyingSpeed() {
      return this.flyingSpeed;
   }

   public void setFlyingSpeed(float param0) {
      this.flyingSpeed = param0;
   }

   public float getWalkingSpeed() {
      return this.walkingSpeed;
   }

   public void setWalkingSpeed(float param0) {
      this.walkingSpeed = param0;
   }
}
