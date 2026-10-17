package net.minecraft.util.profiling.metrics.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;

public class ProfilerSamplerAdapter {
   private final Set<String> previouslyFoundSamplerNames = new ObjectOpenHashSet<>();

   public Set<MetricSampler> newSamplersFoundInProfiler(Supplier<ProfileCollector> param0) {
      Set<MetricSampler> var2 = param0.get()
         .getChartedPaths()
         .stream()
         .filter(l_param0 -> !this.previouslyFoundSamplerNames.contains(l_param0.getLeft()))
         .map(l_param1 -> samplerForProfilingPath(param0, l_param1.getLeft(), l_param1.getRight()))
         .collect(Collectors.toSet());

      for (MetricSampler var4 : var2) {
         this.previouslyFoundSamplerNames.add(var4.getName());
      }

      return var2;
   }

   private static MetricSampler samplerForProfilingPath(Supplier<ProfileCollector> param0, String param1, MetricCategory param2) {
      return MetricSampler.create(param1, param2, () -> {
         ActiveProfiler.PathEntry l_var2 = param0.get().getEntry(param1);
         return l_var2 == null ? 0.0 : (double)l_var2.getMaxDuration() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      });
   }
}
