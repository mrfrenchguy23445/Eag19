package net.minecraft.world.level.levelgen.synth;

import java.util.Locale;

public class NoiseUtils {
   public static double biasTowardsExtreme(double param0, double param2) {
      return param0 + Math.sin(Math.PI * param0) * param2 / Math.PI;
   }

   public static void parityNoiseOctaveConfigString(StringBuilder param0, double param1, double param3, double param5, byte[] param7) {
      param0.append(
         String.format(Locale.ROOT, "xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)param1, (float)param3, (float)param5, param7[0], param7[255])
      );
   }

   public static void parityNoiseOctaveConfigString(StringBuilder param0, double param1, double param3, double param5, int[] param7) {
      param0.append(
         String.format(Locale.ROOT, "xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)param1, (float)param3, (float)param5, param7[0], param7[255])
      );
   }
}
