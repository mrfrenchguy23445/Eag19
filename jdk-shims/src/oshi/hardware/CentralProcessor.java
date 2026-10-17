package oshi.hardware;
public class CentralProcessor {

   private static final int LOGICAL_PROCESSOR_COUNT = 1;

   public int getLogicalProcessorCount() {
      return LOGICAL_PROCESSOR_COUNT;
   }
   public long[][] getProcessorCpuLoadTicks() {
      return new long[][]{new long[CentralProcessor.TICK_FIELDS]};
   }
   public double[] getProcessorCpuLoadBetweenTicks(long[][] param0) {
      double[] var1 = new double[CentralProcessor.LOGICAL_PROCESSOR_COUNT];
      int var2 = var1.length;

      while (--var2 >= 0) {
         var1[var2] = 0.0D;
      }

      return var1;
   }
   private static final int TICK_FIELDS = 10;
}
