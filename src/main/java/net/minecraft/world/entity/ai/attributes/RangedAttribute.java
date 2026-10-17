package net.minecraft.world.entity.ai.attributes;

import net.minecraft.util.Mth;

public class RangedAttribute extends Attribute {
   private final double minValue;
   private final double maxValue;

   public RangedAttribute(String param0, double param1, double param3, double param5) {
      super(param0, param1);
      this.minValue = param3;
      this.maxValue = param5;
      if (param3 > param5) {
         throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
      }

      if (param1 < param3) {
         throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
      }

      if (param1 > param5) {
         throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
      }
   }

   public double getMinValue() {
      return this.minValue;
   }

   public double getMaxValue() {
      return this.maxValue;
   }

   @Override
   public double sanitizeValue(double param0) {
      return Double.isNaN(param0) ? this.minValue : Mth.clamp(param0, this.minValue, this.maxValue);
   }
}
