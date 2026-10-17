package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
   public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling");
   public static final String METRICS_DIR_NAME = "metrics";
   public static final String DEVIATIONS_DIR_NAME = "deviations";
   public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String rootFolderName;

   public MetricsPersister(String param0) {
      this.rootFolderName = param0;
   }

   public Path saveReports(Set<MetricSampler> param0, Map<MetricSampler, List<RecordedDeviation>> param1, ProfileResults param2) {
      try {
         Files.createDirectories(PROFILING_RESULTS_DIR);
      } catch (IOException var4) {
         throw new UncheckedIOException(var4);
      }

      try {
         Path var4 = Files.createTempDirectory("minecraft-profiling");
         var4.toFile().deleteOnExit();
         Files.createDirectories(PROFILING_RESULTS_DIR);
         Path var5 = var4.resolve(this.rootFolderName);
         Path var6 = var5.resolve("metrics");
         this.saveMetrics(param0, var6);
         if (!param1.isEmpty()) {
            this.saveDeviations(param1, var5.resolve("deviations"));
         }

         this.saveProfilingTaskExecutionResult(param2, var5);
         return var4;
      } catch (IOException var4) {
         throw new UncheckedIOException(var4);
      }
   }

   private void saveMetrics(Set<MetricSampler> param0, Path param1) {
      if (param0.isEmpty()) {
         throw new IllegalArgumentException("Expected at least one sampler to persist");
      }

      Map<MetricCategory, List<MetricSampler>> var3 = param0.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
      var3.forEach((l_param1, l_param2) -> this.saveCategory(l_param1, (List<MetricSampler>)l_param2, param1));
   }

   private void saveCategory(MetricCategory param0, List<MetricSampler> param1, Path param2) {
      Path var4 = param2.resolve(Util.sanitizeName(param0.getDescription(), ResourceLocation::validPathChar) + ".csv");
      Writer var5 = null;

      try {
         Files.createDirectories(var4.getParent());
         var5 = Files.newBufferedWriter(var4, StandardCharsets.UTF_8);
         CsvOutput.Builder var6 = CsvOutput.builder();
         var6.addColumn("@tick");

         for (MetricSampler var8 : param1) {
            var6.addColumn(var8.getName());
         }

         CsvOutput var7 = var6.build(var5);
         List<MetricSampler.SamplerResult> var8 = param1.stream().map(MetricSampler::result).collect(Collectors.toList());
         int var9 = var8.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
         int var10 = var8.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();

         for (int var11 = var9; var11 <= var10; var11++) {
            int var12 = var11;
            Stream<String> var13 = var8.stream().map(l_param1 -> String.valueOf(l_param1.valueAtTick(var12)));
            Object[] var14 = Stream.concat(Stream.of(String.valueOf(var11)), var13).toArray(String[]::new);
            var7.writeRow(var14);
         }

         LOGGER.info("Flushed metrics to {}", var4);
      } catch (Exception var6) {
         LOGGER.error("Could not save profiler results to {}", var4, var6);
      } finally {
         IOUtils.closeQuietly(var5);
      }
   }

   private void saveDeviations(Map<MetricSampler, List<RecordedDeviation>> param0, Path param1) {
      DateTimeFormatter var3 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
      param0.forEach(
         (l_param2, l_param3) -> l_param3.forEach(
            l2_param3 -> {
               String l2_var4 = var3.format(l2_param3.timestamp);
               Path l2_var5 = param1.resolve(Util.sanitizeName(l_param2.getName(), ResourceLocation::validPathChar))
                  .resolve(String.format(Locale.ROOT, "%d@%s.txt", l2_param3.tick, l2_var4));
               l2_param3.profilerResultAtTick.saveResults(l2_var5);
            }
         )
      );
   }

   private void saveProfilingTaskExecutionResult(ProfileResults param0, Path param1) {
      param0.saveResults(param1.resolve("profiling.txt"));
   }
}
