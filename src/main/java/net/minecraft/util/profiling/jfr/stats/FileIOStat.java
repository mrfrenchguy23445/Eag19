package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public record FileIOStat(Duration duration, @Nullable String path, long bytes) {
   public static FileIOStat.Summary summary(Duration param0, List<FileIOStat> param1) {
      long var2 = param1.stream().mapToLong(l_param0 -> l_param0.bytes).sum();
      return new FileIOStat.Summary(
         var2,
         (double)var2 / param0.getSeconds(),
         param1.size(),
         (double)param1.size() / param0.getSeconds(),
         param1.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus),
         param1.stream()
            .filter(l_param0 -> l_param0.path != null)
            .collect(Collectors.groupingBy(l_param0 -> l_param0.path, Collectors.summingLong(l_param0 -> l_param0.bytes)))
            .entrySet()
            .stream()
            .sorted(Entry.<String, Long>comparingByValue().reversed())
            .map(l_param0 -> Pair.of(l_param0.getKey(), l_param0.getValue()))
            .limit(10L)
            .toList()
      );
   }

   public record Summary(
      long totalBytes,
      double bytesPerSecond,
      long counts,
      double countsPerSecond,
      Duration timeSpentInIO,
      List<Pair<String, Long>> topTenContributorsByTotalBytes
   ) {
   }
}
