package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

public class SmoothSwimmingLookControl extends LookControl {
   private final int maxYRotFromCenter;
   private static final int HEAD_TILT_X = 10;
   private static final int HEAD_TILT_Y = 20;

   public SmoothSwimmingLookControl(Mob param0, int param1) {
      super(param0);
      this.maxYRotFromCenter = param1;
   }

   @Override
   public void tick() {
      if (this.lookAtCooldown > 0) {
         this.lookAtCooldown--;
         this.getYRotD().ifPresent(l_param0 -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, l_param0 + 20.0F, this.yMaxRotSpeed));
         this.getXRotD().ifPresent(l_param0 -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), l_param0 + 10.0F, this.xMaxRotAngle)));
      } else {
         if (this.mob.getNavigation().isDone()) {
            this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), 0.0F, 5.0F));
         }

         this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, this.yMaxRotSpeed);
      }

      float var1 = Mth.wrapDegrees(this.mob.yHeadRot - this.mob.yBodyRot);
      if (var1 < -this.maxYRotFromCenter) {
         this.mob.yBodyRot -= 4.0F;
      } else if (var1 > this.maxYRotFromCenter) {
         this.mob.yBodyRot += 4.0F;
      }
   }
}
