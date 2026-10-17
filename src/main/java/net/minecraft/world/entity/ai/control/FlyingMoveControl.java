package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FlyingMoveControl extends MoveControl {
   private final int maxTurn;
   private final boolean hoversInPlace;

   public FlyingMoveControl(Mob param0, int param1, boolean param2) {
      super(param0);
      this.maxTurn = param1;
      this.hoversInPlace = param2;
   }

   @Override
   public void tick() {
      if (this.operation == MoveControl.Operation.MOVE_TO) {
         this.operation = MoveControl.Operation.WAIT;
         this.mob.setNoGravity(true);
         double var1 = this.wantedX - this.mob.getX();
         double var3 = this.wantedY - this.mob.getY();
         double var5 = this.wantedZ - this.mob.getZ();
         double var7 = var1 * var1 + var3 * var3 + var5 * var5;
         if (var7 < 2.5000003E-7F) {
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
            return;
         }

         float var9 = (float)(Mth.atan2(var5, var1) * 180.0F / (float)Math.PI) - 90.0F;
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), var9, 90.0F));
         float var10;
         if (this.mob.isOnGround()) {
            var10 = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
         } else {
            var10 = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED));
         }

         this.mob.setSpeed(var10);
         double var11 = Math.sqrt(var1 * var1 + var5 * var5);
         if (Math.abs(var3) > 1.0E-5F || Math.abs(var11) > 1.0E-5F) {
            float var13 = (float)(-(Mth.atan2(var3, var11) * 180.0F / (float)Math.PI));
            this.mob.setXRot(this.rotlerp(this.mob.getXRot(), var13, this.maxTurn));
            this.mob.setYya(var3 > 0.0 ? var10 : -var10);
         }
      } else {
         if (!this.hoversInPlace) {
            this.mob.setNoGravity(false);
         }

         this.mob.setYya(0.0F);
         this.mob.setZza(0.0F);
      }
   }
}
