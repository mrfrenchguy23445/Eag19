package net.minecraft.client.resources.metadata.animation;

public class AnimationFrame {
   public static final int UNKNOWN_FRAME_TIME = -1;
   private final int index;
   private final int time;

   public AnimationFrame(int param0) {
      this(param0, -1);
   }

   public AnimationFrame(int param0, int param1) {
      this.index = param0;
      this.time = param1;
   }

   public int getTime(int param0) {
      return this.time == -1 ? param0 : this.time;
   }

   public int getIndex() {
      return this.index;
   }
}
