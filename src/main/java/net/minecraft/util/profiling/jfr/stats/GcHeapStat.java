package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public record GcHeapStat(Instant timestamp, long heapUsed, GcHeapStat.Timing timing) {
   public static GcHeapStat from(RecordedEvent param0) {
      return new GcHeapStat(
         param0.getStartTime(),
         param0.getLong("heapUsed"),
         param0.getString("when").equalsIgnoreCase("before gc") ? GcHeapStat.Timing.BEFORE_GC : GcHeapStat.Timing.AFTER_GC
      );
   }

   public static GcHeapStat.Summary summary(Duration param0, List<GcHeapStat> param1, Duration param2, int param3) {
      return new GcHeapStat.Summary(param0, param2, param3, calculateAllocationRatePerSecond(param1));
   }

   private static double calculateAllocationRatePerSecond(List<GcHeapStat> param0) {
      long var1 = 0L;
      Map<GcHeapStat.Timing, List<GcHeapStat>> var3 = param0.stream().collect(Collectors.groupingBy(l_param0 -> l_param0.timing));
      List<GcHeapStat> var4 = var3.get(GcHeapStat.Timing.BEFORE_GC);
      List<GcHeapStat> var5 = var3.get(GcHeapStat.Timing.AFTER_GC);

      for (int var6 = 1; var6 < var4.size(); var6++) {
         GcHeapStat var7 = var4.get(var6);
         GcHeapStat var8 = var5.get(var6 - 1);
         var1 += var7.heapUsed - var8.heapUsed;
      }

      Duration var6 = Duration.between(param0.get(1).timestamp, param0.get(param0.size() - 1).timestamp);
      return (double)var1 / var6.getSeconds();
   }

   public record Summary(Duration duration, Duration gcTotalDuration, int totalGCs, double allocationRateBytesPerSecond) {
      public float gcOverHead() {
         return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
      }
   }

   enum Timing {
      BEFORE_GC,
      AFTER_GC;
   }
}
