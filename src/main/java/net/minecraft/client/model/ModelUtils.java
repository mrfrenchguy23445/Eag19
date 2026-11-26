package net.minecraft.client.model;

public class ModelUtils {
   public static float rotlerpRad(float param0, float param1, float param2) {
      float var3 = param1 - param0;

      while (var3 < (float) -Math.PI) {
         var3 += (float) (Math.PI * 2);
      }

      while (var3 >= (float) Math.PI) {
         var3 -= (float) (Math.PI * 2);
      }

      return param0 + param2 * var3;
   }
}
