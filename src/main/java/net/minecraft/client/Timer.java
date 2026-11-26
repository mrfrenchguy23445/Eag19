package net.minecraft.client;

public class Timer {
   public float partialTick;
   public float tickDelta;
   private long lastMs;
   private final float msPerTick;

   public Timer(float param0, long param1) {
      this.msPerTick = 1000.0F / param0;
      this.lastMs = param1;
   }

   public int advanceTime(long param0) {
      this.tickDelta = (float)(param0 - this.lastMs) / this.msPerTick;
      this.lastMs = param0;
      this.partialTick = this.partialTick + this.tickDelta;
      int var3 = (int)this.partialTick;
      this.partialTick -= var3;
      return var3;
   }
}
