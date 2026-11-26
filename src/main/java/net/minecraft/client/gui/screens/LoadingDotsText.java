package net.minecraft.client.gui.screens;

public class LoadingDotsText {
   private static final String[] FRAMES = new String[]{"O o o", "o O o", "o o O", "o O o"};
   private static final long INTERVAL_MS = 300L;

   public static String get(long param0) {
      int var2 = (int)(param0 / 300L % FRAMES.length);
      return FRAMES[var2];
   }
}
