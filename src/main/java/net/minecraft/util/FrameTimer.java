package net.minecraft.util;

public class FrameTimer {
   public static final int LOGGING_LENGTH = 240;
   private final long[] loggedTimes = new long[240];
   private int logStart;
   private int logLength;
   private int logEnd;

   public void logFrameDuration(long param0) {
      this.loggedTimes[this.logEnd] = param0;
      this.logEnd++;
      if (this.logEnd == 240) {
         this.logEnd = 0;
      }

      if (this.logLength < 240) {
         this.logStart = 0;
         this.logLength++;
      } else {
         this.logStart = this.wrapIndex(this.logEnd + 1);
      }
   }

   public long getAverageDuration(int param0) {
      int var2 = (this.logStart + param0) % 240;
      int var3 = this.logStart;
      long var4 = 0L;

      while (var3 != var2) {
         var4 += this.loggedTimes[var3];
         var3++;
      }

      return var4 / param0;
   }

   public int scaleAverageDurationTo(int param0, int param1) {
      return this.scaleSampleTo(this.getAverageDuration(param0), param1, 60);
   }

   public int scaleSampleTo(long param0, int param2, int param3) {
      double var5 = (double)param0 / (1000000000L / param3);
      return (int)(var5 * param2);
   }

   public int getLogStart() {
      return this.logStart;
   }

   public int getLogEnd() {
      return this.logEnd;
   }

   public int wrapIndex(int param0) {
      return param0 % 240;
   }

   public long[] getLog() {
      return this.loggedTimes;
   }
}
