package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.jfr.Percentiles;

public record TimedStatSummary<T extends TimedStat>(
   T fastest, T slowest, @Nullable T secondSlowest, int count, Map<Integer, Double> percentilesNanos, Duration totalDuration
) {
   public static <T extends TimedStat> TimedStatSummary<T> summary(List<T> param0) {
      if (param0.isEmpty()) {
         throw new IllegalArgumentException("No values");
      }

      List<T> var1 = param0.stream().sorted(Comparator.comparing(TimedStat::duration)).toList();
      Duration var2 = var1.stream().map(TimedStat::duration).reduce(Duration::plus).orElse(Duration.ZERO);
      T var3 = (T)var1.get(0);
      T var4 = (T)var1.get(var1.size() - 1);
      T var5 = var1.size() > 1 ? var1.get(var1.size() - 2) : null;
      int var6 = var1.size();
      Map<Integer, Double> var7 = Percentiles.evaluate(var1.stream().mapToLong(l_param0 -> l_param0.duration().toNanos()).toArray());
      return new TimedStatSummary<>(var3, var4, var5, var6, var7, var2);
   }
}
