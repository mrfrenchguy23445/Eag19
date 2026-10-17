package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
   private final SampledFloat[] values;

   public MultipliedFloats(SampledFloat... param0) {
      this.values = param0;
   }

   @Override
   public float sample(RandomSource param0) {
      float var2 = 1.0F;

      for (int var3 = 0; var3 < this.values.length; var3++) {
         var2 *= this.values[var3].sample(param0);
      }

      return var2;
   }

   @Override
   public String toString() {
      return "MultipliedFloats" + Arrays.toString(this.values);
   }
}
