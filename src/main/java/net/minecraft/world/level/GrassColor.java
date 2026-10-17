package net.minecraft.world.level;

public class GrassColor {
   private static int[] pixels = new int[65536];

   public static void init(int[] param0) {
      pixels = param0;
   }

   public static int get(double param0, double param2) {
      param2 *= param0;
      int var4 = (int)((1.0 - param0) * 255.0);
      int var5 = (int)((1.0 - param2) * 255.0);
      int var6 = var5 << 8 | var4;
      return var6 >= pixels.length ? -65281 : pixels[var6];
   }

   public static int getDefaultColor() {
      return get(0.5, 1.0);
   }
}
