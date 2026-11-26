package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SmoothSwimmingMoveControl extends MoveControl {
   private static final float FULL_SPEED_TURN_THRESHOLD = 10.0F;
   private static final float STOP_TURN_THRESHOLD = 60.0F;
   private final int maxTurnX;
   private final int maxTurnY;
   private final float inWaterSpeedModifier;
   private final float outsideWaterSpeedModifier;
   private final boolean applyGravity;

   public SmoothSwimmingMoveControl(Mob param0, int param1, int param2, float param3, float param4, boolean param5) {
      super(param0);
      this.maxTurnX = param1;
      this.maxTurnY = param2;
      this.inWaterSpeedModifier = param3;
      this.outsideWaterSpeedModifier = param4;
      this.applyGravity = param5;
   }

   @Override
   public void tick() {
      if (this.applyGravity && this.mob.isInWater()) {
         this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
      }

      if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
         double var1 = this.wantedX - this.mob.getX();
         double var3 = this.wantedY - this.mob.getY();
         double var5 = this.wantedZ - this.mob.getZ();
         double var7 = var1 * var1 + var3 * var3 + var5 * var5;
         if (var7 < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
         } else {
            float var9 = (float)(Mth.atan2(var5, var1) * 180.0F / (float)Math.PI) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), var9, this.maxTurnY));
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
            float var10 = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (this.mob.isInWater()) {
               this.mob.setSpeed(var10 * this.inWaterSpeedModifier);
               double var11 = Math.sqrt(var1 * var1 + var5 * var5);
               if (Math.abs(var3) > 1.0E-5F || Math.abs(var11) > 1.0E-5F) {
                  float var13 = -((float)(Mth.atan2(var3, var11) * 180.0F / (float)Math.PI));
                  var13 = Mth.clamp(Mth.wrapDegrees(var13), -this.maxTurnX, this.maxTurnX);
                  this.mob.setXRot(this.rotlerp(this.mob.getXRot(), var13, 5.0F));
               }

               float var13 = Mth.cos(this.mob.getXRot() * (float) (Math.PI / 180.0));
               float var14 = Mth.sin(this.mob.getXRot() * (float) (Math.PI / 180.0));
               this.mob.zza = var13 * var10;
               this.mob.yya = -var14 * var10;
            } else {
               float var11 = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - var9));
               float var12 = getTurningSpeedFactor(var11);
               this.mob.setSpeed(var10 * this.outsideWaterSpeedModifier * var12);
            }
         }
      } else {
         this.mob.setSpeed(0.0F);
         this.mob.setXxa(0.0F);
         this.mob.setYya(0.0F);
         this.mob.setZza(0.0F);
      }
   }

   private static float getTurningSpeedFactor(float param0) {
      return 1.0F - Mth.clamp((param0 - 10.0F) / 50.0F, 0.0F, 1.0F);
   }
}
