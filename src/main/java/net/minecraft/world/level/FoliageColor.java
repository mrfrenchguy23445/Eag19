package net.minecraft.world.level;

public class FoliageColor {
   private static int[] pixels = new int[65536];

   public static void init(int[] param0) {
      pixels = param0;
   }

   public static int get(double param0, double param2) {
      param2 *= param0;
      int var4 = (int)((1.0 - param0) * 255.0);
      int var5 = (int)((1.0 - param2) * 255.0);
      int var6 = var5 << 8 | var4;
      return var6 >= pixels.length ? getDefaultColor() : pixels[var6];
   }

   public static int getEvergreenColor() {
      return 6396257;
   }

   public static int getBirchColor() {
      return 8431445;
   }

   public static int getDefaultColor() {
      return 4764952;
   }

   public static int getMangroveColor() {
      return 9619016;
   }
}
