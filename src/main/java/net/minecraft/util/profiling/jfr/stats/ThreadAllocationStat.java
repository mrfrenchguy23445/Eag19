package net.minecraft.util.profiling.jfr.stats;

import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public record ThreadAllocationStat(Instant timestamp, String threadName, long totalBytes) {
   private static final String UNKNOWN_THREAD = "unknown";

   public static ThreadAllocationStat from(RecordedEvent param0) {
      RecordedThread var1 = param0.getThread("thread");
      String var2 = var1 == null ? "unknown" : MoreObjects.firstNonNull(var1.getJavaName(), "unknown");
      return new ThreadAllocationStat(param0.getStartTime(), var2, param0.getLong("allocated"));
   }

   public static ThreadAllocationStat.Summary summary(List<ThreadAllocationStat> param0) {
      Map<String, Double> var1 = new TreeMap<>();
      Map<String, List<ThreadAllocationStat>> var2 = param0.stream().collect(Collectors.groupingBy(l_param0 -> l_param0.threadName));
      var2.forEach((l_param1, l_param2) -> {
         if (l_param2.size() >= 2) {
            ThreadAllocationStat l_var3 = l_param2.get(0);
            ThreadAllocationStat l_var4 = l_param2.get(l_param2.size() - 1);
            long l_var5 = Duration.between(l_var3.timestamp, l_var4.timestamp).getSeconds();
            long l_var7 = l_var4.totalBytes - l_var3.totalBytes;
            var1.put(l_param1, (double)l_var7 / l_var5);
         }
      });
      return new ThreadAllocationStat.Summary(var1);
   }

   public record Summary(Map<String, Double> allocationsPerSecondByThread) {
   }
}
