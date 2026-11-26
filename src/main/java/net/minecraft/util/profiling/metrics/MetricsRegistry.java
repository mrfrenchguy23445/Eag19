package net.minecraft.util.profiling.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class MetricsRegistry {
   public static final MetricsRegistry INSTANCE = new MetricsRegistry();
   private final WeakHashMap<ProfilerMeasured, Void> measuredInstances = new WeakHashMap<>();

   private MetricsRegistry() {
   }

   public void add(ProfilerMeasured param0) {
      this.measuredInstances.put(param0, null);
   }

   public List<MetricSampler> getRegisteredSamplers() {
      Map<String, List<MetricSampler>> var1 = this.measuredInstances
         .keySet()
         .stream()
         .flatMap(l_param0 -> l_param0.profiledMetrics().stream())
         .collect(Collectors.groupingBy(MetricSampler::getName));
      return aggregateDuplicates(var1);
   }

   private static List<MetricSampler> aggregateDuplicates(Map<String, List<MetricSampler>> param0) {
      return param0.entrySet().stream().map(l_param0 -> {
         String l_var1 = l_param0.getKey();
         List<MetricSampler> l_var2 = l_param0.getValue();
         return l_var2.size() > 1 ? new MetricsRegistry.AggregatedMetricSampler(l_var1, l_var2) : l_var2.get(0);
      }).collect(Collectors.toList());
   }

   static class AggregatedMetricSampler extends MetricSampler {
      private final List<MetricSampler> delegates;

      AggregatedMetricSampler(String param0, List<MetricSampler> param1) {
         super(param0, param1.get(0).getCategory(), () -> averageValueFromDelegates(param1), () -> beforeTick(param1), thresholdTest(param1));
         this.delegates = param1;
      }

      private static MetricSampler.ThresholdTest thresholdTest(List<MetricSampler> param0) {
         return l_param1 -> param0.stream().anyMatch(l2_param2 -> l2_param2.thresholdTest != null ? l2_param2.thresholdTest.test(l_param1) : false);
      }

      private static void beforeTick(List<MetricSampler> param0) {
         for (MetricSampler var2 : param0) {
            var2.onStartTick();
         }
      }

      private static double averageValueFromDelegates(List<MetricSampler> param0) {
         double var1 = 0.0;

         for (MetricSampler var4 : param0) {
            var1 += var4.getSampler().getAsDouble();
         }

         return var1 / param0.size();
      }

      @Override
      public boolean equals(@Nullable Object param0) {
         if (this == param0) {
            return true;
         }

         if (param0 == null || this.getClass() != param0.getClass()) {
            return false;
         }

         if (!super.equals(param0)) {
            return false;
         }

         MetricsRegistry.AggregatedMetricSampler var2 = (MetricsRegistry.AggregatedMetricSampler)param0;
         return this.delegates.equals(var2.delegates);
      }

      @Override
      public int hashCode() {
         return Objects.hash(super.hashCode(), this.delegates);
      }
   }
}
