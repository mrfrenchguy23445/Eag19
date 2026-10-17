package net.minecraft.util;

public class SmoothDouble {
   private double targetValue;
   private double remainingValue;
   private double lastAmount;

   public double getNewDeltaValue(double param0, double param2) {
      this.targetValue += param0;
      double var5 = this.targetValue - this.remainingValue;
      double var7 = Mth.lerp(0.5, this.lastAmount, var5);
      double var9 = Math.signum(var5);
      if (var9 * var5 > var9 * this.lastAmount) {
         var5 = var7;
      }

      this.lastAmount = var7;
      this.remainingValue += var5 * param2;
      return var5 * param2;
   }

   public void reset() {
      this.targetValue = 0.0;
      this.remainingValue = 0.0;
      this.lastAmount = 0.0;
   }
}
