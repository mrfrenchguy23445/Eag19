package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LookControl implements Control {
   protected final Mob mob;
   protected float yMaxRotSpeed;
   protected float xMaxRotAngle;
   protected int lookAtCooldown;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;

   public LookControl(Mob param0) {
      this.mob = param0;
   }

   public void setLookAt(Vec3 param0) {
      this.setLookAt(param0.x, param0.y, param0.z);
   }

   public void setLookAt(Entity param0) {
      this.setLookAt(param0.getX(), getWantedY(param0), param0.getZ());
   }

   public void setLookAt(Entity param0, float param1, float param2) {
      this.setLookAt(param0.getX(), getWantedY(param0), param0.getZ(), param1, param2);
   }

   public void setLookAt(double param0, double param2, double param4) {
      this.setLookAt(param0, param2, param4, this.mob.getHeadRotSpeed(), this.mob.getMaxHeadXRot());
   }

   public void setLookAt(double param0, double param2, double param4, float param6, float param7) {
      this.wantedX = param0;
      this.wantedY = param2;
      this.wantedZ = param4;
      this.yMaxRotSpeed = param6;
      this.xMaxRotAngle = param7;
      this.lookAtCooldown = 2;
   }

   public void tick() {
      if (this.resetXRotOnTick()) {
         this.mob.setXRot(0.0F);
      }

      if (this.lookAtCooldown > 0) {
         this.lookAtCooldown--;
         this.getYRotD().ifPresent(l_param0 -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, l_param0, this.yMaxRotSpeed));
         this.getXRotD().ifPresent(l_param0 -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), l_param0, this.xMaxRotAngle)));
      } else {
         this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
      }

      this.clampHeadRotationToBody();
   }

   protected void clampHeadRotationToBody() {
      if (!this.mob.getNavigation().isDone()) {
         this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
      }
   }

   protected boolean resetXRotOnTick() {
      return true;
   }

   public boolean isLookingAtTarget() {
      return this.lookAtCooldown > 0;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected Optional<Float> getXRotD() {
      double var1 = this.wantedX - this.mob.getX();
      double var3 = this.wantedY - this.mob.getEyeY();
      double var5 = this.wantedZ - this.mob.getZ();
      double var7 = Math.sqrt(var1 * var1 + var5 * var5);
      return !(Math.abs(var3) > 1.0E-5F) && !(Math.abs(var7) > 1.0E-5F)
         ? Optional.empty()
         : Optional.of((float)(-(Mth.atan2(var3, var7) * 180.0F / (float)Math.PI)));
   }

   protected Optional<Float> getYRotD() {
      double var1 = this.wantedX - this.mob.getX();
      double var3 = this.wantedZ - this.mob.getZ();
      return !(Math.abs(var3) > 1.0E-5F) && !(Math.abs(var1) > 1.0E-5F)
         ? Optional.empty()
         : Optional.of((float)(Mth.atan2(var3, var1) * 180.0F / (float)Math.PI) - 90.0F);
   }

   protected float rotateTowards(float param0, float param1, float param2) {
      float var4 = Mth.degreesDifference(param0, param1);
      float var5 = Mth.clamp(var4, -param2, param2);
      return param0 + var5;
   }

   private static double getWantedY(Entity param0) {
      return param0 instanceof LivingEntity ? param0.getEyeY() : (param0.getBoundingBox().minY + param0.getBoundingBox().maxY) / 2.0;
   }
}
